package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.render.gui.progress.VirtualWindow;
import github.com.gengyoubo.replayneo.api.hook.MinecraftClientExt;
import github.com.gengyoubo.replayneo.platform.feature.replay.InputReplayTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import github.com.gengyoubo.replayneo.api.events.PostRenderCallback;
import github.com.gengyoubo.replayneo.api.events.PreRenderCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import github.com.gengyoubo.replayneo.api.callbacks.PreTickCallback;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin
        extends ReentrantBlockableEventLoop<Runnable>
        implements MCVer.MinecraftMethodAccessor, MinecraftClientExt {
    @Shadow protected abstract void handleKeybinds();

    @Unique
    private VirtualWindow RePlayCore$windowDelegate;

    @Unique
    private RenderTarget RePlayCore$framebufferDelegate;

    public void replayModProcessKeyBinds() {
        handleKeybinds();
    }

    public void replayModExecuteTaskQueue() {
        runAllTasks();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void preTick(boolean unused, CallbackInfo ci) {
        PreTickCallback.EVENT.invoker().preTick();
    }

    @Override
    public void setWindowDelegate(VirtualWindow window) {
        this.RePlayCore$windowDelegate = window;
    }

    @Override
    public void setFramebufferDelegate(RenderTarget framebuffer) {
        this.RePlayCore$framebufferDelegate = framebuffer;
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
        VirtualWindow delegate = this.RePlayCore$windowDelegate;
        if (delegate != null && delegate.isBound()) {
            Window window = ((Minecraft) (Object) this).getWindow();
            delegate.onResolutionChanged(window.getWidth(), window.getHeight());
            ci.cancel();
        }
    }

    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void useGuiFramebuffer(CallbackInfoReturnable<RenderTarget> ci) {
        RenderTarget delegate = this.RePlayCore$framebufferDelegate;
        if (delegate != null) {
            ci.setReturnValue(delegate);
        }
    }

    MinecraftMixin() { super(null); }
}
