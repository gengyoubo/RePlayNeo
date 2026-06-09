package github.com.gengyoubo.replayneo.core.render.processor;

import github.com.gengyoubo.replayneo.api.frame.Frame;
import github.com.gengyoubo.replayneo.api.frame.FrameProcessor;

import java.io.IOException;

public abstract class AbstractFrameProcessor<R extends Frame, P extends Frame> implements FrameProcessor<R, P> {
    @Override
    public void close() throws IOException {
    }
}
