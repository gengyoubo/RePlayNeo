package github.com.gengyoubo.replayneo.addon;

import github.com.gengyoubo.replayneo.core.ReplayMod;
import github.com.gengyoubo.replayneo.core.events.PostRenderCallback;
import github.com.gengyoubo.replayneo.core.events.PreRenderCallback;
import github.com.gengyoubo.replayneo.core.versions.MCVer.Keyboard;
import github.com.gengyoubo.replayneo.feature.replay.ReplayHandler;
import github.com.gengyoubo.replayneo.feature.replay.ReplayModReplay;
import github.com.gengyoubo.replayneo.feature.render.events.ReplayOpenedCallback;
import github.com.gengyoubo.replayneo.feature.replay.gui.overlay.GuiReplayOverlay;
import github.com.gengyoubo.replayneo.feature.pathing.element.GuiImage;
import github.com.gengyoubo.replayneo.feature.pathing.element.IGuiImage;
import github.com.gengyoubo.replayneo.core.gui.layout.HorizontalLayout;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FullBrightness extends EventRegistrations implements Extra {
    private ReplayMod core;
    private ReplayModReplay module;

    private final IGuiImage indicator = new GuiImage().setTexture(ReplayMod.TEXTURE, 90, 20, 19, 16).setSize(19, 16);

    private Minecraft mc;
    private boolean active;
    private double originalGamma;

    @Override
    public void register(final ReplayMod mod) throws Exception {
        this.core = mod;
        this.module = ReplayModReplay.instance;
        this.mc = mod.getMinecraft();

        mod.getKeyBindingRegistry().registerKeyBinding("replaymod.input.lighting", Keyboard.KEY_Z, (Runnable) () -> {
            active = !active;
            // need to tick once to update lightmap when replay is paused
            mod.getMinecraft().gameRenderer.tick();
            ReplayHandler replayHandler = module.getReplayHandler();
            if (replayHandler != null) {
                updateIndicator(replayHandler.getOverlay());
            }
        }, true);

        register();
    }

    public Type getType() {
        String str = core.getSettingsRegistry().get(Setting.FULL_BRIGHTNESS);
        for (Type type : Type.values()) {
            if (type.toString().equals(str)) {
                return type;
            }
        }
        return Type.Gamma;
    }

    { on(PreRenderCallback.EVENT, this::preRender); }
    private void preRender() {
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                originalGamma = mc.options.gamma().get();
                mc.options.gamma().set(1000.0);
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.player != null) {
                    mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION
                            , Integer.MAX_VALUE));
                }
            }
        }
    }

    { on(PostRenderCallback.EVENT, this::postRender); }
    private void postRender() {
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                mc.options.gamma().set(originalGamma);
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.player != null) {
                    mc.player.removeEffect(MobEffects.NIGHT_VISION
                    );
                }
            }
        }
    }

    { on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay())); }
    private void updateIndicator(GuiReplayOverlay overlay) {
        if (active) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }

    enum Type {
        Gamma,
        NightVision,
        Both,
        ;

        @Override
        public String toString() {
            return "replaymod.gui.settings.fullbrightness." + name().toLowerCase();
        }
    }
}
