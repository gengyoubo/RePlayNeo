package github.com.gengyoubo.replayneo.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.renderer.RenderStateShard.class)
public class RenderItemMixin {
    @Redirect(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"))
    private static long getEnchantmentTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return Util.getMillis();
    }
}
