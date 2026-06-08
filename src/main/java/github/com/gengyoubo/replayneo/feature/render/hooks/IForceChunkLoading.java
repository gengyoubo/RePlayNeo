package github.com.gengyoubo.replayneo.feature.render.hooks;

import net.minecraft.client.renderer.LevelRenderer;

public interface IForceChunkLoading {
    void replayModRender_setHook(ForceChunkLoadingHook hook);

    static IForceChunkLoading from(LevelRenderer worldRenderer) {
        return (IForceChunkLoading) worldRenderer;
    }
}
