package github.com.gengyoubo.replayneo.feature.render.frame;

import github.com.gengyoubo.replayneo.feature.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public record CubicOpenGlFrame(OpenGlFrame left, OpenGlFrame right, OpenGlFrame front, OpenGlFrame back,
                               OpenGlFrame top, OpenGlFrame bottom) implements Frame {
    public CubicOpenGlFrame {
        Validate.isTrue(left.getFrameId() == right.getFrameId()
                && right.getFrameId() == front.getFrameId()
                && front.getFrameId() == back.getFrameId()
                && back.getFrameId() == top.getFrameId()
                && top.getFrameId() == bottom.getFrameId(), "Frame ids do not match.");
        Validate.isTrue(left.getByteBuffer().remaining() == right.getByteBuffer().remaining()
                && right.getByteBuffer().remaining() == front.getByteBuffer().remaining()
                && front.getByteBuffer().remaining() == back.getByteBuffer().remaining()
                && back.getByteBuffer().remaining() == top.getByteBuffer().remaining()
                && top.getByteBuffer().remaining() == bottom.getByteBuffer().remaining(), "Buffer size does not match.");
    }

    @Override
    public int frameId() {
        return left.getFrameId();
    }

    public OpenGlFrame getLeft() {
        return left;
    }

    public OpenGlFrame getRight() {
        return right;
    }
}
