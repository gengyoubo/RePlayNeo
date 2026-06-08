package github.com.gengyoubo.replayneo.platform.callbacks;

import de.johni0702.minecraft.gui.utils.Event;
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
