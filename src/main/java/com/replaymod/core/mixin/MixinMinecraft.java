package com.replaymod.core.mixin;

import com.replaymod.core.versions.MCVer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import com.replaymod.core.events.PostRenderCallback;
import com.replaymod.core.events.PreRenderCallback;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
        extends ReentrantBlockableEventLoop<Runnable>
        implements MCVer.MinecraftMethodAccessor {
    @Shadow protected abstract void handleKeybinds();

    @Override
    public void replayModProcessKeyBinds() {
        handleKeybinds();
    }

    @Override
    public void replayModExecuteTaskQueue() {
        runAllTasks();
    }

    private static final String GAME_RENDERER_RENDER = "Lnet/minecraft/client/render/GameRenderer;render(FJZ)V";

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

    MixinMinecraft() { super(null); }
}
