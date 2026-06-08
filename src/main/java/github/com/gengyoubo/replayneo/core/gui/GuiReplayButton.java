package github.com.gengyoubo.replayneo.core.gui;

import github.com.gengyoubo.replayneo.GuiRenderer;
import github.com.gengyoubo.replayneo.RenderInfo;
import github.com.gengyoubo.replayneo.feature.pathing.element.GuiButton;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import github.com.gengyoubo.replayneo.RePlayNeo;
import net.minecraft.resources.ResourceLocation;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;

public class GuiReplayButton extends GuiButton {
    public static final ResourceLocation ICON = identifier(RePlayNeo.RESOURCE_NAMESPACE, "logo_button.png");

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        renderer.bindTexture(ICON);
        renderer.drawTexturedRect(3, 3, 0, 0, size.getWidth() - 6, size.getHeight() - 6, 1, 1, 1, 1);
    }
}
