package github.com.gengyoubo.replayneo.api.input;

import github.com.gengyoubo.replayneo.function.KeyInput;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReplayKeyBindingRegistry {
    Binding registerKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInReplay);

    Binding registerRepeatedKeyBinding(String name, int keyCode, Runnable whenPressed, boolean onlyInReplay);

    void registerRaw(int keyCode, Function<KeyInput, Boolean> whenPressed);

    Map<String, Binding> getBindings();

    interface Binding {
        String name();

        String getBoundKey();

        boolean isBound();

        boolean isDown();

        void trigger();

        void registerAutoActivationSupport(boolean active, Consumer<Boolean> update);

        boolean supportsAutoActivation();

        boolean isAutoActivating();

        void setAutoActivating(boolean active);
    }
}
