package github.com.gengyoubo.replayneo.platform.feature.render.frame;

import github.com.gengyoubo.replayneo.platform.feature.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public record ODSOpenGlFrame(CubicOpenGlFrame left, CubicOpenGlFrame right) implements Frame {
    public ODSOpenGlFrame {
        Validate.isTrue(left.getFrameId() == right.getFrameId(), "Frame ids do not match.");
    }

    @Override
    public int frameId() {
        return left.getFrameId();
    }
}
