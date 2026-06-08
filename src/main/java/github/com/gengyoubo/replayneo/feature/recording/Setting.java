package github.com.gengyoubo.replayneo.feature.recording;

import com.replaymod.core.SettingsRegistry;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> RECORD_SINGLEPLAYER = make("recordSingleplayer", "recordsingleplayer");
    public static final Setting<Boolean> RECORD_SERVER = make("recordServer", "recordserver");
    public static final Setting<Boolean> INDICATOR = make("indicator", "indicator");
    public static final Setting<Boolean> AUTO_START_RECORDING = make("autoStartRecording", "autostartrecording");
    public static final Setting<Boolean> AUTO_POST_PROCESS = make("autoPostProcess", null);
    public static final Setting<Boolean> RENAME_DIALOG = make("renameDialog", "rename_recording_dialog");

    private static <T> Setting<T> make(String key, String displayName) {
        return new Setting<>(key, displayName, (T) true);
    }

    public Setting(String key, String displayString, T defaultValue) {
        super("recording", key, displayString == null ? null : "replaymod.gui.settings." + displayString, defaultValue);
    }
}
