package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.core.events.KeyBindingEventCallback;
import github.com.gengyoubo.replayneo.feature.replay.InputReplayTimer;
import github.com.gengyoubo.replayneo.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.function.Click;
import github.com.gengyoubo.replayneo.platform.callbacks.MouseCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private int activeButton;
    @Shadow private double xpos;
    @Shadow private double ypos;
    @Shadow private boolean mouseGrabbed;

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void noGrab(CallbackInfo ci) {
        // Used to be provided by Forge for 1.12.2 and below.
        if (Boolean.valueOf(System.getProperty("fml.noGrab", "false"))) {
            this.mouseGrabbed = true;
            ci.cancel();
        }
    }

    @Inject(method = "onPress(JIII)V", at = @At("HEAD"), cancellable = true)
    private void replayMod_onPress(long window, int button, int action, int mods, CallbackInfo ci) {
        if (this.minecraft.screen == null) {
            return;
        }
        Click click = new Click(this.xpos, this.ypos, button);
        if (action == 1 && MouseCallback.EVENT.invoker().mouseDown(click)) {
            ci.cancel();
        } else if (action == 0 && MouseCallback.EVENT.invoker().mouseUp(click)) {
            ci.cancel();
        }
    }

    @Inject(method = "onPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V", shift = At.Shift.AFTER))
    private void afterKeyBindingTick(CallbackInfo ci) {
        KeyBindingEventCallback.EVENT.invoker().onKeybindingEvent();
    }

    @Inject(method = "onMove(JDD)V", at = @At("HEAD"), cancellable = true)
    private void replayMod_onMove(long window, double x, double y, CallbackInfo ci) {
        if (this.minecraft.screen != null && MouseCallback.EVENT.invoker().mouseDrag(new Click(x, y, this.activeButton), x - this.xpos, y - this.ypos)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "onScroll",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z"
            )
    )
    private boolean mouseScroll(Screen element, double x, double y, double vertical) {
        double horizontal = 0;
        if (MouseCallback.EVENT.invoker().mouseScroll(x, y, horizontal, vertical)) {
            return true;
        } else {
            return element.mouseScrolled(x, y, vertical);
        }
    }

    @Inject(method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private void handleReplayModScroll(
            long _p0, double _p1, double _p2,
            CallbackInfo ci,
            double _offset,
            double _scrollDelta,
            int yOffsetAccumulated
    ) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            InputReplayTimer.handleScroll((int) (yOffsetAccumulated * 120));
            ci.cancel();
        }
    }
}
