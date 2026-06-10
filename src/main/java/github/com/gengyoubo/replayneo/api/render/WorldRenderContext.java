package github.com.gengyoubo.replayneo.api.render;

public interface WorldRenderContext {
    <T> T nativePoseStack(Class<T> type);
}
