package com.replaymod.render.mixin;

import com.replaymod.compat.shaders.ShaderReflection;
import com.replaymod.render.hooks.ForceChunkLoadingHook;
import com.replaymod.render.hooks.IForceChunkLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(LevelRenderer.class)
public abstract class Mixin_ForceChunkLoading implements IForceChunkLoading {
    private ForceChunkLoadingHook replayModRender_hook;

    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender_hook = hook;
    }

    @Shadow private Set<ChunkRenderDispatcher.RenderChunk> chunksToRebuild;

    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;

    @Shadow private boolean needsTerrainUpdate;

    @Shadow protected abstract void setupTerrain(Camera camera_1, Frustum frustum_1, boolean boolean_1, int int_1, boolean boolean_2);

    @Shadow private int frame;

    private boolean passThrough;
    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void forceAllChunks(Camera camera_1, Frustum frustum_1, boolean boolean_1, int int_1, boolean boolean_2, CallbackInfo ci) throws IllegalAccessException {
        if (replayModRender_hook == null) {
            return;
        }
        if (passThrough) {
            return;
        }
        if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
            return;
        }
        ci.cancel();

        passThrough = true;
        try {
            do {
                // Determine which chunks shall be visible
                setupTerrain(camera_1, frustum_1, boolean_1, frame++, boolean_2);

                // Schedule all chunks which need rebuilding (we schedule even important rebuilds because we wait for
                // all of them anyway and this way we can take advantage of threading)
                for (ChunkRenderDispatcher.RenderChunk builtChunk : this.chunksToRebuild) {
                    // MC sometimes schedules invalid chunks when you're outside of loaded chunks (e.g. y > 256)
                    if (builtChunk.hasAllNeighbors()) {
                        builtChunk.setDirty(this.chunkRenderDispatcher);
                    }
                    builtChunk.setNotDirty();
                }
                this.chunksToRebuild.clear();

                // Upload all chunks
                this.needsTerrainUpdate |= ((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.chunkRenderDispatcher).uploadEverythingBlocking();

                // Repeat until no more updates are needed
            } while (this.needsTerrainUpdate);
        } finally {
            passThrough = false;
        }
    }
}
