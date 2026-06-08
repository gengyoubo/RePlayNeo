package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.recording.ReplayModRecording;
import github.com.gengyoubo.replayneo.feature.recording.handler.RecordingEventHandler.RecordingEventSender;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
public abstract class ClientHandshakePacketListenerImplMixin {

    @Final @Shadow
    private Connection connection;

    @Inject(method = "handleCustomQuery", at=@At("HEAD"))
    private void earlyInitiateRecording(ClientboundCustomQueryPacket packet, CallbackInfo ci) {
        rePlay$initiateRecording(packet);
    }

    @Inject(method = "handleGameProfile", at=@At("HEAD"))
    private void lateInitiateRecording(ClientboundGameProfilePacket packet, CallbackInfo ci) {
        rePlay$initiateRecording(packet);
    }

    @Unique
    private void rePlay$initiateRecording(Packet<?> packet) {
        RecordingEventSender eventSender = (RecordingEventSender) MCVer.getMinecraft().levelRenderer;
        if (eventSender.getRecordingEventHandler() != null) {
            return; // already recording
        }
        ReplayModRecording.instance.initiateRecording(this.connection);
        if (eventSender.getRecordingEventHandler() != null) {
            eventSender.getRecordingEventHandler().onPacket(packet);
        }
    }
}
