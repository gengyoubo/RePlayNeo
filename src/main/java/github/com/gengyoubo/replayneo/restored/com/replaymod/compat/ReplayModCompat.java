package github.com.gengyoubo.replayneo.restored.com.replaymod.compat;

import com.replaymod.compat.optifine.DisableFastRender;
import com.replaymod.core.Module;
import org.apache.logging.log4j.Logger;



import com.replaymod.compat.shaders.ShaderBeginRender;

public class ReplayModCompat implements Module {
    public static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    @Override
    public void initClient() {
        new ShaderBeginRender().register();
        new DisableFastRender().register();
    }

}
