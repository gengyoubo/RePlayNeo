package github.com.gengyoubo.replayneo.platform.feature.render.capturer;

import github.com.gengyoubo.replayneo.platform.feature.render.frame.OpenGlFrame;
import github.com.gengyoubo.replayneo.platform.feature.render.rendering.Channel;
import github.com.gengyoubo.replayneo.platform.feature.render.rendering.Frame;
import github.com.gengyoubo.replayneo.core.utils.ByteBufferPool;
import github.com.gengyoubo.replayneo.core.utils.PixelBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


public abstract class PboOpenGlFrameCapturer<F extends Frame, D extends Enum<D> & CaptureData>
        extends OpenGlFrameCapturer<F, D> {
    private final boolean withDepth;
    private final D[] data;
    private PixelBufferObject pbo, otherPBO;

    public PboOpenGlFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, Class<D> type, int framePixels) {
        super(worldRenderer, renderInfo);

        withDepth = renderInfo.getRenderSettings().isDepthMap();
        data = type.getEnumConstants();
        int bufferSize = framePixels * (4 /* bgra */ + (withDepth ? 4 /* float */ : 0)) * data.length;
        pbo = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
        otherPBO = new PixelBufferObject(bufferSize, PixelBufferObject.Usage.READ);
    }

    protected abstract F create(OpenGlFrame[] from);

    private void swapPBOs() {
        PixelBufferObject old = pbo;
        pbo = otherPBO;
        otherPBO = old;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames() + 2;
    }

    private F readFromPbo(ByteBuffer pboBuffer) {
        OpenGlFrame[] frames = new OpenGlFrame[data.length];
        int frameBufferSize = getFrameWidth() * getFrameHeight() * 4;
        for (int i = 0; i < frames.length; i++) {
            ByteBuffer frameBuffer = ByteBufferPool.allocate(frameBufferSize);
            pboBuffer.limit(pboBuffer.position() + frameBufferSize);
            if (false) {
                for (int j = 0; j < frameBufferSize; j += 4) {
                    byte r = pboBuffer.get();
                    byte g = pboBuffer.get();
                    byte b = pboBuffer.get();
                    byte a = pboBuffer.get();
                    frameBuffer.put(b);
                    frameBuffer.put(g);
                    frameBuffer.put(r);
                    frameBuffer.put(a);
                }
            } else {
                frameBuffer.put(pboBuffer);
            }
            frameBuffer.rewind();
            frames[i] = new OpenGlFrame(framesDone - 2, frameSize, 4, frameBuffer);
        }
        return create(frames);
    }

    @Override
    public Map<Channel, F> process() {
        Map<Channel, F> channels = null;

        if (framesDone > 1) {
            // Read pbo to memory
            pbo.bind();
            ByteBuffer pboBuffer = pbo.mapReadOnly();

            channels = new HashMap<>();
            channels.put(Channel.BRGA, readFromPbo(pboBuffer));
            if (withDepth) {
                channels.put(Channel.DEPTH, readFromPbo(pboBuffer));
            }

            pbo.unmap();
            pbo.unbind();
        }

        if (framesDone < renderInfo.getTotalFrames()) {
            float partialTicks = renderInfo.updateForNextFrame();
            // Then fill it again
            for (D data : this.data) {
                renderFrame(framesDone, partialTicks, data);
            }
        }

        framesDone++;
        swapPBOs();
        return channels;
    }

    @Override
    protected OpenGlFrame captureFrame(int frameId, D captureData) {
        pbo.bind();

        int offset = captureData.ordinal() * getFrameWidth() * getFrameHeight() * 4;
        frameBuffer().bindWrite(true);
        GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, offset);
        if (withDepth) {
            offset += data.length * getFrameWidth() * getFrameHeight() * 4;
            GL11.glReadPixels(0, 0, getFrameWidth(), getFrameHeight(), GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, offset);
        }
        frameBuffer().unbindWrite();

        pbo.unbind();
        return null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        pbo.close();
        otherPBO.close();
    }
}
