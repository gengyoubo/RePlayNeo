package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.api.render.RenderSettings;
import github.com.gengyoubo.replayneo.platform.feature.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.platform.feature.replay.camera.CameraEntity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.*;

@Mixin(value = Camera.class)
public abstract class StabilizeCameraMixin {
    @Unique
    private EntityRendererHandler rePlay$getHandler() {
        return ((EntityRendererHandler.IEntityRenderer) getMinecraft().gameRenderer).replayModRender_getHandler();
    }

    @Unique
    private float rePlay$orgYaw;
    @Unique
    private float rePlay$orgPitch;
    @Unique
    private float rePlay$orgPrevYaw;
    @Unique
    private float rePlay$orgPrevPitch;
    @Unique
    private float rePlay$orgRoll;

    // Only relevant on 1.13+ (previously MC always used the non-head yaw) and only for LivingEntity view entities.
    @Unique
    private float rePlay$orgHeadYaw;
    @Unique
    private float rePlay$orgPrevHeadYaw;

    @Inject(method = "setup", at = @At("HEAD"))
    private void replayModRender_beforeSetupCameraTransform(
            BlockGetter blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (rePlay$getHandler() != null) {
            rePlay$orgYaw = entity.getYRot();
            rePlay$orgPitch = entity.getXRot();
            rePlay$orgPrevYaw = entity.yRotO;
            rePlay$orgPrevPitch = entity.xRotO;
            rePlay$orgRoll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;
            if (entity instanceof LivingEntity) {
                rePlay$orgHeadYaw = ((LivingEntity) entity).yHeadRot;
                rePlay$orgPrevHeadYaw = ((LivingEntity) entity).yHeadRotO;
            }
        }
        if (rePlay$getHandler() != null) {
            RenderSettings settings = rePlay$getHandler().getSettings();
            if (settings.isStabilizeYaw()) {
                entity.yRotO = 0;
                entity.setYRot(0);
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).yHeadRotO = ((LivingEntity) entity).yHeadRot = 0;
                }
            }
            if (settings.isStabilizePitch()) {
                entity.xRotO = 0;
                entity.setXRot(0);
            }
            if (settings.isStabilizeRoll() && entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = 0;
            }
        }
    }

    @Inject(method = "setup", at = @At("RETURN"))
    private void replayModRender_afterSetupCameraTransform(
            BlockGetter blockView,
            Entity entity,
            boolean thirdPerson,
            boolean inverseView,
            float partialTicks,
            CallbackInfo ci
    ) {
        if (rePlay$getHandler() != null) {
            entity.setYRot(rePlay$orgYaw);
            entity.setXRot(rePlay$orgPitch);
            entity.yRotO = rePlay$orgPrevYaw;
            entity.xRotO = rePlay$orgPrevPitch;
            if (entity instanceof CameraEntity) {
                ((CameraEntity) entity).roll = rePlay$orgRoll;
            }
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).yHeadRot = rePlay$orgHeadYaw;
                ((LivingEntity) entity).yHeadRotO = rePlay$orgPrevHeadYaw;
            }
        }
    }
}
