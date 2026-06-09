package github.com.gengyoubo.replayneo.core.render.rendering;

import java.io.Closeable;
import java.util.Map;

public interface FrameCapturer<R extends Frame> extends Closeable {

    boolean isDone();

    Map<Channel, R> process();

}
