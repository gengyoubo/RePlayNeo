package github.com.gengyoubo.replayneo.platform.render.blend;

import github.com.gengyoubo.replayneo.api.render.capturer.RenderInfo;
import github.com.gengyoubo.replayneo.api.render.capturer.WorldRenderer;
import github.com.gengyoubo.replayneo.core.render.frame.BitmapFrame;
import github.com.gengyoubo.replayneo.core.render.rendering.Channel;
import github.com.gengyoubo.replayneo.api.frame.FrameCapturer;
import github.com.gengyoubo.replayneo.core.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import net.minecraft.client.Minecraft;

public class BlendFrameCapturer implements FrameCapturer<BitmapFrame> {
    protected final WorldRenderer worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;

    public BlendFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        this.worldRenderer = worldRenderer;
        this.renderInfo = renderInfo;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames();
    }

    @Override
    public Map<Channel, BitmapFrame> process() {
        if (framesDone == 0) {
            BlendState.getState().setup();
        }

        renderInfo.updateForNextFrame();

        BlendState.getState().preFrame(framesDone);
        worldRenderer.renderWorld(Minecraft.getInstance().getFrameTime(), null);
        BlendState.getState().postFrame(framesDone);

        BitmapFrame frame = new BitmapFrame(framesDone++, new Dimension(0, 0), 0, ByteBufferPool.allocate(0));
        return Collections.singletonMap(Channel.BRGA, frame);
    }

    @Override
    public void close() throws IOException {
        BlendState.getState().tearDown();
        BlendState.setState(null);
    }
}

