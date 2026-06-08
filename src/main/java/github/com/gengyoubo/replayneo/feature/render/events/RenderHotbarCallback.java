package github.com.gengyoubo.replayneo.feature.render.events;

import de.johni0702.minecraft.gui.utils.Event;

public interface RenderHotbarCallback {
    Event<RenderHotbarCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (RenderHotbarCallback listener : listeners) {
                    Boolean state = listener.shouldRenderHotbar();
                    if (state != null) {
                        return state;
                    }
                }
                return null;
            }
    );

    Boolean shouldRenderHotbar();
}
