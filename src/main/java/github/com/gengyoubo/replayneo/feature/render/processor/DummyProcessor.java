package github.com.gengyoubo.replayneo.feature.render.processor;

import github.com.gengyoubo.replayneo.feature.render.rendering.Frame;

public class DummyProcessor<F extends Frame> extends AbstractFrameProcessor<F, F> {
    @Override
    public F process(F rawFrame) {
        return rawFrame;
    }
}
