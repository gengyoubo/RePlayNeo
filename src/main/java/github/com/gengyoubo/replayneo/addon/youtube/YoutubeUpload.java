package github.com.gengyoubo.replayneo.addon.youtube;

import com.replaymod.core.ReplayMod;
import github.com.gengyoubo.replayneo.addon.Extra;
import com.replaymod.render.gui.GuiRenderingDone;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screens.Screen;

public class YoutubeUpload extends EventRegistrations implements Extra {
    @Override
    public void register(ReplayMod mod) {
        register();
    }

    { on(InitScreenCallback.EVENT, ((screen, buttons) -> onGuiOpen(screen))); }
    private void onGuiOpen(Screen vanillaGui) {
        AbstractGuiScreen<?> abstractScreen = de.johni0702.minecraft.gui.container.GuiScreen.from(vanillaGui);
    }

    private static class YoutubeButton extends GuiButton {}
}
