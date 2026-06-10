package github.com.gengyoubo.replayneo.api.render;

public interface ReplayRender {
    boolean isVideoRendering();

    void renderReplayHud(ReplayDrawContext context, float partialTick);

    void setGameHudSuppressed(boolean suppressed);
}
