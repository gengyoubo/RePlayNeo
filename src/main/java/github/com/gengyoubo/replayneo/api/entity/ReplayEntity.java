package github.com.gengyoubo.replayneo.api.entity;

import java.util.UUID;

public interface ReplayEntity {
    int id();

    UUID uuid();

    String typeId();

    double x();

    double y();

    double z();
}
