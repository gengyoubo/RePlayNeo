package github.com.gengyoubo.replayneo.api.events;

import github.com.gengyoubo.replayneo.core.utils.Event;

public interface PostRenderCallback {
    Event<PostRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PostRenderCallback listener : listeners) {
                    listener.postRender();
                }
            }
    );

    void postRender();
}
