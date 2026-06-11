package github.com.gengyoubo.replayneo.platform.compat;

import github.com.gengyoubo.replayneo.RePlayNeo;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.Optional;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;

public final class ChangedReplayCompat {
    public static final ResourceLocation TRANSFUR_SYNC_PAYLOAD =
            identifier(RePlayNeo.RESOURCE_NAMESPACE, "changed_transfur");

    private static final String PROCESS_TRANSFUR = "net.ltxprogrammer.changed.process.ProcessTransfur";

    private static Boolean changedAvailable;
    private static Method getPlayerTransfurVariantSafe;
    private static Method setPlayerTransfurVariantNamed;
    private static Method removePlayerTransfurVariant;
    private static Method getFormId;

    private ChangedReplayCompat() {
    }

    public static ClientboundCustomPayloadPacket createTransfurPayload(Player player) {
        if (!isChangedAvailable()) {
            return null;
        }
        ResourceLocation formId = getFormId(player);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(player.getId());
            buf.writeBoolean(formId != null);
            if (formId != null) {
                buf.writeResourceLocation(formId);
            }
            return new ClientboundCustomPayloadPacket(TRANSFUR_SYNC_PAYLOAD, buf);
        } catch (Throwable throwable) {
            buf.release();
            throw throwable;
        }
    }

    public static ResourceLocation getFormId(Player player) {
        if (!isChangedAvailable() || player == null) {
            return null;
        }
        try {
            Optional<?> variant = (Optional<?>) getPlayerTransfurVariantSafe.invoke(null, player);
            if (variant.isEmpty()) {
                return null;
            }
            return (ResourceLocation) getFormId.invoke(variant.get());
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.debug("Could not read Changed transfur form for replay recording.", throwable);
            return null;
        }
    }

    public static boolean applyTransfurPayload(FriendlyByteBuf buf, Level level) {
        if (!isChangedAvailable() || level == null) {
            return true;
        }
        try {
            int entityId = buf.readVarInt();
            boolean hasForm = buf.readBoolean();
            ResourceLocation formId = hasForm ? buf.readResourceLocation() : null;
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof Player player)) {
                RePlayNeo.LOGGER.debug("Changed replay form packet has no player target. entityId={}, form={}", entityId, formId);
                return true;
            }
            if (formId != null) {
                setPlayerTransfurVariantNamed.invoke(null, player, formId);
            } else {
                removePlayerTransfurVariant.invoke(null, player);
            }
            return true;
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.warn("Could not apply Changed replay form packet.", throwable);
            return true;
        }
    }

    private static boolean isChangedAvailable() {
        if (changedAvailable != null) {
            return changedAvailable;
        }
        try {
            Class<?> processTransfur = Class.forName(PROCESS_TRANSFUR);
            getPlayerTransfurVariantSafe = processTransfur.getMethod("getPlayerTransfurVariantSafe", Player.class);
            setPlayerTransfurVariantNamed = processTransfur.getMethod("setPlayerTransfurVariantNamed", Player.class, ResourceLocation.class);
            removePlayerTransfurVariant = processTransfur.getMethod("removePlayerTransfurVariant", Player.class);
            Class<?> transfurVariantInstance = Class.forName("net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance");
            getFormId = transfurVariantInstance.getMethod("getFormId");
            changedAvailable = true;
        } catch (Throwable throwable) {
            changedAvailable = false;
        }
        return changedAvailable;
    }
}
