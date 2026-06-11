package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.debug.ReplaySoundDebug;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Inject(method = "startPlaying(Lnet/minecraft/sounds/Music;)V", at = @At("HEAD"))
    private void replayneo$logReplayMusicStart(Music music, CallbackInfo ci) {
        ReplaySoundDebug.logReplayMusic("MusicManager.startPlaying", music);
    }

    @Inject(method = "stopPlaying(Lnet/minecraft/sounds/Music;)V", at = @At("HEAD"))
    private void replayneo$logReplayMusicStop(Music music, CallbackInfo ci) {
        ReplaySoundDebug.logReplayMusic("MusicManager.stopPlaying", music);
    }
}
