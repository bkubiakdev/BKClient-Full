package dev.bkclient.combatengine.api.utils.tps;

import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventPacket;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

public class TPSCalc {
   private float recoveredField1 = 20.0F;
   private float recoveredField2 = 0.0F;
   private long recoveredField3;
   private long recoveredField4;
   private static final int recoveredField5 = 20;
   private final float[] recoveredField6 = new float[20];
   private int recoveredField7 = 0;

   @EventLink
   public void onPacket(EventPacket e) {
      if (e.getType() == EventPacket.Type.RECEIVE && e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         this.recoveredMethod1();
      }
   }

   public float getTPS() {
      if (this.recoveredField4 == 0L) {
         return this.recoveredField1;
      }

      MinecraftClient var1 = MinecraftClient.getInstance();
      return var1 != null && var1.getNetworkHandler() != null && System.currentTimeMillis() - this.recoveredField4 <= 3500L ? this.recoveredField1 : 20.0F;
   }

   private void recoveredMethod1() {
      long var1 = System.nanoTime();
      this.recoveredField4 = System.currentTimeMillis();
      if (this.recoveredField3 == 0L) {
         this.recoveredField3 = var1;
      } else {
         long var3 = var1 - this.recoveredField3;
         this.recoveredField3 = var1;
         if (var3 > 0L) {
            float var5 = 20.0F;
            float var6 = var5 * (1.0E9F / (float)var3);
            float var7 = MathHelper.clamp(var6, 0.0F, var5);
            this.recoveredField6[this.recoveredField7 % 20] = var7;
            this.recoveredField7++;
            int var8 = Math.min(this.recoveredField7, 20);
            float var9 = 0.0F;

            for (int var10 = 0; var10 < var8; var10++) {
               float var11 = this.recoveredField6[var10];
               var9 += var11;
            }

            this.recoveredField1 = (float)this.round(var9 / var8);
            this.recoveredField2 = this.recoveredField1 - var5;
         }
      }
   }

   public double round(double input) {
      return Math.round(input * 10.0) / 10.0;
   }

   @Generated
   public float getAdjustTicks() {
      return this.recoveredField2;
   }

   @Generated
   public long getTimestamp() {
      return this.recoveredField3;
   }

   @Generated
   public long getLastPacketTime() {
      return this.recoveredField4;
   }

   @Generated
   public float[] getTpsSamples() {
      return this.recoveredField6;
   }

   @Generated
   public int getSampleIndex() {
      return this.recoveredField7;
   }
}
