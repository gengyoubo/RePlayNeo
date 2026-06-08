package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.feature.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.platform.callbacks.RenderHudCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void replayneo$skipHudRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (((EntityRendererHandler.IEntityRenderer) minecraft.gameRenderer).replayModRender_getHandler() != null
                || minecraft.level != null && minecraft.player == null) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void replayneo$renderHudCallbacks(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        RenderHudCallback.EVENT.invoker().renderHud(guiGraphics, partialTick);
    }
}
