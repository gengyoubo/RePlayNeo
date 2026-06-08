package github.com.gengyoubo.replayneo.platform.versions;

import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.Callable;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    private record ScissorBounds(int x, int y, int width, int height) {
            private static final ScissorBounds DISABLED = new ScissorBounds(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);

    }
    private static final ArrayDeque<ScissorBounds> scissorStateStack = new ArrayDeque<>();
    private static ScissorBounds scissorState = ScissorBounds.DISABLED;

    public static void pushScissorState() {
        scissorStateStack.push(scissorState);
    }

    public static void popScissorState() {
        setScissorBounds(scissorStateStack.pop());
    }

    public static void setScissorBounds(int x, int y, int width, int height) {
        setScissorBounds(new ScissorBounds(x, y, width, height));
    }

    public static void setScissorDisabled() {
        setScissorBounds(ScissorBounds.DISABLED);
    }

    private static void setScissorBounds(ScissorBounds newState) {
        ScissorBounds oldState = MCVer.scissorState;
        if (Objects.equals(oldState, newState)) {
            return;
        }

        scissorState = newState;

        boolean isEnabled = newState != ScissorBounds.DISABLED;
        boolean wasEnabled = oldState != ScissorBounds.DISABLED;

        if (isEnabled) {
            if (!wasEnabled) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
            GL11.glScissor(scissorState.x, scissorState.y, scissorState.width, scissorState.height);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    public static Window newScaledResolution(Minecraft mc) {
        return mc.getWindow();
    }

    public static void addDetail(CrashReportCategory category, String name, Callable<String> callable) {
        category.setDetail(name, callable::call);
    }

    public static void bindTexture(ResourceLocation identifier) {
        RenderSystem.setShaderTexture(0, identifier);
    }

    public static Font getFontRenderer() {
        return getMinecraft().font;
    }


    public static void setClipboardString(String text) {
        getMinecraft().keyboardHandler.setClipboard(text);
    }

    public static String getClipboardString() {
        return getMinecraft().keyboardHandler.getClipboard();
    }

    public static Component literalText(String str) {
        return Component.literal(str);
    }

    public static ResourceLocation identifier(String id) {
        return new ResourceLocation(id);
    }

    public static ResourceLocation identifier(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }


    public static abstract class Keyboard {
        public static final int KEY_ESCAPE = GLFW.GLFW_KEY_ESCAPE;
        public static final int KEY_HOME = GLFW.GLFW_KEY_HOME;
        public static final int KEY_END = GLFW.GLFW_KEY_END;
        public static final int KEY_UP = GLFW.GLFW_KEY_UP;
        public static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
        public static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
        public static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;
        public static final int KEY_BACK = GLFW.GLFW_KEY_BACKSPACE;
        public static final int KEY_DELETE = GLFW.GLFW_KEY_DELETE;
        public static final int KEY_RETURN = GLFW.GLFW_KEY_ENTER;
        public static final int KEY_TAB = GLFW.GLFW_KEY_TAB;
        public static final int KEY_A = GLFW.GLFW_KEY_A;
        public static final int KEY_C = GLFW.GLFW_KEY_C;
        public static final int KEY_V = GLFW.GLFW_KEY_V;
        public static final int KEY_X = GLFW.GLFW_KEY_X;

    }
}
