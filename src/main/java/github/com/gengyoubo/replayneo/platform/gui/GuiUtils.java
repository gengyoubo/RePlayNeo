package github.com.gengyoubo.replayneo.platform.gui;

import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import github.com.gengyoubo.replayneo.api.Colors;
import github.com.gengyoubo.replayneo.api.GuiContainer;
import github.com.gengyoubo.replayneo.api.ReplayCrashReport;
import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import github.com.gengyoubo.replayneo.core.gui.container.AbstractGuiScrollable;
import github.com.gengyoubo.replayneo.core.gui.container.GuiPanel;
import github.com.gengyoubo.replayneo.core.gui.container.GuiScrollable;
import github.com.gengyoubo.replayneo.api.gui.element.GuiElement;
import github.com.gengyoubo.replayneo.core.gui.layout.HorizontalLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.VerticalLayout;
import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiButton;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiLabel;
import github.com.gengyoubo.replayneo.platform.gui.popup.GuiInfoPopup;
import github.com.gengyoubo.replayneo.platform.versions.Image;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

public final class GuiUtils {
    private static final Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    public static final Image DEFAULT_THUMBNAIL;

    static {
        Image thumbnail;
        try (InputStream stream = GuiUtils.class.getResourceAsStream("/default_thumb.png")) {
            thumbnail = Image.read(stream);
        } catch (Exception e) {
            thumbnail = new Image(1, 1);
            e.printStackTrace();
        }
        DEFAULT_THUMBNAIL = thumbnail;
    }

    private GuiUtils() {
    }

    public static boolean isCtrlDown() {
        return ReplayPlatforms.get().input().controlDown();
    }

    public static void error(Logger logger, GuiContainer container, Object crashReport, Runnable onClose) {
        String crashReportStr = friendlyReport(crashReport);

        logger.error(crashReportStr);
        logger.debug("Not saving crash report as file already exists: {}", crashReportSaveFile(crashReport));

        logger.trace("Opening crash report popup GUI");
        GuiCrashReportPopup popup = new GuiCrashReportPopup(container, crashReportStr);
        popup.onClosed(() -> {
            logger.trace("Crash report popup closed");
            if (onClose != null) {
                onClose.run();
            }
        });
    }

    private static String friendlyReport(Object crashReport) {
        if (crashReport instanceof ReplayCrashReport replayCrashReport) {
            return replayCrashReport.friendlyReport();
        }
        try {
            return String.valueOf(crashReport.getClass().getMethod("getFriendlyReport").invoke(crashReport));
        } catch (ReflectiveOperationException e) {
            return String.valueOf(crashReport);
        }
    }

    private static String crashReportSaveFile(Object crashReport) {
        if (crashReport instanceof ReplayCrashReport replayCrashReport) {
            return replayCrashReport.saveFile();
        }
        try {
            return String.valueOf(crashReport.getClass().getMethod("getSaveFile").invoke(crashReport));
        } catch (ReflectiveOperationException e) {
            return "unknown";
        }
    }

    public static void denyIfMinimalMode(GuiContainer container, Runnable onPopupClosed, Runnable orElseRun) {
        if (isNotMinimalModeElsePopup(container, onPopupClosed)) {
            orElseRun.run();
        }
    }

    public static boolean ifMinimalModeDoPopup(GuiContainer container, Runnable onPopupClosed) {
        return !isNotMinimalModeElsePopup(container, onPopupClosed);
    }

    public static boolean isNotMinimalModeElsePopup(GuiContainer container, Runnable onPopupClosed) {
        if (!RePlayCore.isMinimalMode()) {
            LOGGER.trace("Minimal mode not active, continuing");
            return true;
        }
        LOGGER.trace("Minimal mode active, denying action, opening popup");

        MinimalModeUnsupportedPopup popup = new MinimalModeUnsupportedPopup(container);
        popup.onClosed(() -> {
            LOGGER.trace("Minimal mode popup closed");
            if (onPopupClosed != null) {
                onPopupClosed.run();
            }
        });
        return false;
    }

    private static class GuiCrashReportPopup extends GuiInfoPopup {
        private final GuiScrollable scrollable;

        public GuiCrashReportPopup(GuiContainer container, String crashReport) {
            super(container);
            setBackgroundColor(Colors.DARK_TRANSPARENT);

            getInfo().addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel().setColor(Colors.BLACK).setI18nText("replaymod.gui.unknownerror"),
                    scrollable = new GuiScrollable().setScrollDirection(AbstractGuiScrollable.Direction.VERTICAL)
                            .setLayout(new VerticalLayout().setSpacing(2))
                            .addElements(null, Arrays.stream(crashReport.replace("\t", "    ").split("\n"))
                                    .map(line -> new GuiLabel().setText(line).setColor(Colors.BLACK))
                                    .toArray(GuiElement[]::new)));

            GuiButton copyToClipboardButton = new GuiButton().setI18nLabel("chat.copy").onClick(() ->
                    MCVer.setClipboardString(crashReport)).setSize(150, 20);
            GuiButton closeButton = getCloseButton();
            popup.removeElement(closeButton);
            popup.addElements(new VerticalLayout.Data(1),
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5)).setSize(305, 20)
                            .addElements(null, copyToClipboardButton, closeButton));

            open();
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            scrollable.setSize(size.getWidth() * 3 / 4, size.getHeight() * 3 / 4);
            super.draw(renderer, size, renderInfo);
        }
    }

    private static class MinimalModeUnsupportedPopup extends GuiInfoPopup {
        private MinimalModeUnsupportedPopup(GuiContainer container) {
            super(container);
            setBackgroundColor(Colors.DARK_TRANSPARENT);

            ProtocolVersion latestVersion = ProtocolVersion.getProtocols()
                    .stream()
                    .max(Comparator.comparing(ProtocolVersion::getVersion))
                    .orElseThrow(RuntimeException::new);
            getInfo().addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel()
                            .setColor(Colors.BLACK)
                            .setI18nText("replaymod.gui.minimalmode.unsupported"),
                    new GuiLabel()
                            .setColor(Colors.BLACK)
                            .setI18nText("replaymod.gui.minimalmode.supportedversion",
                                    "1.7.10 - " + latestVersion.getName()));

            open();
        }
    }
}
