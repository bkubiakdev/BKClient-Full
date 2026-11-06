package dev.bkclient.utility.rotations.modes;

import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.rotations.MoveCorrection;
import dev.bkclient.utility.rotations.RotationHandler;
import net.minecraft.entity.LivingEntity;

public interface AuraRotationMode extends IMinecraft {
   void rotate(Aura var1, RotationHandler var2, LivingEntity var3, MoveCorrection var4);

   default void reset() {
   }
}
