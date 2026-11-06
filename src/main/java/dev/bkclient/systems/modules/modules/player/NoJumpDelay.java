package dev.bkclient.systems.modules.modules.player;

import dev.bkclient.mixin.accessors.LivingEntityAccessor;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.utility.game.EntityUtility;

@ModuleInfo(name = "No Jump Delay", category = ModuleCategory.PLAYER, desc = "Removes the vanilla jump cooldown")
public class NoJumpDelay extends BaseModule {
    @Override
    public void tick() {
        if (!EntityUtility.isInGame()) {
            return;
        }

        ((LivingEntityAccessor) NoJumpDelay.mc.player).setJumpingCooldown(0);
        super.tick();
    }
}
