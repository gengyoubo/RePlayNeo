package github.com.gengyoubo.replayneo.platform.addon;

import github.com.gengyoubo.replayneo.api.other.Extra;
import github.com.gengyoubo.replayneo.api.other.Module;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.platform.addon.advancedscreenshots.AdvancedScreenshots;
import github.com.gengyoubo.replayneo.platform.addon.playeroverview.PlayerOverview;
import github.com.gengyoubo.replayneo.platform.addon.youtube.YoutubeUpload;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReplayModExtras implements Module {
    { instance = this; }
    public static ReplayModExtras instance;

    private static final List<Class<? extends Extra>> builtin = Arrays.asList(
            AdvancedScreenshots.class,
            PlayerOverview.class,
            YoutubeUpload.class,
            FullBrightness.class,
            QuickMode.class,
            HotkeyButtons.class
    );

    private final Map<Class<? extends Extra>, Extra> instances = new HashMap<>();

    public static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    public ReplayModExtras(RePlayCore core) {
        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void initClient() {
        for (Class<? extends Extra> cls : builtin) {
            try {
                Extra extra = cls.newInstance();
                extra.register(RePlayCore.instance);
                instances.put(cls, extra);
            } catch (Throwable t) {
                LOGGER.warn("Failed to load extra {}: ", cls.getName(), t);
            }
        }
    }

    public <T extends Extra> Optional<T> get(Class<T> cls) {
        return Optional.ofNullable(instances.get(cls)).map(cls::cast);
    }
}
