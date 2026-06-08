package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.feature.recording.ServerInfoExt;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerData.class)
public abstract class ServerInfoMixin implements ServerInfoExt {
    @Unique
    private Boolean rePlay$autoRecording;

    public Boolean rePlay$getAutoRecording() {
        return rePlay$autoRecording;
    }

    public void rePlay$setAutoRecording(Boolean autoRecording) {
        this.rePlay$autoRecording = autoRecording;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void serialize(CallbackInfoReturnable<CompoundTag> ci) {
        CompoundTag tag = ci.getReturnValue();
        if (rePlay$autoRecording != null) {
            tag.putBoolean("autoRecording", rePlay$autoRecording);
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void deserialize(CompoundTag tag, CallbackInfoReturnable<ServerData> ci) {
        ServerInfoExt serverInfo = ServerInfoExt.from(ci.getReturnValue());
        if (tag.contains("autoRecording")) {
            serverInfo.setAutoRecording(tag.getBoolean("autoRecording"));
        }
    }

    @Inject(method = "copyNameIconFrom", at = @At("RETURN"))
    public void copyFrom(ServerData serverInfo, CallbackInfo ci) {
        ServerInfoExt from = ServerInfoExt.from(serverInfo);
        this.rePlay$autoRecording = from.getAutoRecording();
    }
}
