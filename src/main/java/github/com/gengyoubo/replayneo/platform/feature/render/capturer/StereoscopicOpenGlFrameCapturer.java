package github.com.gengyoubo.replayneo.platform.feature.render.capturer;

import github.com.gengyoubo.replayneo.api.render.capturer.WorldRenderer;

import github.com.gengyoubo.replayneo.api.render.capturer.RenderInfo;

import github.com.gengyoubo.replayneo.api.render.capturer.CaptureData;

import github.com.gengyoubo.replayneo.core.render.frame.OpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.frame.StereoscopicOpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.rendering.Channel;

import java.util.Collections;
import java.util.Map;

public class StereoscopicOpenGlFrameCapturer
        extends OpenGlFrameCapturer<StereoscopicOpenGlFrame, StereoscopicOpenGlFrameCapturer.Data> {
    public enum Data implements CaptureData {
        LEFT_EYE, RIGHT_EYE
    }

    public StereoscopicOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);
    }

    @Override
    protected int getFrameWidth() {
        return super.getFrameWidth() / 2;
    }

    @Override
    public Map<Channel, StereoscopicOpenGlFrame> process() {
        float partialTicks = renderInfo.updateForNextFrame();
        int frameId = framesDone++;
        OpenGlFrame left = renderFrame(frameId, partialTicks, Data.LEFT_EYE);
        OpenGlFrame right = renderFrame(frameId, partialTicks, Data.RIGHT_EYE);
        StereoscopicOpenGlFrame frame = new StereoscopicOpenGlFrame(left, right);
        return Collections.singletonMap(Channel.BRGA, frame);
    }
}



