package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.combatengine.bridge.AuraRuntime;
import dev.bkclient.combatengine.bridge.SourceModuleAdapter;
import dev.bkclient.combatengine.client.modules.impl.combat.AdaptiveAura;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;

/** UI/lifecycle adapter for the standalone adaptive aura core. */
@ModuleInfo(name = "AuraAdaptive", category = ModuleCategory.COMBAT, desc = "Adaptive aura with full attack flow")
public final class AuraAdaptive extends SourceModuleAdapter {
   public AuraAdaptive() {
      super(AdaptiveAura.INSTANCE);
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
