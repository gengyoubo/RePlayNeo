package github.com.gengyoubo.replayneo.feature.pathing.player;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import github.com.gengyoubo.replayneo.mixin.MinecraftAccessor;
import github.com.gengyoubo.replayneo.mixin.TimerAccessor;
import github.com.gengyoubo.replayneo.feature.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import java.util.Iterator;

import static github.com.gengyoubo.replayneo.core.utils.Utils.DEFAULT_MS_PER_TICK;
import static github.com.gengyoubo.replayneo.core.versions.MCVer.*;

/**
 * Plays a timeline.
 */
public abstract class AbstractTimelinePlayer extends EventRegistrations {
    private final Minecraft mc = getMinecraft();
    private final ReplayHandler replayHandler;
    private Timeline timeline;
    protected long startOffset;
    private boolean wasAsyncMode;
    private Timer orgTimer;
    private long lastTime;
    private long lastTimestamp;
    private ListenableFuture<Void> future;
    private SettableFuture<Void> settableFuture;

    public AbstractTimelinePlayer(ReplayHandler replayHandler) {
        this.replayHandler = replayHandler;
    }

    public ListenableFuture<Void> start(Timeline timeline, long from) {
        startOffset = from;
        return start(timeline);
    }

    public ListenableFuture<Void> start(Timeline timeline) {
        this.timeline = timeline;

        Iterator<Keyframe> iter = Iterables.concat(Iterables.transform(timeline.getPaths(),
                (Function<Path, Iterable<Keyframe>>) input -> {
                    assert input != null;
                    return input.getKeyframes();
                })).iterator();
        if (!iter.hasNext()) {
            lastTimestamp = 0;
        } else {
            lastTimestamp = new Ordering<Keyframe>() {
                @Override
                public int compare(@Nullable Keyframe left, @Nullable Keyframe right) {
                    assert left != null;
                    assert right != null;
                    return Longs.compare(left.getTime(), right.getTime());
                }
            }.max(iter).getTime();
        }

        wasAsyncMode = replayHandler.getReplaySender().isAsyncMode();
        replayHandler.getReplaySender().setSyncModeAndWait();
        register();
        lastTime = 0;

        MinecraftAccessor mcA = (MinecraftAccessor) mc;
        orgTimer = mcA.getTimer();
        ReplayTimer timer = new ReplayTimer();
        mcA.setTimer(timer);

        //noinspection ConstantConditions
        TimerAccessor timerA = (TimerAccessor) timer;
        timerA.setTickLength(DEFAULT_MS_PER_TICK);
        timer.partialTick = timer.ticksThisFrame = 0;
        return future = settableFuture = SettableFuture.create();
    }

    public ListenableFuture<Void> getFuture() {
        return future;
    }

    public boolean isActive() {
        return future != null && !future.isDone();
    }

    { on(ReplayTimer.UpdatedCallback.EVENT, this::onTick); }
    public void onTick() {
        if (future.isDone()) {
            MinecraftAccessor mcA = (MinecraftAccessor) mc;
            mcA.setTimer(orgTimer);
            replayHandler.getReplaySender().setReplaySpeed(0);
            if (wasAsyncMode) {
                replayHandler.getReplaySender().setAsyncMode(true);
            }
            unregister();
            return;
        }
        long time = getTimePassed();
        if (time > lastTimestamp) {
            time = lastTimestamp;
        }

        // Apply to timeline
        timeline.applyToGame(time, replayHandler);
        // Apply a second time in case same of the packets have moved the camera from where it was
        timeline.applyToGame(time, replayHandler);

        // Update minecraft timer
        long replayTime = replayHandler.getReplaySender().currentTimeStamp();
        if (lastTime == 0) {
            // First frame, no change yet
            lastTime = replayTime;
        }
        float timeInTicks = replayTime / 50f;
        float previousTimeInTicks = lastTime / 50f;
        float passedTicks = timeInTicks - previousTimeInTicks;
        Timer renderTickCounter = ((MinecraftAccessor) mc).getTimer();
        if (renderTickCounter instanceof ReplayTimer timer) {
            timer.partialTick += passedTicks;
            timer.ticksThisFrame = (int) timer.partialTick;
            timer.partialTick -= timer.ticksThisFrame;
        }

        lastTime = replayTime;

        if (time >= lastTimestamp) {
            settableFuture.set(null);
        }
    }

    public abstract long getTimePassed();
}
