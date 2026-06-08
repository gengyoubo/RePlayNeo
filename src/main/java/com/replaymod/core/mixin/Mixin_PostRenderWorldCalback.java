package com.replaymod.core.mixin;

import com.replaymod.core.events.PostRenderWorldCallback;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(LevelRenderer.class)
public class Mixin_PostRenderWorldCalback {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void postRenderWorld(CallbackInfo ci, @Local(argsOnly = true) PoseStack matrixStack) {
        PostRenderWorldCallback.EVENT.invoker().postRenderWorld(matrixStack);
    }
}
