package github.com.gengyoubo.replayneo.platform.feature.recording.handler;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.replayneo.core.events.PreRenderCallback;
import github.com.gengyoubo.replayneo.mixin.IntegratedServerAccessor;
import github.com.gengyoubo.replayneo.platform.feature.recording.packet.PacketListener;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.PreTickCallback;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import static github.com.gengyoubo.replayneo.core.versions.MCVer.*;

public class RecordingEventHandler extends EventRegistrations {

    private final Minecraft mc = getMinecraft();
    private final PacketListener packetListener;

    private Double lastX, lastY, lastZ;
    private static final int EQUIPMENT_SLOTS = EquipmentSlot.values().length;
    private final List<ItemStack> playerItems = NonNullList.withSize(EQUIPMENT_SLOTS, ItemStack.EMPTY);
    private int ticksSinceLastCorrection;
    private boolean wasSleeping;
    private int lastRiding = -1;
    private Integer rotationYawHeadBefore;
    private boolean spawnedRecordingPlayer;
    private int loggedPlayerMovementPackets;

    public RecordingEventHandler(PacketListener packetListener) {
        this.packetListener = packetListener;
    }

    @Override
    public void register() {
        super.register();
        ((RecordingEventSender) mc.levelRenderer).setRecordingEventHandler(this);
    }

    @Override
    public void unregister() {
        super.unregister();
        RecordingEventSender recordingEventSender = ((RecordingEventSender) mc.levelRenderer);
        if (recordingEventSender.getRecordingEventHandler() == this) {
            recordingEventSender.setRecordingEventHandler(null);
        }
    }

    public void onPacket(Packet<?> packet) {
        packetListener.save(packet);
    }

    public void onPacket(Connection connection, Packet<?> packet) {
        packetListener.saveObservedPacket(connection, packet);
    }

    public void onDisconnected() {
        packetListener.close();
    }

    public void spawnRecordingPlayer() {
        try {
            LocalPlayer player = mc.player;
            assert player != null;
            packetListener.save(createOwnPlayerInfoPacket(player));
            packetListener.save(new ClientboundAddPlayerPacket(player));
            packetListener.save(new ClientboundSetEntityDataPacket(player.getId(), Objects.requireNonNull(player.getEntityData().getNonDefaultValues())));
            spawnedRecordingPlayer = true;
            github.com.gengyoubo.replayneo.RePlayNeo.LOGGER.warn(
                    "Recording player spawn packet. entityId={}, uuid={}, pos=({}, {}, {}), time={}",
                    player.getId(), player.getUUID(), player.getX(), player.getY(), player.getZ(),
                    packetListener.getCurrentDuration());
            lastX = lastY = lastZ = null;
            playerItems.clear();
            lastRiding = -1;
            wasSleeping = false;
            loggedPlayerMovementPackets = 0;
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private ClientboundPlayerInfoUpdatePacket createOwnPlayerInfoPacket(LocalPlayer player) {
        GameProfile profile = player.getGameProfile();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
                    ClientboundPlayerInfoUpdatePacket.Action.class);
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

    public void onClientEffect(int type, BlockPos pos, int data) {
        try {
            // Send to all other players in ServerWorldEventHandler#playEvent
            packetListener.save(new ClientboundLevelEventPacket(type, pos, data, false));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    { on(PreTickCallback.EVENT, this::onPlayerTick); }
    private void onPlayerTick() {
        if (mc.player == null) return;
        LocalPlayer player = mc.player;
        try {
            if (!spawnedRecordingPlayer) {
                spawnRecordingPlayer();
            }

            boolean force = false;
            if(lastX == null || lastY == null || lastZ == null) {
                force = true;
                lastX = player.getX();
                lastY = player.getY();
                lastZ = player.getZ();
            }

            ticksSinceLastCorrection++;
            if(ticksSinceLastCorrection >= 100) {
                ticksSinceLastCorrection = 0;
                force = true;
            }

            double dx = player.getX() - lastX;
            double dy = player.getY() - lastY;
            double dz = player.getZ() - lastZ;

            lastX = player.getX();
            lastY = player.getY();
            lastZ = player.getZ();

            final double maxRelDist = 8.0;

            Packet packet;
            if (force || Math.abs(dx) > maxRelDist || Math.abs(dy) > maxRelDist || Math.abs(dz) > maxRelDist) {
                packet = new ClientboundTeleportEntityPacket(player);
            } else {
                byte newYaw = (byte) ((int) (player.getYRot() * 256.0F / 360.0F));
                byte newPitch = (byte) ((int) (player.getXRot() * 256.0F / 360.0F));

                packet = new ClientboundMoveEntityPacket.PosRot(
                        player.getId(),
                        (short) Math.round(dx * 4096), (short) Math.round(dy * 4096), (short) Math.round(dz * 4096),
                        newYaw, newPitch
                        , player.onGround()
                );
            }

            if (loggedPlayerMovementPackets++ < 16) {
                github.com.gengyoubo.replayneo.RePlayNeo.LOGGER.warn(
                        "Recording player movement packet. type={}, entityId={}, force={}, dx={}, dy={}, dz={}, pos=({}, {}, {}), time={}",
                        packet instanceof ClientboundTeleportEntityPacket ? "TeleportEntity" : "MoveEntity",
                        player.getId(), force, dx, dy, dz, player.getX(), player.getY(), player.getZ(),
                        packetListener.getCurrentDuration());
            }
            packetListener.save(packet);

            //HEAD POS
            int rotationYawHead = ((int)(player.yHeadRot * 256.0F / 360.0F));

            if(!Objects.equals(rotationYawHead, rotationYawHeadBefore)) {
                packetListener.save(new ClientboundRotateHeadPacket(player, (byte) rotationYawHead));
                rotationYawHeadBefore = rotationYawHead;
            }

            packetListener.save(new ClientboundSetEntityMotionPacket(player.getId(),
                    player.getDeltaMovement()
            ));

            //Animation Packets
            //Swing Animation
            if (player.swinging && player.swingTime == 0) {
                packetListener.save(new ClientboundAnimatePacket(
                        player,
                        player.swingingArm == InteractionHand.MAIN_HAND ? 0 : 3
                ));
            }

            //Inventory Handling
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                int index = slot.ordinal();
                if (!ItemStack.matches(playerItems.get(index), stack)) {
                    // ItemStack has internal mutability, so we need to make a copy now if we want to compare its
                    // current state with future states (e.g. dropping on modern versions will set the count to zero).
                    stack = stack.copy();
                    playerItems.set(index, stack);
                    packetListener.save(new ClientboundSetEquipmentPacket(player.getId(), Collections.singletonList(Pair.of(slot, stack))));
                }
            }

            //Leaving Ride

            Entity vehicle = player.getVehicle();
            int vehicleId = vehicle == null ? -1 : vehicle.getId();
            if (lastRiding != vehicleId) {
                lastRiding = vehicleId;
                packetListener.save(new ClientboundSetEntityLinkPacket(
                        player,
                        vehicle
                ));
            }

            //Sleeping
            if(!player.isSleeping() && wasSleeping) {
                packetListener.save(new ClientboundAnimatePacket(player, 2));
                wasSleeping = false;
            }

        } catch(Exception e1) {
            e1.printStackTrace();
        }
    }

    public void onBlockBreakAnim(int breakerId, BlockPos pos, int progress) {
        Player thePlayer = mc.player;
        if (thePlayer != null && breakerId == thePlayer.getId()) {
            packetListener.save(new ClientboundBlockDestructionPacket(breakerId,
                    pos,
                    progress));
        }
    }

    { on(PreRenderCallback.EVENT, this::checkForGamePaused); }
    private void checkForGamePaused() {
        if (mc.hasSingleplayerServer()) {
            IntegratedServer server =  mc.getSingleplayerServer();
            if (server != null && ((IntegratedServerAccessor) server).isGamePaused()) {
                packetListener.setServerWasPaused();
            }
        }
    }

    public interface RecordingEventSender {
        void setRecordingEventHandler(RecordingEventHandler recordingEventHandler);
        RecordingEventHandler getRecordingEventHandler();
    }
}
