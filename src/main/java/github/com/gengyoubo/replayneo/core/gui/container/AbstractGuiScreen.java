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
package github.com.gengyoubo.replayneo.core.gui.container;

import github.com.gengyoubo.replayneo.core.function.MouseClick;

import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.MinecraftGuiRenderer;
import github.com.gengyoubo.replayneo.OffsetGuiRenderer;
import github.com.gengyoubo.replayneo.RenderInfo;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiElement;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiLabel;
import github.com.gengyoubo.replayneo.api.function.*;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import github.com.gengyoubo.replayneo.platform.gui.GuiCrashReports;
import org.jetbrains.annotations.NotNull;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.literalText;


public abstract class AbstractGuiScreen<T extends AbstractGuiScreen<T>> extends AbstractGuiContainer<T> {

    private final MinecraftGuiScreen wrapped = new MinecraftGuiScreen();

    private Dimension screenSize;

    private Background background = Background.DEFAULT;

    private boolean enabledRepeatedKeyEvents = true;

    private GuiLabel title;

    protected boolean suppressVanillaKeys;

    public net.minecraft.client.gui.screens.Screen toMinecraft() {
        return wrapped;
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        if (size == null) {
            size = screenSize;
        }
        if (renderInfo.layer() == 0) {
            if (title != null) {
                title.layout(title.getMinSize(), renderInfo);
            }
        }
        super.layout(size, renderInfo);
        if (renderInfo.layer() == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                tooltip.layout(tooltip.getMinSize(), renderInfo);
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        if (renderInfo.layer() == 0) {
            switch (background) {
                case NONE:
                    break;
                case DEFAULT:
                    wrapped.renderBackground((net.minecraft.client.gui.GuiGraphics) renderer.getContext());
                    break;
                case TRANSPARENT:
                    int top = 0xc0_10_10_10, bottom = 0xd0_10_10_10;
                    renderer.drawRect(0, 0, size.getWidth(), size.getHeight(), top, top, bottom, bottom);
                    break;
                case DIRT:
                    wrapped.renderDirtBackground((net.minecraft.client.gui.GuiGraphics) renderer.getContext());
                    break;
            }
            if (title != null) {
                ReadableDimension titleSize = title.getMinSize();
                int x = screenSize.getWidth() / 2 - titleSize.getWidth() / 2;
                OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, new Point(x, 10), new Dimension(0, 0));
                title.draw(eRenderer, titleSize, renderInfo);
            }
        }
        super.draw(renderer, size, renderInfo);
        if (renderInfo.layer() == getMaxLayer()) {
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
        return screenSize;
    }

    @Override
    public ReadableDimension getMaxSize() {
        return screenSize;
    }

    public void setEnabledRepeatedKeyEvents(boolean enableRepeatKeyEvents) {
        this.enabledRepeatedKeyEvents = enableRepeatKeyEvents;
    }

    public void display() {
        getMinecraft().setScreen(toMinecraft());
    }

    public Background getBackground() {
        return this.background;
    }

    public boolean isEnabledRepeatedKeyEvents() {
        return this.enabledRepeatedKeyEvents;
    }

    public GuiLabel getTitle() {
        return this.title;
    }

    public void setBackground(Background background) {
        this.background = background;
    }

    public void setTitle(GuiLabel title) {
        this.title = title;
    }

    protected class MinecraftGuiScreen extends net.minecraft.client.gui.screens.Screen {
        private boolean active;

        protected MinecraftGuiScreen() {
            super(null);
        }

        @Override
        public @NotNull net.minecraft.network.chat.Component getTitle() {
            GuiLabel title = AbstractGuiScreen.this.title;
            return literalText(title == null ? "" : title.getText());
        }


        @Override
        public void render(@NotNull net.minecraft.client.gui.GuiGraphics stack, int mouseX, int mouseY, float partialTicks) {
            // The Forge loading screen apparently leaves one of the textures of the GlStateManager in an
            // incorrect state which can cause the whole screen to just remain white. This is a workaround.

            int layers = getMaxLayer();
            RenderInfo renderInfo = new RenderInfo(partialTicks, mouseX, mouseY, 0);
            for (int layer = 0; layer <= layers; layer++) {
                layout(screenSize, renderInfo.layer(layer));
            }
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            for (int layer = 0; layer <= layers; layer++) {
                draw(renderer, screenSize, renderInfo.layer(layer));
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            KeyInput keyInput = new KeyInput(keyCode, scanCode, modifiers);
            if (!invokeHandlers(KeyHandler.class, e -> e.handleKey(keyInput))) {
                if (suppressVanillaKeys) {
                    return false;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        @Override
        public boolean charTyped(char keyChar, int modifiers) {
            CharInput charInput = new CharInput(keyChar, modifiers);
            if (!invokeHandlers(CharHandler.class, e -> e.handleChar(charInput))) {
                if (suppressVanillaKeys) {
                    return false;
                }
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
        public void removed() {
            invokeAll(Closeable.class, Closeable::close);
            active = false;
        }

        @Override
        public void init() {
            active = false;
            screenSize = new Dimension(width, height);
            invokeAll(Loadable.class, Loadable::load);
        }

        public T getWrapper() {
            return AbstractGuiScreen.this.getThis();
        }
    }

    public enum Background {
        NONE, DEFAULT, TRANSPARENT, DIRT
    }
}

