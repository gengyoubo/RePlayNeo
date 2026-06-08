package com.replaymod.core.mixin;

import net.minecraft.client.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Timer.class)
public interface TimerAccessor {
    @Accessor("msPerTick")
    float getTickLength();
    @Accessor("msPerTick")
    @Mutable
    void setTickLength(float value);
}
