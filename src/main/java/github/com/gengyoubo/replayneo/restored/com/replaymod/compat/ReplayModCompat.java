package github.com.gengyoubo.replayneo.restored.com.replaymod.compat;

import github.com.gengyoubo.replayneo.restored.com.replaymod.compat.optifine.DisableFastRender;
import github.com.gengyoubo.replayneo.core.Module;
import org.apache.logging.log4j.Logger;



import github.com.gengyoubo.replayneo.restored.com.replaymod.compat.shaders.ShaderBeginRender;

public class ReplayModCompat implements Module {
    public static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    @Override
    public void initClient() {
        new ShaderBeginRender().register();
        new DisableFastRender().register();
    }

}
