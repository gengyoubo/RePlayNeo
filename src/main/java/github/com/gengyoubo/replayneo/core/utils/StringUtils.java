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
package github.com.gengyoubo.replayneo.core.utils;

import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;

public class StringUtils {
    public static String[] splitStringInMultipleRows(String string, int maxWidth) {
        if(string == null) return new String[0];
        Font fontRenderer = MCVer.getFontRenderer();
        List<String> rows = new ArrayList<>();
        String remaining = string;
        while(!remaining.isEmpty()) {
            String[] split = remaining.split(" ");
            StringBuilder b = new StringBuilder();
            for(String sp : split) {
                b.append(sp).append(" ");
                if (fontRenderer.width(b.toString().trim()) > maxWidth) {
                    b = new StringBuilder(b.substring(0, b.toString().trim().length() - (sp.length())));
                    break;
                }
            }
            String trimmed = b.toString().trim();
            rows.add(trimmed);
            try {
                remaining = remaining.substring(trimmed.length() + 1);
            } catch(Exception e) {
                break;
            }
        }

        return rows.toArray(new String[0]);
    }
}
