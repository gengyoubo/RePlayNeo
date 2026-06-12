package github.com.gengyoubo.replayneo.platform.feature.editor;

import github.com.gengyoubo.replayneo.api.other.Module;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.platform.feature.editor.handler.GuiHandler;
import org.apache.logging.log4j.Logger;

public class ReplayModEditor implements Module {
    { instance = this; }
    public static ReplayModEditor instance;

    private final RePlayCore core;

    public static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    public ReplayModEditor(RePlayCore core) {
        this.core = core;
    }

    @Override
    public void initClient() {
        new GuiHandler().register();
    }

    public RePlayCore getCore() {
        return core;
    }
}
