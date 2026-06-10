package github.com.gengyoubo.replayneo.platform.addon;
import github.com.gengyoubo.replayneo.api.Extra;
import github.com.gengyoubo.replayneo.platform.gui.ReplayTextures;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.versions.MCVer.Keyboard;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.feature.render.events.ReplayOpenedCallback;
import github.com.gengyoubo.replayneo.platform.feature.replay.gui.overlay.GuiReplayOverlay;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiImage;
import github.com.gengyoubo.replayneo.core.gui.layout.HorizontalLayout;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;

public class QuickMode extends EventRegistrations implements Extra {
    private ReplayModReplay module;

    private final GuiImage indicator = new GuiImage().setTexture(ReplayTextures.TEXTURE, 40, 100, 16, 16).setSize(16, 16);

    @Override
    public void register(final ReplayMod mod) {
        this.module = ReplayModReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.quickmode", Keyboard.KEY_Q, () -> {
            ReplayHandler replayHandler = module.getReplayHandler();
            if (replayHandler == null) {
                return;
            }
            replayHandler.getReplaySender().setSyncModeAndWait();
            mod.runLaterWithoutLock(() -> replayHandler.ensureQuickModeInitialized(() -> {
                boolean enabled = !replayHandler.isQuickMode();
                updateIndicator(replayHandler.getOverlay(), enabled);
                replayHandler.setQuickMode(enabled);
                replayHandler.getReplaySender().setAsyncMode(true);
            }));
        }, true);

        register();
    }

    {
        on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay(), replayHandler.isQuickMode()));
    }

    private void updateIndicator(GuiReplayOverlay overlay, boolean enabled) {
        if (enabled) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }
}
