package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.versions.ScreenExt;
import github.com.gengyoubo.replayneo.platform.callbacks.InitScreenCallback;
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
public class ScreenMixin implements ScreenExt {

    @Shadow @Final private List<GuiEventListener> children;

    @Unique
    private boolean replayMod$passEvents;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
    private void preInit(CallbackInfo ci) {
        rePlay$firePreInit();
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        rePlay$firePostInit();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void preResize(CallbackInfo ci) {
        rePlay$firePreInit();
    }
    
    @Inject(method = "resize", at = @At("TAIL"))
    private void resize(CallbackInfo ci) {
        rePlay$firePostInit();
    }

    @Unique
    private void rePlay$firePreInit() {
        InitScreenCallback.Pre.EVENT.invoker().preInitScreen((Screen) (Object) this);
    }

    @Unique
    private void rePlay$firePostInit() {
        InitScreenCallback.EVENT.invoker().initScreen(
                (Screen) (Object) this,
                Collections2.transform(Collections2.filter(this.children, it -> it instanceof AbstractButton), it -> (AbstractButton) it)
        );
    }

    @Override
    public boolean rePlay$doesPassEvents() {
        return this.replayMod$passEvents;
    }

    @Override
    public void rePlay$setPassEvents(boolean passEvents) {
        this.replayMod$passEvents = passEvents;
    }
}
