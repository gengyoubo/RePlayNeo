package github.com.gengyoubo.replayneo.platform.feature.render.hooks;

import github.com.gengyoubo.replayneo.core.utils.Event;

public interface FogStateCallback {
    Event<FogStateCallback> EVENT = Event.create((listeners) ->
            (enabled) -> {
                for (FogStateCallback listener : listeners) {
                    listener.fogStateChanged(enabled);
                }
            }
    );

    void fogStateChanged(boolean enabled);
}
