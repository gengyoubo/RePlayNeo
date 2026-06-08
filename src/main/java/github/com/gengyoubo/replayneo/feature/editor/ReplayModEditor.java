package github.com.gengyoubo.replayneo.feature.editor;

import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.editor.handler.GuiHandler;
import org.apache.logging.log4j.Logger;

public class ReplayModEditor implements Module {
    { instance = this; }
    public static final ReplayModEditor instance;

    private final ReplayMod core;

    public static final Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

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
