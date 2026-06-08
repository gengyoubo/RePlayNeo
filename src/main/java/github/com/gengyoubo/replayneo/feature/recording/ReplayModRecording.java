package github.com.gengyoubo.replayneo.feature.recording;

import github.com.gengyoubo.replayneo.core.KeyBindingRegistry;
import github.com.gengyoubo.replayneo.core.Module;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.versions.MCVer.Keyboard;
import github.com.gengyoubo.replayneo.feature.recording.handler.ConnectionEventHandler;
import github.com.gengyoubo.replayneo.feature.recording.handler.GuiHandler;
import github.com.gengyoubo.replayneo.mixin.NetworkManagerAccessor;
import github.com.gengyoubo.replayneo.feature.recording.packet.PacketListener;
import github.com.gengyoubo.replayneo.feature.replay.ReplayHandler;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.Logger;


import net.minecraft.network.Connection;


public class ReplayModRecording implements Module {

    private static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;
    private static AttributeKey<Void> ATTR_CHECKED = AttributeKey.newInstance("ReplayModRecording_checked");

    { instance = this; }
    public static ReplayModRecording instance;

    private final ReplayMod core;

    private ConnectionEventHandler connectionEventHandler;

    public ReplayModRecording(ReplayMod mod) {
        core = mod;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.marker", Keyboard.KEY_M, () -> {
            PacketListener packetListener = connectionEventHandler.getPacketListener();
            if (packetListener != null) {
                packetListener.addMarker(null);
                core.printInfoToChat("replaymod.chat.addedmarker");
            }
        }, false);
    }

    @Override
    public void initClient() {
        connectionEventHandler = new ConnectionEventHandler(LOGGER, core);

        new GuiHandler(core).register();
    }


    public void initiateRecording(Connection networkManager) {
        Channel channel = ((NetworkManagerAccessor) networkManager).getChannel();
        if (channel.pipeline().get(ReplayHandler.PACKET_HANDLER_NAME) != null) return;
        if (channel.hasAttr(ATTR_CHECKED)) return;
        channel.attr(ATTR_CHECKED).set(null);
        connectionEventHandler.onConnectedToServerEvent(networkManager);
    }

    public ConnectionEventHandler getConnectionEventHandler() {
        return connectionEventHandler;
    }
}
