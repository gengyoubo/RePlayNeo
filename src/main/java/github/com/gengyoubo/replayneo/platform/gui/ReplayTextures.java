package github.com.gengyoubo.replayneo.platform.gui;

import github.com.gengyoubo.replayneo.RePlayNeo;
import net.minecraft.resources.ResourceLocation;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;

public final class ReplayTextures {
    public static final ResourceLocation TEXTURE = identifier(RePlayNeo.RESOURCE_NAMESPACE, "replay_gui.png");
    public static final int TEXTURE_SIZE = 256;
    public static final ResourceLocation LOGO_FAVICON = identifier(RePlayNeo.RESOURCE_NAMESPACE, "favicon_logo.png");

    private ReplayTextures() {
    }
}
