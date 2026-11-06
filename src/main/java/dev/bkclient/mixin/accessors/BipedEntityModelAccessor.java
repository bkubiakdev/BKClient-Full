package dev.bkclient.mixin.accessors;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BipedEntityModel.class)
public interface BipedEntityModelAccessor {
   @Accessor("head")
   ModelPart bkclient$getHead();

   @Accessor("hat")
   ModelPart bkclient$getHat();

   @Accessor("body")
   ModelPart bkclient$getBody();

   @Accessor("rightArm")
   ModelPart bkclient$getRightArm();

   @Accessor("leftArm")
   ModelPart bkclient$getLeftArm();

   @Accessor("rightLeg")
   ModelPart bkclient$getRightLeg();

   @Accessor("leftLeg")
   ModelPart bkclient$getLeftLeg();
}
