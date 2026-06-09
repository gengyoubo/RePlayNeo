package github.com.gengyoubo.replayneo.api.camera;

public interface ReplayCamera {
    ReplayCameraPose pose();

    void setPose(ReplayCameraPose pose);

    void setControlledEntity(int entityId);
}
