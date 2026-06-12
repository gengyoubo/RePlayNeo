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
package github.com.gengyoubo.replayneo.platform.gui.element;

import github.com.gengyoubo.replayneo.api.gui.element.IGuiLabel;

import github.com.gengyoubo.replayneo.core.gui.element.AbstractGuiElement;
import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import github.com.gengyoubo.replayneo.api.other.GuiContainer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import github.com.gengyoubo.replayneo.core.gui.container.GuiPanel;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import java.util.List;

import java.util.Optional;

import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.literalText;

public abstract class AbstractGuiLabel<T extends AbstractGuiLabel<T>> extends AbstractGuiElement<T> implements IGuiLabel<T> {
    private String text = "";

    private ReadableColor color = ReadableColor.WHITE, disabledColor = ReadableColor.GREY;

    public AbstractGuiLabel() {
    }

    public AbstractGuiLabel(GuiContainer<GuiPanel> container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);
        Font fontRenderer = MCVer.getFontRenderer();
        List<String> lines = fontRenderer.getSplitter().splitLines(literalText(text), size.getWidth(), Style.EMPTY).stream()
                .map(it -> it.visit(Optional::of)).filter(Optional::isPresent).map(Optional::get)
                .toList();
        int y = 0;
        for (String line : lines) {
            renderer.drawString(0, y, isEnabled() ? color : disabledColor, line);
            y+=fontRenderer.lineHeight;
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        Font fontRenderer = MCVer.getFontRenderer();
        return new Dimension(fontRenderer.width(text), fontRenderer.lineHeight);
    }

    @Override
    public ReadableDimension getMaxSize() {
        return getMinSize();
    }

    @Override
    public T setText(String text) {
        this.text = text;
        return getThis();
    }

    @Override
    public T setI18nText(String text, Object... args) {
        return setText(I18n.get(text, args));
    }

    @Override
    public T setColor(ReadableColor color) {
        this.color = color;
        return getThis();
    }

    @Override
    public T setDisabledColor(ReadableColor disabledColor) {
        this.disabledColor = disabledColor;
        return getThis();
    }

    public String getText() {
        return this.text;
    }

    public ReadableColor getColor() {
        return this.color;
    }

    public ReadableColor getDisabledColor() {
        return this.disabledColor;
    }
}
