package github.com.gengyoubo.replayneo.api.input;

public interface ReplayInput {
    ReplayKeyBindingRegistry keyBindingRegistry();

    ReplayKeyBinding registerKey(String id, int defaultKeyCode, boolean replayOnly);

    void registerRawKey(int keyCode, ReplayKeyHandler handler);
}
