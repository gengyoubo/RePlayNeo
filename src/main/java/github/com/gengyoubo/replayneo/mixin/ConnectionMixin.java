package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.feature.recording.handler.RecordingEventHandler;
import github.com.gengyoubo.replayneo.platform.feature.recording.packet.PacketListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.RunningOnDifferentThreadException;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Unique
    private static final Logger replayneo$logger = RePlayNeo.LOGGER;

    @Shadow
    private Channel channel;

    @Shadow
    public abstract PacketFlow getReceiving();

    @Shadow
    public abstract boolean isMemoryConnection();

    @Shadow
    public abstract void disconnect(Component reason);

    @Shadow
    protected abstract ConnectionProtocol getCurrentProtocol();

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void replayneo$blockWrongLocalOutboundPacket(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) {
        if (!replayneo$isWrongLocalOutboundPacket(packet)) {
            return;
        }

        replayneo$logger.error(
                "Blocked wrong-direction local outbound packet. flow={}, sending={}, protocol={}, packet={}, channel={}, open={}, active={}, local={}, remote={}, pipeline={}",
                replayneo$safe(() -> String.valueOf(this.getReceiving())),
                replayneo$safe(() -> String.valueOf(this.getReceiving().getOpposite())),
                replayneo$safe(() -> String.valueOf(this.getCurrentProtocol())),
                replayneo$className(packet),
                replayneo$safe(() -> String.valueOf(channel.id())),
                replayneo$safe(() -> String.valueOf(channel.isOpen())),
                replayneo$safe(() -> String.valueOf(channel.isActive())),
                replayneo$safe(() -> String.valueOf(channel.localAddress())),
                replayneo$safe(() -> String.valueOf(channel.remoteAddress())),
                replayneo$pipelineNames(channel.pipeline()),
                new IllegalStateException("Wrong-direction local outbound packet")
        );
        this.disconnect(replayneo$disconnectReason(packet));
        ci.cancel();
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void replayneo$logOriginalException(ChannelHandlerContext ctx, Throwable throwable, CallbackInfo ci) {
        replayneo$logger.error(
                "Connection exceptionCaught root cause. flow={}, sending={}, memory={}, protocol={}, listener={}, channel={}, open={}, active={}, local={}, remote={}, pipeline={}",
                replayneo$safe(() -> String.valueOf(this.getReceiving())),
                replayneo$safe(() -> String.valueOf(this.getReceiving().getOpposite())),
                replayneo$safe(() -> String.valueOf(this.isMemoryConnection())),
                replayneo$safe(() -> String.valueOf(this.getCurrentProtocol())),
                replayneo$safe(() -> replayneo$className(((Connection) (Object) this).getPacketListener())),
                replayneo$safe(() -> String.valueOf(channel.id())),
                replayneo$safe(() -> String.valueOf(channel.isOpen())),
                replayneo$safe(() -> String.valueOf(channel.isActive())),
                replayneo$safe(() -> String.valueOf(channel.localAddress())),
                replayneo$safe(() -> String.valueOf(channel.remoteAddress())),
                replayneo$pipelineNames(channel.pipeline()),
                throwable
        );
    }

    @Redirect(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
    private void replayneo$handlePacketWithDiagnostics(Packet<?> packet, net.minecraft.network.PacketListener packetListener) {
        try {
            replayneo$handlePacketUnchecked(packet, packetListener);
        } catch (RunningOnDifferentThreadException exception) {
            throw exception;
        } catch (Throwable throwable) {
            replayneo$logPacketHandlingFailure(packet, packetListener, throwable);
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (throwable instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(throwable);
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void replayneo$recordMemoryPacket(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        RecordingEventHandler handler = replayneo$getMemoryRecordingHandler();
        if (handler != null) {
            try {
                handler.onPacket((Connection) (Object) this, packet);
            } catch (Throwable throwable) {
                replayneo$logger.error(
                        "Local memory recording failed while observing packet. flow={}, protocol={}, packet={}, channel={}, open={}, active={}, local={}, remote={}, pipeline={}",
                        replayneo$safe(() -> String.valueOf(this.getReceiving())),
                        replayneo$safe(() -> String.valueOf(this.getCurrentProtocol())),
                        replayneo$className(packet),
                        replayneo$safe(() -> String.valueOf(channel.id())),
                        replayneo$safe(() -> String.valueOf(channel.isOpen())),
                        replayneo$safe(() -> String.valueOf(channel.isActive())),
                        replayneo$safe(() -> String.valueOf(channel.localAddress())),
                        replayneo$safe(() -> String.valueOf(channel.remoteAddress())),
                        replayneo$pipelineNames(channel.pipeline()),
                        throwable
                );
            }
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

    @Unique
    private boolean replayneo$isWrongLocalOutboundPacket(Packet<?> packet) {
        if (!this.isMemoryConnection()) {
            return false;
        }

        ConnectionProtocol protocol = this.getCurrentProtocol();
        PacketFlow receiving = this.getReceiving();
        PacketFlow sending = receiving.getOpposite();
        return replayneo$getPacketId(protocol, sending, packet) < 0
                && replayneo$getPacketId(protocol, receiving, packet) >= 0;
    }

    @Unique
    private static int replayneo$getPacketId(ConnectionProtocol protocol, PacketFlow flow, Packet<?> packet) {
        try {
            return protocol.getPacketId(flow, packet);
        } catch (Throwable throwable) {
            return -1;
        }
    }

    @Unique
    private static Component replayneo$disconnectReason(Packet<?> packet) {
        if (packet instanceof ClientboundLoginDisconnectPacket loginDisconnectPacket) {
            return loginDisconnectPacket.getReason();
        }
        if (packet instanceof ClientboundDisconnectPacket disconnectPacket) {
            return disconnectPacket.getReason();
        }
        return Component.translatable("multiplayer.disconnect.invalid_packet");
    }

    @Unique
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void replayneo$handlePacketUnchecked(Packet packet, net.minecraft.network.PacketListener packetListener) {
        packet.handle(packetListener);
    }

    @Unique
    private void replayneo$logPacketHandlingFailure(Packet<?> packet, net.minecraft.network.PacketListener packetListener, Throwable throwable) {
        replayneo$logger.error(
                "Connection packet handling failed. flow={}, memory={}, protocol={}, packet={}, listener={}, channel={}, open={}, active={}, local={}, remote={}, pipeline={}",
                replayneo$safe(() -> String.valueOf(this.getReceiving())),
                replayneo$safe(() -> String.valueOf(this.isMemoryConnection())),
                replayneo$safe(() -> String.valueOf(channel.attr(Connection.ATTRIBUTE_PROTOCOL).get())),
                replayneo$className(packet),
                replayneo$className(packetListener),
                replayneo$safe(() -> String.valueOf(channel.id())),
                replayneo$safe(() -> String.valueOf(channel.isOpen())),
                replayneo$safe(() -> String.valueOf(channel.isActive())),
                replayneo$safe(() -> String.valueOf(channel.localAddress())),
                replayneo$safe(() -> String.valueOf(channel.remoteAddress())),
                replayneo$pipelineNames(channel.pipeline()),
                throwable
        );
    }

    @Unique
    private static String replayneo$className(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }

    @Unique
    private static String replayneo$pipelineNames(ChannelPipeline pipeline) {
        try {
            return String.join(" -> ", pipeline.names());
        } catch (Throwable throwable) {
            return "<failed: " + throwable.getClass().getName() + ": " + throwable.getMessage() + ">";
        }
    }

    @Unique
    private static String replayneo$safe(ValueSupplier supplier) {
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            return "<failed: " + throwable.getClass().getName() + ": " + throwable.getMessage() + ">";
        }
    }

    @Unique
    private interface ValueSupplier {
        String get();
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
