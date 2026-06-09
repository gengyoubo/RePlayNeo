package github.com.gengyoubo.replayneo.core.render.processor;

import github.com.gengyoubo.replayneo.api.frame.Frame;

public class DummyProcessor<F extends Frame> extends AbstractFrameProcessor<F, F> {
    @Override
    public F process(F rawFrame) {
        return rawFrame;
    }
}
