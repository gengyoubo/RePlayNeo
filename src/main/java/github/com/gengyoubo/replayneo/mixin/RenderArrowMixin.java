package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.world.entity.projectile.Arrow;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TippableArrowRenderer.class)
public abstract class RenderArrowMixin extends EntityRenderer<Arrow> {
    protected RenderArrowMixin(Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull Arrow entity,
                                @NotNull Frustum camera,
                                double camX, double camY, double camZ) {
        // Force arrows to always render, otherwise they stop rendering when you get close to them
        return ReplayModReplay.instance.getReplayHandler() != null || super.shouldRender(entity, camera, camX, camY, camZ);
    }
}
