package github.com.gengyoubo.replayneo.api.entity;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ReplayEntityLookup {
    Optional<ReplayEntity> byId(int entityId);

    Optional<ReplayEntity> byUuid(UUID uuid);

    Collection<ReplayEntity> players();
}
