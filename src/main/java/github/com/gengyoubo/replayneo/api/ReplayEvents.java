package github.com.gengyoubo.replayneo.api;

/**
 * Event bridge owned by the platform layer and consumed by replay core systems.
 */
public interface ReplayEvents {
    void onClientTick(Runnable listener);

    void onRenderHud(RenderHudListener listener);

    void onReplayOpened(Runnable listener);

    void onReplayClosed(Runnable listener);

    @FunctionalInterface
    interface RenderHudListener {
        void render(ReplayPlatform platform, float partialTick);
    }
}
