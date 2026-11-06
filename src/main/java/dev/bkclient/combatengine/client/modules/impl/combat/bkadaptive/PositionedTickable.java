package dev.bkclient.combatengine.client.modules.impl.combat.bkadaptive;

import net.minecraft.util.math.Vec3d;

public interface PositionedTickable {
    Vec3d pos();

    void tick();
}
