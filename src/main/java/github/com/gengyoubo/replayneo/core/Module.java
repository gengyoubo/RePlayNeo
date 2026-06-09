package github.com.gengyoubo.replayneo.core;

import github.com.gengyoubo.replayneo.RePlayNeo;

public interface Module {
    default void initCommon() {
        RePlayNeo.LOGGER.debug("Initializing common module");
    }

    // FMLClientSetupEvent for 1.13+, FMLInitializationEvent (if client) below
    default void initClient() {}

    default void registerKeyBindings(KeyBindingRegistry registry) {}
}
