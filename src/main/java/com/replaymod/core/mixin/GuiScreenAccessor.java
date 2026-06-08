package com.replaymod.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface GuiScreenAccessor {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry> T invokeAddButton(T drawableElement);
}
