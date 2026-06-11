package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.debug.ReplaySoundDebug;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void replayneo$logReplaySound(SoundInstance sound, CallbackInfo ci) {
        ReplaySoundDebug.logReplaySound("SoundEngine.play", sound);
    }

    @Inject(method = "playDelayed(Lnet/minecraft/client/resources/sounds/SoundInstance;I)V", at = @At("HEAD"))
    private void replayneo$logDelayedReplaySound(SoundInstance sound, int delay, CallbackInfo ci) {
        ReplaySoundDebug.logReplaySound("SoundEngine.playDelayed+" + delay, sound);
    }
}
