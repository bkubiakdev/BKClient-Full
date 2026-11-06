package dev.bkclient.mixin.accessors;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("prevBodyYaw")
    void setPrevBodyYaw(float value);

    @Accessor("prevHeadYaw")
    void setPrevHeadYaw(float value);

    @Accessor("jumpingCooldown")
    void setJumpingCooldown(int value);
}
