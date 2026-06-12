package github.com.gengyoubo.replayneo.api.other;

public interface Exporter {
    default void setup() {}

    default void tearDown() {}

    default void preFrame(int frame) {}

    default void postFrame(int frame) {}
}
