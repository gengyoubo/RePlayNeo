package github.com.gengyoubo.replayneo.api.events;

import github.com.gengyoubo.replayneo.core.utils.Event;

public interface KeyBindingEventCallback {
    Event<KeyBindingEventCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (KeyBindingEventCallback listener : listeners) {
                    listener.onKeybindingEvent();
                }
            }
    );

    void onKeybindingEvent();
}
