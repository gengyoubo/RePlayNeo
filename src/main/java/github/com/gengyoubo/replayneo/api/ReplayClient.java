package github.com.gengyoubo.replayneo.api;

public interface ReplayClient {
    boolean isOnClientThread();

    void execute(Runnable task);

    void sendTranslatedMessage(String translationKey, Object... args);

    void sendReplayMessage(boolean warning, String translationKey, Object... args);

    String translate(String translationKey, Object... args);

    int textWidth(String text);

    boolean isReplayOpen();

    ReplayCrashReport crashReport(Throwable throwable, String title);
}
