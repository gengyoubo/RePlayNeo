package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.function.CharInput;
import de.johni0702.minecraft.gui.function.KeyInput;
import de.johni0702.minecraft.gui.versions.callbacks.KeyboardCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardListener {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true)
    private void replayMod_keyPress(long window, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (this.minecraft.screen == null) {
            return;
        }
        KeyInput input = new KeyInput(keyCode, scanCode, modifiers);
        if (action == 1 && KeyboardCallback.EVENT.invoker().keyPressed(input)) {
            ci.cancel();
        } else if (action == 0 && KeyboardCallback.EVENT.invoker().keyReleased(input)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped(JII)V", at = @At("HEAD"), cancellable = true)
    private void replayMod_charTyped(long window, int keyChar, int modifiers, CallbackInfo ci) {
        if (this.minecraft.screen != null && KeyboardCallback.EVENT.invoker().charTyped(new CharInput((char) keyChar, modifiers))) {
            ci.cancel();
        }
    }
}
