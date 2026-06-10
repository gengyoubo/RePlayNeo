package github.com.gengyoubo.replayneo.platform.render.events;

import github.com.gengyoubo.replayneo.core.utils.Event;

public interface RenderSpectatorCrosshairCallback {
    Event<RenderSpectatorCrosshairCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (RenderSpectatorCrosshairCallback listener : listeners) {
                    Boolean state = listener.shouldRenderSpectatorCrosshair();
                    if (state != null) {
                        return state;
                    }
                }
                return null;
            }
    );

    Boolean shouldRenderSpectatorCrosshair();
}
