package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.render.capturer.IrisODSFrameCapturer;
import net.coderbot.iris.Iris;
import github.com.gengyoubo.replayneo.core.RePlayCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Pseudo
@Mixin(value = Iris.class, remap = false)
public class LoadIrisOdsShaderPackMixin {
    @Redirect(method = "loadExternalShaderpack", at = @At(value = "INVOKE", target = "Lnet/coderbot/iris/Iris;getShaderpacksDirectory()Ljava/nio/file/Path;"))
    private static Path loadReplayModOdsPack(String name) {
        if (IrisODSFrameCapturer.INSTANCE != null && IrisODSFrameCapturer.SHADER_PACK_NAME.equals(name)) {
            try {
                return Path.of(RePlayCore.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            } catch (Exception e) {
                throw new RuntimeException("Failed to get mod container for RePlayCore", e);
            }
        } else {
            return Iris.getShaderpacksDirectory();
        }
    }
}
