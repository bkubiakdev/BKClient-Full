package dev.bkclient.mixin.accessors;

import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MatrixStack.Entry.class)
public interface MatrixStackEntryAccessor {
   @Accessor("canSkipNormalization")
   boolean bk$canSkipNormalization();
}
