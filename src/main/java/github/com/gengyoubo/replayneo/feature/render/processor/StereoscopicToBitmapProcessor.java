package github.com.gengyoubo.replayneo.feature.render.processor;

import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.frame.StereoscopicOpenGlFrame;
import com.replaymod.render.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

import static com.replaymod.render.utils.Utils.openGlBytesToBitmap;

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
