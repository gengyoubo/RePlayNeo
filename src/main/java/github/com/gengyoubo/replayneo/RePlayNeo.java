package github.com.gengyoubo.replayneo;

import github.com.gengyoubo.replayneo.api.ReplayRuntime;
import github.com.gengyoubo.replayneo.platform.ForgeReplayPlatform;
import github.com.gengyoubo.replayneo.platform.ForgeReplayRuntime;
import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;
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

    public RePlayNeo() {
        instance = this;
        LOGGER.info("Loading {}", MOD_NAME);
        ReplayPlatforms.install(new ForgeReplayPlatform());
        ForgeRenderSettingsDefaults.install();
        this.backend = new ForgeReplayRuntime();
    }

    public ReplayRuntime getBackend() {
        return backend;
    }
}
