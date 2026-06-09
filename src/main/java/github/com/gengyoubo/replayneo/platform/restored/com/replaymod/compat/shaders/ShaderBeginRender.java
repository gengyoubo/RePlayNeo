package github.com.gengyoubo.replayneo.platform.restored.com.replaymod.compat.shaders;

import github.com.gengyoubo.replayneo.api.events.PreRenderCallback;
import github.com.gengyoubo.replayneo.platform.feature.render.hooks.EntityRendererHandler;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.Minecraft;

public class ShaderBeginRender extends EventRegistrations {

    private final Minecraft mc = Minecraft.getInstance();

    { on(PreRenderCallback.EVENT, this::onRenderTickStart); }
    private void onRenderTickStart() {
        if (ShaderReflection.shaders_beginRender == null) return;
        if (ShaderReflection.config_isShaders == null) return;

        try {
            // check if video is being rendered
            if (((EntityRendererHandler.IEntityRenderer) mc.gameRenderer).replayModRender_getHandler() == null)
                return;

            // check if Shaders are enabled
            if (!(boolean) (ShaderReflection.config_isShaders.invoke(null))) return;

            ShaderReflection.shaders_beginRender.invoke(null, mc,
                    mc.gameRenderer.getMainCamera(),
                    mc.getFrameTime(), 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
