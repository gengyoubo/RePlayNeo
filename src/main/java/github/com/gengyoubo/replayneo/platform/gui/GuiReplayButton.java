package github.com.gengyoubo.replayneo.platform.gui;

import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import github.com.gengyoubo.replayneo.api.render.GuiRenderer;
import github.com.gengyoubo.replayneo.RePlayNeo;
import github.com.gengyoubo.replayneo.api.render.RenderInfo;
import github.com.gengyoubo.replayneo.platform.gui.element.GuiButton;
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
