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

import github.com.gengyoubo.replayneo.core.gui.element.AbstractComposedGuiElement;
import github.com.gengyoubo.replayneo.core.gui.element.AbstractGuiClickable;
import github.com.gengyoubo.replayneo.core.gui.element.AbstractGuiElement;
import github.com.gengyoubo.replayneo.api.gui.element.ComposedGuiElement;
import github.com.gengyoubo.replayneo.api.gui.element.GuiElement;
import github.com.gengyoubo.replayneo.api.gui.element.IGuiClickable;
import github.com.gengyoubo.replayneo.api.gui.element.IGuiToggleButton;
import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import github.com.gengyoubo.replayneo.api.GuiContainer;
import github.com.gengyoubo.replayneo.api.function.Click;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;

public abstract class AbstractGuiToggleButton<V, T extends AbstractGuiToggleButton<V, T>>
        extends AbstractGuiButton<T> implements IGuiToggleButton<V,T> {

    private int selected;

    private V[] values;

    public AbstractGuiToggleButton() {
    }

    public AbstractGuiToggleButton(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        String orgLabel = getLabel();
        setLabel(orgLabel + ": " + values[selected]);
        super.draw(renderer, size, renderInfo);
        setLabel(orgLabel);
    }

    @Override
    public void onClick(Click click) {
        selected = (selected + 1) % values.length;
        super.onClick(click);
    }

    @SafeVarargs
    @Override
    public final T setValues(V... values) {
        this.values = values;
        return getThis();
    }

    @Override
    public T setSelected(int selected) {
        this.selected = selected;
        return getThis();
    }

    @Override
    public V getSelectedValue() {
        return values[selected];
    }

    public int getSelected() {
        return this.selected;
    }

    public V[] getValues() {
        return this.values;
    }
}
