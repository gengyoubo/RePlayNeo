package github.com.gengyoubo.replayneo.feature.render.events;

import github.com.gengyoubo.replayneo.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.core.utils.Event;

import java.io.IOException;

public interface ReplayOpenedCallback {
    Event<ReplayOpenedCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayOpenedCallback listener : listeners) {
                    listener.replayOpened(replayHandler);
                }
            });

    void replayOpened(ReplayHandler replayHandler) throws IOException;
}
