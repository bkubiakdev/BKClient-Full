package dev.bkclient.combatengine.api.storages.implement.helpertstorages.enumvar;

import dev.bkclient.combatengine.client.modules.impl.combat.Aura;
import dev.bkclient.combatengine.client.modules.impl.combat.PacketCriticals;
import dev.bkclient.combatengine.client.modules.impl.player.NoJumpDelay;

/** The same singleton references used by the source Aura and its dependencies. */
public final class ModuleClass {
   public static final Aura aura = Aura.INSTANCE;
   public static final PacketCriticals packetCriticals = PacketCriticals.INSTANCE;
   public static final NoJumpDelay noJumpDelay = NoJumpDelay.INSTANCE;
}
