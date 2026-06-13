package github.com.gengyoubo.replayneo.platform.debug;

import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;

public final class ReplaySoundDebug {
    private static final long RIGHT_CLICK_TRACE_WINDOW_MS = 1500L;
    private static final int MAX_SOUNDS_PER_CLICK = 12;
    private static final int MAX_REPLAY_SOUNDS = 40;

    private static long lastReplayRightClickMs;
    private static int loggedSoundsForClick;
    private static int loggedReplaySounds;

    private ReplaySoundDebug() {
    }

    public static void markReplayRightClick(int button, int action) {
        if (button != 1 || action != 1 || !isInReplay()) {
            return;
        }
        lastReplayRightClickMs = System.currentTimeMillis();
        loggedSoundsForClick = 0;
        RePlayNeo.LOGGER.warn("Replay sound trace armed after right click.");
    }

    public static void logReplaySound(String entryPoint, SoundInstance sound) {
        if (sound == null || !isInReplay()) {
            return;
        }
        long elapsed = System.currentTimeMillis() - lastReplayRightClickMs;
        boolean rightClickTrace = elapsed >= 0
                && elapsed <= RIGHT_CLICK_TRACE_WINDOW_MS
                && loggedSoundsForClick < MAX_SOUNDS_PER_CLICK;
        boolean importantMusicTrace = isMusicLike(sound.getSource());
        boolean replayTrace = loggedReplaySounds < MAX_REPLAY_SOUNDS;
        if (!rightClickTrace && !replayTrace && !importantMusicTrace) {
            return;
        }
        if (rightClickTrace) {
            loggedSoundsForClick++;
        }
        if (replayTrace) {
            loggedReplaySounds++;
        }
        Minecraft minecraft = Minecraft.getInstance();
        String screen = minecraft.screen == null ? "null" : minecraft.screen.getClass().getName();
//        RePlayNeo.LOGGER.warn(
//                "Replay sound trace: entry={}, id={}, source={}, relative={}, pos=({}, {}, {}), afterRightClick={}, delayMs={}, screen={}",
//                entryPoint,
//                sound.getLocation(), sound.getSource(), sound.isRelative(), sound.getX(), sound.getY(), sound.getZ(),
//                rightClickTrace, elapsed, screen, new Throwable("Replay sound caller stack"));
    }

    public static void logReplayMusic(String entryPoint, Music music) {
        if (music == null || !isInReplay()) {
            return;
        }
//        RePlayNeo.LOGGER.warn("Replay music trace: entry={}, event={}, minDelay={}, maxDelay={}, replaceCurrentMusic={}",
//                entryPoint, music.getEvent().value().getLocation(), music.getMinDelay(), music.getMaxDelay(),
//                music.replaceCurrentMusic(), new Throwable("Replay music caller stack"));
    }

    public static boolean isInReplay() {
        ReplayModReplay replay = ReplayModReplay.instance;
        return replay != null && replay.getReplayHandler() != null;
    }

    private static boolean isMusicLike(SoundSource source) {
        return source == SoundSource.MUSIC || source == SoundSource.RECORDS || source == SoundSource.AMBIENT;
    }
}
