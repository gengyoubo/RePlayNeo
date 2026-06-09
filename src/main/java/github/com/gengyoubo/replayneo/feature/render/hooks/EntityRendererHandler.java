package github.com.gengyoubo.replayneo.feature.render.hooks;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.events.PreRenderHandCallback;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.render.RenderSettings;
import github.com.gengyoubo.replayneo.feature.render.Setting;
import github.com.gengyoubo.replayneo.feature.render.capturer.CaptureData;
import github.com.gengyoubo.replayneo.feature.render.capturer.RenderInfo;
import github.com.gengyoubo.replayneo.feature.render.capturer.WorldRenderer;
import github.com.gengyoubo.replayneo.mixin.GameRendererAccessor;
import github.com.gengyoubo.replayneo.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.core.events.PostRenderCallback;
import github.com.gengyoubo.replayneo.core.events.PreRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class EntityRendererHandler extends EventRegistrations implements WorldRenderer {
    public final Minecraft mc = MCVer.getMinecraft();

    protected final RenderSettings settings;

    private final RenderInfo renderInfo;

    public CaptureData data;

    public boolean omnidirectional;

    private final long startTime;

    private long fakeFinishTimeNano;

    public EntityRendererHandler(RenderSettings settings, RenderInfo renderInfo) {
        this.settings = settings;
        this.renderInfo = renderInfo;

        this.startTime = Util.getNanos();

        on(PreRenderHandCallback.EVENT, () -> omnidirectional);

        ((IEntityRenderer) mc.gameRenderer).replayModRender_setHandler(this);
        register();
    }

    @Override
    public void renderWorld(final float partialTicks, CaptureData data) {
        this.data = data;
        long offsetMillis;
        if (ReplayMod.instance.getSettingsRegistry().get(Setting.FRAME_TIME_FROM_WORLD_TIME)) {
            offsetMillis = ReplayModReplay.instance.getReplayHandler().getReplaySender().currentTimeStamp();
        } else {
            offsetMillis = renderInfo.getFramesDone() * 1_000L / settings.getFramesPerSecond();
        }
        long frameStartTimeNano = startTime + offsetMillis * 1_000_000L;
        renderWorld(partialTicks, frameStartTimeNano);
    }

    public void renderWorld(float partialTicks, long finishTimeNano) {
        fakeFinishTimeNano = finishTimeNano;
        PreRenderCallback.EVENT.invoker().preRender();

        if (mc.level != null && mc.player != null) {
            GameRendererAccessor gameRenderer = (GameRendererAccessor) mc.gameRenderer;
            Screen orgScreen = mc.screen;
            boolean orgPauseOnLostFocus = mc.options.pauseOnLostFocus;
            boolean orgRenderHand = gameRenderer.getRenderHand();
            try {
                mc.screen = null; // do not want to render the current screen (that'd just be the progress gui)
                mc.options.pauseOnLostFocus = false; // do not want the pause menu to open if the window is unfocused
                if (omnidirectional) {
                    // makes no sense, we wouldn't even know where to put it
                    gameRenderer.setRenderHand(false);
                }

                mc.gameRenderer.render(partialTicks, finishTimeNano, true);
            } finally {
                mc.screen = orgScreen;
                mc.options.pauseOnLostFocus = orgPauseOnLostFocus;
                gameRenderer.setRenderHand(orgRenderHand);
            }
        }

        PostRenderCallback.EVENT.invoker().postRender();
    }

    @Override
    public void close() {
        ((IEntityRenderer) mc.gameRenderer).replayModRender_setHandler(null);
        unregister();
    }

    @Override
    public void setOmnidirectional(boolean omnidirectional) {
        this.omnidirectional = omnidirectional;
    }

    public RenderSettings getSettings() {
        return this.settings;
    }

    public RenderInfo getRenderInfo() {
        return this.renderInfo;
    }

    public long getFakeFinishTimeNano() {
        return fakeFinishTimeNano;
    }

    public interface IEntityRenderer {
        void replayModRender_setHandler(EntityRendererHandler handler);
        EntityRendererHandler replayModRender_getHandler();
    }
}
