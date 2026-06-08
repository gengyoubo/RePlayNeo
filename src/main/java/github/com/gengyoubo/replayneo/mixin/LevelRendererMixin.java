package github.com.gengyoubo.replayneo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.restored.com.replaymod.compat.shaders.ShaderReflection;
import github.com.gengyoubo.replayneo.core.events.PostRenderWorldCallback;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.recording.handler.RecordingEventHandler;
import github.com.gengyoubo.replayneo.feature.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.feature.render.hooks.ForceChunkLoadingHook;
import github.com.gengyoubo.replayneo.feature.render.hooks.IForceChunkLoading;
import github.com.gengyoubo.replayneo.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.feature.replay.ReplayModReplay;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements IForceChunkLoading, RecordingEventHandler.RecordingEventSender {
    @Shadow @Final private Minecraft minecraft;
    @Final
    @Shadow private ObjectArrayList<ChunkRenderDispatcher.RenderChunk> renderChunksInFrustum;
    @Shadow private ChunkRenderDispatcher chunkRenderDispatcher;
    @Shadow private boolean needsFullRenderChunkUpdate;

    @Unique
    private ForceChunkLoadingHook replayModRender$hook;

    @Unique
    private boolean replayModRender$passThrough;

    @Unique
    private RecordingEventHandler replayMod$recordingEventHandler;

    @Shadow protected abstract void setupRender(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

    @Unique
    @Override
    public void replayModRender_setHook(ForceChunkLoadingHook hook) {
        this.replayModRender$hook = hook;
    }

    @Unique
    public void rePlay$setRecordingEventHandler(RecordingEventHandler recordingEventHandler) {
        this.replayMod$recordingEventHandler = recordingEventHandler;
    }

    @Unique
    public RecordingEventHandler rePlay$getRecordingEventHandler() {
        return replayMod$recordingEventHandler;
    }

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void forceAllChunks(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci) throws IllegalAccessException {
        if (replayModRender$hook == null) {
            return;
        }
        if (replayModRender$passThrough) {
            return;
        }
        if (ShaderReflection.shaders_isShadowPass != null && (boolean) ShaderReflection.shaders_isShadowPass.get(null)) {
            return;
        }
        ci.cancel();

        replayModRender$passThrough = true;
        try {
            do {
                setupRender(camera, frustum, hasForcedFrustum, spectator);

                for (ChunkRenderDispatcher.RenderChunk builtChunk : this.renderChunksInFrustum) {
                    if (builtChunk.hasAllNeighbors()) {
                        builtChunk.setDirty(true);
                    }
                    builtChunk.setNotDirty();
                }
                this.renderChunksInFrustum.clear();

                this.needsFullRenderChunkUpdate |= ((ForceChunkLoadingHook.IBlockOnChunkRebuilds) this.chunkRenderDispatcher).uploadEverythingBlocking();
            } while (this.needsFullRenderChunkUpdate);
        } finally {
            replayModRender$passThrough = false;
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
        PostRenderWorldCallback.EVENT.invoker().postRenderWorld(matrixStack);
    }

    @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
    private void replayModRender_drawSelectionBox(CallbackInfo ci) {
        if (((EntityRendererHandler.IEntityRenderer) this.minecraft.gameRenderer).replayModRender_getHandler() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    public void saveBlockBreakProgressPacket(int breakerId, BlockPos pos, int progress, CallbackInfo info) {
        if (replayMod$recordingEventHandler != null) {
            replayMod$recordingEventHandler.onBlockBreakAnim(breakerId, pos, progress);
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
}
