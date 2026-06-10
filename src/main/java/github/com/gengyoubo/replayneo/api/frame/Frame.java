package github.com.gengyoubo.replayneo.api.frame;

public interface Frame {
    int frameId();

    default int getFrameId() {
        return frameId();
    }
}
