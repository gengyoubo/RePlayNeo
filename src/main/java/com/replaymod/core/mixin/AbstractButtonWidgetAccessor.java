package com.replaymod.core.mixin;

import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractButton.class)
public interface AbstractButtonWidgetAccessor {
    @Accessor
    int getHeight();
}
