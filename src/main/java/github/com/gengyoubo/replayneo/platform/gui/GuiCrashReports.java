package github.com.gengyoubo.replayneo.platform.gui;

import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import github.com.gengyoubo.replayneo.api.other.GuiContainer;
import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

public final class GuiCrashReports {
    private GuiCrashReports() {
    }

    public static RuntimeException layout(Throwable throwable, RenderInfo renderInfo, Object container, Object layout) {
        CrashReport crashReport = CrashReport.forThrowable(throwable, "Gui Layout");
        addRenderInfo(crashReport, renderInfo);
        CrashReportCategory category = crashReport.addCategory("Gui container details");
        MCVer.addDetail(category, "Container", container::toString);
        MCVer.addDetail(category, "Layout", layout::toString);
        return new ReportedException(crashReport);
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
        CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering Gui");
        addRenderInfo(crashReport, renderInfo);
        CrashReportCategory category = crashReport.addCategory("Gui container details");
        MCVer.addDetail(category, "Container", container::toString);
        MCVer.addDetail(category, "Width", () -> "" + size.getWidth());
        MCVer.addDetail(category, "Height", () -> "" + size.getHeight());
        MCVer.addDetail(category, "Layout", layout::toString);
        category = crashReport.addCategory("Gui element details");
        MCVer.addDetail(category, "Element", element::toString);
        MCVer.addDetail(category, "Position", position::toString);
        MCVer.addDetail(category, "Size", elementSize::toString);
        if (element instanceof GuiContainer<?> guiContainer) {
            MCVer.addDetail(category, "Layout", () -> guiContainer.getLayout().toString());
        }
        return new ReportedException(crashReport);
    }

    public static RuntimeException tooltip(
            Throwable throwable,
            RenderInfo renderInfo,
            Object container,
            ReadableDimension size,
            Object tooltip,
            ReadablePoint position,
            ReadableDimension tooltipSize
    ) {
        CrashReport crashReport = CrashReport.forThrowable(throwable, "Rendering Gui Tooltip");
        addRenderInfo(crashReport, renderInfo);
        CrashReportCategory category = crashReport.addCategory("Gui container details");
        MCVer.addDetail(category, "Container", container::toString);
        MCVer.addDetail(category, "Width", () -> "" + size.getWidth());
        MCVer.addDetail(category, "Height", () -> "" + size.getHeight());
        category = crashReport.addCategory("Tooltip details");
        MCVer.addDetail(category, "Element", tooltip::toString);
        MCVer.addDetail(category, "Position", position::toString);
        MCVer.addDetail(category, "Size", tooltipSize::toString);
        return new ReportedException(crashReport);
    }

    private static void addRenderInfo(CrashReport crashReport, RenderInfo renderInfo) {
        CrashReportCategory category = crashReport.addCategory("Render info details");
        MCVer.addDetail(category, "Partial Tick", () -> "" + renderInfo.partialTick());
        MCVer.addDetail(category, "Mouse X", () -> "" + renderInfo.mouseX());
        MCVer.addDetail(category, "Mouse Y", () -> "" + renderInfo.mouseY());
        MCVer.addDetail(category, "Layer", () -> "" + renderInfo.layer());
    }
}
