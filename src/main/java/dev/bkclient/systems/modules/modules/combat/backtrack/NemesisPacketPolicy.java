package dev.bkclient.systems.modules.modules.combat.backtrack;

import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.*;

/** Packet categories used by the supplied Nemesis Backtrack, mapped to 1.21.4. */
public final class NemesisPacketPolicy {
   private NemesisPacketPolicy() {}
   public static boolean shouldDelay(Class<?> type) {
      return CommonPingS2CPacket.class.isAssignableFrom(type)
            || EntitySetHeadYawS2CPacket.class.isAssignableFrom(type)
            || EntityS2CPacket.class.isAssignableFrom(type)
            || EntityPositionS2CPacket.class.isAssignableFrom(type)
            || EntityPositionSyncS2CPacket.class.isAssignableFrom(type)
            || EntityVelocityUpdateS2CPacket.class.isAssignableFrom(type)
            || ParticleS2CPacket.class.isAssignableFrom(type)
            || ExplosionS2CPacket.class.isAssignableFrom(type);
   }
}
