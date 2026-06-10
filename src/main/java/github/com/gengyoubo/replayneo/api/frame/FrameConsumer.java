package github.com.gengyoubo.replayneo.api.frame;

import github.com.gengyoubo.replayneo.core.render.rendering.Channel;

import java.io.Closeable;
import java.util.Map;

public interface FrameConsumer<P extends Frame> extends Closeable {

    void consume(Map<Channel, P> channels);

    boolean isParallelCapable();

}
