package github.com.gengyoubo.replayneo;

import github.com.gengyoubo.replayneo.core.ReplayModBackend;
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

    private final ReplayModBackend backend;

    public RePlayNeo() {
        instance = this;
        LOGGER.info("Loading {}", MOD_NAME);
        this.backend = new ReplayModBackend();
    }

    public ReplayModBackend getBackend() {
        return backend;
    }
}
