package github.com.gengyoubo.replayneo.platform.feature.recording.gui;

import github.com.gengyoubo.replayneo.core.SettingsRegistry;
import github.com.gengyoubo.replayneo.platform.feature.recording.Setting;
import github.com.gengyoubo.replayneo.platform.gui.MinecraftGuiRenderer;
import github.com.gengyoubo.replayneo.core.utils.EventRegistrations;
import github.com.gengyoubo.replayneo.api.callbacks.RenderHudCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import static github.com.gengyoubo.replayneo.platform.gui.ReplayTextures.TEXTURE;
import static github.com.gengyoubo.replayneo.platform.gui.ReplayTextures.TEXTURE_SIZE;

/**
 * Renders overlay during recording.
 */
public class GuiRecordingOverlay extends EventRegistrations {
    private final Minecraft mc;
    private final SettingsRegistry settingsRegistry;
    private final GuiRecordingControls guiControls;

    public GuiRecordingOverlay(Minecraft mc, SettingsRegistry settingsRegistry, GuiRecordingControls guiControls) {
        this.mc = mc;
        this.settingsRegistry = settingsRegistry;
        this.guiControls = guiControls;
    }

    { on(RenderHudCallback.EVENT, (stack, partialTicks) -> renderRecordingIndicator(stack)); }
    private void renderRecordingIndicator(GuiGraphics stack) {
        if (guiControls.isStopped()) return;
        if (settingsRegistry.get(Setting.INDICATOR)) {
            Font fontRenderer = mc.font;
            String text = guiControls.isPaused() ? I18n.get("replaymod.gui.paused") : I18n.get("replaymod.gui.recording");
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            renderer.drawString(30, 18 - (fontRenderer.lineHeight / 2), 0xffffffff, text.toUpperCase());
            renderer.bindTexture(TEXTURE);
            renderer.drawTexturedRect(10, 10, 58, 20, 16, 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
