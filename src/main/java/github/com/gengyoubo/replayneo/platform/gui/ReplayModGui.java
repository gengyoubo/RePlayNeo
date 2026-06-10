package github.com.gengyoubo.replayneo.platform.gui;

import github.com.gengyoubo.replayneo.api.Module;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.platform.gui.container.GuiScreen;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;

public class ReplayModGui implements Module {
    public static ReplayModGui instance;

    private final RePlayCore core;
    private final GuiBackgroundProcesses backgroundProcesses = new GuiBackgroundProcesses();

    public ReplayModGui(RePlayCore core) {
        instance = this;
        this.core = core;
    }

    @Override
    public void registerKeyBindings(ReplayKeyBindingRegistry registry) {
        registry.registerKeyBinding(
                "replaymod.input.settings",
                0,
                () -> new GuiReplaySettings(null, core.getSettingsRegistry()).display(),
                false
        );
    }

    @Override
    public void initClient() {
        backgroundProcesses.register();
        core.runPostStartup(() -> core.files.initialScan(this::recoverReplay));
    }

    public GuiBackgroundProcesses getBackgroundProcesses() {
        return backgroundProcesses;
    }

    private void recoverReplay(Path original) {
        new RestoreReplayGui(core, GuiScreen.wrap(Minecraft.getInstance().screen), original.toFile()).display();
    }
}
