package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.combatengine.bridge.AuraRuntime;
import dev.bkclient.combatengine.bridge.SourceModuleAdapter;
import dev.bkclient.combatengine.client.modules.impl.combat.Aura2;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;

/**
 * Aura V3 host for the aura-working combat flow. The source's native JNI
 * adapter cannot be copied into a Fabric Java client; the game-facing part is
 * hosted by the existing BKAdaptive Aura core while AuraV3Math ports the source
 * engine's pure rotation/targeting primitives without JNI.
 */
@ModuleInfo(name = "AuraV3", category = ModuleCategory.COMBAT, desc = "BKClient/BKAdaptive aura engine port")
public final class AuraV3 extends SourceModuleAdapter {
   public AuraV3() {
      super(Aura2.embedded());
      AuraRuntime.initialize();
   }

   @Override
   public void onEnable() {
      AuraRuntime.selectAura(this);
      super.onEnable();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      AuraRuntime.clearAura(this);
      BKClientInit.getInstance().getTargetManager().setCurrentTarget(null);
   }
}
