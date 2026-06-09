package github.com.gengyoubo.replayneo.api.world;

import java.util.Optional;

public interface ReplayWorld {
    Optional<String> dimensionId();

    long gameTime();

    void markBlockDirty(int x, int y, int z);
}
