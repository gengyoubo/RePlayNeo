package github.com.gengyoubo.replayneo.core.gui;

import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;

public final class GuiCrashReports {
    private GuiCrashReports() {
    }

    public static RuntimeException layout(Throwable throwable, RenderInfo renderInfo, Object container, Object layout) {
        return new IllegalStateException(
                "Gui layout failed: container=" + container
                        + ", layout=" + layout
                        + ", " + describe(renderInfo),
                throwable
        );
    }

    public static RuntimeException render(
            Throwable throwable,
            RenderInfo renderInfo,
            Object container,
            ReadableDimension size,
            Object layout,
            Object element,
            ReadablePoint position,
            ReadableDimension elementSize
    ) {
        return new IllegalStateException(
                "Gui render failed: container=" + container
                        + ", size=" + size
                        + ", layout=" + layout
                        + ", element=" + element
                        + ", position=" + position
                        + ", elementSize=" + elementSize
                        + ", " + describe(renderInfo),
                throwable
        );
    }

    private static String describe(RenderInfo renderInfo) {
        return "partialTick=" + renderInfo.partialTick()
                + ", mouse=(" + renderInfo.mouseX() + ", " + renderInfo.mouseY() + ")"
                + ", layer=" + renderInfo.layer();
    }
}
