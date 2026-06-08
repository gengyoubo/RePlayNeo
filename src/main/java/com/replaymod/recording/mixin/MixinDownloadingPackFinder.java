package com.replaymod.recording.mixin;

import com.replaymod.recording.packet.ResourcePackRecorder;
import de.johni0702.minecraft.gui.utils.Consumer;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DownloadedPackSource.class)
public abstract class MixinDownloadingPackFinder implements ResourcePackRecorder.IDownloadingPackFinder {
    private Consumer<File> requestCallback;

    @Override
    public void setRequestCallback(Consumer<File> callback) {
        requestCallback = callback;
    }

    @Inject(method = "loadServerPack(Ljava/io/File;Lnet/minecraft/server/packs/repository/PackSource;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private void recordDownloadedPack(
            File file,
            PackSource arg,
            CallbackInfoReturnable ci
    ) {
        if (requestCallback != null) {
            requestCallback.consume(file);
            requestCallback = null;
        }
    }
}
