package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.render.gui.progress.VirtualWindow;
import github.com.gengyoubo.replayneo.feature.render.hooks.MinecraftClientExt;
import github.com.gengyoubo.replayneo.feature.replay.InputReplayTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import github.com.gengyoubo.replayneo.core.events.PostRenderCallback;
import github.com.gengyoubo.replayneo.core.events.PreRenderCallback;

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

    public void replayModProcessKeyBinds() {
        handleKeybinds();
    }

    public void replayModExecuteTaskQueue() {
        runAllTasks();
    }

    @Unique
    public void rePlay$setWindowDelegate(VirtualWindow window) {
        this.replayMod$windowDelegate = window;
    }

    @Unique
    public void rePlay$setFramebufferDelegate(RenderTarget framebuffer) {
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
