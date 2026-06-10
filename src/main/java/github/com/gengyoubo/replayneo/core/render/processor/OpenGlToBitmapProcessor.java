package github.com.gengyoubo.replayneo.core.render.processor;

import github.com.gengyoubo.replayneo.core.render.frame.OpenGlFrame;
import github.com.gengyoubo.replayneo.core.render.frame.BitmapFrame;
import github.com.gengyoubo.replayneo.core.utils.ByteBufferPool;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

import static github.com.gengyoubo.replayneo.core.render.utils.Utils.openGlBytesToBitmap;

public class OpenGlToBitmapProcessor extends AbstractFrameProcessor<OpenGlFrame, BitmapFrame> {

    @Override
    public BitmapFrame process(OpenGlFrame rawFrame) {
        ReadableDimension size = rawFrame.size();
        int width = size.getWidth();
        int height = size.getHeight();
        int bpp = rawFrame.bytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(width * height * bpp);
        openGlBytesToBitmap(rawFrame, 0, 0, result, width);
        ByteBufferPool.release(rawFrame.byteBuffer());
        return new BitmapFrame(rawFrame.frameId(), new Dimension(width, height), bpp, result);
    }
}
