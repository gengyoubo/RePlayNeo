package github.com.gengyoubo.replayneo.core.files;

import github.com.gengyoubo.replayneo.core.Setting;
import github.com.gengyoubo.replayneo.core.SettingsRegistry;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static github.com.gengyoubo.replayneo.core.files.RePlayMethod.encodeFileName;
import static github.com.gengyoubo.replayneo.core.utils.Utils.ensureDirectoryExists;

public class ReplayFoldersService {
    private final Path mcDir;
    private final SettingsRegistry settings;

    public ReplayFoldersService(Path mcDir, SettingsRegistry settings) {
        this.mcDir = mcDir;
        this.settings = settings;
    }

    public Path getReplayFolder() throws IOException {
        return ensureDirectoryExists(mcDir.resolve(settings.get(Setting.RECORDING_PATH)));
    }

    /**
     * Folder into which replay backups are saved before the MarkerProcessor is unleashed.
     */
    public Path getRawReplayFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("raw"));
    }

    /**
     * Folder into which replays are recorded.
     * Distinct from the main folder, so they cannot be opened while they are still saving.
     */
    public Path getRecordingFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("recording"));
    }

    /**
     * Folder in which replay cache files are stored.
     * Distinct from the recording folder cause people kept confusing them with recordings.
     */
    public Path getCacheFolder() throws IOException {
        Path path = ensureDirectoryExists(mcDir.resolve(settings.get(Setting.CACHE_PATH)));
        try {
            Files.setAttribute(path, "dos:hidden", true);
        } catch (UnsupportedOperationException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public Path getCachePathForReplay(Path replay) throws IOException {
        Path replayFolder = getReplayFolder();
        Path cacheFolder = getCacheFolder();
        Path relative = replayFolder.toAbsolutePath().relativize(replay.toAbsolutePath());
        return cacheFolder.resolve(
                encodeFileName(
                        relative.toString()));
    }

    public Path getReplayPathForCache(Path cache) throws IOException {
        String relative = URLDecoder.decode(cache.getFileName().toString(), StandardCharsets.UTF_8);
        Path replayFolder = getReplayFolder();
        return replayFolder.resolve(relative);
    }
}
