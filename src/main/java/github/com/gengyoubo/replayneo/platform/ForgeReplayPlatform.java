package github.com.gengyoubo.replayneo.platform;

import github.com.gengyoubo.replayneo.api.ReplayClient;
import github.com.gengyoubo.replayneo.api.ReplayPlatform;
import github.com.gengyoubo.replayneo.api.camera.ReplayCamera;
import github.com.gengyoubo.replayneo.api.camera.ReplayCameraPose;
import github.com.gengyoubo.replayneo.api.entity.ReplayEntity;
import github.com.gengyoubo.replayneo.api.entity.ReplayEntityLookup;
import github.com.gengyoubo.replayneo.api.input.ReplayInput;
import github.com.gengyoubo.replayneo.api.network.ReplayNetwork;
import github.com.gengyoubo.replayneo.api.network.ReplayPacket;
import github.com.gengyoubo.replayneo.api.network.ReplayPacketListener;
import github.com.gengyoubo.replayneo.api.render.ReplayDrawContext;
import github.com.gengyoubo.replayneo.api.render.ReplayRender;
import github.com.gengyoubo.replayneo.api.world.ReplayWorld;
import github.com.gengyoubo.replayneo.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.input.ForgeReplayInput;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class ForgeReplayPlatform implements ReplayPlatform {
    private final ForgeReplayInput input = new ForgeReplayInput();
    private final ReplayClient client = new ForgeReplayClient();
    private final ReplayWorld world = new UnsupportedReplayWorld();
    private final ReplayCamera camera = new UnsupportedReplayCamera();
    private final ReplayEntityLookup entities = new UnsupportedReplayEntityLookup();
    private final ReplayNetwork network = new UnsupportedReplayNetwork();
    private final ReplayRender render = new UnsupportedReplayRender();

    @Override
    public ReplayClient client() {
        return client;
    }

    @Override
    public ReplayWorld world() {
        return world;
    }

    @Override
    public ReplayCamera camera() {
        return camera;
    }

    @Override
    public ReplayEntityLookup entities() {
        return entities;
    }

    @Override
    public ReplayInput input() {
        return input;
    }

    public ForgeReplayInput forgeInput() {
        return input;
    }

    @Override
    public ReplayNetwork network() {
        return network;
    }

    @Override
    public ReplayRender render() {
        return render;
    }

    private static class ForgeReplayClient implements ReplayClient {
        @Override
        public boolean isOnClientThread() {
            return Minecraft.getInstance().isSameThread();
        }

        @Override
        public void execute(Runnable task) {
            Minecraft.getInstance().execute(task);
        }

        @Override
        public void sendTranslatedMessage(String translationKey, Object... args) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui != null) {
                minecraft.gui.getChat().addMessage(Component.translatable(translationKey, args));
            }
        }

        @Override
        public boolean isReplayOpen() {
            return ReplayModReplay.instance.getReplayHandler() != null;
        }
    }

    private static UnsupportedOperationException unsupported(String area) {
        return new UnsupportedOperationException("Replay platform " + area + " adapter is not migrated yet.");
    }

    private static class UnsupportedReplayWorld implements ReplayWorld {
        @Override
        public Optional<String> dimensionId() {
            throw unsupported("world");
        }

        @Override
        public long gameTime() {
            throw unsupported("world");
        }

        @Override
        public void markBlockDirty(int x, int y, int z) {
            throw unsupported("world");
        }
    }

    private static class UnsupportedReplayCamera implements ReplayCamera {
        @Override
        public ReplayCameraPose pose() {
            throw unsupported("camera");
        }

        @Override
        public void setPose(ReplayCameraPose pose) {
            throw unsupported("camera");
        }

        @Override
        public void setControlledEntity(int entityId) {
            throw unsupported("camera");
        }
    }

    private static class UnsupportedReplayEntityLookup implements ReplayEntityLookup {
        @Override
        public Optional<ReplayEntity> byId(int entityId) {
            throw unsupported("entity");
        }

        @Override
        public Optional<ReplayEntity> byUuid(UUID uuid) {
            throw unsupported("entity");
        }

        @Override
        public Collection<ReplayEntity> players() {
            throw unsupported("entity");
        }
    }

    private static class UnsupportedReplayNetwork implements ReplayNetwork {
        @Override
        public void send(ReplayPacket packet) {
            throw unsupported("network");
        }

        @Override
        public void addListener(ReplayPacketListener listener) {
            throw unsupported("network");
        }
    }

    private static class UnsupportedReplayRender implements ReplayRender {
        @Override
        public boolean isVideoRendering() {
            throw unsupported("render");
        }

        @Override
        public void renderReplayHud(ReplayDrawContext context, float partialTick) {
            throw unsupported("render");
        }

        @Override
        public void setGameHudSuppressed(boolean suppressed) {
            throw unsupported("render");
        }
    }
}
