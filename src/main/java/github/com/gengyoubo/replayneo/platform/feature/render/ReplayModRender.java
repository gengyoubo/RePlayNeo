package github.com.gengyoubo.replayneo.platform.feature.render;

import github.com.gengyoubo.replayneo.api.Module;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.utils.Utils;
import github.com.gengyoubo.replayneo.core.utils.RenderJob;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.platform.feature.render.events.ReplayClosedCallback;
import github.com.gengyoubo.replayneo.platform.feature.render.events.ReplayOpenedCallback;
import com.replaymod.replaystudio.replay.ReplayFile;
import github.com.gengyoubo.replayneo.platform.gui.container.VanillaGuiScreen;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;

public class ReplayModRender extends EventRegistrations implements Module {
    { instance = this; }
    public static ReplayModRender instance;

    private final ReplayMod core;

    public static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    private ReplayFile replayFile;
    private final List<RenderJob> renderQueue = new ArrayList<>();

    public ReplayModRender(ReplayMod core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    public ReplayMod getCore() {
        return core;
    }

    @Override
    public void initClient() {
        register();
    }

    public File getVideoFolder() {
        String path = core.getSettingsRegistry().get(Setting.RENDER_PATH);
        File folder = new File(path.startsWith("./") ? core.getMinecraft().gameDirectory : null, path);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Cannot create video folder."));
        }
        return folder;
    }

    public Path getRenderSettingsPath() {
        return core.getMinecraft().gameDirectory.toPath().resolve("config/replaymod-rendersettings.json");
    }

    public List<RenderJob> getRenderQueue() {
        return renderQueue;
    }

    { on(ReplayOpenedCallback.EVENT, this::onReplayOpened); }
    private void onReplayOpened(ReplayHandler replayHandler) {
        replayFile = replayHandler.getReplayFile();
        try {
            renderQueue.addAll(RenderJob.readQueue(replayFile));
        } catch (IOException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Reading timeline"));
        }
    }

    { on(ReplayClosedCallback.EVENT, replayHandler -> onReplayClosed()); }
    private void onReplayClosed() {
        renderQueue.clear();
        replayFile = null;
    }

    public void saveRenderQueue() {
        try {
            RenderJob.writeQueue(replayFile, renderQueue);
        } catch (IOException e) {
            e.printStackTrace();
            VanillaGuiScreen screen = VanillaGuiScreen.wrap(getCore().getMinecraft().screen);
            CrashReport report = CrashReport.forThrowable(e, "Reading timeline");
            Utils.error(LOGGER, screen, report, () -> {});
        }
    }
}
