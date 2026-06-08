package github.com.gengyoubo.replayneo.feature.render.frame;

import github.com.gengyoubo.replayneo.feature.render.rendering.Frame;
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
