package github.com.gengyoubo.replayneo.core.render.rendering;

import java.io.Closeable;
import java.util.Map;

public interface FrameConsumer<P extends Frame> extends Closeable {

    void consume(Map<Channel, P> channels);

    boolean isParallelCapable();

}
