package github.com.gengyoubo.replayneo.feature.editor.handler;

import com.replaymod.core.utils.Utils;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.editor.gui.GuiEditReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
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
