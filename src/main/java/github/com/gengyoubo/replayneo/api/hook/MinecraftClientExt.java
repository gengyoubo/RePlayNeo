package github.com.gengyoubo.replayneo.api.hook;

import com.mojang.blaze3d.pipeline.RenderTarget;
import github.com.gengyoubo.replayneo.platform.render.gui.progress.VirtualWindow;
import net.minecraft.client.Minecraft;

public interface MinecraftClientExt {
    void setWindowDelegate(VirtualWindow window);
    void setFramebufferDelegate(RenderTarget framebuffer);

    static MinecraftClientExt get(Minecraft mc) {
        return (MinecraftClientExt) mc;
    }
}
