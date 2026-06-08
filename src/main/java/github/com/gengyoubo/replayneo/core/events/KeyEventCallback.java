package github.com.gengyoubo.replayneo.core.events;

import github.com.gengyoubo.replayneo.function.KeyInput;
import github.com.gengyoubo.replayneo.core.utils.Event;

public interface KeyEventCallback {
    Event<KeyEventCallback> EVENT = Event.create((listeners) ->
            (keyInput, action) -> {
                for (KeyEventCallback listener : listeners) {
                    if (listener.onKeyEvent(keyInput, action)) {
                        return true;
                    }
                }
                return false;
            }
    );

    int ACTION_RELEASE = org.lwjgl.glfw.GLFW.GLFW_RELEASE;
    int ACTION_PRESS = org.lwjgl.glfw.GLFW.GLFW_PRESS;

    boolean onKeyEvent(KeyInput keyInput, int action);
}
