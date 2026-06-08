package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.core.events.PreRenderHandCallback;
import github.com.gengyoubo.replayneo.feature.render.capturer.CubicOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.feature.render.capturer.StereoscopicOpenGlFrameCapturer;
import github.com.gengyoubo.replayneo.feature.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.feature.replay.camera.CameraEntity;
import github.com.gengyoubo.replayneo.platform.callbacks.PostRenderScreenCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements EntityRendererHandler.IEntityRenderer {
    @Unique
    private static final String EXTRACT_GUI = "render";
    @Unique
    private static final String PROJECTION_MATRIX = "getProjectionMatrix";
    @Unique
    private static final String SET_PERSPECTIVE = "Lorg/joml/Matrix4f;setPerspective(FFFF)Lorg/joml/Matrix4f;";
    @Unique
    private static final boolean SET_PERSPECTIVE_REMAP = false;
    @Unique
    private static final float OMNIDIRECTIONAL_FOV = (float) Math.PI / 2;

    @Shadow @Final
    Minecraft minecraft;

    @Unique
    private EntityRendererHandler replayModRender$handler;

    @Unique
    private GuiGraphics replayMod$context;

    @Override
    public void replayModRender_setHandler(EntityRendererHandler handler) {
        this.replayModRender$handler = handler;
    }

    @Override
    public EntityRendererHandler replayModRender_getHandler() {
        return replayModRender$handler;
    }

    @ModifyArg(method = EXTRACT_GUI, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private GuiGraphics captureContext(GuiGraphics context) {
        this.replayMod$context = context;
        return context;
    }

    @Inject(method = EXTRACT_GUI, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void postRenderScreen(
            float partialTicks, long nanoTime,
            boolean renderWorld,
            CallbackInfo ci
    ) {
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(replayMod$context, partialTicks);
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getXRot()F"
            )
    )
    private void applyRoll(float float_1, long long_1, PoseStack matrixStack, CallbackInfo ci) {
        Entity entity = this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity();
        if (entity instanceof CameraEntity) {
            matrixStack.mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(0, 0, 1, ((CameraEntity) entity).roll));
        }
    }

    @ModifyArg(method = PROJECTION_MATRIX, at = @At(value = "INVOKE", target = SET_PERSPECTIVE, remap = SET_PERSPECTIVE_REMAP), index = 0)
    private float replayModRender_perspective_fov(float fovY) {
        return rePlay$isOmnidirectional() ? OMNIDIRECTIONAL_FOV : fovY;
    }

    @ModifyArg(method = PROJECTION_MATRIX, at = @At(value = "INVOKE", target = SET_PERSPECTIVE, remap = SET_PERSPECTIVE_REMAP), index = 1)
    private float replayModRender_perspective_aspect(float aspect) {
        return rePlay$isOmnidirectional() ? 1 : aspect;
    }

    @Inject(method = "getProjectionMatrix", at = @At("RETURN"), cancellable = true)
    private void replayModRender_setupStereoscopicProjection(CallbackInfoReturnable<Matrix4f> ci) {
        if (replayModRender_getHandler() != null) {
            Matrix4f offset;
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                offset = new Matrix4f().translation(0.07f, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                offset = new Matrix4f().translation(-0.07f, 0, 0);
            } else {
                return;
            }
            offset.mul(ci.getReturnValue());
            ci.setReturnValue(offset);
        }
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void replayModRender_setupStereoscopicProjection(float partialTicks, long frameStartNano, PoseStack matrixStack, CallbackInfo ci) {
        if (replayModRender_getHandler() != null) {
            if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.LEFT_EYE) {
                matrixStack.translate(0.1f, 0, 0);
            } else if (replayModRender_getHandler().data == StereoscopicOpenGlFrameCapturer.Data.RIGHT_EYE) {
                matrixStack.translate(-0.1f, 0, 0);
            }
        }
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void replayModRender_setupCubicFrameRotation(
            float partialTicks,
            long frameStartNano,
            PoseStack matrixStack,
            CallbackInfo ci
    ) {
        if (replayModRender_getHandler() != null) {
            replayModRender_getHandler();
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void preRenderHand(CallbackInfo ci) {
        if (PreRenderHandCallback.EVENT.invoker().preRenderHand()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "renderItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;getPlayerMode()Lnet/minecraft/world/level/GameType;"
            )
    )
    private GameType getGameMode(MultiPlayerGameMode interactionManager) {
        LocalPlayer camera = this.minecraft.player;
        if (camera instanceof CameraEntity) {
            // Alternative doesn't matter; the caller only checks for equality to SPECTATOR.
            return camera.isSpectator() ? GameType.SPECTATOR : GameType.SURVIVAL;
        }
        return interactionManager.getPlayerMode();
    }

    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"), index = 0
    )
    private int replayModRender_skipGuiDepthClearWhenRecordingDepth(int mask) {
        EntityRendererHandler handler = replayModRender_getHandler();
        if (handler != null && handler.getSettings().isDepthMap()) {
            mask = mask & ~GL11.GL_DEPTH_BUFFER_BIT;
        }
        return mask;
    }

    @ModifyArg(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"),
            index = 0
    )
    private int replayModRender_skipHandDepthClearWhenRecordingDepth(int mask) {
        EntityRendererHandler handler = replayModRender_getHandler();
        if (handler != null && handler.getSettings().isDepthMap()) {
            mask = mask & ~GL11.GL_DEPTH_BUFFER_BIT;
        }
        return mask;
    }

    @Unique
    private boolean rePlay$isOmnidirectional() {
        return replayModRender_getHandler() != null && replayModRender_getHandler().omnidirectional;
    }
}
