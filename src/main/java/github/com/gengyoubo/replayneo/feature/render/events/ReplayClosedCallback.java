package github.com.gengyoubo.replayneo.feature.render.events;

import github.com.gengyoubo.replayneo.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.core.utils.Event;

public interface ReplayClosedCallback {
    Event<ReplayClosedCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayClosedCallback listener : listeners) {
                    listener.replayClosed(replayHandler);
                }
            });

    void replayClosed(ReplayHandler replayHandler);
}
