package github.com.gengyoubo.replayneo.platform;

import github.com.gengyoubo.replayneo.api.ReplayCrashReport;
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
import github.com.gengyoubo.replayneo.core.camera.CameraPose;
import github.com.gengyoubo.replayneo.platform.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.camera.CameraEntity;
import github.com.gengyoubo.replayneo.platform.input.ForgeReplayInput;
import github.com.gengyoubo.replayneo.api.ReplaySectionDirtyAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ForgeReplayPlatform implements ReplayPlatform {
    private final ForgeReplayInput input = new ForgeReplayInput();
    private final ReplayClient client = new ForgeReplayClient();
    private final ReplayWorld world = new ForgeReplayWorld();
    private final ReplayCamera camera = new ForgeReplayCamera();
    private final ReplayEntityLookup entities = new ForgeReplayEntityLookup();
    private final ReplayNetwork network = new LocalReplayNetwork();
    private final ReplayRender render = new ForgeReplayRender();

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
        public void sendReplayMessage(boolean warning, String translationKey, Object... args) {
            Minecraft minecraft = Minecraft.getInstance();
            if (!minecraft.isSameThread()) {
                execute(() -> sendReplayMessage(warning, translationKey, args));
                return;
            }
            if (minecraft.gui == null) {
                return;
            }

            Style coloredDarkGray = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
            Style coloredGold = Style.EMPTY.withColor(ChatFormatting.GOLD);
            Style alert = Style.EMPTY.withColor(warning ? ChatFormatting.RED : ChatFormatting.DARK_GREEN);
            Component text = Component.literal("[").setStyle(coloredDarkGray)
                    .append(Component.translatable("replaymod.title").setStyle(coloredGold))
                    .append(Component.literal("] "))
                    .append(Component.translatable(translationKey, args).setStyle(alert));
            minecraft.gui.getChat().addMessage(text);
        }

        @Override
        public String translate(String translationKey, Object... args) {
            return net.minecraft.client.resources.language.I18n.get(translationKey, args);
        }

        @Override
        public int textWidth(String text) {
            return Minecraft.getInstance().font.width(text);
        }

        @Override
        public boolean isReplayOpen() {
            return ReplayModReplay.instance.getReplayHandler() != null;
        }

        @Override
        public ReplayCrashReport crashReport(Throwable throwable, String title) {
            CrashReport report = CrashReport.forThrowable(throwable, title);
            return new ForgeReplayCrashReport(report);
        }
    }

    private record ForgeReplayCrashReport(CrashReport report) implements ReplayCrashReport {
        @Override
        public String friendlyReport() {
            return report.getFriendlyReport();
        }

        @Override
        public String saveFile() {
            return String.valueOf(report.getSaveFile());
        }
    }

    private static class ForgeReplayWorld implements ReplayWorld {
        @Override
        public Optional<String> dimensionId() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return Optional.empty();
            }
            return Optional.of(minecraft.level.dimension().location().toString());
        }

        @Override
        public long gameTime() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.level == null ? 0L : minecraft.level.getGameTime();
        }

        @Override
        public void markBlockDirty(int x, int y, int z) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.levelRenderer instanceof ReplaySectionDirtyAccess access) {
                access.replayneo$markSectionDirty(
                        SectionPos.blockToSectionCoord(x),
                        SectionPos.blockToSectionCoord(y),
                        SectionPos.blockToSectionCoord(z),
                        false);
            }
        }
    }

    private static class ForgeReplayCamera implements ReplayCamera {
        @Override
        public ReplayCameraPose pose() {
            Entity camera = currentCameraEntity();
            if (camera == null) {
                return new CameraPose(0, 0, 0, 0, 0, 0);
            }
            float roll = camera instanceof CameraEntity replayCamera ? replayCamera.roll : 0;
            return new CameraPose(
                    camera.getX(),
                    camera.getY(),
                    camera.getZ(),
                    camera.getYRot(),
                    camera.getXRot(),
                    roll);
        }

        @Override
        public void setPose(ReplayCameraPose pose) {
            Entity camera = currentCameraEntity();
            if (camera instanceof CameraEntity replayCamera) {
                replayCamera.setCameraPosition(pose.x(), pose.y(), pose.z());
                replayCamera.setCameraRotation(pose.yaw(), pose.pitch(), pose.roll());
            } else if (camera != null) {
                camera.setPos(pose.x(), pose.y(), pose.z());
                camera.setYRot(pose.yaw());
                camera.setXRot(pose.pitch());
            }
        }

        @Override
        public void setControlledEntity(int entityId) {
            Minecraft minecraft = Minecraft.getInstance();
            Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            if (replayHandler != null) {
                if (entity != null) {
                    replayHandler.spectateEntity(entity);
                } else {
                    replayHandler.spectateCamera();
                }
            } else if (entity != null) {
                minecraft.setCameraEntity(entity);
            }
        }

        private Entity currentCameraEntity() {
            Minecraft minecraft = Minecraft.getInstance();
            Entity camera = minecraft.getCameraEntity();
            return camera != null ? camera : minecraft.player;
        }
    }

    private static class ForgeReplayEntityLookup implements ReplayEntityLookup {
        @Override
        public Optional<ReplayEntity> byId(int entityId) {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.level == null ? Optional.empty() : Optional.ofNullable(minecraft.level.getEntity(entityId)).map(ForgeReplayEntity::new);
        }

        @Override
        public Optional<ReplayEntity> byUuid(UUID uuid) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return Optional.empty();
            }
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (uuid.equals(entity.getUUID())) {
                    return Optional.of(new ForgeReplayEntity(entity));
                }
            }
            return Optional.empty();
        }

        @Override
        public Collection<ReplayEntity> players() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return Collections.emptyList();
            }
            List<ReplayEntity> players = new ArrayList<>();
            for (Player player : minecraft.level.players()) {
                players.add(new ForgeReplayEntity(player));
            }
            return players;
        }
    }

    private record ForgeReplayEntity(Entity entity) implements ReplayEntity {
        @Override
        public int id() {
            return entity.getId();
        }

        @Override
        public UUID uuid() {
            return entity.getUUID();
        }

        @Override
        public String typeId() {
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        }

        @Override
        public double x() {
            return entity.getX();
        }

        @Override
        public double y() {
            return entity.getY();
        }

        @Override
        public double z() {
            return entity.getZ();
        }
    }

    private static class LocalReplayNetwork implements ReplayNetwork {
        private final List<ReplayPacketListener> listeners = new CopyOnWriteArrayList<>();

        @Override
        public void send(ReplayPacket packet) {
            for (ReplayPacketListener listener : listeners) {
                listener.onPacket(packet);
            }
        }

        @Override
        public void addListener(ReplayPacketListener listener) {
            listeners.add(listener);
        }
    }

    private static class ForgeReplayRender implements ReplayRender {
        private volatile boolean gameHudSuppressed;

        @Override
        public boolean isVideoRendering() {
            Minecraft minecraft = Minecraft.getInstance();
            return ((EntityRendererHandler.IEntityRenderer) minecraft.gameRenderer).replayModRender_getHandler() != null;
        }

        @Override
        public void renderReplayHud(ReplayDrawContext context, float partialTick) {
            // GUI drawing still flows through RenderHudCallback; this hook exists for core-facing migration.
        }

        @Override
        public void setGameHudSuppressed(boolean suppressed) {
            this.gameHudSuppressed = suppressed;
        }

        public boolean isGameHudSuppressed() {
            return gameHudSuppressed;
        }
    }
}
