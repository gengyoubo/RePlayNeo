package github.com.gengyoubo.replayneo.core.events;

import github.com.gengyoubo.replayneo.core.utils.Event;

public interface PreRenderHandCallback {
    Event<PreRenderHandCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreRenderHandCallback listener : listeners) {
                    if (listener.preRenderHand()) {
                        return true;
                    }
                }
                return false;
            }
    );

    boolean preRenderHand();
}
