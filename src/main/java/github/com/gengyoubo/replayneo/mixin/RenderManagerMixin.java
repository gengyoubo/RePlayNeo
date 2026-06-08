package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.render.hooks.EntityRendererHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.joml.Quaternionf;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class RenderManagerMixin {
    @Shadow private Quaternionf cameraOrientation;

    @Inject(method = "render", at = @At("HEAD"))
    private void replayModRender_reorientForCubicRendering(Entity entity, double dx, double dy, double dz,
                                                           float iDoNotKnow,
                                                           float partialTicks,
                                                           PoseStack matrixStack,
                                                           MultiBufferSource vertexConsumerProvider,
                                                           int int_1,
                                                           CallbackInfo ci) {
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
