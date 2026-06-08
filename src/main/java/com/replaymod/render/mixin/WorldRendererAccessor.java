package com.replaymod.render.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import net.minecraft.client.renderer.LevelRenderer;

@Mixin(LevelRenderer.class)
public interface WorldRendererAccessor {
}
