package github.com.gengyoubo.replayneo.platform.addon.youtube;

import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.api.Extra;
import github.com.gengyoubo.replayneo.platform.gui.container.AbstractGuiScreen;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiButton;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screens.Screen;

public class YoutubeUpload extends EventRegistrations implements Extra {
    @Override
    public void register(RePlayCore mod) {
        register();
    }

    { on(InitScreenCallback.EVENT, ((screen, buttons) -> onGuiOpen(screen))); }
    private void onGuiOpen(Screen vanillaGui) {
        AbstractGuiScreen<?> abstractScreen = github.com.gengyoubo.replayneo.platform.gui.container.GuiScreen.from(vanillaGui);
    }

    private static class YoutubeButton extends GuiButton {}
}
