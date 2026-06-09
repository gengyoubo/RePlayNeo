package github.com.gengyoubo.replayneo.api;

import github.com.gengyoubo.replayneo.api.camera.ReplayCamera;
import github.com.gengyoubo.replayneo.api.entity.ReplayEntityLookup;
import github.com.gengyoubo.replayneo.api.input.ReplayInput;
import github.com.gengyoubo.replayneo.api.network.ReplayNetwork;
import github.com.gengyoubo.replayneo.api.render.ReplayRender;
import github.com.gengyoubo.replayneo.api.world.ReplayWorld;

/**
 * Boundary between replay core code and the active Minecraft/loader implementation.
 */
public interface ReplayPlatform {
    ReplayClient client();

    ReplayWorld world();

    ReplayCamera camera();

    ReplayEntityLookup entities();

    ReplayInput input();

    ReplayNetwork network();

    ReplayRender render();
}
