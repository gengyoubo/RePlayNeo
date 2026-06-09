package github.com.gengyoubo.replayneo.platform.feature.render.capturer;

import github.com.gengyoubo.replayneo.core.render.frame.OpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.frame.StereoscopicOpenGlFrame;

public class StereoscopicPboOpenGlFrameCapturer
        extends PboOpenGlFrameCapturer<StereoscopicOpenGlFrame, StereoscopicOpenGlFrameCapturer.Data> {

    public StereoscopicPboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo, StereoscopicOpenGlFrameCapturer.Data.class,
                renderInfo.getFrameSize().getWidth() / 2 * renderInfo.getFrameSize().getHeight());
    }

    @Override
    protected int getFrameWidth() {
        return super.getFrameWidth() / 2;
    }

    @Override
    protected StereoscopicOpenGlFrame create(OpenGlFrame[] from) {
        return new StereoscopicOpenGlFrame(from[0], from[1]);
    }
}
