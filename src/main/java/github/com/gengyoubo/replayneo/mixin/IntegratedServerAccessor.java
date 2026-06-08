package com.replaymod.recording.mixin;

import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IntegratedServer.class)
public interface IntegratedServerAccessor {
    // TODO probably https://github.com/ReplayMod/remap/issues/10
    @Accessor("paused")
    boolean isGamePaused();
}
