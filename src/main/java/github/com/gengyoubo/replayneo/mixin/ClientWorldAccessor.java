package github.com.gengyoubo.replayneo.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientLevel.class)
public interface ClientWorldAccessor {
    @Accessor
    net.minecraft.world.level.entity.EntityTickList getTickingEntities();
}
