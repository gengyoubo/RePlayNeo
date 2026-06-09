package github.com.gengyoubo.replayneo.platform.render;

import github.com.gengyoubo.replayneo.core.render.RenderSettings;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import static github.com.gengyoubo.replayneo.platform.feature.render.ReplayModRender.LOGGER;

public final class ForgeRenderSettingsDefaults {
    private ForgeRenderSettingsDefaults() {
    }

    public static void install() {
        RenderSettings.setTranslator(I18n::get);
        RenderSettings.setFfmpegFinder(ForgeRenderSettingsDefaults::findFFmpeg);
    }

    private static String findFFmpeg() {
        switch (Util.getPlatform()) {
            case WINDOWS:
                File dotMinecraft = MCVer.getMinecraft().gameDirectory;
                File inDotMinecraft = new File(dotMinecraft, "ffmpeg/bin/ffmpeg.exe");
                if (inDotMinecraft.exists()) {
                    LOGGER.debug("FFmpeg found in .minecraft/ffmpeg");
                    return inDotMinecraft.getAbsolutePath();
                }
                try {
                    Path[] result = new Path[1];
                    Files.walkFileTree(dotMinecraft.toPath(), new SimpleFileVisitor<>() {
                        @Override
                        public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                            if ("ffmpeg.exe".equals(file.getFileName().toString())) {
                                result[0] = file;
                                return FileVisitResult.TERMINATE;
                            }
                            return super.visitFile(file, attrs);
                        }
                    });
                    if (result[0] != null) {
                        return result[0].toAbsolutePath().toString();
                    }
                } catch (IOException e) {
                    LOGGER.debug("Error searching .minecraft for ffmpeg.exe:", e);
                }
                break;
            case OSX:
                for (String path : new String[]{"/usr/local/bin/ffmpeg", "/usr/bin/ffmpeg"}) {
                    File file = new File(path);
                    if (file.exists()) {
                        LOGGER.debug("Found FFmpeg at {}", path);
                        return path;
                    }
                    LOGGER.debug("FFmpeg not located at {}", path);
                }
                for (String path : new String[]{"/usr/local", "/opt/homebrew"}) {
                    File homebrewFolder = new File(path + "/Cellar/ffmpeg");
                    String[] homebrewVersions = homebrewFolder.list();
                    if (homebrewVersions == null) {
                        continue;
                    }
                    Optional<File> latestOpt = Arrays.stream(homebrewVersions)
                            .map(ComparableVersion::new)
                            .sorted(Comparator.reverseOrder())
                            .map(ComparableVersion::toString)
                            .map(v -> new File(new File(homebrewFolder, v), "bin/ffmpeg"))
                            .filter(File::exists)
                            .findFirst();
                    if (latestOpt.isPresent()) {
                        File latest = latestOpt.get();
                        LOGGER.debug("Found {} versions of FFmpeg installed with homebrew, chose {}",
                                homebrewVersions.length, latest);
                        return latest.getAbsolutePath();
                    }
                }
                break;
            case LINUX:
            case SOLARIS:
            case UNKNOWN:
        }
        LOGGER.debug("Using default FFmpeg executable");
        return "ffmpeg";
    }
}
