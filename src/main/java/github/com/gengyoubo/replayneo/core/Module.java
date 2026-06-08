package github.com.gengyoubo.replayneo.core;

public interface Module {
    default void initCommon() {}

    // FMLClientSetupEvent for 1.13+, FMLInitializationEvent (if client) below
    default void initClient() {}

    default void registerKeyBindings(KeyBindingRegistry registry) {}
}
