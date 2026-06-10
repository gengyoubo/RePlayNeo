package github.com.gengyoubo.replayneo.core.utils;

import com.google.common.net.PercentEscaper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.function.Consumer;


public class Utils {
    private static final Logger LOGGER = github.com.gengyoubo.replayneo.RePlayNeo.LOGGER;

    public static final float DEFAULT_MS_PER_TICK = (float) 1000 / 20;

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
