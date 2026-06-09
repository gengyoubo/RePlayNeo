package github.com.gengyoubo.replayneo.platform.feature.render.processor;

import github.com.gengyoubo.replayneo.platform.feature.render.frame.CubicOpenGlFrame;
import github.com.gengyoubo.replayneo.platform.feature.render.frame.BitmapFrame;
import github.com.gengyoubo.replayneo.core.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;

import java.nio.ByteBuffer;

import static github.com.gengyoubo.replayneo.platform.restored.com.replaymod.render.utils.Utils.openGlBytesToBitmap;

public class CubicToBitmapProcessor extends AbstractFrameProcessor<CubicOpenGlFrame, BitmapFrame> {

    @Override
    public BitmapFrame process(CubicOpenGlFrame rawFrame) {
        int size = rawFrame.left().getSize().getWidth();
        int bpp = rawFrame.left().getBytesPerPixel();
        int width = size * 4;
        int height = size * 3;
        ByteBuffer result = ByteBufferPool.allocate(width * height * bpp);
        openGlBytesToBitmap(rawFrame.left(), 0, size, result, width);
        openGlBytesToBitmap(rawFrame.front(), size, size, result, width);
        openGlBytesToBitmap(rawFrame.right(), size * 2, size, result, width);
        openGlBytesToBitmap(rawFrame.back(), size * 3, size, result, width);
        openGlBytesToBitmap(rawFrame.top(), size, 0, result, width);
        openGlBytesToBitmap(rawFrame.bottom(), size, size * 2, result, width);
        ByteBufferPool.release(rawFrame.left().getByteBuffer());
        ByteBufferPool.release(rawFrame.right().getByteBuffer());
        ByteBufferPool.release(rawFrame.front().getByteBuffer());
        ByteBufferPool.release(rawFrame.back().getByteBuffer());
        ByteBufferPool.release(rawFrame.top().getByteBuffer());
        ByteBufferPool.release(rawFrame.bottom().getByteBuffer());
        return new BitmapFrame(rawFrame.frameId(), new Dimension(width, height), bpp, result);
    }
}
