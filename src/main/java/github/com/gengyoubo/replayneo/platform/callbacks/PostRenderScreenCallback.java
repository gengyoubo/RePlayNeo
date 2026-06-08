package github.com.gengyoubo.replayneo.platform.callbacks;

import de.johni0702.minecraft.gui.utils.Event;
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
