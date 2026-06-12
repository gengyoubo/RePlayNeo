package github.com.gengyoubo.replayneo.api.callbacks;

import github.com.gengyoubo.replayneo.core.utils.Event;
import net.minecraft.client.gui.GuiGraphics;

public interface RenderHudCallback {
    Event<RenderHudCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for (RenderHudCallback listener : listeners) {
                    listener.renderHud(stack, partialTicks);
                }
            }
    );

    void renderHud(GuiGraphics context, float partialTicks);
}
