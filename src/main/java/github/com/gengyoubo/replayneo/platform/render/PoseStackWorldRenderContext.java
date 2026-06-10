package github.com.gengyoubo.replayneo.platform.render;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.api.render.WorldRenderContext;

public record PoseStackWorldRenderContext(PoseStack poseStack) implements WorldRenderContext {
    @Override
    public <T> T nativePoseStack(Class<T> type) {
        return type.cast(poseStack);
    }
}
