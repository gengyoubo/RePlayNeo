package github.com.gengyoubo.replayneo.api.render;

/**
 * Loader-neutral drawing surface. Platform implementations may wrap Minecraft GUI types internally.
 */
public interface ReplayDrawContext {
    int width();

    int height();
}
