package github.com.gengyoubo.replayneo.feature.render.rendering;

public interface Frame {
    int frameId();

    default int getFrameId() {
        return frameId();
    }
}
