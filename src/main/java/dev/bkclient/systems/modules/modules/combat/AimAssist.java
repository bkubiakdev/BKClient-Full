package dev.bkclient.systems.modules.modules.combat;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.combatengine.bridge.SourceModuleAdapter;
@ModuleInfo(name="AimAssist",category=ModuleCategory.COMBAT)
public final class AimAssist extends SourceModuleAdapter {
 public AimAssist(){super(dev.bkclient.combatengine.client.modules.impl.combat.AimAssistant.INSTANCE);}
}
