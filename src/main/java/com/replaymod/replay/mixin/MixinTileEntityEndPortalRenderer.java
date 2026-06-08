package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.render.RenderPhase$PortalTexturing")
public class MixinTileEntityEndPortalRenderer {
    @Redirect(method = "method_23557", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    static
    private long replayModReplay_getPortalTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return Util.getMillis();
    }
}
