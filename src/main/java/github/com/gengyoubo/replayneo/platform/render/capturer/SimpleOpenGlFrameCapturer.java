package github.com.gengyoubo.replayneo.platform.render.capturer;

import github.com.gengyoubo.replayneo.api.render.capturer.WorldRenderer;

import github.com.gengyoubo.replayneo.api.render.capturer.RenderInfo;

import github.com.gengyoubo.replayneo.api.render.capturer.CaptureData;

import github.com.gengyoubo.replayneo.core.render.frame.OpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.rendering.Channel;

import java.util.Collections;
import java.util.Map;

public class SimpleOpenGlFrameCapturer extends OpenGlFrameCapturer<OpenGlFrame, CaptureData> {

    public SimpleOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);
    }

    @Override
    public Map<Channel, OpenGlFrame> process() {
        float partialTicks = renderInfo.updateForNextFrame();
        OpenGlFrame frame = renderFrame(framesDone++, partialTicks);
        return Collections.singletonMap(Channel.BRGA, frame);
    }
}



