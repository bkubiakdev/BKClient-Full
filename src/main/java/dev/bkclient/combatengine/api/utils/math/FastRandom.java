package dev.bkclient.combatengine.api.utils.math;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class FastRandom extends Random {
   private final ThreadLocalRandom recoveredField1 = ThreadLocalRandom.current();
   private Random recoveredField2 = null;
   private volatile boolean recoveredField3;
   private volatile boolean recoveredField4;
   private volatile long recoveredField5;

   private void recoveredMethod1() {
      if (this.recoveredField2 == null) {
         this.recoveredField2 = new Random(this.recoveredField5);
         this.recoveredField4 = false;
      } else if (this.recoveredField4) {
         this.recoveredField2.setSeed(this.recoveredField5);
         this.recoveredField4 = false;
      }
   }

   public static long mix(long left, long right) {
      left *= left * 6364136223846793005L + 1442695040888963407L;
      return left + right;
   }

   @Override
   public void setSeed(long seed) {
      this.recoveredField5 = seed;
      this.recoveredField3 = true;
      this.recoveredField4 = true;
   }

   @Override
   public void nextBytes(byte[] bytes) {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         this.recoveredField2.nextBytes(bytes);
      } else {
         this.recoveredField1.nextBytes(bytes);
      }
   }

   @Override
   public int nextInt() {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         return this.recoveredField2.nextInt();
      } else {
         return this.recoveredField1.nextInt();
      }
   }

   @Override
   public int nextInt(int bound) {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         return this.recoveredField2.nextInt(bound);
      } else {
         return this.recoveredField1.nextInt(bound);
      }
   }

   @Override
   public long nextLong() {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         return this.recoveredField2.nextLong();
      } else {
         return this.recoveredField1.nextLong();
      }
   }

   @Override
   public boolean nextBoolean() {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         return this.recoveredField2.nextBoolean();
      } else {
         return this.recoveredField1.nextBoolean();
      }
   }

   @Override
   public float nextFloat() {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         return this.recoveredField2.nextFloat();
      } else {
         return this.recoveredField1.nextFloat();
      }
   }

   @Override
   public double nextDouble() {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         return this.recoveredField2.nextDouble();
      } else {
         return this.recoveredField1.nextDouble();
      }
   }

   @Override
   public double nextGaussian() {
      if (this.recoveredField3) {
         this.recoveredMethod1();
         return this.recoveredField2.nextGaussian();
      } else {
         return this.recoveredField1.nextGaussian();
      }
   }
}
