package dev.bkclient.mixin.accessors;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("prevX")
    void setPrevX(double prevX);

    @Accessor("prevY")
    void setPrevY(double prevY);

    @Accessor("prevZ")
    void setPrevZ(double prevZ);

    @Accessor("lastRenderX")
    void setLastRenderX(double lastRenderX);

    @Accessor("lastRenderY")
    void setLastRenderY(double lastRenderY);

    @Accessor("lastRenderZ")
    void setLastRenderZ(double lastRenderZ);

    @Accessor("prevYaw")
    void setPrevYaw(float prevYaw);

    @Accessor("prevPitch")
    void setPrevPitch(float prevPitch);
}
