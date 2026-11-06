package dev.bkclient.systems.modules.modules.movement;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;

@ModuleInfo(name = "Auto Sprint", category = ModuleCategory.MOVEMENT, enabledByDefault = true)
public class AutoSprint extends BaseModule {
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> mc.options.sprintKey.setPressed(true);
}
