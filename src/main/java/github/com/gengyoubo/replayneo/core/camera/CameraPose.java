package github.com.gengyoubo.replayneo.core.camera;

import github.com.gengyoubo.replayneo.api.camera.ReplayCameraPose;

public record CameraPose(double x, double y, double z, float yaw, float pitch, float roll) implements ReplayCameraPose {
}
