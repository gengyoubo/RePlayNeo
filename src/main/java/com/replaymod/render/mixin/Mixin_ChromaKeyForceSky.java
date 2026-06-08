package com.replaymod.render.mixin;

import com.replaymod.render.hooks.EntityRendererHandler;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Forces the sky to always render when chroma keying is active. Ordinarily it only renders when the render distance is
 * at 4 or greater.
 */
@Mixin(LevelRenderer.class)
public abstract class Mixin_ChromaKeyForceSky {
    @Shadow @Final private Minecraft minecraft;

    // Sky appears to be rendered unconditionally now
}
