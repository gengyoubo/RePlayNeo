package github.com.gengyoubo.replayneo.feature.pathing.preview;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PostRenderWorldCallback;
import com.replaymod.core.versions.MCVer;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.Location;
import github.com.gengyoubo.replayneo.feature.pathing.ReplayModSimplePathing;
import github.com.gengyoubo.replayneo.feature.pathing.SPTimeline;
import github.com.gengyoubo.replayneo.RePlayNeo;
import com.replaymod.simplepathing.gui.GuiPathing;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;


import static com.replaymod.core.versions.MCVer.bindTexture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import static com.replaymod.core.ReplayMod.TEXTURE;
import static com.replaymod.core.versions.MCVer.emitLine;
import static com.replaymod.core.versions.MCVer.popMatrix;
import static com.replaymod.core.versions.MCVer.pushMatrix;
import static de.johni0702.minecraft.gui.versions.MCVer.identifier;

public class PathPreviewRenderer extends EventRegistrations {
    private static final ResourceLocation CAMERA_HEAD = identifier(RePlayNeo.RESOURCE_NAMESPACE, "camera_head.png");
    private static final Minecraft mc = MCVer.getMinecraft();

    private static final int SLOW_PATH_COLOR = 0xffcccc;
    private static final int FAST_PATH_COLOR = 0x660000;
    private static final double FASTEST_PATH_SPEED = 0.01;

    private final ReplayModSimplePathing mod;
    private final ReplayHandler replayHandler;

    public PathPreviewRenderer(ReplayModSimplePathing mod, ReplayHandler replayHandler) {
        this.mod = mod;
        this.replayHandler = replayHandler;
    }

    { on(PostRenderWorldCallback.EVENT, this::renderCameraPath); }
    private void renderCameraPath(PoseStack matrixStack) {
        if (!replayHandler.getReplaySender().isAsyncMode() || mc.options.hideGui) return;

        Entity view = mc.getCameraEntity();
        if (view == null) return;

        GuiPathing guiPathing = mod.getGuiPathing();
        if (guiPathing == null) return;
        EntityPositionTracker entityTracker = guiPathing.getEntityTracker();

        SPTimeline timeline = mod.getCurrentTimeline();
        if (timeline == null) return;
        Path path = timeline.getPositionPath();
        if (path.getKeyframes().isEmpty()) return;
        Path timePath = timeline.getTimePath();

        path.update();

        int renderDistance = mc.options.renderDistance().get() * 16;
        int renderDistanceSquared = renderDistance * renderDistance;

        Vector3f viewPos = new Vector3f(
                (float) view.getX(),
                (float) view.getY()
                ,
                (float) view.getZ()
        );

        pushMatrix();
        try {

            RenderSystem.getModelViewStack().mulPoseMatrix(matrixStack.last().pose());
            RenderSystem.applyModelViewMatrix();

            for (PathSegment segment : path.getSegments()) {
                Interpolator interpolator = segment.getInterpolator();
                Keyframe start = segment.getStartKeyframe();
                Keyframe end = segment.getEndKeyframe();
                long diff = (int) (end.getTime() - start.getTime());

                boolean spectator = interpolator.getKeyframeProperties().contains(SpectatorProperty.PROPERTY);
                if (spectator && entityTracker == null) {
                    continue; // Cannot render spectator positions when entity tracker is not yet loaded
                }
                // Spectator segments have 20 lines per second (at least 10) whereas normal segments have a fixed 100
                long steps = spectator ? Math.max(diff / 50, 10) : 100;
                Vector3f prevPos = null;
                for (int i = 0; i <= steps; i++) {
                    long time = start.getTime() + diff * i / steps;
                    if (spectator) {
                        Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
                        Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                        if (entityId.isPresent() && replayTime.isPresent()) {
                            Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                            if (loc != null) {
                                Vector3f pos = loc2Vec(loc);
                                if (prevPos != null) {
                                    drawConnection(viewPos, prevPos, pos, 0x0000ffff, renderDistanceSquared);
                                }
                                prevPos = pos;
                                continue;
                            }
                        }
                    } else {
                        Optional<Vector3f> optPos = path.getValue(CameraProperties.POSITION, time).map(this::tripleD2Vec);
                        if (optPos.isPresent()) {
                            Vector3f pos = optPos.get();
                            if (prevPos != null) {
                                double distance = Math.sqrt(distanceSquared(prevPos, pos));
                                double speed = Math.min(distance / ((double) diff / steps), FASTEST_PATH_SPEED);
                                double speedFraction = speed / FASTEST_PATH_SPEED;
                                int color = interpolateColor(speedFraction);
                                drawConnection(viewPos, prevPos, pos, (color << 8) | 0xff, renderDistanceSquared);
                            }
                            prevPos = pos;
                            continue;
                        }
                    }
                    prevPos = null;
                }
            }

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            path.getKeyframes().stream()
                    .map(k -> Pair.of(k, k.getValue(CameraProperties.POSITION).map(this::tripleD2Vec)))
                    .filter(p -> p.getRight().isPresent())
                    .map(p -> Pair.of(p.getLeft(), p.getRight().get()))
                    .filter(p -> distanceSquared(p.getRight(), viewPos) < renderDistanceSquared)
                    .sorted(new KeyframeComparator(viewPos)) // Need to render the furthest first
                    .forEachOrdered(p -> drawPoint(viewPos, p.getRight(), p.getLeft()));

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            int time = guiPathing.timeline.getCursorPosition();
            Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
            if (entityId.isPresent()) {
                // Spectating an entity
                if (entityTracker != null) {
                    Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                    if (replayTime.isPresent()) {
                        Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                        if (loc != null) {
                            drawCamera(viewPos, loc2Vec(loc), new Vector3f(loc.getYaw(), loc.getPitch(), 0f));
                        }
                    }
                }
            } else {
                // Normal camera path
                Optional<Vector3f> cameraPos = path.getValue(CameraProperties.POSITION, time).map(this::tripleD2Vec);
                Optional<Vector3f> cameraRot = path.getValue(CameraProperties.ROTATION, time).map(this::tripleF2Vec);
                if (cameraPos.isPresent() && cameraRot.isPresent()) {
                    drawCamera(viewPos, cameraPos.get(), cameraRot.get());
                }
            }
        } finally {
            popMatrix();
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private Vector3f loc2Vec(Location loc) {
        return new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
    }

    private Vector3f tripleD2Vec(Triple<Double, Double, Double> loc) {
        return new Vector3f(loc.getLeft().floatValue(), loc.getMiddle().floatValue(), loc.getRight().floatValue());
    }

    private Vector3f tripleF2Vec(Triple<Float, Float, Float> loc) {
        return new Vector3f(loc.getLeft(), loc.getMiddle(), loc.getRight());
    }

    private static int interpolateColor(double weight) {
        return (interpolateColorComponent((com.replaymod.simplepathing.preview.PathPreviewRenderer.SLOW_PATH_COLOR >> 16) & 0xff, (com.replaymod.simplepathing.preview.PathPreviewRenderer.FAST_PATH_COLOR >> 16) & 0xff, weight) << 16)
                | (interpolateColorComponent((com.replaymod.simplepathing.preview.PathPreviewRenderer.SLOW_PATH_COLOR >> 8) & 0xff, (com.replaymod.simplepathing.preview.PathPreviewRenderer.FAST_PATH_COLOR >> 8) & 0xff, weight) << 8)
                | interpolateColorComponent(com.replaymod.simplepathing.preview.PathPreviewRenderer.SLOW_PATH_COLOR & 0xff, com.replaymod.simplepathing.preview.PathPreviewRenderer.FAST_PATH_COLOR & 0xff, weight);
    }

    private static int interpolateColorComponent(int c1, int c2, double weight) {
        return (int) (c1 + (1 - Math.pow(Math.E, -4 * weight)) * (c2 - c1)) & 0xff;
    }

    private static double distanceSquared(Vector3f p1, Vector3f p2) {
        return Vector3f.sub(p1, p2, null).lengthSquared();
    }

    private void drawConnection(Vector3f view, Vector3f pos1, Vector3f pos2, int color, int renderDistanceSquared) {
        if (distanceSquared(view, pos1) > renderDistanceSquared) return;
        if (distanceSquared(view, pos2) > renderDistanceSquared) return;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        emitLine(new PoseStack(), buffer, Vector3f.sub(pos1, view, null), Vector3f.sub(pos2, view, null), color, 3f);

        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.disableCull();
        tessellator.end();
        RenderSystem.enableCull();
    }

    private void drawPoint(Vector3f view, Vector3f pos, Keyframe keyframe) {

        bindTexture(TEXTURE);

        float posX = 80f / ReplayMod.TEXTURE_SIZE;
        float posY = 0f;
        float size = 10f / ReplayMod.TEXTURE_SIZE;

        if (mod.isSelected(keyframe)) {
            posY += size;
        }

        if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
            posX += size;
        }

        float minX = -0.5f;
        float minY = -0.5f;
        float maxX = 0.5f;
        float maxY = 0.5f;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        vertex(buffer, minX, minY, 0, posX + size, posY + size, 255);
        vertex(buffer, minX, maxY, 0, posX + size, posY, 255);
        vertex(buffer, maxX, maxY, 0, posX, posY, 255);
        vertex(buffer, maxX, minY, 0, posX, posY + size, 255);

        pushMatrix();

        Vector3f t = Vector3f.sub(pos, view, null);
        GL11.glTranslatef(t.x, t.y, t.z);
        GL11.glRotatef(-mc.getEntityRenderDispatcher().camera.getYRot(), 0, 1, 0);
        GL11.glRotatef(mc.getEntityRenderDispatcher().camera.getXRot(), 1, 0, 0);

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        tessellator.end();

        popMatrix();
    }

    private void drawCamera(Vector3f view, Vector3f pos, Vector3f rot) {

        bindTexture(CAMERA_HEAD);

        pushMatrix();

        Vector3f t = Vector3f.sub(pos, view, null);
        GL11.glTranslatef(t.x, t.y, t.z);
        GL11.glRotatef(-rot.x, 0, 1, 0); // Yaw
        GL11.glRotatef(rot.y, 1, 0, 0); // Pitch
        GL11.glRotatef(rot.z, 0, 0, 1); // Roll

        //draw the position line
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);

        emitLine(new PoseStack(), buffer, new Vector3f(0, 0, 0), new Vector3f(0, 0, 2), 0x00ff00aa, 3f);

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        tessellator.end();


        // draw camera cube

        float cubeSize = 0.5f;

        float r = -cubeSize/2;

        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        //back
        vertex(buffer, r, r + cubeSize, r, 3 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r, 4*8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r, r, 4*8/64f, 2*8/64f, 200);
        vertex(buffer, r, r, r, 3*8/64f, 2*8/64f, 200);

        //front
        vertex(buffer, r + cubeSize, r, r + cubeSize, 2 * 8 / 64f, 2*8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r + cubeSize, 2 * 8 / 64f, 8/64f, 200);
        vertex(buffer, r, r + cubeSize, r + cubeSize, 8 / 64f, 8 / 64f, 200);
        vertex(buffer, r, r, r + cubeSize, 8 / 64f, 2*8/64f, 200);

        //left
        vertex(buffer, r + cubeSize, r + cubeSize, r, 0, 8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r + cubeSize, 8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r, r + cubeSize, 8/64f, 2*8/64f, 200);
        vertex(buffer, r+cubeSize, r, r, 0, 2*8/64f, 200);

        //right
        vertex(buffer, r, r + cubeSize, r + cubeSize, 2*8/64f, 8/64f, 200);
        vertex(buffer, r, r + cubeSize, r, 3*8/64f, 8/64f, 200);
        vertex(buffer, r, r, r, 3*8/64f, 2*8/64f, 200);
        vertex(buffer, r, r, r + cubeSize, 2 * 8 / 64f, 2 * 8 / 64f, 200);

        //bottom
        vertex(buffer, r + cubeSize, r, r, 3*8/64f, 0, 200);
        vertex(buffer, r + cubeSize, r, r + cubeSize, 3*8/64f, 8/64f, 200);
        vertex(buffer, r, r, r + cubeSize, 2*8/64f, 8/64f, 200);
        vertex(buffer, r, r, r, 2 * 8 / 64f, 0, 200);

        //top
        vertex(buffer, r, r + cubeSize, r, 8/64f, 0, 200);
        vertex(buffer, r, r + cubeSize, r + cubeSize, 8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r + cubeSize, 2*8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r, 2 * 8 / 64f, 0, 200);

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        tessellator.end();

        popMatrix();
    }

    private void vertex(BufferBuilder buffer, float x, float y, float z, float u, float v, int alpha) {
        buffer.vertex(x, y, z).uv(u, v).color(255, 255, 255, alpha).endVertex();
    }

    private static class KeyframeComparator implements Comparator<Pair<Keyframe, Vector3f>> {
        private final Vector3f viewPos;

        public KeyframeComparator(Vector3f viewPos) {
            this.viewPos = viewPos;
        }

        @Override
        public int compare(Pair<Keyframe, Vector3f> o1, Pair<Keyframe, Vector3f> o2) {
            return -Double.compare(distanceSquared(o1.getRight(), viewPos), distanceSquared(o2.getRight(), viewPos));
        }
    }
}
