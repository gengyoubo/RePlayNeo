package github.com.gengyoubo.replayneo.api;

/**
 * Public entry point for replay services exposed to modules that do not need to know about Minecraft or Forge classes.
 */
public interface ReplayAPI {
    ReplayPlatform platform();

    ReplayEvents events();
}

