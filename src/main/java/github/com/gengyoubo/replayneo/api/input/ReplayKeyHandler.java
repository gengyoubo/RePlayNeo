package github.com.gengyoubo.replayneo.api.input;

@FunctionalInterface
public interface ReplayKeyHandler {
    boolean handle(ReplayKeyInput input);
}
