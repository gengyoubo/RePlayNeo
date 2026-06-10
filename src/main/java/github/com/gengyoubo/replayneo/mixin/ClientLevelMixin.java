package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.feature.recording.handler.RecordingEventHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;


@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {
    @Final
    @Shadow
    private Minecraft minecraft;

    @SuppressWarnings("ConstantConditions")
    protected ClientLevelMixin() {
        super(null, null, null, null, null, false, false, 0, 0);
    }

    @Unique
    private RecordingEventHandler replayModRecording_getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) this.minecraft.levelRenderer).getRecordingEventHandler();
    }

    // Sounds that are emitted by thePlayer no longer take the long way over the server
    // but are instead played directly by the client. The server only sends these sounds to
    // other clients so we have to record them manually.
    // E.g. Block place sounds
    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"))
    public void replayModRecording_recordClientSound(
            Player player,
            double x, double y, double z,
            Holder<SoundEvent> sound,
            SoundSource category,
            float volume, float pitch,
            long seed,
            CallbackInfo ci) {
        if (player == this.minecraft.player) {
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                // Sent to all other players in ServerWorldEventHandler#playSoundToAllNearExcept
                handler.onPacket(new ClientboundSoundPacket(
                        sound, category, x, y, z, volume, pitch
                        , seed
                ));
            }
        }
    }

    // Same goes for level events (also called effects). E.g. door open, block break, etc.
    @Inject(method = "levelEvent", at = @At("HEAD"))
    private void playLevelEvent(
            Player player,
            int type, BlockPos pos, int data, CallbackInfo ci
    ) {
        if (player == this.minecraft.player) {
            // We caused this event, the server won't send it to us
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                handler.onClientEffect(type, pos, data);
            }
        }
    }
}
