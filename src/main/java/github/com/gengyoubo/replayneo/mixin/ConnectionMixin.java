package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.recording.handler.RecordingEventHandler;
import github.com.gengyoubo.replayneo.feature.recording.packet.PacketListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    private Channel channel;

    @Shadow
    public abstract PacketFlow getReceiving();

    @Shadow
    public abstract boolean isMemoryConnection();

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void replayneo$recordMemoryPacket(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        RecordingEventHandler handler = replayneo$getMemoryRecordingHandler();
        if (handler != null) {
            handler.onPacket((Connection) (Object) this, packet);
        }
    }

    @Inject(method = "channelInactive", at = @At("HEAD"))
    private void replayneo$closeMemoryRecorder(ChannelHandlerContext ctx, CallbackInfo ci) {
        RecordingEventHandler handler = replayneo$getMemoryRecordingHandler();
        if (handler != null) {
            handler.onDisconnected();
        }
    }

    @Unique
    private RecordingEventHandler replayneo$getMemoryRecordingHandler() {
        if (!this.isMemoryConnection() || this.getReceiving() != PacketFlow.CLIENTBOUND) {
            return null;
        }
        if (channel.pipeline().get(PacketListener.RAW_RECORDER_KEY) != null) {
            return null;
        }

        LevelRenderer levelRenderer = MCVer.getMinecraft().levelRenderer;
        if (!(levelRenderer instanceof RecordingEventHandler.RecordingEventSender sender)) {
            return null;
        }

        return sender.getRecordingEventHandler();
    }

    @Inject(method = "setupCompression", at = @At("RETURN"))
    private void ensureReplayModRecorderIsAfterDecompress(CallbackInfo ci) {
        ChannelHandler recorder = null;
        for (Map.Entry<String, ChannelHandler> entry : channel.pipeline()) {
            String key = entry.getKey();
            if (PacketListener.RAW_RECORDER_KEY.equals(key)) {
                recorder = entry.getValue();
            }
            if (PacketListener.DECOMPRESS_KEY.equals(key)) {
                if (recorder != null) {
                    // If we've already found the recorder, then that means decompress is after recorder. That's no good
                    // because it means the recorder is getting compressed packets, we need to move the recorder.
                    channel.pipeline().remove(recorder);
                    channel.pipeline().addBefore(PacketListener.DECODER_KEY, PacketListener.RAW_RECORDER_KEY, recorder);
                    return;
                }
            }
        }
    }
}
