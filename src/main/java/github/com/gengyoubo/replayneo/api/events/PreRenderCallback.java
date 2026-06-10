package github.com.gengyoubo.replayneo.api.events;

import github.com.gengyoubo.replayneo.core.utils.Event;

public interface PreRenderCallback {
    Event<PreRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreRenderCallback listener : listeners) {
                    listener.preRender();
                }
            }
    );

    void preRender();
}
