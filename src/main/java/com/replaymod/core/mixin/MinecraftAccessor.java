package com.replaymod.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import net.minecraft.network.Connection;
import java.util.concurrent.CompletableFuture;



@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("timer")
    Timer getTimer();
    @Accessor("timer")
    @Mutable
    void setTimer(Timer value);

    @Accessor
    CompletableFuture<Void> getPendingReload();
    @Accessor
    void setPendingReload(CompletableFuture<Void> value);


    @Accessor("delayedCrash")
    Supplier<CrashReport> getCrashReporter();


    @Accessor("pendingConnection")
    void setConnection(Connection connection);
}
