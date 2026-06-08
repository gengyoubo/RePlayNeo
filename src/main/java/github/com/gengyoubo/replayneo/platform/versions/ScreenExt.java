package github.com.gengyoubo.replayneo.platform.versions;

public interface ScreenExt {
    boolean doesPassEvents();
    void setPassEvents(boolean passEvents);

    default boolean rePlay$doesPassEvents() {
        return doesPassEvents();
    }

    default void rePlay$setPassEvents(boolean passEvents) {
        setPassEvents(passEvents);
    }
}
