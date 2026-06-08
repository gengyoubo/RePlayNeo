package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.function.Click;
import de.johni0702.minecraft.gui.versions.callbacks.MouseCallback;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MouseHandler.class)
public abstract class MixinMouseListener {
    @Accessor // Note: for some reason Mixin doesn't include this in the refmap json if it's just a @Shadow field
    abstract int getActiveButton();

    @Inject(method = "method_1611", at = @At("HEAD"), cancellable = true)
    private static void mouseDown(boolean[] result, Screen screen, double x, double y, int button, CallbackInfo ci) {
        if (MouseCallback.EVENT.invoker().mouseDown(new Click(x, y, button))) {
            result[0] = true;
            ci.cancel();
        }
    }

    @Inject(method = "method_1605", at = @At("HEAD"), cancellable = true)
    private static void mouseUp(boolean[] result, Screen screen, double x, double y, int button, CallbackInfo ci) {
        if (MouseCallback.EVENT.invoker().mouseUp(new Click(x, y, button))) {
            result[0] = true;
            ci.cancel();
        }
    }

    @Inject(method = "method_1602", at = @At("HEAD"), cancellable = true)
    private void mouseDrag(Screen screen, double x, double y, double dx, double dy, CallbackInfo ci) {
        if (MouseCallback.EVENT.invoker().mouseDrag(new Click(x, y, getActiveButton()), dx, dy)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "onScroll",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z"
            )
    )
    private boolean mouseScroll(Screen element, double x, double y,
                                double vertical
    ) {
        double horizontal = 0;
        if (MouseCallback.EVENT.invoker().mouseScroll(x, y, horizontal, vertical)) {
            return true;
        } else {
            return element.mouseScrolled(x, y, vertical);
        }
    }
}
