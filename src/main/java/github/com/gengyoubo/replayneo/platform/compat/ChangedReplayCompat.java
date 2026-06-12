package github.com.gengyoubo.replayneo.platform.compat;

import github.com.gengyoubo.replayneo.RePlayNeo;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static github.com.gengyoubo.replayneo.platform.versions.MCVer.identifier;

public final class ChangedReplayCompat {
    public static final ResourceLocation TRANSFUR_SYNC_PAYLOAD =
            identifier(RePlayNeo.RESOURCE_NAMESPACE, "changed_transfur");

    private static final String PROCESS_TRANSFUR = "net.ltxprogrammer.changed.process.ProcessTransfur";

    private static Boolean changedAvailable;
    private static Method getPlayerTransfurVariantSafe;
    private static Method getPlayerTransfurVariantRaw;
    private static Method setPlayerTransfurVariantNamed;
    private static Method setPlayerTransfurVariantFull;
    private static Method removePlayerTransfurVariant;
    private static Method getFormId;
    private static Method variantFor;
    private static Method saveVariant;
    private static Method loadVariant;
    private static Method getMorphProgression;
    private static Method setTemporaryForSuit;
    private static Method setTransfurVariantDirect;
    private static Method setTransfurProgressDirect;
    private static Method hazardContext;
    private static Method registryGetValue;
    private static Method renderForm;
    private static Method getTransfurProgressionAt;
    private static Field transfurVariantRegistry;
    private static Field temporaryFromSuit;
    private static Field transfurProgression;
    private static Field transfurProgressionO;
    private static Field playerLatexVariant;
    private static Object defaultTransfurCause;
    private static boolean advancedSyncAvailable;
    private static int renderLogCount;
    private static int renderMissLogCount;
    private static int postApplyLogCount;
    private static final Map<Integer, PendingTransfur> pendingTransfurs = new ConcurrentHashMap<>();
    private static final Map<Integer, AppliedTransfur> activeTransfurs = new ConcurrentHashMap<>();

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
                Optional<?> variant = getPlayerTransfurVariant(player);
                CompoundTag data = advancedSyncAvailable
                        ? variant.map(ChangedReplayCompat::saveVariantData).orElseGet(CompoundTag::new)
                        : new CompoundTag();
                float progress = advancedSyncAvailable
                        ? variant.map(ChangedReplayCompat::getVariantProgress).orElse(1.0f)
                        : 1.0f;
                boolean temporary = advancedSyncAvailable
                        && variant.map(ChangedReplayCompat::isTemporaryFromSuit).orElse(false);
                buf.writeFloat(progress);
                buf.writeBoolean(temporary);
                buf.writeNbt(data);
                RePlayNeo.LOGGER.info("Recording Changed replay form. entityId={}, form={}, progress={}, temporary={}",
                        player.getId(), formId, progress, temporary);
            } else {
                RePlayNeo.LOGGER.info("Recording Changed replay form removal. entityId={}", player.getId());
            }
            return new ClientboundCustomPayloadPacket(TRANSFUR_SYNC_PAYLOAD, buf);
        } catch (Throwable throwable) {
            buf.release();
            RePlayNeo.LOGGER.warn("Could not create Changed replay form packet.", throwable);
            return null;
        }
    }

    public static ResourceLocation getFormId(Player player) {
        if (!isChangedAvailable() || player == null) {
            return null;
        }
        try {
            Optional<?> variant = getPlayerTransfurVariant(player);
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
            float progress = hasForm && buf.isReadable() ? buf.readFloat() : 1.0f;
            boolean temporary = hasForm && buf.isReadable() && buf.readBoolean();
            CompoundTag data = hasForm && buf.isReadable() ? buf.readNbt() : new CompoundTag();
            Entity entity = level.getEntity(entityId);
            if (!(entity instanceof Player player)) {
                pendingTransfurs.put(entityId, new PendingTransfur(hasForm, formId, progress, temporary, data));
                RePlayNeo.LOGGER.info("Queued Changed replay form until player entity exists. entityId={}, form={}",
                        entityId, formId);
                return true;
            }
            applyTransfurOnClientThread(player, hasForm, formId, progress, temporary, data);
            return true;
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.warn("Could not apply Changed replay form packet.", throwable);
            return true;
        }
    }

    public static boolean hasRenderableForm(Player player) {
        if (!isChangedAvailable() || player == null) {
            return false;
        }
        try {
            Object variant = getPlayerTransfurVariantRaw.invoke(null, player);
            if (variant == null) {
                variant = restoreActiveTransfur(player);
            }
            if (variant == null) {
                return false;
            }
            if (getTransfurProgressionAt != null) {
                Object progression = getTransfurProgressionAt.invoke(variant, 1.0f);
                RePlayNeo.LOGGER.debug("Changed replay form is renderable. entityId={}, progression={}",
                        player.getId(), progression);
            }
            return true;
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.debug("Could not check Changed replay render form.", throwable);
            return false;
        }
    }

    public static boolean renderReplayForm(Player player, Object poseStack, Object bufferSource, int packedLight, float partialTick) {
        if (!hasRenderableForm(player)) {
            if (renderMissLogCount++ < 8) {
                RePlayNeo.LOGGER.info("Changed replay render skipped; no form on render thread. entityId={}, playerClass={}",
                        player.getId(), player.getClass().getName());
            }
            return false;
        }
        try {
            renderForm.invoke(null, player, poseStack, bufferSource, packedLight, partialTick);
            if (renderLogCount++ < 5) {
                RePlayNeo.LOGGER.info("Rendered Changed replay form. entityId={}, form={}", player.getId(), getFormId(player));
            }
            return true;
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.warn("Could not render Changed replay form.", throwable);
            return false;
        }
    }

    public static void applyPendingTransfur(Entity entity) {
        if (!isChangedAvailable() || !(entity instanceof Player player)) {
            return;
        }
        PendingTransfur pending = pendingTransfurs.remove(entity.getId());
        if (pending == null) {
            return;
        }
        try {
            applyTransfurOnClientThread(player, pending.hasForm, pending.formId, pending.progress, pending.temporary, pending.data);
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.warn("Could not apply queued Changed replay form.", throwable);
        }
    }

    private static void applyTransfurOnClientThread(Player player, boolean hasForm, ResourceLocation formId,
                                                    float progress, boolean temporary, CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();
        Runnable task = () -> {
            try {
                applyTransfur(player, hasForm, formId, progress, temporary, data);
            } catch (Throwable throwable) {
                RePlayNeo.LOGGER.warn("Could not apply Changed replay form on client thread.", throwable);
            }
        };
        if (minecraft.isSameThread()) {
            task.run();
        } else {
            minecraft.tell(task);
        }
    }

    private static void applyTransfur(Player player, boolean hasForm, ResourceLocation formId, float progress,
                                      boolean temporary, CompoundTag data) throws ReflectiveOperationException {
        if (hasForm && formId != null) {
            Object instance;
            if (advancedSyncAvailable && setPlayerTransfurVariantFull != null) {
                Object variant = getTransfurVariant(formId);
                instance = variantFor.invoke(null, variant, player);
                if (instance != null) {
                    transfurProgression.setFloat(instance, progress);
                    transfurProgressionO.setFloat(instance, progress);
                    setTemporaryForSuit.invoke(instance, temporary);
                    setPlayerTransfurVariant(player, instance, progress);
                }
            } else {
                instance = setPlayerTransfurVariantNamed.invoke(null, player, formId);
            }
            if (advancedSyncAvailable && instance != null && data != null) {
                loadVariant.invoke(instance, data);
                transfurProgression.setFloat(instance, progress);
                transfurProgressionO.setFloat(instance, progress);
                setTemporaryForSuit.invoke(instance, temporary);
                setPlayerTransfurVariant(player, instance, progress);
            }
            activeTransfurs.put(player.getId(), new AppliedTransfur(formId, progress, temporary, data, instance));
            logPostApplyState(player, instance);
            RePlayNeo.LOGGER.info("Applied Changed replay form. entityId={}, form={}, progress={}, temporary={}, dataKeys={}",
                    player.getId(), formId, progress, temporary, data == null ? 0 : data.size());
        } else {
            activeTransfurs.remove(player.getId());
            removePlayerTransfurVariant.invoke(null, player);
            setPlayerTransfurVariant(player, null, 0.0f);
            logPostApplyState(player, null);
            RePlayNeo.LOGGER.info("Removed Changed replay form. entityId={}", player.getId());
        }
    }

    private static Object restoreActiveTransfur(Player player) throws ReflectiveOperationException {
        AppliedTransfur active = activeTransfurs.get(player.getId());
        if (active == null || active.instance == null) {
            return null;
        }
        transfurProgression.setFloat(active.instance, active.progress);
        transfurProgressionO.setFloat(active.instance, active.progress);
        setTemporaryForSuit.invoke(active.instance, active.temporary);
        setPlayerTransfurVariant(player, active.instance, active.progress);
        return active.instance;
    }

    private static void setPlayerTransfurVariant(Player player, Object instance, float progress) throws ReflectiveOperationException {
        setTransfurVariantDirect.invoke(player, instance);
        if (setTransfurProgressDirect != null) {
            setTransfurProgressDirect.invoke(player, progress);
        }
        Field field = getPlayerLatexVariantField(player);
        if (field != null) {
            field.set(player, instance);
        }
    }

    private static void logPostApplyState(Player player, Object expected) {
        if (postApplyLogCount++ >= 8) {
            return;
        }
        try {
            Object raw = getPlayerTransfurVariantRaw.invoke(null, player);
            Object field = null;
            Field latexField = getPlayerLatexVariantField(player);
            if (latexField != null) {
                field = latexField.get(player);
            }
            RePlayNeo.LOGGER.info("Changed replay post-apply state. entityId={}, expected={}, raw={}, field={}, playerClass={}",
                    player.getId(), expected != null, raw != null, field != null, player.getClass().getName());
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.debug("Could not inspect Changed replay post-apply state.", throwable);
        }
    }

    private static Field getPlayerLatexVariantField(Player player) {
        if (playerLatexVariant != null) {
            return playerLatexVariant;
        }
        Class<?> type = player.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("latexVariant");
                field.setAccessible(true);
                playerLatexVariant = field;
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private record PendingTransfur(boolean hasForm, ResourceLocation formId, float progress, boolean temporary,
                                   CompoundTag data) {
    }

    private record AppliedTransfur(ResourceLocation formId, float progress, boolean temporary, CompoundTag data,
                                   Object instance) {
    }

    @SuppressWarnings("unchecked")
    private static Optional<?> getPlayerTransfurVariant(Player player) throws ReflectiveOperationException {
        return (Optional<?>) getPlayerTransfurVariantSafe.invoke(null, player);
    }

    private static CompoundTag saveVariantData(Object variant) {
        try {
            return (CompoundTag) saveVariant.invoke(variant);
        } catch (Throwable throwable) {
            RePlayNeo.LOGGER.debug("Could not save Changed replay form data.", throwable);
            return new CompoundTag();
        }
    }

    private static float getVariantProgress(Object variant) {
        try {
            return (float) getMorphProgression.invoke(variant);
        } catch (Throwable throwable) {
            return 1.0f;
        }
    }

    private static boolean isTemporaryFromSuit(Object variant) {
        try {
            return temporaryFromSuit.getBoolean(variant);
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static Object getTransfurVariant(ResourceLocation formId) throws ReflectiveOperationException {
        Object holder = transfurVariantRegistry.get(null);
        return registryGetValue.invoke(holder, formId);
    }

    private static boolean isChangedAvailable() {
        if (changedAvailable != null) {
            return changedAvailable;
        }
        try {
            Class<?> processTransfur = Class.forName(PROCESS_TRANSFUR);
            getPlayerTransfurVariantRaw = processTransfur.getMethod("getPlayerTransfurVariant", Player.class);
            getPlayerTransfurVariantSafe = processTransfur.getMethod("getPlayerTransfurVariantSafe", Player.class);
            setPlayerTransfurVariantNamed = processTransfur.getMethod("setPlayerTransfurVariantNamed", Player.class, ResourceLocation.class);
            removePlayerTransfurVariant = processTransfur.getMethod("removePlayerTransfurVariant", Player.class);
            Class<?> transfurVariantInstance = Class.forName("net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance");
            getFormId = transfurVariantInstance.getMethod("getFormId");
            getTransfurProgressionAt = transfurVariantInstance.getMethod("getTransfurProgression", float.class);
            Class<?> formRenderHandler = Class.forName("net.ltxprogrammer.changed.client.FormRenderHandler");
            Class<?> poseStack = Class.forName("com.mojang.blaze3d.vertex.PoseStack");
            Class<?> multiBufferSource = Class.forName("net.minecraft.client.renderer.MultiBufferSource");
            renderForm = formRenderHandler.getMethod("renderForm", Player.class, poseStack, multiBufferSource, int.class, float.class);
            changedAvailable = true;
            RePlayNeo.LOGGER.info("Changed replay compatibility enabled.");

            try {
                initializeAdvancedSync(processTransfur, transfurVariantInstance);
                advancedSyncAvailable = true;
                RePlayNeo.LOGGER.info("Changed replay compatibility enabled full variant sync.");
            } catch (Throwable throwable) {
                advancedSyncAvailable = false;
                RePlayNeo.LOGGER.warn("Changed replay compatibility will use form-id sync only.", throwable);
            }
        } catch (Throwable throwable) {
            changedAvailable = false;
            advancedSyncAvailable = false;
            RePlayNeo.LOGGER.warn("Changed replay compatibility is unavailable.", throwable);
        }
        return changedAvailable;
    }

    @SuppressWarnings("unchecked")
    private static void initializeAdvancedSync(Class<?> processTransfur, Class<?> transfurVariantInstance) throws ReflectiveOperationException {
        Class<?> transfurVariant = Class.forName("net.ltxprogrammer.changed.entity.variant.TransfurVariant");

        saveVariant = transfurVariantInstance.getMethod("save");
        loadVariant = transfurVariantInstance.getMethod("load", CompoundTag.class);
        variantFor = transfurVariantInstance.getMethod("variantFor", transfurVariant, Player.class);
        getMorphProgression = transfurVariantInstance.getMethod("getMorphProgression");
        setTemporaryForSuit = transfurVariantInstance.getMethod("setTemporaryForSuit", boolean.class);
        temporaryFromSuit = transfurVariantInstance.getDeclaredField("isTemporaryFromSuit");
        temporaryFromSuit.setAccessible(true);
        transfurProgression = transfurVariantInstance.getField("transfurProgression");
        transfurProgressionO = transfurVariantInstance.getField("transfurProgressionO");

        Class<?> transfurContext = Class.forName("net.ltxprogrammer.changed.entity.TransfurContext");
        Class<?> transfurCause = Class.forName("net.ltxprogrammer.changed.entity.TransfurCause");
        defaultTransfurCause = findTransfurCause(transfurCause);
        hazardContext = transfurContext.getMethod("hazard", transfurCause);
        setPlayerTransfurVariantFull = processTransfur.getMethod("setPlayerTransfurVariant",
                Player.class, transfurVariant, transfurContext, float.class, boolean.class);

        Class<?> changedRegistry = Class.forName("net.ltxprogrammer.changed.init.ChangedRegistry");
        transfurVariantRegistry = changedRegistry.getField("TRANSFUR_VARIANT");
        Class<?> playerDataExtension = Class.forName("net.ltxprogrammer.changed.entity.PlayerDataExtension");
        setTransfurVariantDirect = playerDataExtension.getMethod("setTransfurVariant", transfurVariantInstance);
        setTransfurProgressDirect = playerDataExtension.getMethod("setTransfurProgress", float.class);
        Class<?> registryHolder = Class.forName("net.ltxprogrammer.changed.init.ChangedRegistry$RegistryHolder");
        registryGetValue = registryHolder.getMethod("getValue", ResourceLocation.class);
    }

    private static Object findTransfurCause(Class<?> transfurCause) {
        for (String name : new String[] {"DEFAULT", "FLOOR_HAZARD", "CRYSTAL", "SYRINGE"}) {
            Object constant = findEnumConstantByName(transfurCause, name);
            if (constant != null) {
                return constant;
            }
        }

        Object[] constants = transfurCause.getEnumConstants();
        if (constants != null && constants.length > 0) {
            return constants[0];
        }
        throw new IllegalStateException("Changed TransfurCause has no enum constants.");
    }

    private static Object findEnumConstantByName(Class<?> enumType, String name) {
        Object[] constants = enumType.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals(name)) {
                return constant;
            }
        }
        return null;
    }
}
