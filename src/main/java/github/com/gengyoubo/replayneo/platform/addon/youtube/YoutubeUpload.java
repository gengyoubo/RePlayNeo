package github.com.gengyoubo.replayneo.platform.addon.youtube;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.api.Extra;
import github.com.gengyoubo.replayneo.core.gui.container.AbstractGuiScreen;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiButton;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screens.Screen;

public class YoutubeUpload extends EventRegistrations implements Extra {
    @Override
    public void register(ReplayMod mod) {
        register();
    }

    { on(InitScreenCallback.EVENT, ((screen, buttons) -> onGuiOpen(screen))); }
    private void onGuiOpen(Screen vanillaGui) {
        AbstractGuiScreen<?> abstractScreen = github.com.gengyoubo.replayneo.core.gui.container.GuiScreen.from(vanillaGui);
    }

    private static class YoutubeButton extends GuiButton {}
}
