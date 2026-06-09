package github.com.gengyoubo.replayneo.platform.feature.render.events;

import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.core.utils.Event;

import java.io.IOException;

public interface ReplayClosingCallback {
    Event<ReplayClosingCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayClosingCallback listener : listeners) {
                    listener.replayClosing(replayHandler);
                }
            });

    void replayClosing(ReplayHandler replayHandler) throws IOException;
}
