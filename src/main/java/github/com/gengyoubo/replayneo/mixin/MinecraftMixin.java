package com.replaymod.core.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.gui.progress.VirtualWindow;
import com.replaymod.render.hooks.MinecraftClientExt;
import com.replaymod.replay.InputReplayTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin
        extends ReentrantBlockableEventLoop<Runnable>
        implements MCVer.MinecraftMethodAccessor, MinecraftClientExt {
    @Shadow protected abstract void handleKeybinds();

    @Unique
    private VirtualWindow replayMod$windowDelegate;

    @Unique
    private RenderTarget replayMod$framebufferDelegate;

    @Override
    public void replayModProcessKeyBinds() {
        handleKeybinds();
    }

    @Override
    public void replayModExecuteTaskQueue() {
        runAllTasks();
    }

    @Override
    public void setWindowDelegate(VirtualWindow window) {
        this.replayMod$windowDelegate = window;
    }

    @Override
    public void setFramebufferDelegate(RenderTarget framebuffer) {
        this.replayMod$framebufferDelegate = framebuffer;
    }

    @Inject(method = "runTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"))
    private void preRender(boolean unused, CallbackInfo ci) {
        PreRenderCallback.EVENT.invoker().preRender();
    }

    @Inject(method = "runTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V",
                    shift = At.Shift.AFTER))
    private void postRender(boolean unused, CallbackInfo ci) {
        PostRenderCallback.EVENT.invoker().postRender();
    }

    @Inject(method = "runTick", at = @At(value = "CONSTANT", args = "stringValue=scheduledExecutables"))
    private void updateInReplay(CallbackInfo ci) {
        InputReplayTimer.updateInReplay();
    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"), cancellable = true)
    private void suppressResizeDuringRender(CallbackInfo ci) {
        VirtualWindow delegate = this.replayMod$windowDelegate;
        if (delegate != null && delegate.isBound()) {
            Window window = ((Minecraft) (Object) this).getWindow();
            delegate.onResolutionChanged(window.getWidth(), window.getHeight());
            ci.cancel();
        }
    }

    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void useGuiFramebuffer(CallbackInfoReturnable<RenderTarget> ci) {
        RenderTarget delegate = this.replayMod$framebufferDelegate;
        if (delegate != null) {
            ci.setReturnValue(delegate);
        }
    }

    MinecraftMixin() { super(null); }
}
