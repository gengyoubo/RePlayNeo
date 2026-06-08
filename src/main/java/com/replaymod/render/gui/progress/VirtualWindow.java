package com.replaymod.render.gui.progress;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.replaymod.render.hooks.MinecraftClientExt;
import com.replaymod.render.mixin.MainWindowAccessor;
import de.johni0702.minecraft.gui.function.Closeable;
import net.minecraft.client.Minecraft;

public class VirtualWindow implements Closeable {
    private final Minecraft mc;
    private final Window window;
    private final MainWindowAccessor acc;

    private final RenderTarget guiFramebuffer;
    private boolean isBound;
    private int framebufferWidth, framebufferHeight;

    private int gameWidth, gameHeight;


    public VirtualWindow(Minecraft mc) {
        this.mc = mc;
        this.window = mc.getWindow();
        this.acc = (MainWindowAccessor) (Object) this.window;

        framebufferWidth = acc.getFramebufferWidth();
        framebufferHeight = acc.getFramebufferHeight();

        guiFramebuffer = new MainTarget(framebufferWidth, framebufferHeight);

        MinecraftClientExt.get(mc).setWindowDelegate(this);
    }

    @Override
    public void close() {
        guiFramebuffer.destroyBuffers();

        MinecraftClientExt.get(mc).setWindowDelegate(null);
    }

    public void bind() {
        gameWidth = acc.getFramebufferWidth();
        gameHeight = acc.getFramebufferHeight();
        acc.setFramebufferWidth(framebufferWidth);
        acc.setFramebufferHeight(framebufferHeight);
        applyScaleFactor();
        isBound = true;
    }

    public void unbind() {
        acc.setFramebufferWidth(gameWidth);
        acc.setFramebufferHeight(gameHeight);
        applyScaleFactor();
        isBound = false;
    }

    public void beginWrite() {
        MinecraftClientExt.get(mc).setFramebufferDelegate(guiFramebuffer);
        guiFramebuffer.bindWrite(true);
    }

    public void endWrite() {
        guiFramebuffer.unbindWrite();
        MinecraftClientExt.get(mc).setFramebufferDelegate(null);
    }

    public void flip() {
        guiFramebuffer.blitToScreen(framebufferWidth, framebufferHeight);

        window.updateDisplay();
    }

    /**
     * Updates the size of the window's framebuffer. Must only be called while this window is bound.
     */
    public void onResolutionChanged(int newWidth, int newHeight) {
        if (newWidth == 0 || newHeight == 0) {
            // These can be zero on Windows if minimized.
            // Creating zero-sized framebuffers however will throw an error, so we never want to switch to zero values.
            return;
        }

        if (framebufferWidth == newWidth && framebufferHeight == newHeight) {
            return; // size is unchanged, nothing to do
        }

        framebufferWidth = newWidth;
        framebufferHeight = newHeight;

        guiFramebuffer.resize(newWidth, newHeight
                , false
        );

        applyScaleFactor();
        if (mc.screen != null) {
            mc.screen.resize(mc, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }
    }

    private void applyScaleFactor() {
        window.setGuiScale(window.calculateScale(((Integer) mc.options.guiScale().get()), mc.isEnforceUnicode()));
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public boolean isBound() {
        return isBound;
    }
}
