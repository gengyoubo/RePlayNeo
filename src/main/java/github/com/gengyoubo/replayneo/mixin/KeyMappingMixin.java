package com.replaymod.core.mixin;

import com.replaymod.core.ReplayMod;
import com.replaymod.replay.ReplayModReplay;
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
    @Unique private static Collection<KeyMapping> keyBindings() { return KeyMappingMixin.ALL.values(); }

    @Unique private static final List<KeyMapping> temporarilyRemoved = new ArrayList<>();

    @Inject(method = "resetMapping", at = @At("HEAD"))
    private static void preContextualKeyBindings(CallbackInfo ci) {
        ReplayMod mod = ReplayMod.instance;
        if (mod == null) {
            return;
        }
        Set<KeyMapping> onlyInReplay = mod.getKeyBindingRegistry().getOnlyInReplay();
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            // In replay, remove any conflicting key bindings, so that ours are guaranteed in
            keyBindings().removeIf(KeyMapping -> {
                for (KeyMapping exclusiveBinding : onlyInReplay) {
                    if (KeyMapping.equals(exclusiveBinding) && KeyMapping != exclusiveBinding) {
                        temporarilyRemoved.add(KeyMapping);
                        return true;
                    }
                }
                return false;
            });
        } else {
            // Not in a replay, remove all replay-exclusive keybindings
            keyBindings().removeIf(KeyMapping -> {
                if (onlyInReplay.contains(KeyMapping)) {
                    temporarilyRemoved.add(KeyMapping);
                    return true;
                }
                return false;
            });
        }
    }

    @Inject(method = "resetMapping", at = @At("RETURN"))
    private static void postContextualKeyBindings(CallbackInfo ci) {
        for (KeyMapping keyMapping : temporarilyRemoved) {
            KeyMappingMixin.ALL.put(keyMapping.getName(), keyMapping);
        }
        temporarilyRemoved.clear();
    }
}
