package github.com.gengyoubo.replayneo.platform.gui;

import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;

public final class MouseUtils {
    private MouseUtils() {
    }

    public static Point getMousePos() {
        return ReplayPlatforms.get().input().mousePosition();
    }

    public static Point getScaledDimensions() {
        return ReplayPlatforms.get().input().scaledDimensions();
    }
}
