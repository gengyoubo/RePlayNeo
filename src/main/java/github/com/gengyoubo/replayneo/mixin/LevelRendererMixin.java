package github.com.gengyoubo.replayneo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.api.events.PostRenderWorldCallback;
import github.com.gengyoubo.replayneo.platform.render.PoseStackWorldRenderContext;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.feature.recording.handler.RecordingEventHandler;
import github.com.gengyoubo.replayneo.platform.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.platform.render.hooks.ForceChunkLoadingHook;
import github.com.gengyoubo.replayneo.api.hook.IForceChunkLoading;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.api.ReplaySectionDirtyAccess;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements IForceChunkLoading, RecordingEventHandler.RecordingEventSender, ReplaySectionDirtyAccess {
    @Shadow @Final private Minecraft minecraft;
    @Final
    @Shadow private ObjectArrayList<?> renderChunksInFrustum;
    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;
    @Shadow private boolean needsFullRenderChunkUpdate;
    @Shadow private ViewArea viewArea;
    @Shadow protected abstract void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean rerenderOnMainThread);

    @Unique
    private ForceChunkLoadingHook replayModRender$hook;

    @Unique
    private boolean replayModRender$passThrough;

    @Unique
    private RecordingEventHandler RePlayCore$recordingEventHandler;

    @Unique
    private final List<PendingSectionDirty> replayneo$pendingSectionDirties = new ArrayList<>();

    @Shadow protected abstract void setupRender(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void replayneo$skipSectionDirtyWithoutViewArea(int sectionX, int sectionY, int sectionZ, boolean rerenderOnMainThread, CallbackInfo ci) {
        if (this.viewArea == null) {
            this.replayneo$pendingSectionDirties.add(new PendingSectionDirty(sectionX, sectionY, sectionZ, rerenderOnMainThread));
            ci.cancel();
        }
    }

    @Unique
    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender$hook = hook;
    }

    @Unique
    @Override
    public void replayneo$markSectionDirty(int sectionX, int sectionY, int sectionZ, boolean rerenderOnMainThread) {
        this.setSectionDirty(sectionX, sectionY, sectionZ, rerenderOnMainThread);
    }

    @Override
    public void setRecordingEventHandler(RecordingEventHandler recordingEventHandler) {
        this.RePlayCore$recordingEventHandler = recordingEventHandler;
    }

    @Override
    public RecordingEventHandler getRecordingEventHandler() {
        return RePlayCore$recordingEventHandler;
    }

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void forceAllChunks(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci) {
        if (replayModRender$hook == null) {
            return;
        }
        if (replayModRender$passThrough) {
            return;
        }
        ci.cancel();

        replayModRender$passThrough = true;
        try {
            do {
                setupRender(camera, frustum, hasForcedFrustum, spectator);

                boolean uploadedChunks = ((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.chunkRenderDispatcher).uploadEverythingBlocking();
                this.needsFullRenderChunkUpdate |= uploadedChunks;
                if (this.needsFullRenderChunkUpdate) {
                    this.renderChunksInFrustum.clear();
                }
            } while (this.needsFullRenderChunkUpdate);
        } finally {
            replayModRender$passThrough = false;
        }
    }

    @Inject(method = "setupRender", at = @At("RETURN"))
    private void replayneo$flushPendingSectionDirties(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci) {
        if (this.viewArea == null || this.replayneo$pendingSectionDirties.isEmpty()) {
            return;
        }

        List<PendingSectionDirty> pending = new ArrayList<>(this.replayneo$pendingSectionDirties);
        this.replayneo$pendingSectionDirties.clear();
        for (PendingSectionDirty dirty : pending) {
            this.setSectionDirty(dirty.sectionX(), dirty.sectionY(), dirty.sectionZ(), dirty.rerenderOnMainThread());
        }
    }

    @Inject(
            method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", remap = false, shift = At.Shift.AFTER),
            cancellable = true)
    private void chromaKeyingSky(PoseStack poseStack, Matrix4f matrix, float partialTick, Camera camera, boolean skyFog, Runnable runnable, CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) Minecraft.getInstance().gameRenderer).replayModRender_getHandler();
        if (handler != null) {
            ReadableColor color = handler.getSettings().getChromaKeyingColor();
            if (color != null) {
                com.mojang.blaze3d.systems.RenderSystem.clearColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1);
                com.mojang.blaze3d.systems.RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, false);
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void postRenderWorld(CallbackInfo ci, @Local(argsOnly = true) PoseStack matrixStack) {
        PostRenderWorldCallback.EVENT.invoker().postRenderWorld(new PoseStackWorldRenderContext(matrixStack));
    }

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void replayModRender_drawSelectionBox(CallbackInfo ci) {
        if (((EntityRendererHandler.IEntityRenderer) this.minecraft.gameRenderer).replayModRender_getHandler() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    public void saveBlockBreakProgressPacket(int breakerId, BlockPos pos, int progress, CallbackInfo info) {
        if (RePlayCore$recordingEventHandler != null) {
            RePlayCore$recordingEventHandler.onBlockBreakAnim(breakerId, pos, progress);
        }
    }

    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private long replayneo$getWorldBorderTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return MCVer.milliTime();
    }

    @Unique
    private record PendingSectionDirty(int sectionX, int sectionY, int sectionZ, boolean rerenderOnMainThread) {}
}
