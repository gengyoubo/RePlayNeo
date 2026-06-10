package github.com.gengyoubo.replayneo.api;

import com.replaymod.replaystudio.data.ModInfo;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.core.scheduler.Scheduler;

import java.nio.file.Path;
import java.util.Collection;

public interface ReplayRuntime {
    String getVersion();

    String getMinecraftVersion();

    boolean isModLoaded(String id);

    Path getGameDirectory();

    Collection<ModInfo> getInstalledNetworkMods();

    ReplayKeyBindingRegistry keyBindingRegistry();

    Scheduler scheduler();

    int getProtocolVersion();

    void configureI18n();

    void sendReplayMessage(boolean warning, String message, Object... args);

    default void executeOnClient(Runnable runnable) {
        runnable.run();
    }
}

