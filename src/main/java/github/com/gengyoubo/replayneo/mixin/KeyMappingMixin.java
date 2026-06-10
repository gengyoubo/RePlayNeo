package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.platform.input.ForgeKeyBindingRegistry;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * We have bunch of keybindings which only have an effect while in a replay but heavily conflict with vanilla ones
 * otherwise. To work around this, we prevent our keybindings (or conflicting ones) from making it into the keysByCode
 * map, depending on the current context.
 */
@Mixin(KeyMapping.class)
public class KeyMappingMixin {
    @Shadow @Final private static Map<String, KeyMapping> ALL;
    @Unique private static Collection<KeyMapping> rePlay$keyBindings() { return KeyMappingMixin.ALL.values(); }

    @Unique private static final List<KeyMapping> rePlay$temporarilyRemoved = new ArrayList<>();

    @Inject(method = "resetMapping", at = @At("HEAD"))
    private static void preContextualKeyBindings(CallbackInfo ci) {
        RePlayCore mod = RePlayCore.instance;
        if (mod == null) {
            return;
        }
        if (!(mod.getKeyBindingRegistry() instanceof ForgeKeyBindingRegistry keyBindingRegistry)) {
            return;
        }
        Set<KeyMapping> onlyInReplay = keyBindingRegistry.getOnlyInReplay();
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            // In replay, remove any conflicting key bindings, so that ours are guaranteed in
            rePlay$keyBindings().removeIf(KeyMapping -> {
                if (onlyInReplay.contains(KeyMapping)) {
                    return false;
                }
                for (KeyMapping exclusiveBinding : onlyInReplay) {
                    if (KeyMapping.same(exclusiveBinding) && KeyMapping != exclusiveBinding) {
                        rePlay$temporarilyRemoved.add(KeyMapping);
                        return true;
                    }
                }
                return false;
            });
        } else {
            // Not in a replay, remove all replay-exclusive keybindings
            rePlay$keyBindings().removeIf(KeyMapping -> {
                if (onlyInReplay.contains(KeyMapping)) {
                    rePlay$temporarilyRemoved.add(KeyMapping);
                    return true;
                }
                return false;
            });
        }
    }

    @Inject(method = "resetMapping", at = @At("RETURN"))
    private static void postContextualKeyBindings(CallbackInfo ci) {
        for (KeyMapping keyMapping : rePlay$temporarilyRemoved) {
            KeyMappingMixin.ALL.put(keyMapping.getName(), keyMapping);
        }
        rePlay$temporarilyRemoved.clear();
    }

}
