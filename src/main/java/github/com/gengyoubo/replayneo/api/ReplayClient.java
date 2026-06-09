package github.com.gengyoubo.replayneo.api;

public interface ReplayClient {
    boolean isOnClientThread();

    void execute(Runnable task);

    void sendTranslatedMessage(String translationKey, Object... args);

    boolean isReplayOpen();
}
