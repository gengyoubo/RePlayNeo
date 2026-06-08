package github.com.gengyoubo.replayneo.feature.render.processor;

import com.replaymod.render.rendering.Frame;

public class DummyProcessor<F extends Frame> extends AbstractFrameProcessor<F, F> {
    @Override
    public F process(F rawFrame) {
        return rawFrame;
    }
}
