package dev.bkclient.systems.modules.modules.combat;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.combatengine.bridge.SourceModuleAdapter;
@ModuleInfo(name="TriggerBot",category=ModuleCategory.COMBAT)
public final class TriggerBot extends SourceModuleAdapter {
 public TriggerBot(){super(dev.bkclient.combatengine.client.modules.impl.combat.TriggerBot.INSTANCE);}
}
