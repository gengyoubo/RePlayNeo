package github.com.gengyoubo.replayneo.core;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> NOTIFICATIONS = make();
    public static final Setting<String> RECORDING_PATH = advanced("recordingPath", "./replay_recordings/");
    public static final Setting<String> CACHE_PATH = advanced("cachePath", "./.replay_cache/");

    private static <T> Setting<T> make() {
        return new Setting<>("core", "notifications", "notifications", (T) Boolean.TRUE);
    }

    private static <T> Setting<T> advanced(String key, T defaultValue) {
        return new Setting<>("advanced", key, null, defaultValue);
    }

    public Setting(String category, String key, String displayString, T defaultValue) {
        super(category, key, displayString == null ? null : "replaymod.gui.settings." + displayString, defaultValue);
    }
}
