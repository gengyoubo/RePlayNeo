package github.com.gengyoubo.replayneo.platform.feature.render.processor;

import github.com.gengyoubo.replayneo.platform.feature.render.rendering.Frame;
import github.com.gengyoubo.replayneo.platform.feature.render.rendering.FrameProcessor;

import java.io.IOException;

public abstract class AbstractFrameProcessor<R extends Frame, P extends Frame> implements FrameProcessor<R, P> {
    @Override
    public void close() throws IOException {
    }
}
