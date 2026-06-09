package github.com.gengyoubo.replayneo.platform.feature.replay;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.api.Module;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.utils.ModCompat;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.core.versions.MCVer.Keyboard;
import github.com.gengyoubo.replayneo.platform.feature.replay.camera.CameraController;
import github.com.gengyoubo.replayneo.platform.feature.replay.camera.CameraControllerRegistry;
import github.com.gengyoubo.replayneo.platform.feature.replay.camera.CameraEntity;
import github.com.gengyoubo.replayneo.platform.feature.replay.camera.ClassicCameraController;
import github.com.gengyoubo.replayneo.platform.feature.replay.camera.VanillaCameraController;
import github.com.gengyoubo.replayneo.platform.feature.replay.gui.screen.GuiModCompatWarning;
import github.com.gengyoubo.replayneo.platform.feature.replay.handler.GuiHandler;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import github.com.gengyoubo.replayneo.api.function.Click;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ReplayModReplay implements Module {

    { instance = this; }
    public static ReplayModReplay instance;

    private final ReplayMod core;
    public ReplayKeyBindingRegistry.Binding keyPlayPause;

    private final CameraControllerRegistry cameraControllerRegistry = new CameraControllerRegistry();

    public static Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    private ReplayHandler replayHandler;

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }

    public ReplayModReplay(ReplayMod core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void registerKeyBindings(ReplayKeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.marker", Keyboard.KEY_M, () -> {
            if (replayHandler != null ) {
                CameraEntity camera = replayHandler.getCameraEntity();
                if (camera != null) {
                    Marker marker = new Marker();
                    marker.setTime(replayHandler.getReplaySender().currentTimeStamp());
                    marker.setX(camera.getX());
                    marker.setY(camera.getY());
                    marker.setZ(camera.getZ());
                    marker.setYaw(camera.getYRot());
                    marker.setPitch(camera.getXRot());
                    marker.setRoll(camera.roll);
                    replayHandler.getOverlay().timeline.addMarker(marker);
                }
            }
        }, true);

        registry.registerKeyBinding("replaymod.input.thumbnail", Keyboard.KEY_N, () -> {
            if (replayHandler != null) {
                Minecraft mc = MCVer.getMinecraft();
                ListenableFuture<NoGuiScreenshot> future = NoGuiScreenshot.take(mc, 1280, 720);
                Futures.addCallback(future, new FutureCallback<>() {
                    @Override
                    public void onSuccess(NoGuiScreenshot result) {
                        try {
                            core.printInfoToChat("replaymod.chat.savingthumb");
                            @SuppressWarnings("deprecation") // there's no easy way to produce jpg images from NativeImage
                            BufferedImage image = result.getImage().toBufferedImage();
                            // Encoding with alpha fails on OpenJDK and produces broken image on Sun JDK.
                            BufferedImage bgrImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                            Graphics graphics = bgrImage.getGraphics();
                            graphics.drawImage(image, 0, 0, null);
                            graphics.dispose();
                            replayHandler.getReplayFile().writeThumb(bgrImage);
                            core.printInfoToChat("replaymod.chat.savedthumb");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Throwable t) {
                        t.printStackTrace();
                        core.printWarningToChat("replaymod.chat.failedthumb");
                    }
                }, Runnable::run);
            }
        }, true);

        keyPlayPause = registry.registerKeyBinding("replaymod.input.playpause", Keyboard.KEY_P, () -> {
            if (replayHandler != null) {
                replayHandler.getOverlay().playPauseButton.onClick(new Click(-1, -1, 0, 0));
            }
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.rollclockwise", Keyboard.KEY_L, () -> {
            // Noop, actual handling logic in CameraEntity#update
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.rollcounterclockwise", Keyboard.KEY_J, () -> {
            // Noop, actual handling logic in CameraEntity#update
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("replaymod.input.resettilt", Keyboard.KEY_K, () -> Optional.ofNullable(replayHandler).map(ReplayHandler::getCameraEntity).ifPresent(c -> c.roll = 0), true);
    }

    @Override
    public void initClient() {
        cameraControllerRegistry.register("replaymod.camera.classic", ClassicCameraController::new);
        cameraControllerRegistry.register("replaymod.camera.vanilla", cameraEntity -> new VanillaCameraController(core.getMinecraft(), cameraEntity));

        new GuiHandler(this).register();
    }

    public void startReplay(File file) throws IOException {
        startReplay(core.files.open(file.toPath()));
    }

    public void startReplay(ReplayFile replayFile) throws IOException {
        startReplay(replayFile, true, true);
    }

    public ReplayHandler startReplay(ReplayFile replayFile, boolean checkModCompat, boolean asyncMode) throws IOException {
        if (replayHandler != null) {
            replayHandler.endReplay();
        }
        if (checkModCompat) {
            ModCompat.ModInfoDifference modDifference = new ModCompat.ModInfoDifference(replayFile.getModInfo());
            if (!modDifference.getMissing().isEmpty() || !modDifference.getDiffering().isEmpty()) {
                GuiModCompatWarning screen = new GuiModCompatWarning(modDifference);
                screen.loadButton.onClick(() -> {
                    try {
                        startReplay(replayFile, false, asyncMode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                screen.display();
                return null;
            }
        }
        replayHandler = new ReplayHandler(replayFile, asyncMode);
        KeyMapping.resetMapping(); // see KeyMappingMixin

        return replayHandler;
    }

    public void forcefullyStopReplay() {
        replayHandler = null;
        KeyMapping.resetMapping(); // see KeyMappingMixin
    }

    public ReplayMod getCore() {
        return core;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public CameraControllerRegistry getCameraControllerRegistry() {
        return cameraControllerRegistry;
    }

    public CameraController createCameraController(CameraEntity cameraEntity) {
        String controllerName = core.getSettingsRegistry().get(Setting.CAMERA);
        return cameraControllerRegistry.create(controllerName, cameraEntity);
    }
}
