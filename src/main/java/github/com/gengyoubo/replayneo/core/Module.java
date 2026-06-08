package github.com.gengyoubo.replayneo.core;

public interface Module {
    // FMLClientSetupEvent for 1.13+, FMLInitializationEvent (if client) below
    default void initClient() {}
}
