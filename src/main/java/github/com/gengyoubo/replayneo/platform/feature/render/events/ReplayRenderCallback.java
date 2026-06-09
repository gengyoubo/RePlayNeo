package github.com.gengyoubo.replayneo.platform.feature.render.events;

import github.com.gengyoubo.replayneo.platform.feature.render.rendering.VideoRenderer;
import github.com.gengyoubo.replayneo.core.utils.Event;

public interface ReplayRenderCallback {
    interface Pre {
        Event<Pre> EVENT = Event.create((listeners) ->
                (renderer) -> {
                    for (Pre listener : listeners) {
                        listener.beforeRendering(renderer);
                    }
                });

        void beforeRendering(VideoRenderer renderer);
    }

    interface Post {
        Event<Post> EVENT = Event.create((listeners) ->
                (renderer) -> {
                    for (Post listener : listeners) {
                        listener.afterRendering(renderer);
                    }
                });

        void afterRendering(VideoRenderer renderer);
    }
}
