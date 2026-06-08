package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import com.google.common.collect.Collections2;

// Increased priority so we can consider existing third-party buttons when choosing the position for our button
@Mixin(value = Screen.class, priority = 1100)
public class MixinScreen {

    @Shadow @Final private List<GuiEventListener> children;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
    private void preInit(CallbackInfo ci) {
        firePreInit();
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        firePostInit();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void preResize(CallbackInfo ci) {
        firePreInit();
    }
    
    @Inject(method = "resize", at = @At("TAIL"))
    private void resize(CallbackInfo ci) {
        firePostInit();
    }

    @Unique
    private void firePreInit() {
        InitScreenCallback.Pre.EVENT.invoker().preInitScreen((Screen) (Object) this);
    }

    @Unique
    private void firePostInit() {
        InitScreenCallback.EVENT.invoker().initScreen(
                (Screen) (Object) this,
                Collections2.transform(Collections2.filter(this.children, it -> it instanceof ClickableWidget), it -> (ClickableWidget) it)
        );
    }
}
