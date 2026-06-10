package github.com.gengyoubo.replayneo.core.render.frame;

import github.com.gengyoubo.replayneo.api.frame.Frame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

public record OpenGlFrame(int frameId, ReadableDimension size, int bytesPerPixel,
                          ByteBuffer byteBuffer) implements Frame {
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
