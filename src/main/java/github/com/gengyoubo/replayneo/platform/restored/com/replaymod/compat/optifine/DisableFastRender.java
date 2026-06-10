package github.com.gengyoubo.replayneo.platform.restored.com.replaymod.compat.optifine;

import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import github.com.gengyoubo.replayneo.platform.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.render.events.ReplayRenderCallback;

public class DisableFastRender extends EventRegistrations {

    private final Minecraft mc = Minecraft.getInstance();

    private boolean wasFastRender = false;

    { on(ReplayRenderCallback.Pre.EVENT, renderer -> onRenderBegin()); }
    private void onRenderBegin() {
        if (!MCVer.hasOptifine()) return;

        try {
            wasFastRender = (boolean) OptifineReflection.gameSettings_ofFastRender.get(mc.options);
            OptifineReflection.gameSettings_ofFastRender.set(mc.options, false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    { on(ReplayRenderCallback.Post.EVENT, renderer -> onRenderEnd()); }
    private void onRenderEnd() {
        if (!MCVer.hasOptifine()) return;

        try {
            OptifineReflection.gameSettings_ofFastRender.set(mc.options, wasFastRender);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
