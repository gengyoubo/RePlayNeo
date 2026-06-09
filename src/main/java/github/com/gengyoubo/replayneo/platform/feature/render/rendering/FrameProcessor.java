package github.com.gengyoubo.replayneo.platform.feature.render.rendering;

import java.io.Closeable;

public interface FrameProcessor<R extends Frame, P extends Frame> extends Closeable {

    P process(R rawFrame);

}
