package github.com.gengyoubo.replayneo.api.callbacks;

import github.com.gengyoubo.replayneo.core.utils.Event;
import net.minecraft.client.gui.GuiGraphics;

public interface PostRenderScreenCallback {
    Event<PostRenderScreenCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for (PostRenderScreenCallback listener : listeners) {
                    listener.postRenderScreen(stack, partialTicks);
                }
            }
    );

    void postRenderScreen(GuiGraphics context, float partialTicks);
}
