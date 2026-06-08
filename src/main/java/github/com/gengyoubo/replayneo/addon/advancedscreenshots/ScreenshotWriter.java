package github.com.gengyoubo.replayneo.addon.advancedscreenshots;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.utils.Utils;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.addon.ReplayModExtras;
import github.com.gengyoubo.replayneo.feature.render.frame.BitmapFrame;
import github.com.gengyoubo.replayneo.feature.render.rendering.Channel;
import github.com.gengyoubo.replayneo.feature.render.rendering.FrameConsumer;
import github.com.gengyoubo.replayneo.feature.replay.ReplayModReplay;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import github.com.gengyoubo.replayneo.platform.versions.Image;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import net.minecraft.CrashReport;

public class ScreenshotWriter implements FrameConsumer<BitmapFrame> {

    private final File outputFile;

    public ScreenshotWriter(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void consume(Map<Channel, BitmapFrame> channels) {
        BitmapFrame frame = channels.get(Channel.BRGA);

        // skip the first frame, in which not all chunks are properly loaded
        if (frame.frameId() == 0) return;

        final ReadableDimension frameSize = frame.size();
        try (Image img = new Image(frameSize.getWidth(), frameSize.getHeight())) {
            for (int y = 0; y < frameSize.getHeight(); y++) {
                for (int x = 0; x < frameSize.getWidth(); x++) {
                    byte b = frame.byteBuffer().get();
                    byte g = frame.byteBuffer().get();
                    byte r = frame.byteBuffer().get();
                    byte a = frame.byteBuffer().get();

                    img.setRGBA(x, y, r, g, b, 0xff);
                }
            }

            outputFile.getParentFile().mkdirs();
            img.writePNG(outputFile);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.forThrowable(e, "Exporting frame");
            MCVer.getMinecraft().delayCrashRaw(report);
        } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Exporting frame");

            ReplayMod.instance.runLater(() -> Utils.error(ReplayModExtras.LOGGER,
                    ReplayModReplay.instance.getReplayHandler().getOverlay(),
                    report, null));
        }
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isParallelCapable() {
        return false;
    }
}
