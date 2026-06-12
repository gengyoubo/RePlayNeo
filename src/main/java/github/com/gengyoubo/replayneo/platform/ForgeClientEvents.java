package github.com.gengyoubo.replayneo.platform;

import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.api.callbacks.PostRenderScreenCallback;
import github.com.gengyoubo.replayneo.api.callbacks.RenderHudCallback;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RePlayNeo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    @SubscribeEvent
    public static void postRenderScreen(ScreenEvent.Render.Post event) {
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void postRenderGui(RenderGuiEvent.Post event) {
        RenderHudCallback.EVENT.invoker().renderHud(event.getGuiGraphics(), event.getPartialTick());
    }
}
