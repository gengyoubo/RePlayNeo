package github.com.gengyoubo.replayneo.feature.recording.packet;

import com.github.steveice10.netty.buffer.PooledByteBufAllocator;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.google.gson.Gson;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.utils.Restrictions;
import github.com.gengyoubo.replayneo.core.utils.Utils;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.editor.gui.MarkerProcessor;
import github.com.gengyoubo.replayneo.feature.recording.ReplayModRecording;
import github.com.gengyoubo.replayneo.feature.recording.Setting;
import github.com.gengyoubo.replayneo.feature.recording.gui.GuiSavingReplay;
import github.com.gengyoubo.replayneo.feature.recording.handler.ConnectionEventHandler;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import github.com.gengyoubo.replayneo.core.gui.container.VanillaGuiScreen;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.world.entity.Entity;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static github.com.gengyoubo.replayneo.core.versions.MCVer.*;
import static com.replaymod.replaystudio.util.Utils.writeInt;

import java.util.ArrayList;

@ChannelHandler.Sharable // so we can re-order it
public class PacketListener extends ChannelInboundHandlerAdapter {

    public static final String RAW_RECORDER_KEY = "replay_recorder_raw";
    public static final String DECODED_RECORDER_KEY = "replay_recorder_decoded";

    public static final String DECOMPRESS_KEY = "decompress";
    public static final String DECODER_KEY = "decoder";

    private static final Minecraft mc = getMinecraft();
    private static final Logger logger = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    private final ReplayMod core;
    private final Path outputPath;
    private final ReplayFile replayFile;

    private final ResourcePackRecorder resourcePackRecorder;

    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private final ReplayOutputStream packetOutputStream;

    private final ReplayMetaData metaData;

    private final Channel channel;
    private Packet currentRawPacket;

    private final long startTime;
    private long lastSentPacket;
    private long timePassedWhilePaused;
    private volatile boolean serverWasPaused;
    private volatile boolean closed;

    /**
     * Used to keep track of the last metadata save job submitted to the save service and
     * as such prevents unnecessary writes.
     */
    private final AtomicInteger lastSaveMetaDataId = new AtomicInteger();

    public PacketListener(ReplayMod core, Channel channel, Path outputPath, ReplayFile replayFile, ReplayMetaData metaData) throws IOException {
        this.core = core;
        this.channel = channel;
        this.outputPath = outputPath;
        this.replayFile = replayFile;
        this.metaData = metaData;
        this.resourcePackRecorder = new ResourcePackRecorder(replayFile);
        this.packetOutputStream = replayFile.writePacketData();
        this.startTime = metaData.getDate();

        saveMetaData();
    }

    private void saveMetaData() {
        int id = lastSaveMetaDataId.incrementAndGet();
        saveService.submit(() -> {
            if (lastSaveMetaDataId.get() != id) {
                return; // Another job has been scheduled, it will do the hard work.
            }
            try {
                synchronized (replayFile) {
                    if (ReplayMod.isMinimalMode()) {
                        metaData.setFileFormat("MCPR");
                        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
                        metaData.setProtocolVersion(MCVer.getProtocolVersion());
                        metaData.setGenerator("ReplayMod in Minimal Mode");

                        try (OutputStream out = replayFile.write("metaData.json")) {
                            String json = (new Gson()).toJson(metaData);
                            out.write(json.getBytes());
                        }
                    } else {
                        replayFile.writeMetaData(MCVer.getPacketTypeRegistry(State.LOGIN), metaData);
                    }
                }
            } catch (IOException e) {
                logger.error("Writing metadata:", e);
            }
        });
    }

    public void save(net.minecraft.network.protocol.Packet packet) {
        try {
            for (Packet encoded : encodeObservedPackets(packet)) {
                save(encoded);
            }
        } catch (Exception e) {
            logger.error("Encoding packet:", e);
        }
    }

    public void saveObservedPacket(Connection connection, net.minecraft.network.protocol.Packet<?> packet) {
        if (packet instanceof ClientboundHelloPacket) {
            return;
        }
        if (packet instanceof ClientboundLoginCompressionPacket) {
            return;
        }

        if (packet instanceof ClientboundCustomPayloadPacket customPayloadPacket
                && Restrictions.PLUGIN_CHANNEL.equals(customPayloadPacket.getIdentifier())) {
            save(new ClientboundDisconnectPacket(Component.literal("Please update to view this replay.")));
        }

        if (packet instanceof ClientboundAddPlayerPacket addPlayerPacket) {
            UUID uuid = addPlayerPacket.getPlayerId();
            Set<String> uuids = new HashSet<>(Arrays.asList(metaData.getPlayers()));
            uuids.add(uuid.toString());
            metaData.setPlayers(uuids.toArray(new String[0]));
            saveMetaData();
        }

        if (packet instanceof ClientboundResourcePackPacket resourcePackPacket) {
            save(resourcePackRecorder.handleResourcePack(connection, resourcePackPacket));
            return;
        }

        save(packet);
    }

    public void save(Packet packet) {
        // If we're not on the main thread (i.e. we're on the netty thread), then we need to schedule the saving
        // to happen on the main thread so we can guarantee correct ordering of inbound and inject packets.
        // Otherwise, injected packets may end up further down the packet stream than they were supposed to and other
        // inbound packets which may rely on the injected packet would behave incorrectly when played back.
        if (!mc.isSameThread()) {
            // Note that we must use the same queue as regular packets, otherwise stuff will be out of order!
            mc.tell(() -> save(packet));
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (serverWasPaused) {
                timePassedWhilePaused = now - startTime - lastSentPacket;
                serverWasPaused = false;
            }
            int timestamp = (int) (now - startTime - timePassedWhilePaused);
            lastSentPacket = timestamp;
            PacketData packetData = new PacketData(timestamp, packet);
            saveService.submit(() -> {
                try {
                    if (ReplayMod.isMinimalMode()) {
                        // Minimal mode, ReplayStudio might not know our packet ids, so we cannot use it
                        com.github.steveice10.netty.buffer.ByteBuf packetIdBuf = PooledByteBufAllocator.DEFAULT.buffer();
                        com.github.steveice10.netty.buffer.ByteBuf packetBuf = packetData.getPacket().getBuf();
                        try {
                            new ByteBufNetOutput(packetIdBuf).writeVarInt(packetData.getPacket().getId());

                            int packetIdLen = packetIdBuf.readableBytes();
                            int packetBufLen = packetBuf.readableBytes();
                            writeInt(packetOutputStream, (int) packetData.getTime());
                            writeInt(packetOutputStream, packetIdLen + packetBufLen);
                            packetIdBuf.readBytes(packetOutputStream, packetIdLen);
                            packetBuf.getBytes(packetBuf.readerIndex(), packetOutputStream, packetBufLen);
                        } finally {
                            packetIdBuf.release();
                            packetBuf.release();
                        }
                    } else {
                        packetOutputStream.write(packetData);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch(Exception e) {
            logger.error("Writing packet:", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        close();
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        metaData.setDuration((int) lastSentPacket);
        saveMetaData();

        core.runLater(() -> {
            ConnectionEventHandler connectionEventHandler = ReplayModRecording.instance.getConnectionEventHandler();
            if (connectionEventHandler.getPacketListener() == this) {
                connectionEventHandler.reset();
            }
        });

        GuiSavingReplay guiSavingReplay = new GuiSavingReplay(core);
        new Thread(() -> {
            core.runLater(guiSavingReplay::open);

            saveService.shutdown();
            try {
                saveService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Waiting for save service termination:", e);
            }
            try {
                packetOutputStream.close();
            } catch (IOException e) {
                logger.error("Failed to close packet output stream:", e);
            }

            List<Pair<Path, ReplayMetaData>> outputPaths;
            synchronized (replayFile) {
                try {
                    if (!MarkerProcessor.producesAnyOutput(replayFile)) {
                        // Immediately close the saving popup, the user doesn't care about it
                        core.runLater(guiSavingReplay::close);

                        // If we crash right here, on the next start we'll prompt the user for recovery
                        // but we don't really want that, so drop a marker file to skip recovery for this replay.
                        Path noRecoverMarker = outputPath.resolveSibling(outputPath.getFileName() + ".no_recover");
                        Files.createFile(noRecoverMarker);

                        // We still have the replay, so we just save it (at least for a few weeks) in case they change their mind
                        String replayName = FilenameUtils.getBaseName(outputPath.getFileName().toString());
                        Path rawFolder = ReplayMod.instance.folders.getRawReplayFolder();
                        Path rawPath = rawFolder.resolve(outputPath.getFileName());
                        for (int i = 1; Files.exists(rawPath); i++) {
                            rawPath = rawPath.resolveSibling(replayName + "." + i + ".mcpr");
                        }
                        replayFile.saveTo(rawPath.toFile());
                        replayFile.close();

                        // Done, clean up the marker
                        Files.delete(noRecoverMarker);
                        return;
                    }

                    replayFile.save();
                    replayFile.close();

                    if (core.getSettingsRegistry().get(Setting.AUTO_POST_PROCESS) && !ReplayMod.isMinimalMode()) {
                        outputPaths = MarkerProcessor.apply(outputPath, guiSavingReplay.getProgressBar()::setProgress);
                    } else {
                        outputPaths = Collections.singletonList(Pair.of(outputPath, metaData));
                    }
                } catch (Exception e) {
                    logger.error("Saving replay file:", e);
                    CrashReport crashReport = CrashReport.forThrowable(e, "Saving replay file");
                    core.runLater(() -> Utils.error(logger, VanillaGuiScreen.wrap(mc.screen), crashReport, guiSavingReplay::close));
                    return;
                }
            }

            core.runLater(() -> guiSavingReplay.presentRenameDialog(outputPaths));
        }).start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ConnectionProtocol connectionState = getConnectionState();

        Packet packet = null;
        if (msg instanceof ByteBuf buf) {
            // for regular connections, we're expecting to observe `ByteBuf`s here
            if (buf.readableBytes() > 0) {
                packet = decodePacket(connectionState, buf);
            }
        } else if (msg instanceof net.minecraft.network.protocol.Packet) {
            // for integrated server connections MC is passing the packet objects directly, so we need to encode them
            // ourselves to be able to store them
            List<Packet> packets = encodeObservedPackets((net.minecraft.network.protocol.Packet<?>) msg);
            if (packets.size() > 1) {
                packets.forEach(this::save);
                super.channelRead(ctx, msg);
                return;
            }
            packet = packets.isEmpty() ? null : packets.get(0);
        }

        currentRawPacket = packet;
        try {
            super.channelRead(ctx, msg);
        } finally {
            if (currentRawPacket != null) {
                currentRawPacket.release();
                currentRawPacket = null;
            }
        }
    }

    private ConnectionProtocol getConnectionState() {
        AttributeKey<ConnectionProtocol> key = Connection.ATTRIBUTE_PROTOCOL;
        return channel.attr(key).get();
    }

    private Packet encodeMcPacket(ConnectionProtocol connectionState, net.minecraft.network.protocol.Packet packet) throws Exception {
        Integer packetId = connectionState.getPacketId(PacketFlow.CLIENTBOUND, packet);
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            packet.write(new FriendlyByteBuf(byteBuf));
            return new Packet(
                    MCVer.getPacketTypeRegistry(connectionState),
                    packetId,
                    com.github.steveice10.netty.buffer.Unpooled.wrappedBuffer(
                            byteBuf.array(),
                            byteBuf.arrayOffset(),
                            byteBuf.readableBytes()
                    )
            );
        } finally {
            byteBuf.release();
        }
    }

    private List<Packet> encodeObservedPackets(net.minecraft.network.protocol.Packet<?> packet) throws Exception {
        BundlerInfo.Provider bundlerProvider = channel.attr(BundlerInfo.BUNDLER_PROVIDER).get();
        if (bundlerProvider == null) {
            return Collections.singletonList(encodeMcPacket(getConnectionState(), packet));
        }

        BundlerInfo bundlerInfo = bundlerProvider.getBundlerInfo(PacketFlow.CLIENTBOUND);
        if (bundlerInfo == null) {
            return Collections.singletonList(encodeMcPacket(getConnectionState(), packet));
        }

        List<Packet> packets = new ArrayList<>(1);
        bundlerInfo.unbundlePacket(packet, unbundledPacket -> {
            try {
                packets.add(encodeMcPacket(getConnectionState(), unbundledPacket));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return packets;
    }

    private static Packet decodePacket(ConnectionProtocol connectionState, ByteBuf buf) {
        FriendlyByteBuf packetBuf = new FriendlyByteBuf(buf.slice());
        int packetId = packetBuf.readVarInt();
        byte[] bytes = new byte[packetBuf.readableBytes()];
        packetBuf.readBytes(bytes);
        return new Packet(
                MCVer.getPacketTypeRegistry(connectionState),
                packetId,
                com.github.steveice10.netty.buffer.Unpooled.wrappedBuffer(bytes)
        );
    }

    public void addMarker(String name) {
        addMarker(name, (int) getCurrentDuration());
    }

    public void addMarker(String name, int timestamp) {
        Entity view = mc.getCameraEntity();

        Marker marker = new Marker();
        marker.setName(name);
        marker.setTime(timestamp);
        if (view != null) {
            marker.setX(view.getX());
            marker.setY(view.getY());
            marker.setZ(view.getZ());
            marker.setYaw(view.getYRot());
            marker.setPitch(view.getXRot());
        }
        // Roll is always 0
        saveService.submit(() -> {
            synchronized (replayFile) {
                try {
                    Set<Marker> markers = replayFile.getMarkers().or(HashSet::new);
                    markers.add(marker);
                    replayFile.writeMarkers(markers);
                } catch (IOException e) {
                    logger.error("Writing markers:", e);
                }
            }
        });
    }

    public long getCurrentDuration() {
        return lastSentPacket;
    }

    public void setServerWasPaused() {
        this.serverWasPaused = true;
    }

    public ResourcePackRecorder getResourcePackRecorder() {
        return resourcePackRecorder;
    }

    public class DecodedPacketListener extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            if (msg instanceof ClientboundHelloPacket) {
                super.channelRead(ctx, msg);
                return;
            }
            if (msg instanceof ClientboundLoginCompressionPacket) {
                super.channelRead(ctx, msg);
                return;
            }

            if (msg instanceof ClientboundCustomPayloadPacket packet) {
                if (Restrictions.PLUGIN_CHANNEL.equals(packet.getIdentifier())) {
                    save(new ClientboundDisconnectPacket(Component.literal("Please update to view this replay.")));
                }
            }

            if (msg instanceof ClientboundAddPlayerPacket) {
                UUID uuid = ((ClientboundAddPlayerPacket) msg).getPlayerId();
                Set<String> uuids = new HashSet<>(Arrays.asList(metaData.getPlayers()));
                uuids.add(uuid.toString());
                metaData.setPlayers(uuids.toArray(new String[0]));
                saveMetaData();
            }

            if (msg instanceof ClientboundResourcePackPacket) {
                Connection connection = ctx.pipeline().get(Connection.class);
                save(resourcePackRecorder.handleResourcePack(connection, (ClientboundResourcePackPacket) msg));
                return;
            }


            if (currentRawPacket != null) {
                save(currentRawPacket);
                currentRawPacket = null;
            }

            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

            super.write(ctx, msg, promise);
        }
    }
}
