package github.com.gengyoubo.replayneo.api.pathing;

public interface TimelinePlaybackTarget {
    void applyCameraPosition(double x, double y, double z);

    void applyCameraRotation(float yaw, float pitch, float roll);

    void applyReplayTime(int time);

    void spectateEntity(int entityId);
}

