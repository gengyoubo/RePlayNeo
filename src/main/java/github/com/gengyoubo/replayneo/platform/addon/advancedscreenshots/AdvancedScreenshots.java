package github.com.gengyoubo.replayneo.platform.addon.advancedscreenshots;

import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.api.Extra;

public class AdvancedScreenshots implements Extra {

    private RePlayCore mod;

    @Override
    public void register(RePlayCore mod) {
        this.mod = mod;
    }

    private static AdvancedScreenshots instance; { instance = this; }
    public static void take() {
        if (instance != null) {
            instance.takeScreenshot();
        }
    }

    private void takeScreenshot() {
        RePlayCore.instance.runLater(() -> new GuiCreateScreenshot(mod).open());
    }
}
