package github.com.gengyoubo.replayneo.addon.advancedscreenshots;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.addon.Extra;

public class AdvancedScreenshots implements Extra {

    private ReplayMod mod;

    @Override
    public void register(ReplayMod mod) {
        this.mod = mod;
    }

    private static AdvancedScreenshots instance; { instance = this; }
    public static void take() {
        if (instance != null) {
            instance.takeScreenshot();
        }
    }

    private void takeScreenshot() {
        ReplayMod.instance.runLater(() -> new GuiCreateScreenshot(mod).open());
    }
}
