package github.com.gengyoubo.replayneo.platform.versions;

import net.minecraft.server.packs.PathPackResources;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class JGuiResourcePack {
    public static final String NAME = "replaymod_jgui";
    private static final PathPackResources PACK = create();

    private JGuiResourcePack() {
    }

    public static PathPackResources get() {
        return PACK;
    }

    private static PathPackResources create() {
        File folder = new File("../jGui/src/main/resources");
        if (!folder.exists()) {
            folder = new File("../../../jGui/src/main/resources");
            if (!folder.exists()) {
                return null;
            }
        }
        return new PathPackResources(NAME, folder.toPath(), true) {
            @Override
            public @NotNull String packId() {
                return NAME;
            }

            @Override
            public net.minecraft.server.packs.resources.IoSupplier<InputStream> getRootResource(String @NotNull ... segments) {
                if (segments.length == 1 && segments[0].equals("pack.mcmeta")) {
                    return () -> new ByteArrayInputStream(generatePackMeta());
                }
                return super.getRootResource(segments);
            }

            private byte[] generatePackMeta() {
                int version = 4;
                return ("{\"pack\": {\"description\": \"dummy pack for jGui resources in dev-env\", \"pack_format\": "
                        + version + "}}").getBytes(StandardCharsets.UTF_8);
            }
        };
    }
}
