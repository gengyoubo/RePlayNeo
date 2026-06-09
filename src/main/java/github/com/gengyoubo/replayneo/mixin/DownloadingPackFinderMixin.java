package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.feature.recording.packet.ResourcePackRecorder;
import github.com.gengyoubo.replayneo.core.utils.Consumer;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DownloadedPackSource.class)
public abstract class DownloadingPackFinderMixin implements ResourcePackRecorder.IDownloadingPackFinder {
    @Unique
    private Consumer<File> rePlay$requestCallback;

    @Unique
    public void rePlay$setRequestCallback(Consumer<File> callback) {
        rePlay$requestCallback = callback;
    }

    @Inject(method = "setServerPack", at = @At("HEAD"))
    private void recordDownloadedPack(
            File file,
            PackSource arg,
            CallbackInfoReturnable<?> ci
    ) {
        if (rePlay$requestCallback != null) {
            rePlay$requestCallback.consume(file);
            rePlay$requestCallback = null;
        }
    }
}
