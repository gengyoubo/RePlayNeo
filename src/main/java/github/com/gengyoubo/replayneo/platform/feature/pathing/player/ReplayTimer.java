package github.com.gengyoubo.replayneo.platform.feature.pathing.player;

import github.com.gengyoubo.replayneo.core.utils.Event;
import net.minecraft.client.Timer;

/**
 * A timer that does not advance by itself.
 */
public class ReplayTimer extends Timer {
    public int ticksThisFrame;

    public ReplayTimer() {
        super(0, 0);
    }

    @Override
    // This should be handled by Remap but it isn't (was handled before a9724e3).
    public
    int
    advanceTime(
            long sysClock
    ) {
        UpdatedCallback.EVENT.invoker().onUpdate();
        return ticksThisFrame;
    }


    public interface UpdatedCallback {
        Event<UpdatedCallback> EVENT = Event.create((listeners) ->
                () -> {
                    for (UpdatedCallback listener : listeners) {
                        listener.onUpdate();
                    }
                }
        );
        void onUpdate();
    }
}
