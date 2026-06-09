package github.com.gengyoubo.replayneo.platform.feature.render.capturer;

import github.com.gengyoubo.replayneo.platform.feature.render.RenderSettings;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

public interface RenderInfo {
    ReadableDimension getFrameSize();

    int getFramesDone();

    int getTotalFrames();

    float updateForNextFrame();

    RenderSettings getRenderSettings();
}
