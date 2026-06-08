package github.com.gengyoubo.replayneo.feature.render.processor;

import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ODSToBitmapProcessor extends AbstractFrameProcessor<ODSOpenGlFrame, BitmapFrame> {
    private final EquirectangularToBitmapProcessor processor;

    public ODSToBitmapProcessor(int outputWidth, int outputHeight, int sphericalFovX) {
        processor = new EquirectangularToBitmapProcessor(outputWidth, outputHeight / 2, sphericalFovX);
    }

    @Override
    public BitmapFrame process(ODSOpenGlFrame rawFrame) {
        BitmapFrame leftFrame = processor.process(rawFrame.left());
        BitmapFrame rightFrame = processor.process(rawFrame.right());
        ReadableDimension size = new Dimension(leftFrame.size().getWidth(), leftFrame.size().getHeight() * 2);
        int bpp = rawFrame.left().getLeft().getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(size.getWidth() * size.getHeight() * bpp);
        result.put(leftFrame.byteBuffer());
        result.put(rightFrame.byteBuffer());
        result.rewind();
        ByteBufferPool.release(leftFrame.byteBuffer());
        ByteBufferPool.release(rightFrame.byteBuffer());
        return new BitmapFrame(rawFrame.frameId(), size, bpp, result);
    }

    @Override
    public void close() throws IOException {
        processor.close();
    }

    public int getFrameSize() {
        return processor.getFrameSize();
    }
}
