package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.replayneo.platform.feature.replay.ReplayModReplay;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.client.debug.ChangedDebugRenderer", remap = false)
public abstract class ChangedDebugRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void replayneo$skipChangedDebugRendererInReplay(PoseStack poseStack,
                                                            MultiBufferSource.BufferSource bufferSource,
                                                            double cameraX,
                                                            double cameraY,
                                                            double cameraZ,
                                                            CallbackInfo ci) {
        ReplayModReplay replay = ReplayModReplay.instance;
        if (replay != null && replay.getReplayHandler() != null) {
            ci.cancel();
        }
    }
}
