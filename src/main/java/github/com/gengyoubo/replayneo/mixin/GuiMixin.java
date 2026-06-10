package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.platform.render.events.RenderHotbarCallback;
import github.com.gengyoubo.replayneo.platform.render.events.RenderSpectatorCrosshairCallback;
import github.com.gengyoubo.replayneo.platform.callbacks.RenderHudCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "canRenderCrosshairForSpectator", at = @At("HEAD"), cancellable = true)
    private void shouldRenderSpectatorCrosshair(CallbackInfoReturnable<Boolean> ci) {
        Boolean state = RenderSpectatorCrosshairCallback.EVENT.invoker().shouldRenderSpectatorCrosshair();
        if (state != null) {
            ci.setReturnValue(state);
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void shouldRenderHotbar(CallbackInfo ci) {
        Boolean state = RenderHotbarCallback.EVENT.invoker().shouldRenderHotbar();
        if (state == Boolean.FALSE) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/Options;renderDebug:Z")
    )
    private void renderOverlay(GuiGraphics stack, float partialTicks, CallbackInfo ci) {
        RenderHudCallback.EVENT.invoker().renderHud(stack, partialTicks);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void replayModRender_skipHudDuringRender(CallbackInfo ci) {
        if (((EntityRendererHandler.IEntityRenderer) Minecraft.getInstance().gameRenderer).replayModRender_getHandler() != null) {
            ci.cancel();
        }
    }
}
