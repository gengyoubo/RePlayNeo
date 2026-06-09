package github.com.gengyoubo.replayneo.api;

import com.replaymod.replaystudio.data.ModInfo;

import java.nio.file.Path;
import java.util.Collection;

public interface ReplayRuntime {
    String getVersion();

    String getMinecraftVersion();

    boolean isModLoaded(String id);

    Path getGameDirectory();

    Collection<ModInfo> getInstalledNetworkMods();
}

