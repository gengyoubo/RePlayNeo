package github.com.gengyoubo.replayneo.platform.versions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import github.com.gengyoubo.replayneo.mixin.MinecraftAccessor;
import github.com.gengyoubo.replayneo.restored.com.replaymod.gradle.remap.Pattern;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;
import net.minecraft.core.NonNullList;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

class Patterns {
    @Pattern
    private static void addCrashCallable(CrashReportCategory category, String name, CrashReportDetail<String> callable) {
        category.setDetail(name, callable);
    }

    @Pattern
    private static double Entity_getX(Entity entity) {
        return entity.getX();
    }

    @Pattern
    private static double Entity_getY(Entity entity) {
        return entity.getY();
    }

    @Pattern
    private static double Entity_getZ(Entity entity) {
        return entity.getZ();
    }

    @Pattern
    private static void Entity_setYaw(Entity entity, float value) {
        entity.setYRot(value);
    }

    @Pattern
    private static float Entity_getYaw(Entity entity) {
        return entity.getYRot();
    }

    @Pattern
    private static void Entity_setPitch(Entity entity, float value) {
        entity.setXRot(value);
    }

    @Pattern
    private static float Entity_getPitch(Entity entity) {
        return entity.getXRot();
    }

    @Pattern
    private static void Entity_setPos(Entity entity, double x, double y, double z) {
        entity.setPosRaw(x, y, z);
    }

    @Pattern
    private static int getX(AbstractButton button) {
        return button.getX();
    }

    @Pattern
    private static int getY(AbstractButton button) {
        return button.getY();
    }

    @Pattern
    private static void setX(AbstractButton button, int value) {
        button.setX(value);
    }

    @Pattern
    private static void setY(AbstractButton button, int value) {
        button.setY(value);
    }

    @Pattern
    private static void setWidth(AbstractButton button, int value) {
        button.setWidth(value);
    }

    @Pattern
    private static int getWidth(AbstractButton button) {
        return button.getWidth();
    }

    @Pattern
    private static int getHeight(AbstractButton button) {
        return button.getHeight();
    }

    @Pattern
    private static String readString(FriendlyByteBuf buffer, int max) {
        return buffer.readUtf(max);
    }

    @Pattern
    private static Entity getRenderViewEntity(Minecraft mc) {
        return mc.getCameraEntity();
    }

    @Pattern
    private static void setRenderViewEntity(Minecraft mc, Entity entity) {
        mc.setCameraEntity(entity);
    }

    @Pattern
    private static Entity getVehicle(Entity passenger) {
        return passenger.getVehicle();
    }

    @Pattern
    private static Inventory getInventory(Player entity) {
        return entity.getInventory();
    }

    @Pattern
    private static Iterable<Entity> loadedEntityList(ClientLevel world) {
        return world.entitiesForRendering();
    }

    @Pattern
    private static List<? extends Player> playerEntities(Level world) {
        return world.players();
    }

    @Pattern
    private static boolean isOnMainThread(Minecraft mc) {
        return mc.isSameThread();
    }

    @Pattern
    private static void scheduleOnMainThread(Minecraft mc, Runnable runnable) {
        mc.tell(runnable);
    }

    @Pattern
    private static Window getWindow(Minecraft mc) {
        return mc.getWindow();
    }

    @Pattern
    private static BufferBuilder Tesselator_getBuffer(Tesselator tessellator) {
        return tessellator.getBuilder();
    }

    @Pattern
    private static void VertexConsumer_next(VertexConsumer buffer) {
        buffer.endVertex();
    }

    @Pattern
    private static Tesselator Tesselator_getInstance() {
        return Tesselator.getInstance();
    }

    @Pattern
    private static EntityRenderDispatcher getEntityRenderDispatcher(Minecraft mc) {
        return mc.getEntityRenderDispatcher();
    }

    @Pattern
    private static float getCameraYaw(EntityRenderDispatcher dispatcher) {
        return dispatcher.camera.getYRot();
    }

    @Pattern
    private static float getCameraPitch(EntityRenderDispatcher dispatcher) {
        return dispatcher.camera.getXRot();
    }

    @Pattern
    private static float getRenderPartialTicks(Minecraft mc) {
        return mc.getFrameTime();
    }

    @Pattern
    private static TextureManager getTextureManager(Minecraft mc) {
        return mc.getTextureManager();
    }

    @Pattern
    private static String getBoundKeyName(KeyMapping keyBinding) {
        return keyBinding.getTranslatedKeyMessage().getString();
    }

    @Pattern
    private static SimpleSoundInstance master(ResourceLocation sound, float pitch) {
        return SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(sound), pitch);
    }

    @Pattern
    private static boolean isKeyBindingConflicting(KeyMapping a, KeyMapping b) {
        return a.equals(b);
    }

    @Pattern
    private static void BufferBuilder_beginLineStrip(BufferBuilder buffer, VertexFormat vertexFormat) {
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    }

    @Pattern
    private static void BufferBuilder_beginLines(BufferBuilder buffer) {
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    }

    @Pattern
    private static void BufferBuilder_beginQuads(BufferBuilder buffer, VertexFormat vertexFormat) {
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, vertexFormat);
    }

    @Pattern
    private static void GL11_glLineWidth(float width) {
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(width);
    }

    @Pattern
    private static void GL11_glTranslatef(float x, float y, float z) {
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().translate(x, y, z);
    }

    @Pattern
    private static void GL11_glRotatef(float angle, float x, float y, float z) {
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().mulPose(github.com.gengyoubo.replayneo.platform.versions.LegacyMCVer.quaternion(angle, new org.joml.Vector3f(x, y, z)));
    }

    @SuppressWarnings("rawtypes") // preprocessor bug: doesn't work with generics
    @Pattern
    private static void Futures_addCallback(ListenableFuture future, FutureCallback callback) {
        Futures.addCallback(future, callback, Runnable::run);
    }

    @Pattern
    private static void setCrashReport(Minecraft mc, CrashReport report) {
        mc.delayCrashRaw(report);
    }

    @Pattern
    private static ReportedException crashReportToException(Minecraft mc) {
        return new ReportedException(((MinecraftAccessor) mc).getCrashReporter().get());
    }

    @Pattern
    private static boolean haveDelayedCrash(Minecraft mc) {
        return ((MinecraftAccessor) mc).getCrashReporter() != null;
    }

    @Pattern
    private static Vec3 getTrackedPosition(Entity entity) {
        return entity.getPositionCodec().decode(0, 0, 0);
    }

    @Pattern
    private static Component newTextLiteral(String str) {
        return net.minecraft.network.chat.Component.literal(str);
    }

    @Pattern
    private static Component newTextTranslatable(String key, Object...args) {
        return net.minecraft.network.chat.Component.translatable(key, args);
    }

    @Pattern
    private static Vec3 getTrackedPos(Entity entity) {
        return entity.getPositionCodec().decode(0, 0, 0);
    }

    @Pattern
    private static void setGamma(Options options, double value) {
        options.gamma().set(value);
    }

    @Pattern
    private static double getGamma(Options options) {
        return options.gamma().get();
    }

    @Pattern
    private static int getViewDistance(Options options) {
        return options.renderDistance().get();
    }

    @Pattern
    private static double getFov(Options options) {
        return options.fov().get();
    }

    @Pattern
    private static int getGuiScale(Options options) {
        return options.guiScale().get();
    }

    @Pattern
    private static Resource getResource(ResourceManager manager, ResourceLocation id) throws IOException {
        return manager.getResourceOrThrow(id);
    }

    @Pattern
    private static List<ItemStack> DefaultedList_ofSize_ItemStack_Empty(int size) {
        return NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Pattern
    private static void setSoundVolume(Options options, SoundSource category, float value) {
        options.getSoundSourceOptionInstance(category).set((double) value);
    }

    @Pattern
    private static SoundEvent SoundEvent_of(ResourceLocation identifier) {
        return SoundEvent.createVariableRangeEvent(identifier);
    }

    @Pattern
    private static Vector3f POSITIVE_X() {
        return new org.joml.Vector3f(1, 0, 0);
    }

    @Pattern
    private static Vector3f POSITIVE_Y() {
        return new org.joml.Vector3f(0, 1, 0);
    }

    @Pattern
    private static Vector3f POSITIVE_Z() {
        return new org.joml.Vector3f(0, 0, 1);
    }

    @Pattern
    private static Quaternionf getDegreesQuaternion(Vector3f axis, float angle) {
        return new org.joml.Quaternionf().fromAxisAngleDeg(axis, angle);
    }

    @Pattern
    private static void Quaternion_mul(Quaternionf left, Quaternionf right) {
        left.mul(right);
    }

    @Pattern
    private static float Quaternion_getX(Quaternionf q) {
        return q.x;
    }

    @Pattern
    private static float Quaternion_getY(Quaternionf q) {
        return q.y;
    }

    @Pattern
    private static float Quaternion_getZ(Quaternionf q) {
        return q.z;
    }

    @Pattern
    private static float Quaternion_getW(Quaternionf q) {
        return q.w;
    }

    @Pattern
    private static Quaternionf Quaternion_copy(Quaternionf source) {
        return new org.joml.Quaternionf(source);
    }

    @Pattern
    private static void Matrix4f_multiply(Matrix4f left, Matrix4f right) {
        left.mul(right);
    }

    @Pattern
    private static Matrix4f Matrix4f_translate(float x, float y, float z) {
        return new Matrix4f().translation(x, y, z);
    }

    @Pattern
    private static Matrix4f Matrix4f_perspectiveMatrix(float left, float right, float top, float bottom, float zNear, float zFar) {
        return github.com.gengyoubo.replayneo.platform.versions.LegacyMCVer.ortho(left, right, top, bottom, zNear, zFar);
    }

    @Pattern
    private static Registry<? extends Registry<?>> REGISTRIES() {
        return net.minecraft.core.registries.BuiltInRegistries.REGISTRY;
    }

    @Pattern
    public Level getWorld(Entity entity) {
        return entity.level();
    }

    @Pattern
    public Object channel(ClientboundCustomPayloadPacket packet) {
        return packet.getIdentifier();
    }

    @Pattern
    public Integer getPacketId(ConnectionProtocol state, PacketFlow side, Packet<?> packet) {
        return state.getPacketId(side, packet);
    }

    @Pattern
    public int UnloadChunkPacket_getX(ClientboundForgetLevelChunkPacket packet) {
        return packet.getX();
    }

    @Pattern
    public int UnloadChunkPacket_getZ(ClientboundForgetLevelChunkPacket packet) {
        return packet.getZ();
    }

    @Pattern
    public UUID getId(ClientboundPlayerInfoUpdatePacket.Entry entry) {
        return entry.profileId();
    }

    @Pattern
    public ResourceLocation getSkinTexture(AbstractClientPlayer player) {
        return player.getSkinTextureLocation();
    }

    @Pattern
    public boolean isDebugHudEnabled(Minecraft mc) {
        return mc.options.renderDebug;
    }

    @Pattern
    public Component getMessage(ClientboundDisconnectPacket packet) {
        return packet.getReason();
    }
}
