package github.com.gengyoubo.replayneo.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.LevelRenderer;

@Mixin(LevelRenderer.class)
public interface WorldRendererAccessor {
}
