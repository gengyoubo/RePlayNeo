package github.com.gengyoubo.replayneo.core.utils;

import com.google.common.net.PercentEscaper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import github.com.gengyoubo.replayneo.api.Colors;
import github.com.gengyoubo.replayneo.api.ReplayCrashReport;
import github.com.gengyoubo.replayneo.core.ReplayMod;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.core.gui.RenderInfo;
import github.com.gengyoubo.replayneo.core.gui.container.AbstractGuiScrollable;
import github.com.gengyoubo.replayneo.api.GuiContainer;
import github.com.gengyoubo.replayneo.core.gui.container.GuiPanel;
import github.com.gengyoubo.replayneo.core.gui.container.GuiScrollable;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiButton;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiElement;
import github.com.gengyoubo.replayneo.platform.feature.pathing.element.GuiLabel;
import github.com.gengyoubo.replayneo.core.gui.layout.HorizontalLayout;
import github.com.gengyoubo.replayneo.core.gui.layout.VerticalLayout;
import github.com.gengyoubo.replayneo.core.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import github.com.gengyoubo.replayneo.platform.ReplayPlatforms;
import github.com.gengyoubo.replayneo.platform.versions.Image;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;


public class Utils {
    private static final Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    public static final float DEFAULT_MS_PER_TICK = (float) 1000 / 20;

    private static InputStream getResourceAsStream(String path) {
        return Utils.class.getResourceAsStream(path);
    }

    public static final Image DEFAULT_THUMBNAIL;

    static {
        Image thumbnail;
        try {
            thumbnail = Image.read(getResourceAsStream("/default_thumb.png"));
        } catch (Exception e) {
            thumbnail = new Image(1, 1);
            e.printStackTrace();
        }
        DEFAULT_THUMBNAIL = thumbnail;
    }


    /**
     * Neither the root certificate of LetsEncrypt nor the root that cross-signed it is included in the default
     * Java keystore prior to 8u101.
     * Therefore whenever a connection to the replaymod.com site is made, this SSLContext has to be used instead.
     * It has been constructed to include the necessary root certificates.
     * @see #SSL_SOCKET_FACTORY
     */
    public static final SSLContext SSL_CONTEXT;

    /**
     * @see #SSL_CONTEXT
     */
    public static final SSLSocketFactory SSL_SOCKET_FACTORY;

    static {
        // Largely from https://community.letsencrypt.org/t/134/37
        try (InputStream in = getResourceAsStream("/dst_root_ca_x3.pem")){
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(in);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("1", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustManagerFactory.getTrustManagers(), null);
            SSL_CONTEXT = ctx;
            SSL_SOCKET_FACTORY = ctx.getSocketFactory();
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertSecondsToShortString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(String.format("%02d", hours)).append(":");
        builder.append(String.format("%02d", min)).append(":");
        builder.append(String.format("%02d", sec));

        return builder.toString();
    }

    public static Dimension fitIntoBounds(ReadableDimension toFit, ReadableDimension bounds) {
        int width = toFit.getWidth();
        int height = toFit.getHeight();

        float w = (float) width / bounds.getWidth();
        float h = (float) height / bounds.getHeight();

        if (w > h) {
            height = (int) (height / w);
            width = (int) (width / w);
        } else {
            height = (int) (height / h);
            width = (int) (width / h);
        }

        return new Dimension(width, height);
    }

    public static boolean isValidEmailAddress(String mail) {
        return mail.matches("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");
    }

    private static final PercentEscaper REPLAY_NAME_ENCODER = new PercentEscaper(".-_ ", false);

    public static Path replayNameToPath(Path folder, String replayName) {
        // If we can, prefer directly using the replay name as the file name
        if (isUsable(folder, replayName + ".mcpr")) {
            return folder.resolve(replayName + ".mcpr");
        } else {
            // otherwise, fall back to percent encoding
            return folder.resolve(REPLAY_NAME_ENCODER.escape(replayName) + ".mcpr");
        }
    }

    /**
     * Checks whether a given file name is actually usable with the file system / operating system at the given folder.
     */
    private static boolean isUsable(Path folder, String fileName) {
        if (fileName.contains(folder.getFileSystem().getSeparator())) {
            return false; // file name contains the name separator, definitely not usable
        }

        Path path;
        try {
            path = folder.resolve(fileName);
        } catch (InvalidPathException e) {
            return false; // file name contains invalid characters, definitely not usable
        }
        if (Files.exists(path)) {
            return true; // if it already exits, it's definitely usable
        }

        // Otherwise, there's no sure way to know, so we just gotta try
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            outputStream.flush();
        } catch (IOException e) {
            return false;
        }

        // Looking good, but now we gotta clean up that mess (and Anti-Virus / Cloud Sync are know to lock them)
        int attempts = 0;
        while (true) {
            try {
                Files.delete(path);
                return true;
            } catch (IOException e) {
                if (attempts++ > 100) {
                    LOGGER.warn("Repeatedly failed to clean up temporary test file at {}: ", path, e);
                    return false; // while we were able to use it, it's taken now and we can't get it back
                }
            }
        }
    }

    public static String fileNameToReplayName(String fileName) {
        String baseName = FilenameUtils.getBaseName(fileName);
        try {
            return URLDecoder.decode(baseName, Charsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return baseName;
        }
    }

    public static boolean isCtrlDown() {
        return ReplayPlatforms.get().input().controlDown();
    }

    public static <T> void addCallback(ListenableFuture<T> future, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable T result) {
                onSuccess.accept(result);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                onFailure.accept(t);
            }
        }, Runnable::run);
    }

    public static void error(Logger logger, GuiContainer container, Object crashReport, Runnable onClose) {
        // Convert crash report to string
        String crashReportStr = friendlyReport(crashReport);

        // Log via logger
        logger.error(crashReportStr);

        // Try to save the crash report
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

    private static class GuiCrashReportPopup extends GuiInfoPopup {
        private final GuiScrollable scrollable;

        public GuiCrashReportPopup(GuiContainer container, String crashReport) {
            super(container);
            setBackgroundColor(Colors.DARK_TRANSPARENT);

            // Add crash report to scrollable info
            getInfo().addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel().setColor(Colors.BLACK).setI18nText("replaymod.gui.unknownerror"),
                    scrollable = new GuiScrollable().setScrollDirection(AbstractGuiScrollable.Direction.VERTICAL)
                            .setLayout(new VerticalLayout().setSpacing(2))
                            .addElements(null,Arrays.stream(crashReport.replace("\t", "    ").split("\n")).map(
                                    l -> new GuiLabel().setText(l).setColor(Colors.BLACK)).toArray(GuiElement[]::new)));

            // Replace close button with panel containing close and copy buttons
            GuiButton copyToClipboardButton = new GuiButton().setI18nLabel("chat.copy").onClick(() ->
                    MCVer.setClipboardString(crashReport)).setSize(150, 20);
            GuiButton closeButton = getCloseButton();
            popup.removeElement(closeButton);
            popup.addElements(new VerticalLayout.Data(1),
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5)).setSize(305, 20)
                            .addElements(null, copyToClipboardButton, closeButton));

            // And open it
            open();
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            // Re-size the scrollable containing the crash report to 3/4 of the screen
            scrollable.setSize(size.getWidth() * 3 / 4, size.getHeight() * 3 / 4);
            super.draw(renderer, size, renderInfo);
        }
    }

    public static <T extends Throwable> void throwIfInstanceOf(Throwable t, Class<T> cls) throws T {
        if (cls.isInstance(t)) {
            throw cls.cast(t);
        }
    }
    public static void throwIfUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
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
        if (!ReplayMod.isMinimalMode()) {
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

    public static <T> T configure(T instance, Consumer<T> configure) {
        configure.accept(instance);
        return instance;
    }

    /**
     * Like {@link Files#createDirectories(Path, FileAttribute[])} but doesn't explode if it's a symlink.
     */
    public static Path ensureDirectoryExists(Path path) throws IOException {
        // Who in their right mind thought the default behavior of throwing when the target is a link to a directory
        // was the preferred behavior?! Everyone has to fall for this at least once to learn it...
        // https://bugs.openjdk.java.net/browse/JDK-8130464
        return Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
    }
}
