package com.replaymod.replay.mixin;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface FirstPersonRendererAccessor {
    @Accessor("mainHandItem")
    void setItemStackMainHand(ItemStack value);
    @Accessor("offHandItem")
    void setItemStackOffHand(ItemStack value);
    @Accessor("mainHandHeight")
    void setEquippedProgressMainHand(float value);
    @Accessor("oMainHandHeight")
    void setPrevEquippedProgressMainHand(float value);
    @Accessor("offHandHeight")
    void setEquippedProgressOffHand(float value);
    @Accessor("oOffHandHeight")
    void setPrevEquippedProgressOffHand(float value);
}
