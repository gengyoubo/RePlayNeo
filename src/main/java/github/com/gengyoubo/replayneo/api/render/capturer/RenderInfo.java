package github.com.gengyoubo.replayneo.api.render.capturer;

import github.com.gengyoubo.replayneo.api.render.RenderSettings;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

public interface RenderInfo {
    ReadableDimension getFrameSize();

    int getFramesDone();

    int getTotalFrames();

    float updateForNextFrame();

    RenderSettings getRenderSettings();
}

