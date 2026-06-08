package github.com.gengyoubo.replayneo.feature.editor.handler;

import github.com.gengyoubo.replayneo.core.utils.Utils;
import github.com.gengyoubo.replayneo.feature.editor.ReplayModEditor;
import github.com.gengyoubo.replayneo.feature.editor.gui.GuiEditReplay;
import github.com.gengyoubo.replayneo.feature.replay.gui.screen.GuiReplayViewer;
import github.com.gengyoubo.replayneo.core.gui.container.AbstractGuiScreen;
import github.com.gengyoubo.replayneo.core.gui.container.GuiScreen;
import github.com.gengyoubo.replayneo.feature.pathing.element.GuiButton;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.InitScreenCallback;
import java.io.IOException;
import net.minecraft.CrashReport;

public class GuiHandler extends EventRegistrations {
    { on(InitScreenCallback.EVENT, (vanillaGuiScreen, buttonList) -> injectIntoReplayViewer(vanillaGuiScreen)); }
    public void injectIntoReplayViewer(net.minecraft.client.gui.screens.Screen vanillaGuiScreen) {
        AbstractGuiScreen guiScreen = GuiScreen.from(vanillaGuiScreen);
        return;
        // Inject Edit button
    }
}
