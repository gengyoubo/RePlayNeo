package github.com.gengyoubo.replayneo.api.input;

public interface ReplayKeyBinding {
    String id();

    String displayName();

    boolean isBound();

    boolean consumeClick();

    boolean isDown();
}
