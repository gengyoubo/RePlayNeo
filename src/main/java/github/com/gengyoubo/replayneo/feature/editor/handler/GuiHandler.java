package github.com.gengyoubo.replayneo.feature.editor.handler;

import github.com.gengyoubo.replayneo.core.gui.container.AbstractGuiScreen;
import github.com.gengyoubo.replayneo.core.gui.container.GuiScreen;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.InitScreenCallback;

public class GuiHandler extends EventRegistrations {
    { on(InitScreenCallback.EVENT, (vanillaGuiScreen, buttonList) -> injectIntoReplayViewer(vanillaGuiScreen)); }
    public void injectIntoReplayViewer(net.minecraft.client.gui.screens.Screen vanillaGuiScreen) {
        AbstractGuiScreen guiScreen = GuiScreen.from(vanillaGuiScreen);
        // Inject Edit button
    }
}
