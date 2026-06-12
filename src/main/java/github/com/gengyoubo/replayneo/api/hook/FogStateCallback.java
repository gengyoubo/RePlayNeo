package github.com.gengyoubo.replayneo.api.hook;

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
