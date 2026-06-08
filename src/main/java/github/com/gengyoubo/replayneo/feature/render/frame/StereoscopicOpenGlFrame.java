package github.com.gengyoubo.replayneo.feature.render.frame;

import github.com.gengyoubo.replayneo.feature.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public record StereoscopicOpenGlFrame(OpenGlFrame left, OpenGlFrame right) implements Frame {
    public StereoscopicOpenGlFrame {
        Validate.isTrue(left.getFrameId() == right.getFrameId(), "Frame ids do not match.");
        Validate.isTrue(left.getByteBuffer().remaining() == right.getByteBuffer().remaining(), "Buffer size does not match.");
    }

    @Override
    public int frameId() {
        return left.getFrameId();
    }
}
