package github.com.gengyoubo.replayneo.feature.replay;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraController;
import com.replaymod.replay.camera.CameraEntity;
import de.johni0702.minecraft.gui.versions.ScreenExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.lwjgl.glfw.GLFW;


public class InputReplayTimer {
    public static void updateInReplay() {
        ReplayModReplay mod = ReplayModReplay.instance;
        Minecraft mc = mod.getCore().getMinecraft();

        ReplayMod.instance.runTasks();


        // If we are in a replay, we have to manually process key and mouse events as the
        // tick speed may vary or there may not be any ticks at all (when the replay is paused)
        if (mod.getReplayHandler() != null && mc.level != null && mc.player != null) {
            if (mc.screen == null || ((ScreenExt) mc.screen).doesPassEvents()) {
                GLFW.glfwPollEvents();
                MCVer.processKeyBinds();
            }
            mc.keyboardHandler.tick();

            // As of 1.18.2, this screen always stays open for at least two seconds, and requires ticking to close.
            // Thanks, but we'll have none of that (at least while in a replay).
            if (mc.screen instanceof ReceivingLevelScreen) {
                mc.screen.onClose();
            }

        }
    }

    public static void handleScroll(int wheel) {
        if (wheel != 0) {
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            if (replayHandler != null) {
                CameraEntity cameraEntity = replayHandler.getCameraEntity();
                if (cameraEntity != null) {
                    CameraController controller = cameraEntity.getCameraController();
                    while (wheel > 0) {
                        controller.increaseSpeed();
                        wheel--;
                    }
                    while (wheel < 0) {
                        controller.decreaseSpeed();
                        wheel++;
                    }
                }
            }
        }
    }
}
