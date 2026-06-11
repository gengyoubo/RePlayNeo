package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.debug.ReplaySoundDebug;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void replayneo$logReplaySound(SoundInstance sound, CallbackInfo ci) {
        ReplaySoundDebug.logReplaySound("SoundManager.play", sound);
    }
}
