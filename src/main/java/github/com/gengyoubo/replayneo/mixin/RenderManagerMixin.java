package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.platform.camera.CameraEntity;
import github.com.gengyoubo.replayneo.platform.compat.ChangedReplayCompat;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.render.hooks.EntityRendererHandler;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Quaternionf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class RenderManagerMixin {
    @Shadow private Quaternionf cameraOrientation;

    @Shadow public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    private static boolean replayneo$renderingChangedReplayForm;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void replayModRender_reorientForCubicRendering(Entity entity, double dx, double dy, double dz,
                                                           float iDoNotKnow,
                                                           float partialTicks,
                                                           PoseStack matrixStack,
                                                           MultiBufferSource vertexConsumerProvider,
                                                           int int_1,
                                                           CallbackInfo ci) {
        ReplayModReplay replay = ReplayModReplay.instance;
        if (!replayneo$renderingChangedReplayForm
                && replay != null
                && replay.getReplayHandler() != null
                && entity instanceof Player player
                && !(entity instanceof CameraEntity)) {
            replayneo$renderingChangedReplayForm = true;
            try {
                EntityRenderer<? super Entity> renderer = this.getRenderer(entity);
                Vec3 offset = renderer.getRenderOffset(entity, partialTicks);
                matrixStack.pushPose();
                matrixStack.translate(dx + offset.x(), dy + offset.y(), dz + offset.z());
                try {
                    if (ChangedReplayCompat.renderReplayForm(player, matrixStack, vertexConsumerProvider, int_1, partialTicks)) {
                        ci.cancel();
                        return;
                    }
                } finally {
                    matrixStack.popPose();
                }
            } finally {
                replayneo$renderingChangedReplayForm = false;
            }
        }

        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler != null && handler.omnidirectional) {
            double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            double yaw = -Math.atan2(dx, dz);
            this.cameraOrientation = new Quaternionf()
                    .mul(new Quaternionf().rotationY((float) -yaw))
                    .mul(new Quaternionf().rotationX((float) pitch));
        }
    }
}
