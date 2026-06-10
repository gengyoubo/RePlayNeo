package github.com.gengyoubo.replayneo.platform.feature.recording.handler;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.utils.ModCompat;
import github.com.gengyoubo.replayneo.core.utils.Utils;
import github.com.gengyoubo.replayneo.platform.feature.editor.gui.MarkerProcessor;
import github.com.gengyoubo.replayneo.platform.feature.recording.ServerInfoExt;
import github.com.gengyoubo.replayneo.platform.feature.recording.Setting;
import github.com.gengyoubo.replayneo.platform.feature.recording.gui.GuiRecordingControls;
import github.com.gengyoubo.replayneo.platform.feature.recording.gui.GuiRecordingOverlay;
import github.com.gengyoubo.replayneo.mixin.NetworkManagerAccessor;
import github.com.gengyoubo.replayneo.platform.feature.recording.packet.PacketListener;
import github.com.gengyoubo.replayneo.platform.feature.recording.packet.PacketListener.DecodedPacketListener;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import io.netty.channel.Channel;
import org.apache.logging.log4j.Logger;

import github.com.gengyoubo.replayneo.mixin.ClientLoginNetworkHandlerAccessor;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.Connection;
import net.minecraft.world.level.Level;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.getMinecraft;

/**
 * Handles connection events and initiates recording if enabled.
 */
public class ConnectionEventHandler {

    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static final Minecraft mc = getMinecraft();

    private final Logger logger;
    private final ReplayMod core;

    private RecordingEventHandler recordingEventHandler;
    private PacketListener packetListener;
    private GuiRecordingOverlay guiOverlay;
    private GuiRecordingControls guiControls;

    public ConnectionEventHandler(Logger logger, ReplayMod core) {
        this.logger = logger;
        this.core = core;
    }

    public void onConnectedToServerEvent(Connection networkManager) {
        try {
            boolean local = networkManager.isMemoryConnection();
            if (local) {
                if (mc.getSingleplayerServer() != null && Objects.requireNonNull(mc.getSingleplayerServer().getLevel(Level.OVERWORLD)).isDebug()) {
                    logger.info("Debug World recording is not supported.");
                    return;
                }
                if(!core.getSettingsRegistry().get(Setting.RECORD_SINGLEPLAYER)) {
                    logger.info("Singleplayer Recording is disabled");
                    return;
                }
            } else {
                if(!core.getSettingsRegistry().get(Setting.RECORD_SERVER)) {
                    logger.info("Multiplayer Recording is disabled");
                    return;
                }
            }

            ServerData serverInfo;
            serverInfo = networkManager.getPacketListener() instanceof ClientLoginNetworkHandlerAccessor loginNetworkHandler
                    ? loginNetworkHandler.getServerData()
                    : null;

            String worldName;
            String serverName = null;
            boolean autoStart = core.getSettingsRegistry().get(Setting.AUTO_START_RECORDING);
            if (local) {
                worldName = mc.getSingleplayerServer().getWorldData().getLevelName();
                serverName = worldName;
            } else if (mc.isConnectedToRealms()) {
                // we can't access the server name without tapping too deep in the Realms Library
                worldName = "A Realms Server";
            } else if (serverInfo != null) {
                worldName = serverInfo.ip;
                if (!I18n.get("selectServer.defaultName").equals(serverInfo.name)) {
                    serverName = serverInfo.name;
                }

                Boolean autoStartServer = ServerInfoExt.from(serverInfo).getAutoRecording();
                if (autoStartServer != null) {
                    autoStart = autoStartServer;
                }
            } else {
                logger.info("Recording not started as the world is neither local nor remote (probably a replay).");
                return;
            }

            if (ReplayMod.isMinimalMode()) {
                // Recording controls are not supported in minimal mode, so always auto-start
                autoStart = true;
            }

            String name = sdf.format(Calendar.getInstance().getTime());
            Path outputPath = Utils.replayNameToPath(core.folders.getRecordingFolder(), name);
            ReplayFile replayFile = core.files.open(outputPath);

            replayFile.writeModInfo(ModCompat.getInstalledNetworkMods());

            ReplayMetaData metaData = new ReplayMetaData();
            metaData.setSingleplayer(local);
            metaData.setServerName(worldName);
            metaData.setCustomServerName(serverName);
            metaData.setGenerator("ReplayMod v" + ReplayMod.instance.getVersion());
            metaData.setDate(System.currentTimeMillis());
            metaData.setMcVersion(ReplayMod.instance.getMinecraftVersion());

            Channel channel = ((NetworkManagerAccessor) networkManager).getChannel();
            packetListener = new PacketListener(core, channel, outputPath, replayFile, metaData);

            if (!local) {
                if (channel.pipeline().get(PacketListener.DECODER_KEY) != null) {
                    // Regular channel, we'll inject our recorder directly before the decoder
                    channel.pipeline().addBefore(PacketListener.DECODER_KEY, PacketListener.RAW_RECORDER_KEY, packetListener);
                    channel.pipeline().addAfter(PacketListener.DECODER_KEY, PacketListener.DECODED_RECORDER_KEY, packetListener.new DecodedPacketListener());
                } else {
                    channel.pipeline().addFirst(PacketListener.RAW_RECORDER_KEY, packetListener);
                    channel.pipeline().addAfter(PacketListener.RAW_RECORDER_KEY, PacketListener.DECODED_RECORDER_KEY, packetListener.new DecodedPacketListener());
                }
            }

            recordingEventHandler = new RecordingEventHandler(packetListener);
            recordingEventHandler.register();

            guiControls = new GuiRecordingControls(core, packetListener, autoStart);
            guiControls.register();

            guiOverlay = new GuiRecordingOverlay(mc, core.getSettingsRegistry(), guiControls);
            guiOverlay.register();

            if (autoStart) {
                core.printInfoToChat("replaymod.chat.recordingstarted");
            } else {
                packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT, 0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            core.printWarningToChat("replaymod.chat.recordingfailed");
        }
    }

    public void reset() {
        if (packetListener != null) {
            hideRecordingUi();
            if (recordingEventHandler != null) {
                recordingEventHandler.unregister();
            }
            recordingEventHandler = null;
            packetListener = null;
        }
    }

    public void hideRecordingUi() {
        if (guiControls != null) {
            guiControls.close();
        }
        guiControls = null;
        if (guiOverlay != null) {
            guiOverlay.unregister();
        }
        guiOverlay = null;
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }

    public RecordingEventHandler getRecordingEventHandler() {
        return recordingEventHandler;
    }
}
