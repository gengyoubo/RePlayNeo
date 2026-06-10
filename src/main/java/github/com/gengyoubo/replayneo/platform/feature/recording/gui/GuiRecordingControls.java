package github.com.gengyoubo.replayneo.platform.feature.recording.gui;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.utils.Utils;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.feature.editor.gui.MarkerProcessor;
import github.com.gengyoubo.replayneo.platform.feature.recording.ReplayModRecording;
import github.com.gengyoubo.replayneo.platform.feature.recording.handler.RecordingEventHandler;
import github.com.gengyoubo.replayneo.platform.feature.recording.packet.PacketListener;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.core.gui.container.GuiPanel;
import github.com.gengyoubo.replayneo.platform.gui.container.VanillaGuiScreen;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiButton;
import github.com.gengyoubo.replayneo.core.gui.layout.CustomLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.HorizontalLayout;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.callbacks.InitScreenCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class GuiRecordingControls extends EventRegistrations {
    private ReplayMod core;
    private PacketListener packetListener;
    private boolean paused;
    private boolean stopped;
    private volatile boolean closed;

    private final GuiPanel panel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(4));

    private final GuiButton buttonPauseResume = new GuiButton(panel).onClick(() -> {
        if (Utils.ifMinimalModeDoPopup(panel, () -> {})) return;
        if (paused) {
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_END_CUT);
            spawnRecordingPlayer();
        } else {
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT);
        }
        paused = !paused;
        playToggleSound();
        updateState();
    }).setSize(98, 20);

    private final GuiButton buttonStartStop = new GuiButton(panel).onClick(() -> {
        if (Utils.ifMinimalModeDoPopup(panel, () -> {})) return;
        if (stopped) {
            paused = false;
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_END_CUT);
            spawnRecordingPlayer();
            core.printInfoToChat("replaymod.chat.recordingstarted");
        } else {
            int timestamp = (int) packetListener.getCurrentDuration();
            if (!paused) {
                packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT, timestamp);
            }
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_SPLIT, timestamp + 1);
        }
        stopped = !stopped;
        playToggleSound();
        updateState();
    }).setSize(98, 20);

    public GuiRecordingControls(ReplayMod core, PacketListener packetListener, boolean autoStart) {
        this.core = core;
        this.packetListener = packetListener;

        paused = stopped = !autoStart;

        updateState();
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (panel.getContainer() != null) {
            panel.getContainer().removeElement(panel);
        }
        unregister();
        panel.setDisabled();
        buttonPauseResume.setDisabled();
        buttonStartStop.setDisabled();
    }

    private void spawnRecordingPlayer() {
        RecordingEventHandler handler = ReplayModRecording.instance.getConnectionEventHandler().getRecordingEventHandler();
        if (handler != null) {
            handler.spawnRecordingPlayer();
        }
    }

    private void updateState() {
        buttonPauseResume.setI18nLabel("replaymod.gui.recording." + (paused ? "resume" : "pause"));
        buttonStartStop.setI18nLabel("replaymod.gui.recording." + (stopped ? "start" : "stop"));

        buttonPauseResume.setEnabled(!stopped);
    }

    private void playToggleSound() {
        core.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FLINTANDSTEEL_USE, 1.0F));
    }

    { on(InitScreenCallback.EVENT, this::injectIntoIngameMenu); }
    private void injectIntoIngameMenu(Screen guiScreen,
                                      Collection<AbstractButton> buttonList
    ) {
        if (!shouldShowOn(guiScreen)) {
            return;
        }
        if (buttonList.isEmpty()) {
            return; // menu-less pause (F3+Esc)
        }
        Function<Integer, Integer> yPos =
                MCVer.findButton(buttonList, "menu.returnToMenu", 1).or(() -> MCVer.findButton(buttonList, "menu.disconnect", 1))
                        .<Function<Integer, Integer>>map(it -> (height) -> it.getY())
                        .orElse((height) -> height / 4 + 120 - 16);
        VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(guiScreen);
        if (panel.getContainer() != null && panel.getContainer() != vanillaGui) {
            panel.getContainer().removeElement(panel);
        }
        vanillaGui.setLayout(new CustomLayout<github.com.gengyoubo.replayneo.platform.gui.container.GuiScreen>(vanillaGui.getLayout()) {
            @Override
            protected void layout(github.com.gengyoubo.replayneo.platform.gui.container.GuiScreen container, int width, int height) {
                if (!shouldShowOn(guiScreen)) {
                    size(panel, 0, 0);
                    pos(panel, -10000, -10000);
                    if (panel.getContainer() != null) {
                        panel.getContainer().removeElement(panel);
                    }
                    return;
                }
                pos(panel, width / 2 - 100, yPos.apply(height) + 16 + 8);
            }
        }).addElements(null, panel);
    }

    private boolean shouldShowOn(Screen guiScreen) {
        if (closed || packetListener == null || guiScreen == null || guiScreen.getClass() != PauseScreen.class) {
            return false;
        }

        Minecraft minecraft = core.getMinecraft();
        if (minecraft.screen != guiScreen || minecraft.level == null || minecraft.player == null || minecraft.getConnection() == null) {
            return false;
        }

        return ReplayModReplay.instance == null || ReplayModReplay.instance.getReplayHandler() == null;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isStopped() {
        return stopped;
    }
}
