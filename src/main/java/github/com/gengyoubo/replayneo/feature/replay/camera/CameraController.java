package github.com.gengyoubo.replayneo.feature.replay.camera;

public interface CameraController {
    void update(float partialTicksPassed);

    void increaseSpeed();
    void decreaseSpeed();
}
