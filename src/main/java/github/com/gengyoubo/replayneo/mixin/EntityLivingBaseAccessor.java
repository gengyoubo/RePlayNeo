package github.com.gengyoubo.replayneo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nonnull;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public interface EntityLivingBaseAccessor {
    @Accessor("DATA_LIVING_ENTITY_FLAGS")
    @Nonnull
    @SuppressWarnings("ConstantConditions")
    static EntityDataAccessor<Byte> getLivingFlags() { return null; }

    @Accessor("lerpX")
    double getInterpTargetX();

    @Accessor("lerpY")
    double getInterpTargetY();

    @Accessor("lerpZ")
    double getInterpTargetZ();

    @Accessor("lerpYRot")
    double getInterpTargetYaw();

    @Accessor("lerpXRot")
    double getInterpTargetPitch();

    @Accessor("useItemRemaining")
    int getActiveItemStackUseCount();

    @Accessor("useItemRemaining")
    void setActiveItemStackUseCount(int value);
}
