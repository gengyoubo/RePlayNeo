package github.com.gengyoubo.replayneo.platform.feature.replay;

import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.mojang.authlib.GameProfile;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.platform.compat.ChangedReplayCompat;
import github.com.gengyoubo.replayneo.platform.network.Restrictions;
import github.com.gengyoubo.replayneo.platform.camera.CameraEntity;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.replay.ReplayFile;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.PreTickCallback;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;


import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;

import static github.com.gengyoubo.replayneo.core.utils.Utils.DEFAULT_MS_PER_TICK;
import static github.com.gengyoubo.replayneo.platform.versions.MCVer.*;
import static com.replaymod.replaystudio.util.Utils.readInt;
import static github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay.LOGGER;

/**
 * Sends replay packets to netty channels.
 * Even though {@link Sharable}, this should never be added to multiple pipes at once, it may however be re-added when
 * the replay restart from the beginning.
 */
@Sharable
public class FullReplaySender extends ChannelInboundHandlerAdapter implements ReplaySender {
    /**
     * These packets are ignored completely during replay.
     */
    private static final List<Class> BAD_PACKETS = Arrays.asList(
            ClientboundHelloPacket.class, // workaround for an issue where RePlayCore prior to 2.6.20 would record these
            ClientboundBlockChangedAckPacket.class,
            ClientboundOpenBookPacket.class,
            ClientboundOpenScreenPacket.class,
            ClientboundUpdateRecipesPacket.class,
            ClientboundUpdateAdvancementsPacket.class,
            ClientboundSelectAdvancementsTabPacket.class,
            ClientboundSetCameraPacket.class,
            ClientboundSetTitleTextPacket.class,
            ClientboundSetHealthPacket.class,
            ClientboundHorseScreenOpenPacket.class,
            ClientboundContainerClosePacket.class,
            ClientboundContainerSetSlotPacket.class,
            ClientboundContainerSetDataPacket.class,
            ClientboundOpenSignEditorPacket.class,
            ClientboundAwardStatsPacket.class,
            ClientboundSetExperiencePacket.class,
            ClientboundPlayerAbilitiesPacket.class
    );

    private static final int TP_DISTANCE_LIMIT = 128;

    /**
     * The replay handler responsible for the current replay.
     */
    private final ReplayHandler replayHandler;

    /**
     * Whether to work in async mode.
     * <p>
     * When in async mode, a separate thread send packets and waits according to their delays.
     * This is default in normal playback mode.
     * <p>
     * When in sync mode, no packets will be sent until {@link #sendPacketsTill(int)} is called.
     * This is used during path playback and video rendering.
     */
    protected boolean asyncMode;

    /**
     * Timestamp of the last packet sent in milliseconds since the start.
     */
    protected int lastTimeStamp;

    /**
     * @see #currentTimeStamp()
     */
    protected int currentTimeStamp;

    /**
     * The replay file.
     */
    protected final ReplayFile replayFile;

    /**
     * The channel used to send packets to minecraft.
     */
    protected Channel channel;

    /**
     * The replay input stream from which new packets are read.
     * When accessing this stream make sure to synchronize on {@code this} as it's used from multiple threads.
     */
    protected ReplayInputStream replayIn;

    /**
     * The next packet that should be sent.
     * This is required as some actions such as jumping to a specified timestamp have to peek at the next packet.
     */
    protected PacketData nextPacket;

    /**
     * Which protocol (state) we're currently in.
     */
    private PacketTypeRegistry registry = getPacketTypeRegistry(State.LOGIN);

    /**
     * Whether we need to restart the current replay. E.g. when jumping backwards in time
     */
    protected boolean startFromBeginning = true;

    /**
     * Whether to terminate the replay. This only has an effect on the async mode and is {@code true} during sync mode.
     */
    protected boolean terminate;

    /**
     * The speed of the replay. 1 is normal, 2 is twice as fast, 0.5 is half speed and 0 is frozen
     */
    protected double replaySpeed = 1f;

    /**
     * Whether the world has been loaded and the dirt-screen should go away.
     */
    protected boolean hasWorldLoaded;

    /**
     * Whether we are currently in the middle of a bundle packet.
     */
    protected boolean inBundle;
    private boolean replayneo$loggedFirstMovePacket;
    private int replayneo$missingMoveEntityWarnings;
    private int replayneo$controlledMoveEntityWarnings;
    private int replayneo$addPlayerWarnings;
    private int replayneo$lastReplayPlayerEntityId = -1;
    private int replayneo$loggedReplayPlayerMovePackets;
    private int replayneo$droppedMalformedBundlePackets;

    /**
     * The minecraft instance.
     */
    protected final Minecraft mc = getMinecraft();

    /**
     * The total length of this replay in milliseconds.
     */
    protected final int replayLength;

    /**
     * Our actual entity id that the server gave to us.
     */
    protected int actualID = -1;

    /**
     * Whether to allow (process) the next player movement packet.
     * <p>
     * Must only be accessed from the main thread.
     */
    protected boolean allowMovement;

    /**
     * Directory to which resource packs are extracted.
     */
    private final File tempResourcePackFolder = Files.createTempDir();

    private final EventHandler events = new EventHandler();

    /**
     * Create a new replay sender.
     * @param file The replay file
     */
    public FullReplaySender(ReplayHandler replayHandler, ReplayFile file) throws IOException {
        this.replayHandler = replayHandler;
        this.replayFile = file;
        this.replayLength = file.getMetaData().getDuration();

        events.register();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Set whether this replay sender operates in async mode.
     * When in async mode, it will send packets timed from a separate thread.
     * When not in async mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * @param asyncMode {@code true} to enable async mode
     */
    @Override
    public void setAsyncMode(boolean asyncMode) {
        if (this.asyncMode == asyncMode) return;
        this.asyncMode = asyncMode;
        if (asyncMode) {
            synchronized (this) {
                this.terminate = false;
                notifyAll();
            }
            new Thread(asyncSender, "RePlayCore-async-sender").start();
        } else {
            synchronized (this) {
                this.terminate = true;
                notifyAll();
            }
        }
    }

    @Override
    public boolean isAsyncMode() {
        return asyncMode;
    }

    /**
     * Set whether this replay sender  to operate in sync mode.
     * When in sync mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * This call will block until the async worker thread has stopped.
     */
    @Override
    public void setSyncModeAndWait() {
        if (!this.asyncMode) return;
        this.asyncMode = false;
        synchronized (this) {
            this.terminate = true;
            notifyAll();
        }
        // This will wait for the worker thread to leave the synchronized code part
    }

    /**
     * Return a fake system tile in milliseconds value that respects slowdown/speedup/pause and works in both,
     * sync and async mode.
     * Note: For sync mode this returns the last value passed to {@link #sendPacketsTill(int)}.
     * @return The timestamp in milliseconds since the start of the replay
     */
    @Override
    public int currentTimeStamp() {
        if (asyncMode && !paused()) {
            return (int) ((System.currentTimeMillis() - realTimeStart) * realTimeStartSpeed);
        } else {
            return lastTimeStamp;
        }
    }

    /**
     * Terminate this replay sender.
     */
    public void terminateReplay() {
        if (terminate) {
            return;
        }
        synchronized (this) {
            terminate = true;
            notifyAll();
        }
        syncSender.shutdown();
        events.unregister();
        try {
            channel.pipeline().fireChannelInactive();
            channel.pipeline().close();
            FileUtils.deleteDirectory(tempResourcePackFolder);
        } catch(Exception e) {
            LOGGER.warn("Failed to clean up replay sender.", e);
        }
    }

    private static class EventHandler extends EventRegistrations {
        { on(PreTickCallback.EVENT, this::onWorldTick); }

        private void onWorldTick() {
            LOGGER.debug("World tick event received");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // When in async mode and the replay sender shut down, then don't send packets
        if(terminate && asyncMode) {
            return;
        }

        if (msg instanceof Packet p) {
            try {

                p = processPacket(p);
                if (p != null) {
                    replayneo$ensurePlayerInfoBeforeAddPlayer(ctx, p);
                    replayneo$logReplayAddPlayerPacket(p);
                    replayneo$logReplayMovementPacket(p);
                    super.channelRead(ctx, p);
                }

                maybeRemoveDeadEntities(p);

                if (p instanceof ClientboundLevelChunkWithLightPacket) {
                    Runnable doLightUpdates = () -> {
                        ClientLevel world = mc.level;
                        if (world != null) {
                            MutableBoolean done = new MutableBoolean();
                            world.queueLightUpdate(done::setTrue);
                            while (!done.booleanValue()) {
                                world.pollLightUpdates();
                            }
                            LevelLightEngine provider = world.getChunkSource().getLightEngine();
                            while (provider.hasLightWork()) {
                                provider.runLightUpdates();
                            }
                        }
                    };
                    if (mc.isSameThread()) {
                        doLightUpdates.run();
                    } else {
                        mc.tell(doLightUpdates);
                    }
                }
            } catch (Exception e) {
                // We'd rather not have a failure parsing one packet screw up the whole replay process
                LOGGER.warn("Failed to process replay packet; skipping packet.", e);
            }
        }

    }

    private void replayneo$logReplayMovementPacket(Packet<?> packet) {
        ClientLevel world = mc.level;
        if (world == null) {
            return;
        }

        Integer entityId = null;
        String packetType;
        Entity entity;
        if (packet instanceof ClientboundMoveEntityPacket movePacket) {
            packetType = "MoveEntity";
            entityId = movePacket.entityId;
            entity = movePacket.getEntity(world);
        } else if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
            entityId = teleportPacket.getId();
            packetType = "TeleportEntity";
            entity = world.getEntity(entityId);
        } else {
            return;
        }

        if (entity == null) {
            if (entityId != null && entityId == replayneo$lastReplayPlayerEntityId && replayneo$loggedReplayPlayerMovePackets++ < 16) {
                LOGGER.warn("Replay player movement packet has no target entity. type={}, entityId={}, time={}",
                        packetType, entityId, currentTimeStamp());
            }
            if (replayneo$missingMoveEntityWarnings++ < 8) {
                LOGGER.warn("Replay movement packet has no target entity. type={}, entityId={}, time={}",
                        packetType, entityId, currentTimeStamp());
            }
            return;
        }

        if (entityId == null) {
            entityId = entity.getId();
        }

        if (entityId == replayneo$lastReplayPlayerEntityId && replayneo$loggedReplayPlayerMovePackets++ < 16) {
            LOGGER.warn("Replay player movement packet. type={}, entityId={}, entityClass={}, controlled={}, pos=({}, {}, {}), time={}",
                    packetType, entityId, entity.getClass().getName(), entity.isControlledByLocalInstance(),
                    entity.getX(), entity.getY(), entity.getZ(), currentTimeStamp());
        }

        if (entity.isControlledByLocalInstance()) {
            if (replayneo$controlledMoveEntityWarnings++ < 8) {
                LOGGER.warn("Replay movement packet targets a locally controlled entity and vanilla will ignore it. type={}, entityId={}, entityClass={}, time={}",
                        packetType, entityId, entity.getClass().getName(), currentTimeStamp());
            }
            return;
        }

        if (!replayneo$loggedFirstMovePacket) {
            replayneo$loggedFirstMovePacket = true;
            LOGGER.info("Replay movement packets are reaching entity. type={}, entityId={}, entityClass={}, pos=({}, {}, {}), time={}",
                    packetType, entityId, entity.getClass().getName(), entity.getX(), entity.getY(), entity.getZ(), currentTimeStamp());
        }
    }

    private void replayneo$ensurePlayerInfoBeforeAddPlayer(ChannelHandlerContext ctx, Packet<?> packet) throws Exception {
        if (!(packet instanceof ClientboundAddPlayerPacket addPlayerPacket)) {
            return;
        }

        ClientPacketListener connection = mc.getConnection();
        if (connection == null || connection.getPlayerInfo(addPlayerPacket.getPlayerId()) != null) {
            return;
        }

        ClientboundPlayerInfoUpdatePacket infoPacket = replayneo$createReplayPlayerInfo(addPlayerPacket.getPlayerId());
        LOGGER.warn("Replay AddPlayer had no PlayerInfo; injecting fallback PlayerInfo. entityId={}, uuid={}, time={}",
                addPlayerPacket.getEntityId(), addPlayerPacket.getPlayerId(), currentTimeStamp());
        super.channelRead(ctx, infoPacket);
    }

    private ClientboundPlayerInfoUpdatePacket replayneo$createReplayPlayerInfo(UUID uuid) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                    ClientboundPlayerInfoUpdatePacket.Action.class);
            GameProfile profile = new GameProfile(uuid, "Player");
            buf.writeCollection(Collections.singletonList(profile), (out, entry) -> {
                out.writeUUID(entry.getId());
                out.writeUtf(entry.getName(), 16);
                out.writeGameProfileProperties(entry.getProperties());
            });
            return new ClientboundPlayerInfoUpdatePacket(buf);
        } finally {
            buf.release();
        }
    }

    private void replayneo$logReplayAddPlayerPacket(Packet<?> packet) {
        if (!(packet instanceof ClientboundAddPlayerPacket addPlayerPacket)) {
            return;
        }
        replayneo$lastReplayPlayerEntityId = addPlayerPacket.getEntityId();
        replayneo$loggedReplayPlayerMovePackets = 0;
        ClientLevel world = mc.level;
        Entity existing = world == null ? null : world.getEntity(addPlayerPacket.getEntityId());
        ClientPacketListener connection = mc.getConnection();
        boolean hasPlayerInfo = connection != null && connection.getPlayerInfo(addPlayerPacket.getPlayerId()) != null;
        if (replayneo$addPlayerWarnings++ < 8) {
            LOGGER.warn("Replay AddPlayer packet. entityId={}, uuid={}, hasPlayerInfo={}, existingEntity={}, time={}",
                    addPlayerPacket.getEntityId(), addPlayerPacket.getPlayerId(), hasPlayerInfo,
                    existing == null ? "null" : existing.getClass().getName(), currentTimeStamp());
        }
    }

    // If we do not give minecraft time to tick, there will be dead entity artifacts left in the world
    // Therefore we have to remove all loaded, dead entities manually if we are in sync mode.
    // We do this after every SpawnX packet and after the destroy entities packet.
    private void maybeRemoveDeadEntities(Packet packet) {
        if (asyncMode) {
            return; // MC should have enough time to tick
        }

        boolean relevantPacket = packet instanceof ClientboundAddEntityPacket
                || packet instanceof ClientboundAddPlayerPacket
                || packet instanceof ClientboundAddExperienceOrbPacket
                || packet instanceof ClientboundRemoveEntitiesPacket;
        if (!relevantPacket) {
            return; // don't want to do it too often, only when there's likely to be a dead entity
        }

        mc.tell(() -> {
            ClientLevel world = mc.level;
            if (world != null) {
                removeDeadEntities(world);
            }
        });
    }

    private void removeDeadEntities(ClientLevel world) {
        for (Entity entity : world.entitiesForRendering()) {
            if (entity.isRemoved()) {
                if (entity.getRemovalReason() != null) {
                    world.removeEntity(entity.getId(), entity.getRemovalReason());
                }
            }
        }
    }

    /**
     * Process a packet and return the result.
     * @param p The packet to process
     * @return The processed packet or {@code null} if no packet shall be sent
     */
    protected Packet processPacket(Packet p) throws Exception {
        if (p instanceof ClientboundGameProfilePacket) {
            registry = registry.withLoginSuccess();
            return p;
        }

        if (p instanceof ClientboundCustomPayloadPacket packet) {
            if (ChangedReplayCompat.TRANSFUR_SYNC_PAYLOAD.equals(packet.getIdentifier())) {
                FriendlyByteBuf data = new FriendlyByteBuf(packet.getData().copy());
                try {
                    ChangedReplayCompat.applyTransfurPayload(data, mc.level);
                } finally {
                    data.release();
                }
                return null;
            }
            if (Restrictions.PLUGIN_CHANNEL.equals(packet.getIdentifier())) {
                final String unknown = replayHandler.getRestrictions().handle(packet);
                if (unknown == null) {
                    return null;
                } else {
                    // Failed to parse options, make sure that under no circumstances further packets are parsed
                    terminateReplay();
                    // Then end replay and show error GUI
                    RePlayCore.instance.runLater(() -> {
                        try {
                            replayHandler.endReplay();
                        } catch (IOException e) {
                            LOGGER.error("Failed to close replay after unknown replay restriction.", e);
                        }
                        mc.setScreen(new AlertScreen(
                                () -> mc.setScreen(null),
                                Component.translatable("replaymod.error.unknownrestriction1"),
                                Component.translatable("replaymod.error.unknownrestriction2", unknown)
                        ));
                    });
                }
            }
        }
        if (p instanceof ClientboundDisconnectPacket) {
            Component reason = ((ClientboundDisconnectPacket) p).getReason();
            String message = reason.getString();
            if ("Please update to view this replay.".equals(message)) {
                // This version of the mod supports replay restrictions so we are allowed
                // to remove this packet.
                return null;
            }
        }

        if(BAD_PACKETS.contains(p.getClass())) return null;

        if (p instanceof ClientboundCustomPayloadPacket packet) {
            ResourceLocation channelName = packet.getIdentifier();
            String channelNameStr = channelName.toString();

            if (channelNameStr.startsWith("fabric-screen-handler-api-v")) {
                return null; // we do not want to show modded screens which got opened for the recording player
            }

            // On 1.14+ there's a dedicated OpenWrittenBookS2CPacket now
        }

        if(p instanceof ClientboundResourcePackPacket packet) {
            String url = packet.getUrl();
            if (url.startsWith("replay://")) {
                int id = Integer.parseInt(url.substring("replay://".length()));
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index != null) {
                    String hash = index.get(id);
                    if (hash != null) {
                        File file = new File(tempResourcePackFolder, hash + ".zip");
                        if (!file.exists()) {
                            IOUtils.copy(replayFile.getResourcePack(hash).get(), new FileOutputStream(file));
                        }
                        setServerResourcePack(file);
                    }
                }
                return null;
            }
        }

        if(p instanceof ClientboundLoginPacket packet) {
            int entId = packet.playerId();
            schedulePacketHandler(() -> allowMovement = true);
            actualID = entId;
            entId = -1789435; // Camera entity id should be negative which is an invalid id and can't be used by servers
            p = new ClientboundLoginPacket(
                    entId,
                    packet.hardcore(),
                    GameType.SPECTATOR,
                    GameType.SPECTATOR,
                    packet.levels(),
                    packet.registryHolder(),
                    packet.dimensionType(),
                    packet.dimension(),
                    packet.seed(),
                    0, // max players (has no getter -> never actually used)
                    packet.chunkRadius(),
                    packet.simulationDistance(),
                    packet.reducedDebugInfo()
                    , packet.showDeathScreen()
                    , packet.isDebug()
                    , packet.isFlat()
                    , java.util.Optional.empty()
                    , packet.portalCooldown()
            );
        }

        if(p instanceof ClientboundRespawnPacket respawn) {
            p = new ClientboundRespawnPacket(
                    respawn.getDimensionType(),
                    respawn.getDimension(),
                    respawn.getSeed(),
                    GameType.SPECTATOR,
                    GameType.SPECTATOR,
                    respawn.isDebug(),
                    respawn.isFlat(),
                    (byte) 0
                    , java.util.Optional.empty()
                    , respawn.getPortalCooldown()
            );

            schedulePacketHandler(() -> allowMovement = true);
        }

        if(p instanceof ClientboundPlayerPositionPacket ppl) {
            if(!hasWorldLoaded) hasWorldLoaded = true;

            RePlayCore.instance.runLater(() -> {
                if (mc.screen instanceof ReceivingLevelScreen) {
                    // Close the world loading screen manually in case we swallow the packet
                    mc.setScreen(null);
                }
            });

            if(replayHandler.shouldSuppressCameraMovements()) return null;

            for (RelativeMovement relative : ppl.getRelativeArguments()) {
                if (relative == RelativeMovement.X || relative == RelativeMovement.Y || relative == RelativeMovement.Z) {
                    return null; // At least one of the coordinates is relative, so we don't care
                }
            }

            schedulePacketHandler(new Runnable() {
                @Override
                public void run() {
                    // FIXME: world shouldn't ever be null at this point, now that we use the packet queue
                    //        probably fine to remove on the next non-patch version (don't want to break stuff now)
                    if (mc.level == null || !mc.isSameThread()) {
                        RePlayCore.instance.runLater(this);
                        return;
                    }

                    CameraEntity cent = replayHandler.getCameraEntity();
                    if (!allowMovement && !((Math.abs(cent.getX() - ppl.getX()) > TP_DISTANCE_LIMIT) ||
                            (Math.abs(cent.getZ() - ppl.getZ()) > TP_DISTANCE_LIMIT))) {
                        return;
                    } else {
                        allowMovement = false;
                    }
                    cent.setCameraPosition(ppl.getX(), ppl.getY(), ppl.getZ());
                    cent.setCameraRotation(ppl.getYRot(), ppl.getXRot(), cent.roll);
                }
            });

            return null;
        }

        if(p instanceof ClientboundGameEventPacket pg) {
            // only allow the following packets:
            // 1 - End raining
            // 2 - Begin raining
            //
            // The following values are to control sky color (e.g. if thunderstorm)
            // 7 - Fade value
            // 8 - Fade time
            if (!Arrays.asList(
                    ClientboundGameEventPacket.START_RAINING,
                    ClientboundGameEventPacket.STOP_RAINING,
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE
            ).contains(pg.getEvent())) {
                return null;
            }
        }

        if (p instanceof ClientboundSystemChatPacket || p instanceof ClientboundPlayerChatPacket || p instanceof ClientboundDisguisedChatPacket) {
            if (!ReplayModReplay.instance.getCore().getSettingsRegistry().get(Setting.SHOW_CHAT)) {
                return null;
            }
        }

        if (asyncMode) {
            return processPacketAsync(p);
        } else {
            Packet fp = p;
            schedulePacketHandler(() -> processPacketSync(fp));
            return p;
        }
    }


    /**
     * Returns the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * If 0 is returned, the replay is paused.
     * @return speed multiplier
     */
    @Override
    public double getReplaySpeed() {
        if(!paused()) return replaySpeed;
        else return 0;
    }

    /**
     * Set the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * The speed may not be set to 0 nor to negative values.
     * @param d Speed multiplier
     */
    @Override
    public void setReplaySpeed(final double d) {
        synchronized (this) {
            if (d != 0) {
                this.replaySpeed = d;
                this.realTimeStartSpeed = d;
                this.realTimeStart = System.currentTimeMillis() - (long) (lastTimeStamp / d);
            }
            notifyAll();
        }
        mc.timer.msPerTick = DEFAULT_MS_PER_TICK / (float) d;
    }

    /////////////////////////////////////////////////////////
    //       Asynchronous packet processing                //
    /////////////////////////////////////////////////////////

    /**
     * Timestamp in milliseconds of when we started (or would have started when taking pauses and speed into account)
     * the playback of the replay.
     * Updated only when replay speed changes or on pause/unpause but definitely not on every packet to prevent gradual
     * drifting.
     */
    private long realTimeStart;

    /**
     * The replay speed used for {@link #realTimeStart}.
     * If the target speed differs from this one, the timestamp is recalculated.
     */
    private double realTimeStartSpeed;

    /**
     * There is no waiting performed until a packet with at least this timestamp is reached (but not yet sent).
     * If this is -1, then timing is normal.
     */
    private long desiredTimeStamp = -1;

    /**
     * Runnable which performs timed dispatching of packets from the input stream.
     */
    private final Runnable asyncSender = new Runnable() {
        public void run() {
            try {
                REPLAY_LOOP:
                while (!terminate) {
                    synchronized (FullReplaySender.this) {
                        if (replayIn == null) {
                            replayIn = replayFile.getPacketData(getPacketTypeRegistry(State.LOGIN));
                        }
                        // Packet loop
                        while (true) {
                            try {
                                // When playback is paused and the world has loaded (we don't want any dirt-screens) we sleep
                                while (paused() && hasWorldLoaded && !inBundle) {
                                    // Unless we are going to terminate, restart or jump
                                    if (terminate || startFromBeginning || desiredTimeStamp != -1) {
                                        break;
                                    }
                                    FullReplaySender.this.wait();
                                }

                                if (terminate && !inBundle) {
                                    break REPLAY_LOOP;
                                }

                                if (startFromBeginning) {
                                    // In case we need to restart from the beginning
                                    // break out of the loop sending all packets which will
                                    // cause the replay to be restarted by the outer loop
                                    break;
                                }

                                // Read the next packet if we don't already have one
                                if (nextPacket == null) {
                                    nextPacket = new PacketData(replayIn);
                                }

                                int nextTimeStamp = nextPacket.timestamp;

                                // If we aren't jumping and the world has already been loaded (no dirt-screens) then wait
                                // the required amount to get proper packet timing
                                if (!isHurrying() && hasWorldLoaded && !inBundle) {
                                    // Timestamp of when the next packet should be sent
                                    long expectedTime = realTimeStart + (long) (nextTimeStamp / replaySpeed);
                                    long now = System.currentTimeMillis();
                                    // If the packet should not yet be sent, wait a bit
                                    if (expectedTime > now) {
                                        FullReplaySender.this.wait(expectedTime - now);
                                    }
                                }

                                // Process packet
                                if (!replayneo$dropMalformedBundlePacket(nextPacket)) {
                                    if (nextPacket.type == PacketType.Bundle) inBundle = !inBundle;
                                    channel.pipeline().fireChannelRead(Unpooled.wrappedBuffer(nextPacket.bytes));
                                }
                                nextPacket = null;

                                lastTimeStamp = nextTimeStamp;

                                // MC as of 1.20.2 relies on autoRead, so it can update the connection state on the main
                                // thread before the next packet is read. As such, we need to stall if that was just
                                // enabled.
                                // Might be safe to do the same on older versions too, but I'd rather not poke the
                                // monster that is Forge networking.

                                // In case we finished jumping
                                // We need to check that we aren't planing to restart so we don't accidentally run this
                                // code before we actually restarted
                                if (isHurrying() && lastTimeStamp > desiredTimeStamp && !startFromBeginning) {
                                    desiredTimeStamp = -1;

                                    replayHandler.moveCameraToTargetPosition();

                                    // Pause after jumping (this will also reset realTimeStart accordingly)
                                    setReplaySpeed(0);
                                }
                            } catch (EOFException eof) {
                                // Reached end of file
                                // Pause the replay which will cause it to freeze before getting restarted
                                setReplaySpeed(0);
                                // Then wait until the user tells us to continue
                                while (paused() && hasWorldLoaded && desiredTimeStamp == -1 && !terminate) {
                                    FullReplaySender.this.wait();
                                }

                                if (terminate) {
                                    break REPLAY_LOOP;
                                }
                                break;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break REPLAY_LOOP;
                            } catch (IOException e) {
                                LOGGER.warn("Replay sender failed while reading packet data; stopping replay sender.", e);
                                terminateReplay();
                                break REPLAY_LOOP;
                            }
                        }

                        // Restart the replay.
                        hasWorldLoaded = false;
                        inBundle = false;
                        lastTimeStamp = 0;
                        registry = getPacketTypeRegistry(State.LOGIN);
                        startFromBeginning = false;
                        nextPacket = null;
                        realTimeStart = System.currentTimeMillis();
                        if (replayIn != null) {
                            replayIn.close();
                            replayIn = null;
                        }
                        RePlayCore.instance.runSync(replayHandler::restartedReplay);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Replay sender stopped unexpectedly.", e);
            }
        }
    };

    /**
     * Return whether this replay sender is currently rushing. When rushing, all packets are sent without waiting until
     * a specified timestamp is passed.
     * @return {@code true} if currently rushing, {@code false} otherwise
     */
    public boolean isHurrying() {
        return desiredTimeStamp != -1;
    }

    /**
     * Cancels the hurrying.
     */
    public void stopHurrying() {
        synchronized (this) {
            desiredTimeStamp = -1;
            notifyAll();
        }
    }

    /**
     * Return the timestamp to which this replay sender is currently rushing. All packets with an lower or equal
     * timestamp will be sent out without any sleeping.
     * @return The timestamp in milliseconds since the start of the replay
     */
    public long getDesiredTimestamp() {
        return desiredTimeStamp;
    }

    /**
     * Jumps to the specified timestamp when in async mode by rushing all packets until one with a timestamp greater
     * than the specified timestamp is found.
     * If the timestamp has already passed, this causes the replay to restart and then rush all packets.
     * @param millis Timestamp in milliseconds since the start of the replay
     */
    @Override
    public void jumpToTime(int millis) {
        Preconditions.checkState(asyncMode, "Can only jump in async mode. Use sendPacketsTill(int) instead.");
        synchronized (this) {
            if(millis < lastTimeStamp && !isHurrying()) {
                startFromBeginning = true;
            }

            desiredTimeStamp = millis;
            notifyAll();
        }
    }

    protected Packet processPacketAsync(Packet p) {
        //If hurrying, ignore some packets, except for short durations
        if(desiredTimeStamp - lastTimeStamp > 1000) {
            if(p instanceof ClientboundLevelParticlesPacket) return null;

            if(p instanceof ClientboundAddEntityPacket pso) {
                if (pso.getType() == EntityType.FIREWORK_ROCKET) return null;
            }
        }
        return p;
    }

    /////////////////////////////////////////////////////////
    //        Synchronous packet processing                //
    /////////////////////////////////////////////////////////

    // Even in sync mode, we send from another thread because mods may rely on that
    private final ExecutorService syncSender = Executors.newSingleThreadExecutor(runnable ->
            new Thread(runnable, "RePlayCore-sync-sender"));

    /**
     * Sends all packets until the specified timestamp is reached (inclusive).
     * If the timestamp is smaller than the last packet sent, the replay is restarted from the beginning.
     * @param timestamp The timestamp in milliseconds since the beginning of this replay
     */
    @Override
    public void sendPacketsTill(int timestamp) {
        Preconditions.checkState(!asyncMode, "This method cannot be used in async mode. Use jumpToTime(int) instead.");

        // Submit our target to the sender thread and track its progress
        Future<?> sending = syncSender.submit(() -> doSendPacketsTill(timestamp));

        // Drain the task queue while we are sending (in case a mod blocks the io thread waiting for the main thread)
        while (!sending.isDone()) {
            executeTaskQueue();

            // Wait until the sender thread has made progress
            LockSupport.parkNanos(100_000);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }

        // Everything has been sent, drain the queue one last time
        executeTaskQueue();
    }

    private void doSendPacketsTill(int timestamp) {
        try {
            synchronized (this) {
                if (timestamp == lastTimeStamp) { // Do nothing if we're already there
                    return;
                }
                if (timestamp < lastTimeStamp) { // Restart the replay if we need to go backwards in time
                    hasWorldLoaded = false;
                    inBundle = false;
                    lastTimeStamp = 0;
                    if (replayIn != null) {
                        replayIn.close();
                        replayIn = null;
                    }
                    registry = getPacketTypeRegistry(State.LOGIN);
                    startFromBeginning = false;
                    nextPacket = null;
                    RePlayCore.instance.runSync(replayHandler::restartedReplay);
                }

                if (replayIn == null) {
                    replayIn = replayFile.getPacketData(getPacketTypeRegistry(State.LOGIN));
                }

                while (true) { // Send packets
                    try {
                        PacketData pd;
                        if (nextPacket != null) {
                            // If there is still a packet left from before, use it first
                            pd = nextPacket;
                            nextPacket = null;
                        } else {
                            // Otherwise read one from the input stream
                            pd = new PacketData(replayIn);
                        }

                        int nextTimeStamp = pd.timestamp;
                        if (nextTimeStamp > timestamp && !inBundle) {
                            // We are done sending all packets
                            nextPacket = pd;
                            break;
                        }

                        // Process packet
                        if (!replayneo$dropMalformedBundlePacket(pd)) {
                            if (pd.type == PacketType.Bundle) inBundle = !inBundle;
                            channel.pipeline().fireChannelRead(Unpooled.wrappedBuffer(pd.bytes));
                        }

                        // MC as of 1.20.2 relies on autoRead, so it can update the connection state on the main
                        // thread before the next packet is read. As such, we need to stall if that was just
                        // enabled.
                        // Might be safe to do the same on older versions too, but I'd rather not poke the
                        // monster that is Forge networking.
                    } catch (EOFException eof) {
                        // Shit! We hit the end before finishing our job! What shall we do now?
                        // well, let's just pretend we're done...
                        replayIn = null;
                        break;
                    } catch (IOException e) {
                        LOGGER.warn("Replay sync sender failed while reading packet data; stopping replay sender.", e);
                        terminateReplay();
                        break;
                    }
                }

                // This might be required if we change to async mode anytime soon
                realTimeStart = System.currentTimeMillis() - (long) (timestamp / replaySpeed);
                lastTimeStamp = timestamp;
            }
        } catch (Exception e) {
            LOGGER.error("Replay sync sender stopped unexpectedly.", e);
        }
    }

    private void executeTaskQueue() {
        ((MCVer.MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();
        RePlayCore.instance.runTasks();
    }

    /**
     * Runs the given runnable on the main thread as if it was a packet handler.
     * Note that the packet handler queue has different behavior than the standard RePlayCore queue.
     */
    private void schedulePacketHandler(Runnable runnable) {
        if (mc.isSameThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }

    protected void processPacketSync(Packet p) {
        if (p instanceof ClientboundForgetLevelChunkPacket packet) {
            int x = packet.getX();
            int z = packet.getZ();
            // If the chunk is getting unloaded, we will have to forcefully update the position of all entities
            // within. Otherwise, if there wasn't a game tick recently, there may be entities that have moved
            // out of the chunk by now but are still registered in it. If we do not update those, they will get
            // unloaded even though they shouldn't.
            // Note: This is only half of the truth. Entities may be removed by chunk-unloading, see else-case below.
            // To make things worse, it seems like players were never supposed to be unloaded this way because
            // they will remain glitched in the World#playerEntities list.
            // 1.14+: The update issue remains but only for non-players and the unloading list bug appears to have been
            //        fixed (chunk unloading no longer removes the entities).
            // Get the chunk that will be unloaded
            ClientLevel world = mc.level;
            ChunkSource chunkProvider = Objects.requireNonNull(world).getChunkSource();
            LevelChunk chunk = chunkProvider.getChunkNow(x, z);
            if (chunk != null) {
                List<Entity> entitiesInChunk = new ArrayList<>();
                // Gather all entities in that chunk
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.chunkPosition().equals(chunk.getPos())) {
                        entitiesInChunk.add(entity);
                    }
                }
                for (Entity entity : entitiesInChunk) {
                    // Skip interpolation of position updates coming from server
                    // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
                    forcePositionForVehicleAndSelf(entity);

                    // Check whether the entity has left the chunk
                    // This is now handled automatically in Entity.setPos (called from tick())
                }
            }
        }
    }

    private void forcePositionForVehicleAndSelf(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            forcePositionForVehicleAndSelf(vehicle);
        }

        // Skip interpolation of position updates coming from server
        // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
        int ticks = 0;
        Vec3 prevPos;
        do {
            prevPos = entity.position();
            if (vehicle != null) {
                entity.rideTick();
            } else {
                entity.tick();
            }
        } while (prevPos.distanceToSqr(entity.position()) > 0.0001 && ticks++ < 100);
    }

    private boolean replayneo$dropMalformedBundlePacket(PacketData packetData) {
        if (packetData.type != PacketType.Bundle || packetData.bytes.length <= 1) {
            return false;
        }
        if (replayneo$droppedMalformedBundlePackets++ < 8) {
            LOGGER.warn(
                    "Dropping malformed replay bundle delimiter. timestamp={}, bytes={}, firstByte={}, dropped={}",
                    packetData.timestamp,
                    packetData.bytes.length,
                    packetData.bytes.length == 0 ? -1 : Byte.toUnsignedInt(packetData.bytes[0]),
                    replayneo$droppedMalformedBundlePackets
            );
        }
        return true;
    }

    private static final class PacketData {
        private static final com.github.steveice10.netty.buffer.ByteBuf byteBuf = com.github.steveice10.netty.buffer.Unpooled.buffer();
        private static final NetOutput netOutput = new ByteBufNetOutput(byteBuf);

        private final int timestamp;
        private final byte[] bytes;
        private final PacketType type;

        PacketData(ReplayInputStream in) throws IOException {
            if (RePlayCore.isMinimalMode()) {
                // Minimal mode, we can only read our exact protocol version and cannot use ReplayStudio
                timestamp = readInt(in);
                int length = readInt(in);
                if (timestamp == -1 || length == -1) {
                    throw new EOFException();
                }
                bytes = new byte[length];
                IOUtils.readFully(in, bytes);
                type = PacketType.UnknownLogin;
            } else {
                com.replaymod.replaystudio.PacketData data = in.readPacket();
                if (data == null) {
                    throw new EOFException();
                }
                timestamp = (int) data.getTime();
                com.replaymod.replaystudio.protocol.Packet packet = data.getPacket();
                type = packet.getType();
                // Workaround for ReplayMod 2.7.16-17 saving the LoginSuccess packet with an incorrect packet id
                // A fake one will have been sythesized by ReplayStudo, so we can simply drop the broken one.
                if (packet.getId() == -1) {
                    bytes = new byte[0];
                    return;
                }
                // We need to re-encode ReplayStudio packets, so we can later decode them as NMS packets
                // The main reason we aren't reading them as NMS packets is that we want ReplayStudio to be able
                // to apply ViaVersion (and potentially other magic) to it.
                synchronized (byteBuf) {
                    byteBuf.markReaderIndex(); // Mark the current reader and writer index (should be at start)
                    byteBuf.markWriterIndex();

                    netOutput.writeVarInt(packet.getId());
                    int idSize = byteBuf.readableBytes();
                    int contentSize = packet.getBuf().readableBytes();
                    bytes = new byte[idSize + contentSize]; // Create bytes array of sufficient size
                    byteBuf.readBytes(bytes, 0, idSize);
                    packet.getBuf().readBytes(bytes, idSize, contentSize);

                    byteBuf.resetReaderIndex(); // Reset reader & writer index for next use
                    byteBuf.resetWriterIndex();
                }
                packet.getBuf().release();
            }
        }
    }
}
