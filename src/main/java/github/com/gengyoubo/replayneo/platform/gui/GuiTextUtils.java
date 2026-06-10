package github.com.gengyoubo.replayneo.platform.gui;

import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;

import java.util.ArrayList;
import java.util.List;

public final class GuiTextUtils {
    private GuiTextUtils() {
    }

    public static String[] splitStringInMultipleRows(String string, int maxWidth) {
        if (string == null) {
            return new String[0];
        }
        List<String> rows = new ArrayList<>();
        String remaining = string;
        while (!remaining.isEmpty()) {
            String[] split = remaining.split(" ");
            StringBuilder builder = new StringBuilder();
            for (String part : split) {
                builder.append(part).append(" ");
                if (ReplayPlatforms.get().client().textWidth(builder.toString().trim()) > maxWidth) {
                    builder = new StringBuilder(builder.substring(0, builder.toString().trim().length() - part.length()));
                    break;
                }
            }
            String trimmed = builder.toString().trim();
            rows.add(trimmed);
            try {
                remaining = remaining.substring(trimmed.length() + 1);
            } catch (Exception e) {
                break;
            }
        }

        return rows.toArray(new String[0]);
    }
}
