package github.com.gengyoubo.replayneo.platform.feature.render.rendering;

public interface Frame {
    int frameId();

    default int getFrameId() {
        return frameId();
    }
}
