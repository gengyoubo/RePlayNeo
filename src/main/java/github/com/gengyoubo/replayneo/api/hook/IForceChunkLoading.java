package github.com.gengyoubo.replayneo.api.hook;

import github.com.gengyoubo.replayneo.platform.render.hooks.ForceChunkLoadingHook;
import net.minecraft.client.renderer.LevelRenderer;

public interface IForceChunkLoading {
    void replayModRender_setHook(ForceChunkLoadingHook hook);

    static IForceChunkLoading from(LevelRenderer worldRenderer) {
        return (IForceChunkLoading) worldRenderer;
    }
}
