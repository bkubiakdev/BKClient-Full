package dev.bkclient.combatengine.client.modules.impl.combat.components.neuro;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NeuroWeights {
   private static final String recoveredField1 = "/assets/aura/neuro/rotation.wnn";
   private static final byte[] recoveredField2 = new byte[]{87, 78, 78, 49};
   private static final int recoveredField3 = 2;
   public static final int BASE_OBS = 20;
   public static final int STYLE_OBS = 28;
   public static final int BASE_OUTPUT = 5;
   public static final int STYLE_OUTPUT = 6;
   public static final int FLAG_ZERO_LATENT = 1;
   public final int version;
   public final int flags;
   public final int obs;
   public final int latent;
   public final int hidden;
   public final int head;
   public final int output;
   public final int input;
   public final float yawScale;
   public final float yawMax;
   public final float pitchScale;
   public final float pitchMax;
   public final float sigmaMin;
   public final float sigmaMax;
   public final float gcd;
   public final float[] weightIh;
   public final float[] weightHh;
   public final float[] biasIh;
   public final float[] biasHh;
   public final float[] headWeight;
   public final float[] headBias;
   public final float[] outWeight;
   public final float[] outBias;
   private static volatile NeuroWeights recoveredField4;
   private static volatile boolean recoveredField5;

   private NeuroWeights(int version, ByteBuffer buffer) {
      this.version = version;
      this.flags = version >= 2 ? buffer.getInt() : 0;
      this.obs = buffer.getInt();
      this.latent = buffer.getInt();
      this.hidden = buffer.getInt();
      this.head = buffer.getInt();
      this.output = buffer.getInt();
      this.input = this.obs + this.latent;
      this.yawScale = buffer.getFloat();
      this.yawMax = buffer.getFloat();
      this.pitchScale = buffer.getFloat();
      this.pitchMax = buffer.getFloat();
      this.sigmaMin = buffer.getFloat();
      this.sigmaMax = buffer.getFloat();
      this.gcd = buffer.getFloat();
      this.weightIh = recoveredMethod1(buffer, 3 * this.hidden * this.input);
      this.weightHh = recoveredMethod1(buffer, 3 * this.hidden * this.hidden);
      this.biasIh = recoveredMethod1(buffer, 3 * this.hidden);
      this.biasHh = recoveredMethod1(buffer, 3 * this.hidden);
      this.headWeight = recoveredMethod1(buffer, this.head * this.hidden);
      this.headBias = recoveredMethod1(buffer, this.head);
      this.outWeight = recoveredMethod1(buffer, this.output * this.head);
      this.outBias = recoveredMethod1(buffer, this.output);
   }

   private static float[] recoveredMethod1(ByteBuffer buffer, int count) {
      float[] var2 = new float[count];

      for (int var3 = 0; var3 < count; var3++) {
         var2[var3] = buffer.getFloat();
      }

      return var2;
   }

   public boolean zeroLatent() {
      return (this.flags & 1) != 0;
   }

   public boolean hasAttackHead() {
      return this.output >= 6;
   }

   public static NeuroWeights get() {
      NeuroWeights var0 = recoveredField4;
      if (var0 == null && !recoveredField5) {
         synchronized (NeuroWeights.class) {
            if (recoveredField5) {
               return recoveredField4;
            }

            recoveredField5 = true;

            try (InputStream var2 = NeuroWeights.class.getResourceAsStream("/assets/aura/neuro/rotation.wnn")) {
               recoveredField4 = var2 == null ? null : recoveredMethod2(var2.readAllBytes());
            } catch (IOException | RuntimeException var8) {
               recoveredField4 = null;
            }

            return recoveredField4;
         }
      } else {
         return var0;
      }
   }

   public static NeuroWeights load(Path path) {
      try {
         return recoveredMethod2(Files.readAllBytes(path));
      } catch (IOException | RuntimeException var2) {
         return null;
      }
   }

   private static NeuroWeights recoveredMethod2(byte[] bytes) {
      if (bytes != null && bytes.length >= 48) {
         for (int var1 = 0; var1 < recoveredField2.length; var1++) {
            if (bytes[var1] != recoveredField2[var1]) {
               return null;
            }
         }

         ByteBuffer var4 = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
         var4.position(recoveredField2.length);
         int var2 = var4.getInt();
         if (var2 >= 1 && var2 <= 2) {
            NeuroWeights var3 = new NeuroWeights(var2, var4);
            if (var3.obs != 20 && var3.obs != 28) {
               return null;
            } else if (var3.output < 5 || var3.output > 6) {
               return null;
            } else {
               return var3.hidden > 0 && var3.head > 0 && var3.latent >= 0 ? var3 : null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
