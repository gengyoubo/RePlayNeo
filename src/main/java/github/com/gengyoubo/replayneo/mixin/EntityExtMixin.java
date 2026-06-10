package github.com.gengyoubo.replayneo.mixin;

import github.com.gengyoubo.replayneo.platform.entity.EntityExt;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityExtMixin implements EntityExt {

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Unique
    private float rePlay$trackedYaw = Float.NaN;

    @Unique
    private float rePlay$trackedPitch = Float.NaN;

    @Override
    public float RePlayCore$getTrackedYaw() {
        return !Float.isNaN(this.rePlay$trackedYaw) ? this.rePlay$trackedYaw : this.yRot;
    }

    @Override
    public float RePlayCore$getTrackedPitch() {
        return !Float.isNaN(this.rePlay$trackedPitch) ? this.rePlay$trackedPitch : this.xRot;
    }

    @Override
    public void RePlayCore$setTrackedYaw(float value) {
        this.rePlay$trackedYaw = value;
    }

    @Override
    public void RePlayCore$setTrackedPitch(float value) {
        this.rePlay$trackedPitch = value;
    }
}
