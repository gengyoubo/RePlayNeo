package github.com.gengyoubo.replayneo.platform;

import github.com.gengyoubo.replayneo.api.ReplayPlatform;

public final class ReplayPlatforms {
    private static ReplayPlatform platform;

    private ReplayPlatforms() {
    }

    public static void install(ReplayPlatform platform) {
        ReplayPlatforms.platform = platform;
    }

    public static ReplayPlatform get() {
        if (platform == null) {
            throw new IllegalStateException("Replay platform has not been installed yet.");
        }
        return platform;
    }

    public static boolean isInstalled() {
        return platform != null;
    }
}
