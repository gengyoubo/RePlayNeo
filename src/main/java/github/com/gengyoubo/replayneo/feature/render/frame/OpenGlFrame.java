package github.com.gengyoubo.replayneo.feature.render.frame;

import com.replaymod.render.rendering.Frame;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

import java.nio.ByteBuffer;

public record OpenGlFrame(int frameId, ReadableDimension size, int bytesPerPixel,
                          ByteBuffer byteBuffer) implements Frame {
}
