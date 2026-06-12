package github.com.gengyoubo.replayneo.platform.feature.render;

import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.platform.gui.GuiUtils;

import github.com.gengyoubo.replayneo.api.other.Module;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.core.utils.RenderJob;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.api.events.ReplayClosedCallback;
import github.com.gengyoubo.replayneo.api.events.ReplayOpenedCallback;
import com.replaymod.replaystudio.replay.ReplayFile;
import github.com.gengyoubo.replayneo.platform.gui.container.VanillaGuiScreen;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
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
    public static ReplayModRender instance = null;

    private final RePlayCore core;

    public static final Logger LOGGER = RePlayNeo.LOGGER;

    private ReplayFile replayFile;
    private final List<RenderJob> renderQueue = new ArrayList<>();

    public ReplayModRender(RePlayCore core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    public RePlayCore getCore() {
        return core;
    }

    @Override
    public void initClient() {
        register();
    }

    public File getVideoFolder() {
        String path = core.getSettingsRegistry().get(Setting.RENDER_PATH);
        File baseFolder = path.startsWith("./") ? MCVer.getMinecraft().gameDirectory : null;
        File folder = new File(baseFolder, path);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Cannot create video folder."));
        }
        return folder;
    }

    public Path getRenderSettingsPath() {
        return MCVer.getMinecraft().gameDirectory.toPath().resolve("config/RePlayCore-rendersettings.json");
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
            VanillaGuiScreen screen = VanillaGuiScreen.wrap(MCVer.getMinecraft().screen);
            CrashReport report = CrashReport.forThrowable(e, "Reading timeline");
            GuiUtils.error(LOGGER, screen, report, () -> {});
        }
    }
}
