package github.com.gengyoubo.replayneo.platform.feature.editor;

import github.com.gengyoubo.replayneo.api.Module;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.platform.feature.editor.handler.GuiHandler;
import org.apache.logging.log4j.Logger;

public class ReplayModEditor implements Module {
    { instance = this; }
    public static ReplayModEditor instance;

    private final ReplayMod core;

    public static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    public ReplayModEditor(ReplayMod core) {
        this.core = core;
    }

    @Override
    public void initClient() {
        new GuiHandler().register();
    }

    public ReplayMod getCore() {
        return core;
    }
}
