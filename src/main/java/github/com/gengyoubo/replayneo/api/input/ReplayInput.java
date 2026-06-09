package github.com.gengyoubo.replayneo.api.input;

import de.johni0702.minecraft.gui.utils.lwjgl.Point;

public interface ReplayInput {
    ReplayKeyBindingRegistry keyBindingRegistry();

    ReplayKeyBinding registerKey(String id, int defaultKeyCode, boolean replayOnly);

    void registerRawKey(int keyCode, ReplayKeyHandler handler);

    Point mousePosition();

    Point scaledDimensions();
}
