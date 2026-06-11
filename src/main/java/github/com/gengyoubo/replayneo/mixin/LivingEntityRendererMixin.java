package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.platform.compat.ChangedReplayCompat;
import github.com.gengyoubo.replayneo.platform.camera.CameraEntity;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.*;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static boolean replayneo$renderingChangedReplayForm;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void replayneo$renderChangedReplayForm(LivingEntity entity, float entityYaw, float partialTick,
                                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                                   int packedLight, CallbackInfo ci) {
        ReplayModReplay replay = ReplayModReplay.instance;
        if (replayneo$renderingChangedReplayForm
                || replay == null
                || replay.getReplayHandler() == null
                || !(entity instanceof Player player)
                || entity instanceof CameraEntity) {
            return;
        }

        replayneo$renderingChangedReplayForm = true;
        try {
            if (ChangedReplayCompat.renderReplayForm(player, poseStack, bufferSource, packedLight, partialTick)) {
                ci.cancel();
            }
        } finally {
            replayneo$renderingChangedReplayForm = false;
        }
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_canRenderInvisibleName(
            LivingEntity entity,
            CallbackInfoReturnable<Boolean> ci
    ) {
        Player thePlayer = getMinecraft().player;
        if (thePlayer instanceof CameraEntity && entity.isInvisible()) {
            ci.setReturnValue(false);
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"
            )
    )
    private boolean replayModReplay_shouldInvisibleNotBeRendered(LivingEntity entity, Player thePlayer) {
        return entity.isInvisibleTo(thePlayer);
    }
}
