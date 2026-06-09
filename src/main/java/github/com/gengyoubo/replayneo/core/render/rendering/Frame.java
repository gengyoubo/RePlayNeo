package github.com.gengyoubo.replayneo.core.render.rendering;

public interface Frame {
    int frameId();

    default int getFrameId() {
        return frameId();
    }
}
