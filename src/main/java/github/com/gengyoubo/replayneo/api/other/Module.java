package github.com.gengyoubo.replayneo.api.other;

import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;

public interface Module {
    default void initCommon() {
        RePlayNeo.LOGGER.debug("Initializing common module");
    }

    // FMLClientSetupEvent for 1.13+, FMLInitializationEvent (if client) below
    default void initClient() {}

    default void registerKeyBindings(ReplayKeyBindingRegistry registry) {}
}

