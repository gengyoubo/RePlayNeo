package github.com.gengyoubo.replayneo.api.callbacks;

import github.com.gengyoubo.replayneo.core.utils.Event;
import net.minecraft.client.gui.screens.Screen;

public interface OpenGuiScreenCallback {
    Event<OpenGuiScreenCallback> EVENT = Event.create((listeners) ->
            (screen) -> {
                for (OpenGuiScreenCallback listener : listeners) {
                    listener.openGuiScreen(screen);
                }
            }
    );

    void openGuiScreen(Screen screen);
}
