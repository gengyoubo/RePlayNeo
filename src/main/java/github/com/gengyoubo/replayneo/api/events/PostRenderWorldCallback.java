package github.com.gengyoubo.replayneo.api.events;

import github.com.gengyoubo.replayneo.api.render.WorldRenderContext;
import github.com.gengyoubo.replayneo.core.utils.Event;

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            (WorldRenderContext context) -> {
                for (PostRenderWorldCallback listener : listeners) {
                    listener.postRenderWorld(context);
                }
            }
    );

    void postRenderWorld(WorldRenderContext context);
}
