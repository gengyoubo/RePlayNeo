package github.com.gengyoubo.replayneo.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.replayneo.core.versions.MCVer;
import github.com.gengyoubo.replayneo.feature.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin extends Particle {
    protected SingleQuadParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Shadow
    public abstract float getQuadSize(float partialTicks);

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();

    /**
     * @author RePlayNeo
     * @reason Orient billboard particles per render direction during omnidirectional rendering without mutating the camera.
     */
    @Overwrite
    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp((double) partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp((double) partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp((double) partialTicks, this.zo, this.z) - cameraPos.z());

        Quaternionf quaternion;
        if (replayneo$useOmnidirectionalParticleRotation()) {
            quaternion = replayneo$particleRotation(camera, x, y, z);
        } else if (this.roll == 0.0F) {
            quaternion = camera.rotation();
        } else {
            quaternion = new Quaternionf(camera.rotation());
        }

        if (this.roll != 0.0F) {
            quaternion.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        }

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float size = this.getQuadSize(partialTicks);

        for (Vector3f vertex : vertices) {
            vertex.rotate(quaternion);
            vertex.mul(size);
            vertex.add(x, y, z);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        vertexConsumer.vertex(vertices[0].x(), vertices[0].y(), vertices[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        vertexConsumer.vertex(vertices[1].x(), vertices[1].y(), vertices[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        vertexConsumer.vertex(vertices[2].x(), vertices[2].y(), vertices[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        vertexConsumer.vertex(vertices[3].x(), vertices[3].y(), vertices[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    private boolean replayneo$useOmnidirectionalParticleRotation() {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        return handler != null && handler.omnidirectional;
    }

    private Quaternionf replayneo$particleRotation(Camera camera, float x, float y, float z) {
        Vec3 to = new Vec3(x, y, z);
        if (to.lengthSqr() < 1.0E-8) {
            return new Quaternionf(camera.rotation());
        }

        to = to.normalize();
        float dot = (float) to.z();
        if (dot < -0.999999F) {
            return new Quaternionf(0.0F, 1.0F, 0.0F, 0.0F);
        }

        Quaternionf rotation = new Quaternionf((float) -to.y(), (float) to.x(), 0.0F, 1.0F + dot);
        rotation.normalize();
        return rotation;
    }
}
