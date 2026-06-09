package github.com.gengyoubo.replayneo.platform.feature.render.capturer;

import github.com.gengyoubo.replayneo.platform.feature.render.frame.OpenGlFrame;

public class SimplePboOpenGlFrameCapturer extends PboOpenGlFrameCapturer<OpenGlFrame, SimplePboOpenGlFrameCapturer.SinglePass> {

    public SimplePboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo, SinglePass.class,
                renderInfo.getFrameSize().getWidth() * renderInfo.getFrameSize().getHeight());
    }

    @Override
    protected OpenGlFrame create(OpenGlFrame[] from) {
        return from[0];
    }

    public enum SinglePass implements CaptureData {
        SINGLE_PASS
    }
}
