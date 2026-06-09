package github.com.gengyoubo.replayneo.platform.feature.replay.camera;

public interface CameraController {
    void update(float partialTicksPassed);

    void increaseSpeed();
    void decreaseSpeed();
}
