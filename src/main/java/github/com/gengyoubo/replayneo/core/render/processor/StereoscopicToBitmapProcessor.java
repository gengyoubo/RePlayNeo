package github.com.gengyoubo.replayneo.core.render.processor;

import github.com.gengyoubo.replayneo.core.render.frame.BitmapFrame;
import github.com.gengyoubo.replayneo.core.render.frame.StereoscopicOpenGlFrame;
import github.com.gengyoubo.replayneo.core.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

import static github.com.gengyoubo.replayneo.core.render.utils.Utils.openGlBytesToBitmap;

public class StereoscopicToBitmapProcessor extends AbstractFrameProcessor<StereoscopicOpenGlFrame, BitmapFrame> {
    @Override
    public BitmapFrame process(StereoscopicOpenGlFrame rawFrame) {
        ReadableDimension size = rawFrame.left().getSize();
        int width = size.getWidth();
        int bpp = rawFrame.left().getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(width * 2 * size.getHeight() * bpp);
        openGlBytesToBitmap(rawFrame.left(), 0, 0, result, width * 2);
        openGlBytesToBitmap(rawFrame.right(), size.getWidth(), 0, result, width * 2);
        ByteBufferPool.release(rawFrame.left().getByteBuffer());
        ByteBufferPool.release(rawFrame.right().getByteBuffer());
        return new BitmapFrame(rawFrame.frameId(), new Dimension(width * 2, size.getHeight()), bpp, result);
    }
}
