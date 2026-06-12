/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package github.com.gengyoubo.replayneo.platform.gui.container;

import github.com.gengyoubo.replayneo.core.function.MouseClick;
import github.com.gengyoubo.replayneo.core.function.CharacterInput;
import github.com.gengyoubo.replayneo.core.function.KeyboardInput;

import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.core.gui.container.AbstractGuiContainer;
import github.com.gengyoubo.replayneo.platform.gui.MinecraftGuiRenderer;
import github.com.gengyoubo.replayneo.core.gui.OffsetGuiRenderer;
import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import github.com.gengyoubo.replayneo.api.gui.element.GuiElement;
import github.com.gengyoubo.replayneo.api.function.*;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.platform.gui.MouseUtils;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import github.com.gengyoubo.replayneo.platform.gui.GuiCrashReports;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import github.com.gengyoubo.replayneo.api.other.ScreenExt;
import github.com.gengyoubo.replayneo.api.callbacks.PreTickCallback;
import github.com.gengyoubo.replayneo.api.callbacks.RenderHudCallback;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.literalText;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;


public abstract class AbstractGuiOverlay<T extends AbstractGuiOverlay<T>> extends AbstractGuiContainer<T> {

    private final UserInputGuiScreen userInputGuiScreen = new UserInputGuiScreen();
    private final EventHandler eventHandler = new EventHandler();
    private boolean visible;
    private Dimension screenSize;
    private boolean mouseVisible;
    private boolean closeable = true;

    public boolean isVisible() {
        return visible;
    }

    public Minecraft getMinecraft() {
        return MCVer.getMinecraft();
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            if (visible) {
                invokeAll(Loadable.class, Loadable::load);
                eventHandler.register();
            } else {
                invokeAll(Closeable.class, Closeable::close);
                eventHandler.unregister();
            }
            updateUserInputGui();
        }
        this.visible = visible;
    }

    public boolean isMouseVisible() {
        return mouseVisible;
    }

    public void setMouseVisible(boolean mouseVisible) {
        this.mouseVisible = mouseVisible;
        updateUserInputGui();
    }

    public boolean isCloseable() {
        return closeable;
    }

    public void setCloseable(boolean closeable) {
        this.closeable = closeable;
    }

    /**
     * @see #setAllowUserInput(boolean)
     */
    public boolean isAllowUserInput() {
        return ((ScreenExt) userInputGuiScreen).rePlay$doesPassEvents();
    }

    /**
     * Enable/Disable user input for this overlay while the mouse is visible.
     * User input are things like moving the player, attacking/interacting, key bindings but not input into the
     * GUI elements such as text fields.
     * Default for overlays is {@code true} whereas for normal GUI screens it is {@code false}.
     * @param allowUserInput {@code true} to allow user input, {@code false} to disallow it
     */
    public void setAllowUserInput(boolean allowUserInput) {
        ((ScreenExt) userInputGuiScreen).rePlay$setPassEvents(allowUserInput);
    }

    private void updateUserInputGui() {
        var mc = getMinecraft();
        if (visible) {
            if (mouseVisible) {
                if (mc.screen == null) {
                    mc.setScreen(userInputGuiScreen);
                }
            } else {
                if (mc.screen == userInputGuiScreen) {
                    mc.setScreen(null);
                }
            }
        }
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        if (size == null) {
            updateScreenSize();
            size = screenSize;
        }
        super.layout(size, renderInfo);
        if (mouseVisible && renderInfo.layer() == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                tooltip.layout(tooltip.getMinSize(), renderInfo);
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        updateScreenSize();
        super.draw(renderer, size, renderInfo);
        if (mouseVisible && renderInfo.layer() == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                final ReadableDimension tooltipSize = tooltip.getMinSize();
                int x, y;
                if (renderInfo.mouseX() + 8 + tooltipSize.getWidth() < screenSize.getWidth()) {
                    x = renderInfo.mouseX() + 8;
                } else {
                    x = screenSize.getWidth() - tooltipSize.getWidth() - 1;
                }
                if (renderInfo.mouseY() + 8 + tooltipSize.getHeight() < screenSize.getHeight()) {
                    y = renderInfo.mouseY() + 8;
                } else {
                    y = screenSize.getHeight() - tooltipSize.getHeight() - 1;
                }
                final ReadablePoint position = new Point(x, y);
                try {
                    OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, position, tooltipSize);
                    tooltip.draw(eRenderer, tooltipSize, renderInfo);
                } catch (Exception ex) {
                    throw GuiCrashReports.tooltip(ex, renderInfo, this, size, tooltip, position, tooltipSize);
                }
            }
        }
    }

    @Override
    public ReadableDimension getMinSize() {
        updateScreenSize();
        return screenSize;
    }

    @Override
    public ReadableDimension getMaxSize() {
        updateScreenSize();
        return screenSize;
    }

    private void updateScreenSize() {
        var mc = getMinecraft();
        Window res = MCVer.newScaledResolution(mc);
        if (screenSize == null
                || screenSize.getWidth() != res.getGuiScaledWidth()
                || screenSize.getHeight() != res.getGuiScaledHeight()) {
            screenSize = new Dimension(res.getGuiScaledWidth(), res.getGuiScaledHeight());
        }
    }

    private class EventHandler extends EventRegistrations {
        private EventHandler() {}

        { on(RenderHudCallback.EVENT, this::renderOverlay); }
        private void renderOverlay(net.minecraft.client.gui.GuiGraphics stack, float partialTicks) {
            updateUserInputGui();
            updateScreenSize();
            int layers = getMaxLayer();
            int mouseX = -1, mouseY = -1;
            if (mouseVisible) {
                Point mouse = MouseUtils.getMousePos();
                mouseX = mouse.getX();
                mouseY = mouse.getY();
            }
            RenderInfo renderInfo = new RenderInfo(partialTicks, mouseX, mouseY, 0);
            for (int layer = 0; layer <= layers; layer++) {
                layout(screenSize, renderInfo.layer(layer));
            }
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            for (int layer = 0; layer <= layers; layer++) {
                draw(renderer, screenSize, renderInfo.layer(layer));
            }
        }

        { on(PreTickCallback.EVENT, () -> invokeAll(Tickable.class, Tickable::tick)); }
    }

    protected class UserInputGuiScreen extends net.minecraft.client.gui.screens.Screen {

        UserInputGuiScreen() {
            super(literalText(""));
        }

        {
            ((ScreenExt) this).rePlay$setPassEvents(true);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            KeyInput keyInput = new KeyboardInput(keyCode, scanCode, modifiers);
            if (!invokeHandlers(KeyHandler.class, e -> e.handleKey(keyInput))) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        @Override
        public boolean charTyped(char keyChar, int modifiers) {
            CharInput charInput = new CharacterInput(keyChar, modifiers);
            if (!invokeHandlers(CharHandler.class, e -> e.handleChar(charInput))) {
                return super.charTyped(keyChar, modifiers);
            }
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
            Click click = new MouseClick(mouseX, mouseY, mouseButton);
            return
            invokeHandlers(Clickable.class, e -> e.mouseClick(click));
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
            Click click = new MouseClick(mouseX, mouseY, mouseButton);
            return
            invokeHandlers(Draggable.class, e -> e.mouseRelease(click));
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
            Click click = new MouseClick(mouseX, mouseY, mouseButton);
            return
            invokeHandlers(Draggable.class, e -> e.mouseDrag(click));
        }

        @Override
        public void tick() {
            invokeAll(Tickable.class, Tickable::tick);
        }

        @Override
        public boolean mouseScrolled(
                double mouseX,
                double mouseY,
                double dWheel
        ) {
            Point mouse = new Point((int) mouseX, (int) mouseY);
            int wheel = (int) (dWheel * 120);
            return invokeHandlers(Scrollable.class, e -> e.scroll(mouse, wheel));
        }

        @Override
        public void onClose() {
            if (closeable) {
                super.onClose();
            }
        }

        @Override
        public void removed() {
            if (closeable) {
                mouseVisible = false;
            }
        }


        public AbstractGuiOverlay<T> getOverlay() {
            return AbstractGuiOverlay.this;
        }
    }
}

