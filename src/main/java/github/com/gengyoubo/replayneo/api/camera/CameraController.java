package github.com.gengyoubo.replayneo.api.camera;

public interface CameraController {
    void update(float partialTicksPassed);

    void increaseSpeed();
    void decreaseSpeed();
}
