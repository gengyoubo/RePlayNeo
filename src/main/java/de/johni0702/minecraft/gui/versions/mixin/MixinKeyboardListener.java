package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.function.CharInput;
import de.johni0702.minecraft.gui.function.KeyInput;
import de.johni0702.minecraft.gui.versions.callbacks.KeyboardCallback;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardListener {
    @Inject(
            method = "method_1454",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"),
            cancellable = true
    )
    static
    private void keyPressed(int i, Screen screen, boolean[] bls, int keyCode, int scanCode, int modifiers, CallbackInfo ci) {
        if (KeyboardCallback.EVENT.invoker().keyPressed(new KeyInput(keyCode, scanCode, modifiers))) {
            bls[0] = true;
            ci.cancel();
        }
    }

    @Inject(
            method = "method_1454",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyReleased(III)Z"),
            cancellable = true
    )
    static
    private void keyReleased(int i, Screen screen, boolean[] bls, int keyCode, int scanCode, int modifiers, CallbackInfo ci) {
        if (KeyboardCallback.EVENT.invoker().keyReleased(new KeyInput(keyCode, scanCode, modifiers))) {
            bls[0] = true;
            ci.cancel();
        }
    }

    @Inject(method = "method_1458", at = @At("HEAD"), cancellable = true)
    @Group(min = 1, max = 1, name = "replaymod-jgui-charTyped-int")
    private static void charTyped(GuiEventListener element, int keyChar, int modifiers, CallbackInfo ci) {
        if (KeyboardCallback.EVENT.invoker().charTyped(new CharInput((char) keyChar, modifiers))) {
            ci.cancel();
        }
    }

    @Inject(method = "lambda$onCharEvent$5", at = @At("HEAD"), cancellable = true, remap = false)
    @Group(min = 1, max = 1, name = "replaymod-jgui-charTyped-int")
    private void charTypedOptifine(int keyChar, int modifiers, GuiEventListener element, CallbackInfo ci) {
        if (KeyboardCallback.EVENT.invoker().charTyped(new CharInput((char) keyChar, modifiers))) {
            ci.cancel();
        }
   }

   @Inject(method = "method_1473", at = @At("HEAD"), cancellable = true)
   @Group(min = 1, max = 1, name = "replaymod-jgui-charTyped-char")
   private static void charTyped(GuiEventListener element, char keyChar, int modifiers, CallbackInfo ci) {
       if (KeyboardCallback.EVENT.invoker().charTyped(new CharInput(keyChar, modifiers))) {
           ci.cancel();
       }
   }

    @Inject(method = "lambda$onCharEvent$6", at = @At("HEAD"), cancellable = true, remap = false)
    @Group(min = 1, max = 1, name = "replaymod-jgui-charTyped-char")
    private void charTypedOptifine(char keyChar, int modifiers, GuiEventListener element, CallbackInfo ci) {
        if (KeyboardCallback.EVENT.invoker().charTyped(new CharInput(keyChar, modifiers))) {
            ci.cancel();
        }
    }
}
