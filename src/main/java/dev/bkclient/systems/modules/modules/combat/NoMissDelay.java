package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult;

/**
 * Vanilla resets the attack cooldown when a sword or axe swing hits nothing. While this is on, that reset is skipped
 * (see {@code PlayerEntityMixin}), so a miss does not cost the next hit its charge.
 */
@ModuleInfo(name = "No Miss Delay", category = ModuleCategory.COMBAT, desc = "Removes the sword delay after missing")
public final class NoMissDelay extends BaseModule {
   /** Called from the mixin when the local player's cooldown is about to be reset. */
   public boolean shouldSkipReset() {
      if (!this.isEnabled() || !CombatKit.ready()) return false;
      var held = CombatKit.mc.player.getMainHandStack().getItem();
      return (held instanceof SwordItem || held instanceof AxeItem)
         && CombatKit.mc.crosshairTarget != null && CombatKit.mc.crosshairTarget.getType() == HitResult.Type.MISS;
   }
}
