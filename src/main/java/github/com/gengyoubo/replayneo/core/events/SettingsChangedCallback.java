package github.com.gengyoubo.replayneo.core.events;

import github.com.gengyoubo.replayneo.core.SettingsRegistry;
import github.com.gengyoubo.replayneo.core.utils.Event;

public interface SettingsChangedCallback {
    Event<SettingsChangedCallback> EVENT = Event.create((listeners) ->
            (registry, key) -> {
                for (SettingsChangedCallback listener : listeners) {
                    listener.onSettingsChanged(registry, key);
                }
            }
    );

    void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key);
}
