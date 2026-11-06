package dev.bkclient.combatengine.client.modules.impl.combat;

import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventAttackEntity;
import dev.bkclient.combatengine.client.modules.Module;
import java.util.Random;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class PacketCriticals extends Module {
   public static final PacketCriticals INSTANCE = new PacketCriticals();
   private static final Random recoveredField1 = new Random();
   private int recoveredField2 = 0;
   private int recoveredField3 = 0;

   public PacketCriticals() {
      super("PacketCriticals", "Бьёт критами с обходом античитов", Module.ModuleCategory.COMBAT);
   }

   @EventLink
   public void onAttack(EventAttackEntity event) {
      if (mc.player != null && mc.world != null) {
         if (!mc.player.isOnGround()) {
            if (mc.player.hurtTime <= 0) {
               double var2 = mc.player.getX();
               double var4 = mc.player.getY();
               double var6 = mc.player.getZ();
               double var8 = mc.player.getYaw();
               double var10 = mc.player.getPitch();
               this.recoveredField2 = recoveredField1.nextInt(7);
               switch (this.recoveredField2) {
                  case 0:
                     double var20 = 0.001 + recoveredField1.nextDouble() * 0.005;
                     this.recoveredMethod1(var2, var4 + var20, var6, false);
                     this.recoveredMethod1(var2, var4, var6, false);
                     break;
                  case 1:
                     double var19 = 0.0025 + recoveredField1.nextDouble() * 0.003;
                     this.recoveredMethod1(var2, var4 + var19, var6, false);
                     this.recoveredMethod1(var2, var4 - 0.001, var6, false);
                     this.recoveredMethod1(var2, var4, var6, false);
                     break;
                  case 2:
                     double var18 = -0.1 + recoveredField1.nextDouble() * 0.2;
                     this.recoveredMethod2(var2, var4 + 0.002, var6, var8 + 1.0, var10 + var18, false);
                     this.recoveredMethod2(var2, var4 - 0.001, var6, var8 - 1.0, var10 - var18, false);
                     this.recoveredMethod2(var2, var4, var6, var8, var10, false);
                     break;
                  case 3:
                     for (int var17 = 0; var17 < 3; var17++) {
                        double var13 = (0.001 + recoveredField1.nextDouble() * 0.002) * (var17 + 1);
                        this.recoveredMethod1(var2, var4 + var13, var6, false);
                     }

                     this.recoveredMethod1(var2, var4, var6, false);
                     break;
                  case 4:
                     double var16 = 7.0E-4;

                     for (int var21 = 0; var21 < 6; var21++) {
                        this.recoveredMethod1(var2, var4 + var16 * var21, var6, false);
                     }

                     this.recoveredMethod1(var2, var4, var6, false);
                     break;
                  case 5:
                     this.recoveredMethod1(var2, var4 + 0.001, var6, true);
                     this.recoveredMethod1(var2, var4 + 0.0015, var6, false);
                     this.recoveredMethod1(var2, var4, var6, true);
                     this.recoveredMethod1(var2, var4, var6, false);
                     break;
                  case 6:
                     double var12 = (recoveredField1.nextDouble() - 0.5) * 1.0E-4;
                     double var14 = (recoveredField1.nextDouble() - 0.5) * 1.0E-4;
                     this.recoveredMethod1(var2 + var12, var4 + 0.0015, var6 + var14, false);
                     this.recoveredMethod1(var2 - var12, var4 + 5.0E-4, var6 - var14, false);
                     this.recoveredMethod1(var2, var4, var6, false);
               }

               this.recoveredField3++;
               if (this.recoveredField3 % 10 == 0) {
                  this.recoveredField2 = recoveredField1.nextInt(7);
               }
            }
         }
      }
   }

   private void recoveredMethod1(double x, double y, double z, boolean onGround) {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new PositionAndOnGround(x, y, z, onGround, false));
      }
   }

   private void recoveredMethod2(double x, double y, double z, double yaw, double pitch, boolean onGround) {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new Full(x, y, z, (float)yaw, (float)pitch, onGround, false));
      }
   }
}
