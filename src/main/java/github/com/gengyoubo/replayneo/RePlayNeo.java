package github.com.gengyoubo.replayneo;

import github.com.gengyoubo.replayneo.api.ReplayRuntime;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.platform.ForgeReplayPlatform;
import github.com.gengyoubo.replayneo.platform.ForgeReplayRuntime;
import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;
import github.com.gengyoubo.replayneo.platform.addon.ReplayModExtras;
import github.com.gengyoubo.replayneo.platform.feature.editor.ReplayModEditor;
import github.com.gengyoubo.replayneo.platform.feature.pathing.ReplayModSimplePathing;
import github.com.gengyoubo.replayneo.platform.feature.recording.ReplayModRecording;
import github.com.gengyoubo.replayneo.platform.feature.render.ReplayModRender;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.gui.ReplayModGui;
import github.com.gengyoubo.replayneo.platform.render.ForgeRenderSettingsDefaults;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RePlayNeo.MODID)
public class RePlayNeo {
    public static final String MODID = "replayneo";
    public static final String RESOURCE_NAMESPACE = MODID;
    public static final String MOD_NAME = "RePlayNeo";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static RePlayNeo instance;

    private final ReplayRuntime backend;
    private final RePlayCore core;

    public RePlayNeo() {
        instance = this;
        LOGGER.info("Loading {}", MOD_NAME);
        ReplayPlatforms.install(new ForgeReplayPlatform());
        ForgeRenderSettingsDefaults.install();
        this.backend = new ForgeReplayRuntime();
        this.core = new RePlayCore(backend);
        installModules(core);
    }

    public ReplayRuntime getBackend() {
        return backend;
    }

    public RePlayCore getCore() {
        return core;
    }

    private static void installModules(RePlayCore core) {
        core.addModule(new ReplayModGui(core));
        core.addModule(new ReplayModRecording(core));
        core.addModule(new ReplayModReplay(core));
        core.addModule(new ReplayModRender(core));
        core.addModule(new ReplayModSimplePathing(core));
        core.addModule(new ReplayModEditor(core));
        core.addModule(new ReplayModExtras(core));
        core.initModules();
    }
}
