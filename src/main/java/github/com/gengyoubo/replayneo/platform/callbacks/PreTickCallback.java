package github.com.gengyoubo.replayneo.platform.callbacks;

import github.com.gengyoubo.replayneo.core.utils.Event;

public interface PreTickCallback {
    Event<PreTickCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreTickCallback listener : listeners) {
                    listener.preTick();
                }
            }
    );

    void preTick();
}
