package github.com.gengyoubo.replayneo.platform.feature.render.frame;

import github.com.gengyoubo.replayneo.platform.feature.render.rendering.Frame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;

public record BitmapFrame(int frameId, ReadableDimension size, int bytesPerPixel,
                          ByteBuffer byteBuffer) implements Frame {
    public BitmapFrame {
        Validate.isTrue(size.getWidth() * size.getHeight() * bytesPerPixel == byteBuffer.remaining(),
                "Buffer size is %d (cap: %d) but should be %d",
                byteBuffer.remaining(), byteBuffer.capacity(), size.getWidth() * size.getHeight() * bytesPerPixel);
    }

    public ReadableDimension getSize() {
        return size;
    }

    public int getBytesPerPixel() {
        return bytesPerPixel;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }
}
