package github.com.gengyoubo.replayneo.platform.feature.recording.handler;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.SettingsRegistry;
import github.com.gengyoubo.replayneo.platform.gui.GuiReplayButton;
import github.com.gengyoubo.replayneo.platform.feature.recording.ServerInfoExt;
import github.com.gengyoubo.replayneo.platform.feature.recording.Setting;
import github.com.gengyoubo.replayneo.platform.gui.container.GuiScreen;
import github.com.gengyoubo.replayneo.platform.gui.container.VanillaGuiScreen;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiButton;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiCheckbox;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiToggleButton;
import github.com.gengyoubo.replayneo.core.gui.layout.CustomLayout;
import github.com.gengyoubo.replayneo.platform.gui.popup.GuiInfoPopup;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;

public class GuiHandler extends EventRegistrations {

    private final ReplayMod mod;

    public GuiHandler(ReplayMod mod) {
        this.mod = mod;
    }

    { on(InitScreenCallback.EVENT, (screen, buttons) -> onGuiInit(screen)); }
    private void onGuiInit(Screen gui) {
        if (gui.getClass() == SelectWorldScreen.class) {
            addRecordingCheckbox(gui, Setting.RECORD_SINGLEPLAYER, "singleplayer");
        } else if (gui.getClass() == JoinMultiplayerScreen.class) {
            addRecordingCheckbox(gui, Setting.RECORD_SERVER, "server");
        }

        if (gui instanceof EditServerScreen editServerScreen) {
            VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(gui);
            GuiButton replayButton = new GuiReplayButton().onClick(() -> {
                ServerData serverInfo = editServerScreen.serverData;
                ServerInfoExt serverInfoExt = ServerInfoExt.from(serverInfo);
                Boolean state = serverInfoExt.getAutoRecording();
                GuiToggleButton<String> autoRecording = new GuiToggleButton<String>()
                        .setI18nLabel("replaymod.gui.settings.autostartrecording")
                        .setValues(
                                I18n.get("replaymod.gui.settings.default"),
                                I18n.get("options.off"),
                                I18n.get("options.on")
                        )
                        .setSelected(state == null ? 0 : state ? 2 : 1);
                autoRecording.onClick(() -> {
                    int selected = autoRecording.getSelected();
                    serverInfoExt.setAutoRecording(selected == 0 ? null : selected == 2);
                });
                GuiInfoPopup.open(vanillaGui, autoRecording);
            });
            vanillaGui.setLayout(new CustomLayout<GuiScreen>(vanillaGui.getLayout()) {
                @Override
                protected void layout(GuiScreen container, int width, int height) {
                    size(replayButton, 20, 20);
                    pos(replayButton, width - width(replayButton) - 5, 5);
                }
            }).addElements(null, replayButton);
        }
    }

    private void addRecordingCheckbox(Screen gui, Setting<Boolean> setting, String labelSuffix) {
        SettingsRegistry settingsRegistry = mod.getSettingsRegistry();

        GuiCheckbox recordingCheckbox = new GuiCheckbox()
                .setI18nLabel("replaymod.gui.settings.record" + labelSuffix)
                .setChecked(settingsRegistry.get(setting));
        recordingCheckbox.onClick(() -> {
            settingsRegistry.set(setting, recordingCheckbox.isChecked());
            settingsRegistry.save();
        });

        VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(gui);
        vanillaGui.setLayout(new CustomLayout<GuiScreen>(vanillaGui.getLayout()) {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                //size(recordingCheckbox, 200, 20);
                pos(recordingCheckbox, width - width(recordingCheckbox) - 5, 5);
            }
        }).addElements(null, recordingCheckbox);
    }
}
