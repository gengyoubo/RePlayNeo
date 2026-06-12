package github.com.gengyoubo.replayneo.platform.input;

import com.mojang.blaze3d.platform.Window;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import github.com.gengyoubo.replayneo.api.function.InputWithModifiers;
import github.com.gengyoubo.replayneo.api.input.ReplayInput;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBinding;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyBindingRegistry;
import github.com.gengyoubo.replayneo.api.input.ReplayKeyHandler;
import github.com.gengyoubo.replayneo.core.input.DefaultReplayKeyInput;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class ForgeReplayInput implements ReplayInput {
    private final ForgeKeyBindingRegistry keyBindingRegistry = new ForgeKeyBindingRegistry();
    private final Minecraft minecraft = MCVer.getMinecraft();

    public ForgeReplayInput() {
        InputWithModifiers.setCurrentModifiersSupplier(ForgeReplayInput::currentModifiers);
    }

    @Override
    public ReplayKeyBindingRegistry keyBindingRegistry() {
        return keyBindingRegistry;
    }

    public ForgeKeyBindingRegistry forgeKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    @Override
    public ReplayKeyBinding registerKey(String id, int defaultKeyCode, boolean replayOnly) {
        ReplayKeyBindingRegistry.Binding binding = keyBindingRegistry.registerKeyBinding(id, defaultKeyCode, () -> {
        }, replayOnly);
        return new KeyBindingAdapter(binding);
    }

    @Override
    public void registerRawKey(int keyCode, ReplayKeyHandler handler) {
        keyBindingRegistry.registerRaw(keyCode, keyInput -> handler.handle(new DefaultReplayKeyInput(keyInput.key(), keyInput.scancode(), keyInput.modifiers())));
    }

    @Override
    public Point mousePosition() {
        int mouseX = (int) minecraft.mouseHandler.xpos();
        int mouseY = (int) minecraft.mouseHandler.ypos();
        Window window = MCVer.newScaledResolution(minecraft);
        mouseX = (int) Math.round((double) mouseX * window.getGuiScaledWidth() / window.getScreenWidth());
        mouseY = (int) Math.round((double) mouseY * window.getGuiScaledHeight() / window.getScreenHeight());
        return new Point(mouseX, mouseY);
    }

    @Override
    public Point scaledDimensions() {
        Window window = MCVer.newScaledResolution(minecraft);
        return new Point(window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }

    @Override
    public boolean controlDown() {
        return Screen.hasControlDown();
    }

    private static int currentModifiers() {
        int ctrlMask = Util.getPlatform() == Util.OS.OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
        int ctrl = Screen.hasControlDown() ? ctrlMask : 0;
        int shift = Screen.hasShiftDown() ? GLFW.GLFW_MOD_SHIFT : 0;
        int alt = Screen.hasAltDown() ? GLFW.GLFW_MOD_ALT : 0;
        return ctrl | shift | alt;
    }

    private record KeyBindingAdapter(ReplayKeyBindingRegistry.Binding binding) implements ReplayKeyBinding {

        @Override
            public String id() {
                return binding.name();
            }

            @Override
            public String displayName() {
                return binding.getBoundKey();
            }

            @Override
            public boolean isBound() {
                return binding.isBound();
            }

            @Override
            public boolean consumeClick() {
                if (!binding.isDown()) {
                    return false;
                }
                binding.trigger();
                return true;
            }

            @Override
            public boolean isDown() {
                return binding.isDown();
            }
        }

}
