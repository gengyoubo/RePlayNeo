package github.com.gengyoubo.replayneo.feature.render.capturer;

import com.replaymod.render.RenderSettings;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

public interface RenderInfo {
    ReadableDimension getFrameSize();

    int getFramesDone();

    int getTotalFrames();

    void updateForNextFrame();

    RenderSettings getRenderSettings();
}
