package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.platform.feature.recording.ReplayModRecording;
import github.com.gengyoubo.replayneo.platform.feature.recording.handler.RecordingEventHandler;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique
    private static final Minecraft rePlay$mcStatic = MCVer.getMinecraft();

    @Final
    @Shadow
    private Map<UUID, PlayerInfo> playerInfoMap;

    @Final
    @Shadow
    private Connection connection;

    @Unique
    public RecordingEventHandler rePlay$getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) rePlay$mcStatic.levelRenderer).getRecordingEventHandler();
    }

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void replayneo$initiateLocalRecording(ClientboundLoginPacket packet, CallbackInfo ci) {
        if (!this.connection.isMemoryConnection()) {
            return;
        }
        if (rePlay$getRecordingEventHandler() != null) {
            return;
        }
        ReplayModRecording.instance.initiateRecording(this.connection);
        RecordingEventHandler handler = rePlay$getRecordingEventHandler();
        if (handler != null) {
            handler.onPacket(packet);
        }
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
