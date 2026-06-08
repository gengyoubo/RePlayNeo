package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.recording.handler.RecordingEventHandler;
import github.com.gengyoubo.replayneo.feature.replay.ext.EntityExt;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique
    private static final Minecraft rePlay$mcStatic = MCVer.getMinecraft();

    @Final
    @Shadow
    private Map<UUID, PlayerInfo> playerInfoMap;

    @Unique
    private Entity replayMod$entity;

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float getTrackedYaw(Entity instance) {
        return ((EntityExt) instance).replaymod$getTrackedYaw();
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getXRot()F"))
    private float getTrackedPitch(Entity instance) {
        return ((EntityExt) instance).replaymod$getTrackedPitch();
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double getTrackedX(Entity instance) {
        return instance.isPassenger() ? instance.getX() : instance.getPositionCodec().decode(0, 0, 0).x;
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getY()D"))
    private double getTrackedY(Entity instance) {
        return instance.isPassenger() ? instance.getY() : instance.getPositionCodec().decode(0, 0, 0).y;
    }

    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double getTrackedZ(Entity instance) {
        return instance.isPassenger() ? instance.getZ() : instance.getPositionCodec().decode(0, 0, 0).z;
    }

    @ModifyVariable(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFIZ)V"), ordinal = 0)
    private Entity captureEntity(Entity entity) {
        return this.replayMod$entity = entity;
    }

    @Inject(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At("RETURN"))
    private void resetEntityField(CallbackInfo ci) {
        this.replayMod$entity = null;
    }

    @ModifyArg(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFIZ)V"), index = 3)
    private float captureTrackedYaw(float value) {
        ((EntityExt) this.replayMod$entity).replaymod$setTrackedYaw(value);
        return value;
    }

    @ModifyArg(method = { "handleMoveEntity", "handleTeleportEntity" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFIZ)V"), index = 4)
    private float captureTrackedPitch(float value) {
        ((EntityExt) this.replayMod$entity).replaymod$setTrackedPitch(value);
        return value;
    }

    @Unique
    public RecordingEventHandler rePlay$getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) rePlay$mcStatic.levelRenderer).getRecordingEventHandler();
    }

    @Inject(method = "handlePlayerInfoUpdate", at = @At("HEAD"))
    public void recordOwnJoin(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        if (!rePlay$mcStatic.isSameThread()) return;
        if (rePlay$mcStatic.player == null) return;

        RecordingEventHandler handler = rePlay$getRecordingEventHandler();
        if (handler != null && packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
            for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
                UUID uuid = entry.profile().getId();
                if (uuid.equals(rePlay$mcStatic.player.getGameProfile().getId()) && !this.playerInfoMap.containsKey(uuid)) {
                    handler.spawnRecordingPlayer();
                }
            }
        }
    }

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    public void recordOwnRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        RecordingEventHandler handler = rePlay$getRecordingEventHandler();
        if (handler != null) {
            handler.spawnRecordingPlayer();
        }
    }
}
