package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.addon.playeroverview.PlayerOverview;
import github.com.gengyoubo.replayneo.platform.feature.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.platform.addon.ReplayModExtras;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderer.class, priority = 1200)
public abstract class EntityRendererMixin {
    @Inject(method = "renderNameTag", at = @At("HEAD"), cancellable = true)
    private void replayModRender_areAllNamesHidden(CallbackInfo ci) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler != null && !handler.getSettings().isRenderNameTags()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    public void replayModExtras_isPlayerHidden(Entity entity, @Coerce Object camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> ci) {
        ReplayModExtras.instance.get(PlayerOverview.class).ifPresent(playerOverview -> {
            if (entity instanceof Player player) {
                if (playerOverview.isHidden(player.getUUID())) {
                    ci.setReturnValue(false);
                }
            }
        });
    }
}
