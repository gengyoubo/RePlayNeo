package com.replaymod.core.versions;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector2f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import org.lwjgl.opengl.GL11;


import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;
import com.replaymod.render.mixin.MainWindowAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractButton;
import java.util.concurrent.CompletableFuture;
import org.lwjgl.glfw.GLFW;

import com.replaymod.render.blend.mixin.ParticleAccessor;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.Optional;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    public static int getProtocolVersion() {
        return SharedConstants.getCurrentVersion().getProtocolVersion();
    }

    public static ConnectionProtocol asMc(State state) {
        switch (state) {
            case HANDSHAKE: return ConnectionProtocol.HANDSHAKING;
            case STATUS: return ConnectionProtocol.STATUS;
            case LOGIN: return ConnectionProtocol.LOGIN;
            case PLAY: return ConnectionProtocol.PLAY;
        }
        throw new IllegalArgumentException("Unexpected value: " + state);
    }

    public static State fromMc(ConnectionProtocol mcState) {
        switch (mcState) {
            case HANDSHAKING: return State.HANDSHAKE;
            case STATUS: return State.STATUS;
            case LOGIN: return State.LOGIN;
            case PLAY: return State.PLAY;
        }
        throw new IllegalArgumentException("Unexpected value: " + mcState);
    }

    public static PacketTypeRegistry getPacketTypeRegistry(ConnectionProtocol state) {
        return getPacketTypeRegistry(fromMc(state));
    }

    public static PacketTypeRegistry getPacketTypeRegistry(State state) {
        return PacketTypeRegistry.get(ProtocolVersion.getProtocol(getProtocolVersion()), state);
    }

    public static PacketTypeRegistry getPacketTypeRegistry(boolean loginPhase) {
        return PacketTypeRegistry.get(
                ProtocolVersion.getProtocol(getProtocolVersion()),
                loginPhase ? State.LOGIN : State.PLAY
        );
    }

    public static void resizeMainWindow(Minecraft mc, int width, int height) {
        Window window = mc.getWindow();
        MainWindowAccessor mainWindow = (MainWindowAccessor) (Object) window;
        //noinspection ConstantConditions
        mainWindow.invokeOnFramebufferResize(window.getWindow(), width, height);
    }


    public static
    CompletableFuture<?>
    setServerResourcePack(File file) {
        return getMinecraft().getDownloadedPackSource().setServerPack(
                file,
                net.minecraft.server.packs.repository.PackSource.SERVER
        );
    }

    public static <T> void addCallback(
            CompletableFuture<T> future,
            Consumer<T> success,
            Consumer<Throwable> failure
    ) {
        future.thenAccept(success).exceptionally(throwable -> {
            failure.accept(throwable);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public static List<VertexFormatElement> getElements(VertexFormat vertexFormat) {
        return vertexFormat.getElements();
    }


    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public static void addButton(
            Screen screen,
            AbstractButton button
    ) {
        try {
            java.lang.reflect.Method method;
            try {
                method = Screen.class.getDeclaredMethod("addRenderableWidget", GuiEventListener.class);
            } catch (NoSuchMethodException ignored) {
                method = Screen.class.getDeclaredMethod("m_142416_", GuiEventListener.class);
            }
            method.setAccessible(true);
            method.invoke(screen, button);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to add button to screen", e);
        }
    }

    public static Optional<AbstractButton> findButton(Iterable<? extends GuiEventListener> buttonList, @SuppressWarnings("unused") String text, @SuppressWarnings("unused") int id) {
        final Component message = Component.translatable(text);
        for (GuiEventListener e : buttonList) {
            if (e instanceof ContainerEventHandler) {
                Optional<AbstractButton> button = findButton(((ContainerEventHandler) e).children(), text, id);
                if (button.isPresent()) {
                    return button;
                }
            }
            if (!(e instanceof AbstractButton)) {
                continue;
            }
            AbstractButton b = (AbstractButton) e;
            if (message.equals(b.getMessage())) {
                return Optional.of(b);
            }
            // Fuzzy match (copy does not include children)
            if (b.getMessage() != null && b.getMessage().copy().equals(message)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    public static void processKeyBinds() {
        ((MinecraftMethodAccessor) getMinecraft()).replayModProcessKeyBinds();
    }

    public interface MinecraftMethodAccessor {
        void replayModProcessKeyBinds();
        void replayModExecuteTaskQueue();
    }


    public static long milliTime() {
        return Util.getMillis();
    }

    // TODO: this can be inlined once https://github.com/SpongePowered/Mixin/issues/305 is fixed
    public static Vec3 getPosition(Particle particle, float partialTicks) {
        ParticleAccessor acc = (ParticleAccessor) particle;
        double x = acc.getXo() + (acc.getPosX() - acc.getXo()) * partialTicks;
        double y = acc.getYo() + (acc.getPosY() - acc.getYo()) * partialTicks;
        double z = acc.getZo() + (acc.getPosZ() - acc.getZo()) * partialTicks;
        return new Vec3(x, y, z);
    }


    public static void openFile(File file) {
        Util.getPlatform().openFile(file);
    }

    public static void openURL(URI url) {
        Util.getPlatform().openUri(url);
    }

    public static void pushMatrix() {
        RenderSystem.getModelViewStack().pushPose();
    }

    public static void popMatrix() {
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public static org.joml.Quaternionf quaternion(float angle, org.joml.Vector3f axis) {
        return new org.joml.Quaternionf().fromAxisAngleDeg(axis.x, axis.y, axis.z, angle);
    }
    
    public static Matrix4f ortho(float left, float right, float top, float bottom, float zNear, float zFar) {
        return new Matrix4f().ortho(left, right, bottom, top, zNear, zFar);
    }

    public static void emitLine(PoseStack matrixStack, BufferBuilder buffer, Vector2f p1, Vector2f p2, int color, float lineWidth) {
        emitLine(matrixStack, buffer, new Vector3f(p1.x, p1.y, 0), new Vector3f(p2.x, p2.y, 0), color, lineWidth);
    }

    public static void emitLine(PoseStack matrixStack, BufferBuilder buffer, Vector3f p1, Vector3f p2, int color, float lineWidth) {
        GL11.glLineWidth(lineWidth);
        int r = color >> 24 & 0xff;
        int g = color >> 16 & 0xff;
        int b = color >> 8 & 0xff;
        int a = color & 0xff;
        Vector3f n = Vector3f.sub(p2, p1, null);
        buffer.vertex(matrixStack.last().pose(), p1.x, p1.y, p1.z)
                .color(r, g, b, a)
                .normal(n.x, n.y, n.z)
                ;
        buffer.endVertex();
        buffer.vertex(matrixStack.last().pose(), p2.x, p2.y, p2.z)
                .color(r, g, b, a)
                .normal(n.x, n.y, n.z)
                ;
        buffer.endVertex();
    }

    public static void bindTexture(ResourceLocation id) {
        de.johni0702.minecraft.gui.versions.MCVer.bindTexture(id);
    }


    private static Boolean hasOptifine;
    public static boolean hasOptifine() {
        if (hasOptifine == null) {
            try {
                Class.forName("Config");
                hasOptifine = true;
            } catch (ClassNotFoundException e) {
                hasOptifine = false;
            }
        }
        return hasOptifine;
    }


    public static abstract class Keyboard {
        public static final int KEY_LCONTROL = GLFW.GLFW_KEY_LEFT_CONTROL;
        public static final int KEY_RCONTROL = GLFW.GLFW_KEY_RIGHT_CONTROL;
        public static final int KEY_LSUPER = GLFW.GLFW_KEY_LEFT_SUPER;
        public static final int KEY_RSUPER = GLFW.GLFW_KEY_RIGHT_SUPER;
        public static final int LEFT_CTRL = Util.getPlatform() == Util.OS.OSX ? KEY_LSUPER : KEY_LCONTROL;
        public static final int RIGHT_CTRL = Util.getPlatform() == Util.OS.OSX ? KEY_RSUPER : KEY_RCONTROL;
        public static final int KEY_LSHIFT = GLFW.GLFW_KEY_LEFT_SHIFT;
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
        public static final int KEY_F1 = GLFW.GLFW_KEY_F1;
        public static final int KEY_A = GLFW.GLFW_KEY_A;
        public static final int KEY_B = GLFW.GLFW_KEY_B;
        public static final int KEY_C = GLFW.GLFW_KEY_C;
        public static final int KEY_D = GLFW.GLFW_KEY_D;
        public static final int KEY_E = GLFW.GLFW_KEY_E;
        public static final int KEY_F = GLFW.GLFW_KEY_F;
        public static final int KEY_G = GLFW.GLFW_KEY_G;
        public static final int KEY_H = GLFW.GLFW_KEY_H;
        public static final int KEY_I = GLFW.GLFW_KEY_I;
        public static final int KEY_J = GLFW.GLFW_KEY_J;
        public static final int KEY_K = GLFW.GLFW_KEY_K;
        public static final int KEY_L = GLFW.GLFW_KEY_L;
        public static final int KEY_M = GLFW.GLFW_KEY_M;
        public static final int KEY_N = GLFW.GLFW_KEY_N;
        public static final int KEY_O = GLFW.GLFW_KEY_O;
        public static final int KEY_P = GLFW.GLFW_KEY_P;
        public static final int KEY_Q = GLFW.GLFW_KEY_Q;
        public static final int KEY_R = GLFW.GLFW_KEY_R;
        public static final int KEY_S = GLFW.GLFW_KEY_S;
        public static final int KEY_T = GLFW.GLFW_KEY_T;
        public static final int KEY_U = GLFW.GLFW_KEY_U;
        public static final int KEY_V = GLFW.GLFW_KEY_V;
        public static final int KEY_W = GLFW.GLFW_KEY_W;
        public static final int KEY_X = GLFW.GLFW_KEY_X;
        public static final int KEY_Y = GLFW.GLFW_KEY_Y;
        public static final int KEY_Z = GLFW.GLFW_KEY_Z;

        public static boolean isKeyDown(int keyCode) {
            return InputConstants.isKeyDown(getMinecraft().getWindow().getWindow(), keyCode);
        }

    }
}
