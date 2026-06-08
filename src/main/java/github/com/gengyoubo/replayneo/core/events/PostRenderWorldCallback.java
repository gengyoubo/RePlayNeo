package github.com.gengyoubo.replayneo.core.events;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.core.utils.Event;

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            (PoseStack matrixStack) -> {
                for (PostRenderWorldCallback listener : listeners) {
                    listener.postRenderWorld(matrixStack);
                }
            }
    );

    void postRenderWorld(PoseStack matrixStack);
}
