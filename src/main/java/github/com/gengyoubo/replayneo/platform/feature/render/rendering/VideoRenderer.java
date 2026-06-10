package github.com.gengyoubo.replayneo.platform.feature.render.rendering;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import github.com.gengyoubo.replayneo.mixin.MinecraftAccessor;
import github.com.gengyoubo.replayneo.mixin.TimerAccessor;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.feature.pathing.player.AbstractTimelinePlayer;
import github.com.gengyoubo.replayneo.platform.feature.pathing.player.ReplayTimer;
import github.com.gengyoubo.replayneo.core.pathing.properties.TimestampProperty;
import github.com.gengyoubo.replayneo.platform.feature.render.CameraPathExporter;
import github.com.gengyoubo.replayneo.platform.feature.render.EXRWriter;
import github.com.gengyoubo.replayneo.platform.feature.render.PNGWriter;
import github.com.gengyoubo.replayneo.core.render.RenderSettings;
import github.com.gengyoubo.replayneo.platform.feature.render.ReplayModRender;
import github.com.gengyoubo.replayneo.platform.feature.render.FFmpegWriter;
import github.com.gengyoubo.replayneo.platform.feature.render.blend.BlendState;
import github.com.gengyoubo.replayneo.core.render.capturer.RenderInfo;
import github.com.gengyoubo.replayneo.platform.feature.render.events.ReplayRenderCallback;
import github.com.gengyoubo.replayneo.core.render.frame.BitmapFrame;
import github.com.gengyoubo.replayneo.core.render.rendering.Channel;
import github.com.gengyoubo.replayneo.api.frame.FrameConsumer;
import github.com.gengyoubo.replayneo.platform.feature.render.gui.GuiRenderingDone;
import github.com.gengyoubo.replayneo.platform.feature.render.gui.GuiVideoRenderer;
import github.com.gengyoubo.replayneo.platform.feature.render.gui.progress.VirtualWindow;
import github.com.gengyoubo.replayneo.platform.feature.render.hooks.ForceChunkLoadingHook;
import github.com.gengyoubo.replayneo.platform.feature.render.metadata.MetadataInjector;
import github.com.gengyoubo.replayneo.core.utils.FlawlessFrames;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import github.com.gengyoubo.replayneo.RePlayNeo;
import org.lwjgl.glfw.GLFW;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import java.util.concurrent.CompletableFuture;


import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static github.com.gengyoubo.replayneo.core.utils.Utils.DEFAULT_MS_PER_TICK;
import static github.com.gengyoubo.replayneo.platform.versions.MCVer.*;
import static github.com.gengyoubo.replayneo.platform.feature.render.ReplayModRender.LOGGER;
import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class VideoRenderer implements RenderInfo {
    private static final ResourceLocation SOUND_RENDER_SUCCESS = identifier(RePlayNeo.RESOURCE_NAMESPACE, "render_success");
    private final Minecraft mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private final ReplayHandler replayHandler;
    private final Timeline timeline;
    private final Pipeline renderingPipeline;
    private final FFmpegWriter ffmpegWriter;
    private final CameraPathExporter cameraPathExporter;

    private int fps;
    private boolean mouseWasGrabbed;
    private boolean debugInfoWasShown;
    private Map<SoundSource, Float> originalSoundLevels;

    private TimelinePlayer timelinePlayer;
    private Future<Void> timelinePlayerFuture;
    private ForceChunkLoadingHook forceChunkLoadingHook;

    private int framesDone;
    private int totalFrames;

    private final VirtualWindow guiWindow = new VirtualWindow(mc);
    private final GuiVideoRenderer gui;
    private boolean paused;
    private boolean cancelled;
    private volatile Throwable failureCause;

    public VideoRenderer(RenderSettings settings, ReplayHandler replayHandler, Timeline timeline) throws IOException {
        this.settings = settings;
        this.replayHandler = replayHandler;
        this.timeline = timeline;
        this.gui = new GuiVideoRenderer(this);
        if (settings.getRenderMethod() == RenderSettings.RenderMethod.BLEND) {
            BlendState.setState(new BlendState(settings.getOutputFile()));

            this.renderingPipeline = Pipelines.newBlendPipeline(this);
            this.ffmpegWriter = null;
        } else {
            FrameConsumer<BitmapFrame> frameConsumer;
            if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.EXR) {
                frameConsumer = EXRWriter.create(settings.getOutputFile().toPath(), settings.isIncludeAlphaChannel());
            } else if (settings.getEncodingPreset() == RenderSettings.EncodingPreset.PNG) {
                frameConsumer = new PNGWriter(settings.getOutputFile().toPath(), settings.isIncludeAlphaChannel());
            } else {
                frameConsumer = new FFmpegWriter(this);
            }
            ffmpegWriter = frameConsumer instanceof FFmpegWriter ? (FFmpegWriter) frameConsumer : null;
            FrameConsumer<BitmapFrame> previewingFrameConsumer = new FrameConsumer<>() {
                private int lastFrameId = -1;

                @Override
                public void consume(Map<Channel, BitmapFrame> channels) {
                    BitmapFrame bgra = channels.get(Channel.BRGA);
                    if (bgra != null) {
                        synchronized (this) {
                            int frameId = bgra.frameId();
                            if (lastFrameId < frameId) {
                                lastFrameId = frameId;
                                gui.updatePreview(bgra.byteBuffer(), bgra.size());
                            }
                        }
                    }
                    frameConsumer.consume(channels);
                }

                @Override
                public void close() throws IOException {
                    frameConsumer.close();
                }

                @Override
                public boolean isParallelCapable() {
                    return frameConsumer.isParallelCapable();
                }
            };
            this.renderingPipeline = Pipelines.newPipeline(settings.getRenderMethod(), this, previewingFrameConsumer);
        }

        if (settings.isCameraPathExport()) {
            this.cameraPathExporter = new CameraPathExporter(settings);
        } else {
            this.cameraPathExporter = null;
        }
    }

    /**
     * Render this video.
     */
    public void renderVideo() throws Throwable {
        ReplayRenderCallback.Pre.EVENT.invoker().beforeRendering(this);

        setup();

        // Because this might take some time to prepare we'll render the GUI at least once to not confuse the user
        drawGui();

        ReplayTimer timer = (ReplayTimer) ((MinecraftAccessor) mc).getTimer();

        // Play up to one second before starting to render
        // This is necessary in order to ensure that all entities have at least two position packets
        // and their first position in the recording is correct.
        // Note that it is impossible to also get the interpolation between their latest position
        // and the one in the recording correct as there's no reliable way to tell when the server ticks
        // or when we should be done with the interpolation of the entity
        Optional<Integer> optionalVideoStartTime = timeline.getValue(TimestampProperty.PROPERTY, 0);
        if (optionalVideoStartTime.isPresent()) {
            int videoStart = optionalVideoStartTime.get();

            if (videoStart > 1000) {
                int replayTime = videoStart - 1000;
                timer.partialTick = 0;
                ((TimerAccessor) timer).setTickLength(DEFAULT_MS_PER_TICK);
                while (replayTime < videoStart) {
                    replayTime += 50;
                    replayHandler.getReplaySender().sendPacketsTill(replayTime);
                    tick();
                }
            }
        }


        renderingPipeline.run();

        if (((MinecraftAccessor) mc).getCrashReporter() != null) {
            throw new ReportedException(((MinecraftAccessor) mc).getCrashReporter().get());
        }

        if (settings.isInjectSphericalMetadata()) {
            MetadataInjector.injectMetadata(settings.getRenderMethod(), settings.getOutputFile(),
                    settings.getTargetVideoWidth(), settings.getTargetVideoHeight(),
                    settings.getSphericalFovX(), settings.getSphericalFovY());
        }

        finish();

        ReplayRenderCallback.Post.EVENT.invoker().afterRendering(this);

        if (failureCause != null) {
            throw failureCause;
        }

    }

    @Override
    public float updateForNextFrame() {
        // because the jGui lib uses Minecraft's displayWidth and displayHeight values, update these temporarily
        guiWindow.bind();

        if (!settings.isHighPerformance() || framesDone % fps == 0) {
            while (drawGui() && paused) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Updating the timer will cause the timeline player to update the game state
        ReplayTimer timer = (ReplayTimer) ((MinecraftAccessor) mc).getTimer();
        int elapsedTicks =
        timer.advanceTime(
                MCVer.milliTime()
        );

        executeTaskQueue();


        while (elapsedTicks-- > 0) {
            tick();
        }

        // change Minecraft's display size back
        guiWindow.unbind();

        if (cameraPathExporter != null) {
            cameraPathExporter.recordFrame(timer.partialTick);
        }

        framesDone++;
        return timer.partialTick;
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }

    private void setup() {
        timelinePlayer = new TimelinePlayer(replayHandler);
        timelinePlayerFuture = timelinePlayer.start(timeline);

        // FBOs are always used in 1.14+
        if (mc.options.renderDebug) {
            mc.options.renderDebug = false;
            debugInfoWasShown = true;
        }
        if (mc.mouseHandler.isMouseGrabbed()) {
            mouseWasGrabbed = true;
        }
        mc.mouseHandler.releaseMouse();

        // Mute all sounds except GUI sounds (buttons, etc.)
        originalSoundLevels = new EnumMap<>(SoundSource.class);
        for (SoundSource category : SoundSource.values()) {
            if (category != SoundSource.MASTER) {
                originalSoundLevels.put(category, mc.options.getSoundSourceVolume(category));
                mc.options.getSoundSourceOptionInstance(category).set(0.0);
            }
        }

        fps = settings.getFramesPerSecond();

        long duration = 0;
        for (Path path : timeline.getPaths()) {
            if (!path.isActive()) continue;

            // Prepare path interpolations
            path.updateAll();
            // Find end time
            Collection<Keyframe> keyframes = path.getKeyframes();
            if (!keyframes.isEmpty()) {
                duration = Math.max(duration, getLast(keyframes).getTime());
            }
        }

        totalFrames = (int) (duration*fps/1000);

        if (cameraPathExporter != null) {
            cameraPathExporter.setup(totalFrames);
        }

        gui.toMinecraft().init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

        forceChunkLoadingHook = new ForceChunkLoadingHook(mc.levelRenderer);
    }

    private void finish() {
        if (!timelinePlayerFuture.isDone()) {
            timelinePlayerFuture.cancel(false);
        }
        // Tear down of the timeline player might only happen the next tick after it was cancelled
        timelinePlayer.onTick();

        guiWindow.close();

        // FBOs are always used in 1.14+
        if (debugInfoWasShown) {
            mc.options.renderDebug = true;
        }
        if (mouseWasGrabbed) {
            mc.mouseHandler.grabMouse();
        }
        for (Map.Entry<SoundSource, Float> entry : originalSoundLevels.entrySet()) {
            mc.options.getSoundSourceOptionInstance(entry.getKey()).set((double) entry.getValue());
        }
        mc.setScreen(null);
        forceChunkLoadingHook.uninstall();

        if (hasFailed() && cameraPathExporter != null) {
            try {
                cameraPathExporter.finish();
            } catch (IOException e) {
                setFailure(e);
            }
        }

        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(SOUND_RENDER_SUCCESS), 1));

        try {
            if (hasFailed() && ffmpegWriter != null) {
                new GuiRenderingDone(ReplayModRender.instance, ffmpegWriter.getVideoFile(), totalFrames, settings).display();
            }
        } catch (FFmpegWriter.FFmpegStartupException e) {
            setFailure(e);
        }

        // Finally, resize the Minecraft framebuffer to the actual width/height of the window
        resizeMainWindow(mc, guiWindow.getFramebufferWidth(), guiWindow.getFramebufferHeight());
    }

    private void executeTaskQueue() {
        while (true) {
            while (mc.getOverlay() != null) {
                drawGui();
                ((MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();

            }

            CompletableFuture<Void> resourceReloadFuture = ((MinecraftAccessor) mc).getPendingReload();
            if (resourceReloadFuture != null) {
                ((MinecraftAccessor) mc).setPendingReload(null);
                mc.reloadResourcePacks().thenRun(() -> resourceReloadFuture.complete(null));
                continue;
            }
            break;
        }
        ((MCVer.MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();


        mc.screen = gui.toMinecraft();
    }

    private void tick() {

        mc.tick();
    }

    public boolean drawGui() {
        Window window = mc.getWindow();
        do {
            if (GLFW.glfwWindowShouldClose(window.getWindow()) || ((MinecraftAccessor) mc).getCrashReporter() != null) {
                return false;
            }

            pushMatrix();
            com.mojang.blaze3d.systems.RenderSystem.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                    , false
            );
            guiWindow.beginWrite();

            RenderSystem.clear(256, Minecraft.ON_OSX);
            RenderSystem.setProjectionMatrix(MCVer.ortho(
                    0,
                    (float) (window.getWidth() / window.getGuiScale()),
                    0,
                    (float) (window.getHeight() / window.getGuiScale()),
                    1000,
                    3000
                )
                    , VertexSorting.ORTHOGRAPHIC_Z
            );
            PoseStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.setIdentity();
            matrixStack.translate(0, 0, -2000);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();

            gui.toMinecraft().init(mc, window.getGuiScaledWidth(), window.getGuiScaledHeight());

            // Events are polled on 1.13+ in mainWindow.update which is called later

            int mouseX = (int) mc.mouseHandler.xpos() * window.getGuiScaledWidth() / Math.max(window.getScreenWidth(), 1);
            int mouseY = (int) mc.mouseHandler.ypos() * window.getGuiScaledHeight() / Math.max(window.getScreenHeight(), 1);

            GuiGraphics drawContext = new GuiGraphics(mc, mc.renderBuffers().bufferSource());


            if (mc.getOverlay() != null) {
                Screen orgScreen = mc.screen;
                try {
                    mc.screen = gui.toMinecraft();
                    mc.getOverlay().render(
                            drawContext,
                            mouseX, mouseY, 0);
                } finally {
                    mc.screen = orgScreen;
                }
            } else {
                gui.toMinecraft().tick();
                gui.toMinecraft().render(
                        drawContext,
                        mouseX, mouseY, 0);
            }
            drawContext.flush();

            guiWindow.endWrite();
            popMatrix();
            pushMatrix();
            guiWindow.flip();
            popMatrix();

            if (mc.mouseHandler.isMouseGrabbed()) {
                mc.mouseHandler.releaseMouse();
            }

            return hasFailed() && !cancelled;
        } while (true);
    }

    public int getFramesDone() {
        return framesDone;
    }

    @Override
    public ReadableDimension getFrameSize() {
        return new Dimension(settings.getVideoWidth(), settings.getVideoHeight());
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public int getVideoTime() { return framesDone * 1000 / fps; }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void cancel() {
        if (ffmpegWriter != null) {
            ffmpegWriter.abort();
        }
        this.cancelled = true;
        renderingPipeline.cancel();
    }

    public boolean hasFailed() {
        return failureCause == null;
    }

    public synchronized void setFailure(Throwable cause) {
        if (this.failureCause != null) {
            LOGGER.error("Further failure during failed rendering: ", cause);
        } else {
            LOGGER.error("Failure during rendering: ", cause);
            this.failureCause = cause;
            cancel();
        }
    }

    private class TimelinePlayer extends AbstractTimelinePlayer {
        public TimelinePlayer(ReplayHandler replayHandler) {
            super(replayHandler);
        }

        @Override
        public long getTimePassed() {
            return getVideoTime();
        }
    }

    public static String[] checkCompat(Stream<RenderSettings> settings) {
        return settings.map(VideoRenderer::checkCompat).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public static String[] checkCompat(RenderSettings settings) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("sodium") && !FlawlessFrames.hasSodium()) {
            return new String[] {
                    "Rendering is not supported with your Sodium version.",
                    "It is missing support for the FREX Flawless Frames API.",
                    "Either use the Sodium build from replaymod.com or uninstall Sodium before rendering!",
            };
        }
        if (settings.getRenderMethod() == RenderSettings.RenderMethod.ODS
                && !net.minecraftforge.fml.ModList.get().isLoaded("iris")) {
            return new String[] {
                    "ODS export requires Iris to be installed for Minecraft 1.17 and above.",
                    "Note that it is nevertheless incompatible with other shaders and will simply replace them.",
                    "Get it from: https://modrinth.com/mod/iris",
            };
        }
        return null;
    }
}

