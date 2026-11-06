package dev.bkclient.combatengine.client.modules.impl.combat.components.rotations;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.utils.animation.Easings;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.RotationsSystem;
import dev.bkclient.combatengine.client.modules.impl.combat.components.gcd.GCDUtil;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FuntimeRotation extends RotationsSystem implements QClient {
   private static final long recoveredField1 = 500000L;
   private static final long recoveredField2 = 200000000L;
   private static final long recoveredField3 = 120000000L;
   private static final long recoveredField4 = 30000000L;
   private static final double recoveredField5 = 0.05;
   private static final int recoveredField6 = 1;
   private static final int recoveredField7 = 12;
   private static final int recoveredField8 = 20150;
   private static final float recoveredField9 = 0.8F;
   private static final float recoveredField10 = 1.2F;
   private static final float recoveredField11 = 1.5F;
   private static final float recoveredField12 = 60.0F;
   private static final float recoveredField13 = 24.0F;
   private static final float recoveredField14 = 0.55F;
   private static final float recoveredField15 = 1.25F;
   private final Random recoveredField16 = new Random();
   private final double[] recoveredField17 = new double[12];
   private final double[] recoveredField18 = new double[12];
   private final double[] recoveredField19 = new double[12];
   private final int[] recoveredField20 = new int[12];
   private LivingEntity recoveredField21;
   private FuntimeRotation.AimPhase recoveredField22 = FuntimeRotation.AimPhase.REACTION;
   private Vec3d recoveredField23 = Vec3d.ZERO;
   private float recoveredField24;
   private float recoveredField25;
   private float recoveredField26;
   private float recoveredField27;
   private float recoveredField28;
   private float recoveredField29;
   private float recoveredField30;
   private float recoveredField31;
   private float recoveredField32;
   private float recoveredField33;
   private boolean recoveredField34;
   private double recoveredField35 = 0.5;
   private double recoveredField36 = 0.62;
   private double recoveredField37 = 0.5;
   private double recoveredField38 = 0.5;
   private double recoveredField39 = 0.62;
   private double recoveredField40 = 0.5;
   private double recoveredField41 = 0.5;
   private double recoveredField42 = 0.62;
   private double recoveredField43 = 0.5;
   private double recoveredField44 = 0.5;
   private double recoveredField45 = 0.62;
   private double recoveredField46 = 0.5;
   private double recoveredField47 = 0.5;
   private double recoveredField48 = 0.62;
   private double recoveredField49 = 0.5;
   private double recoveredField50 = 0.5;
   private double recoveredField51 = 0.62;
   private double recoveredField52 = 0.5;
   private double recoveredField53 = 0.5;
   private double recoveredField54 = 0.62;
   private double recoveredField55 = 0.5;
   private double recoveredField56;
   private double recoveredField57;
   private double recoveredField58;
   private double recoveredField59;
   private double recoveredField60 = 2.0;
   private double recoveredField61 = 2.0;
   private double recoveredField62 = 2.8;
   private double recoveredField63 = 0.75;
   private double recoveredField64 = 0.58;
   private double recoveredField65;
   private double recoveredField66;
   private double recoveredField67;
   private double recoveredField68;
   private double recoveredField69;
   private double recoveredField70;
   private float recoveredField71;
   private float recoveredField72;
   private float recoveredField73;
   private float recoveredField74;
   private float recoveredField75;
   private float recoveredField76;
   private float recoveredField77;
   private float recoveredField78;
   private float recoveredField79;
   private float recoveredField80;
   private float recoveredField81;
   private float recoveredField82 = 0.06F;
   private float recoveredField83 = 0.1F;
   private float recoveredField84 = 1.0F;
   private float recoveredField85 = 1.0F;
   private float recoveredField86 = 1.0F;
   private float recoveredField87 = 1.0F;
   private float recoveredField88 = 1.0F;
   private float recoveredField89 = 1.0F;
   private float recoveredField90;
   private float recoveredField91;
   private float recoveredField92;
   private float recoveredField93;
   private float recoveredField94 = 1.0F;
   private float recoveredField95 = 1.0F;
   private float recoveredField96 = 0.28F;
   private float recoveredField97 = 2.4F;
   private float recoveredField98;
   private float recoveredField99;
   private float recoveredField100;
   private float recoveredField101;
   private float recoveredField102;
   private float recoveredField103;
   private float recoveredField104;
   private float recoveredField105;
   private long recoveredField106;
   private long recoveredField107;
   private long recoveredField108;
   private long recoveredField109;
   private long recoveredField110;
   private long recoveredField111;
   private long recoveredField112;
   private long recoveredField113;
   private long recoveredField114;
   private long recoveredField115;
   private long recoveredField116;
   private long recoveredField117;
   private long recoveredField118;
   private long recoveredField119;
   private long recoveredField120;
   private long recoveredField121;
   private long recoveredField122;
   private long recoveredField123;
   private long recoveredField124;
   private long recoveredField125;
   private long recoveredField126;
   private long recoveredField127;
   private long recoveredField128;
   private int recoveredField129 = 20150;
   private int recoveredField130 = 20150;
   private boolean recoveredField131;
   private boolean recoveredField132;
   private boolean recoveredField133;
   private int recoveredField134;
   private int recoveredField135;
   private int recoveredField136 = -1;
   private int recoveredField137 = -1;
   private int recoveredField138;
   private FuntimeRotation.PathModel recoveredField139 = FuntimeRotation.PathModel.MINIMUM_JERK;
   private FuntimeRotation.PathModel recoveredField140 = FuntimeRotation.PathModel.MINIMUM_JERK;
   private FuntimeRotation.PathEase recoveredField141 = FuntimeRotation.PathEase.MINIMUM_JERK;
   private FuntimeRotation.PathEase recoveredField142 = FuntimeRotation.PathEase.MINIMUM_JERK;
   private int recoveredField143;
   private boolean recoveredField144;
   private boolean recoveredField145;
   private boolean recoveredField146;
   private boolean recoveredField147;
   private float recoveredField148;
   private float recoveredField149;
   private float recoveredField150;
   private float recoveredField151;
   private float recoveredField152;
   private float recoveredField153;
   private float recoveredField154;
   private long recoveredField155;
   private long recoveredField156;
   private float recoveredField157;
   private float recoveredField158 = 1.0F;
   private boolean recoveredField159;

   public void reset() {
      this.recoveredField21 = null;
      this.recoveredField22 = FuntimeRotation.AimPhase.REACTION;
      this.recoveredField23 = Vec3d.ZERO;
      this.recoveredField26 = 0.0F;
      this.recoveredField27 = 0.0F;
      this.recoveredField28 = 0.0F;
      this.recoveredField29 = 0.0F;
      this.recoveredField30 = 0.0F;
      this.recoveredField31 = 0.0F;
      this.recoveredField32 = 0.0F;
      this.recoveredField33 = 0.0F;
      this.recoveredField34 = false;
      this.recoveredField35 = this.recoveredField38 = 0.5;
      this.recoveredField36 = this.recoveredField39 = 0.62;
      this.recoveredField37 = this.recoveredField40 = 0.5;
      this.recoveredField41 = this.recoveredField44 = this.recoveredField47 = 0.5;
      this.recoveredField42 = this.recoveredField45 = this.recoveredField48 = 0.62;
      this.recoveredField43 = this.recoveredField46 = this.recoveredField49 = 0.5;
      this.recoveredField50 = this.recoveredField53 = 0.5;
      this.recoveredField51 = this.recoveredField54 = 0.62;
      this.recoveredField52 = this.recoveredField55 = 0.5;
      this.recoveredField56 = this.recoveredField57 = this.recoveredField58 = 0.0;
      this.recoveredField59 = 0.0;
      this.recoveredField60 = this.recoveredField61 = 2.0;
      this.recoveredField62 = 2.8;
      this.recoveredField63 = 0.75;
      this.recoveredField64 = 0.58;
      this.recoveredField65 = this.recoveredField66 = this.recoveredField67 = 0.0;
      this.recoveredField68 = this.recoveredField69 = this.recoveredField70 = 0.0;
      this.recoveredField71 = this.recoveredField72 = 0.0F;
      this.recoveredField73 = this.recoveredField74 = 0.0F;
      this.recoveredField75 = this.recoveredField76 = 0.0F;
      this.recoveredField77 = this.recoveredField78 = 0.0F;
      this.recoveredField79 = this.recoveredField80 = 0.0F;
      this.recoveredField81 = 0.0F;
      this.recoveredField82 = 0.06F;
      this.recoveredField83 = 0.1F;
      this.recoveredField84 = this.recoveredField85 = 1.0F;
      this.recoveredField86 = this.recoveredField87 = 1.0F;
      this.recoveredField88 = this.recoveredField89 = 1.0F;
      this.recoveredField90 = this.recoveredField91 = 0.0F;
      this.recoveredField92 = this.recoveredField93 = 0.0F;
      this.recoveredField94 = this.recoveredField95 = 1.0F;
      this.recoveredField96 = 0.28F;
      this.recoveredField97 = 2.4F;
      this.recoveredField106 = 0L;
      this.recoveredField107 = 0L;
      this.recoveredField108 = 0L;
      this.recoveredField109 = 0L;
      this.recoveredField110 = 0L;
      this.recoveredField111 = 0L;
      this.recoveredField112 = 0L;
      this.recoveredField113 = 0L;
      this.recoveredField114 = 0L;
      this.recoveredField115 = 0L;
      this.recoveredField116 = 0L;
      this.recoveredField117 = 0L;
      this.recoveredField118 = 0L;
      this.recoveredField119 = 0L;
      this.recoveredField120 = 0L;
      this.recoveredField121 = 0L;
      this.recoveredField122 = 0L;
      this.recoveredField123 = 0L;
      this.recoveredField124 = 0L;
      this.recoveredField125 = 0L;
      this.recoveredField126 = 0L;
      this.recoveredField127 = 0L;
      this.recoveredField128 = 0L;
      this.recoveredField129 = 20150;
      this.recoveredField130 = 20150;
      this.recoveredField131 = false;
      this.recoveredField132 = false;
      this.recoveredField133 = mc.player != null && !mc.player.isOnGround();
      this.recoveredMethod31();
      this.recoveredField138 = 0;
      this.recoveredField139 = FuntimeRotation.PathModel.MINIMUM_JERK;
      this.recoveredField140 = FuntimeRotation.PathModel.MINIMUM_JERK;
      this.recoveredField141 = FuntimeRotation.PathEase.MINIMUM_JERK;
      this.recoveredField142 = FuntimeRotation.PathEase.MINIMUM_JERK;
      this.recoveredField143 = 0;
      this.recoveredField144 = false;
      this.recoveredField145 = false;
      this.recoveredField146 = false;
      this.recoveredField147 = false;
      this.recoveredField148 = 0.0F;
      this.recoveredField149 = 0.0F;
      this.recoveredField150 = 0.0F;
      this.recoveredField151 = 0.0F;
      this.recoveredField152 = 0.0F;
      this.recoveredField153 = 0.0F;
      this.recoveredField154 = 0.0F;
      this.recoveredField155 = 0L;
      this.recoveredField156 = 0L;
      this.recoveredField157 = 0.0F;
      this.recoveredField158 = 1.0F;
      if (mc.player != null) {
         this.recoveredField24 = mc.player.getYaw();
         this.recoveredField25 = mc.player.getPitch();
      } else {
         this.recoveredField24 = 0.0F;
         this.recoveredField25 = 0.0F;
      }
   }

   public void setPrecisionLock(boolean precision) {
      long var2 = System.nanoTime();
      if (precision && !this.recoveredField131 && this.recoveredField21 != null) {
         this.recoveredMethod13(var2, true);
      }

      this.recoveredField131 = precision;
      if (precision) {
         this.recoveredField122 = Math.max(this.recoveredField122, var2 + 120000000L);
      }
   }

   public void setVisibleAim(boolean visible) {
      this.recoveredField159 = visible;
   }

   public void onAttack() {
      if (mc.player != null && this.recoveredField21 != null) {
         long var1 = System.nanoTime();
         this.recoveredField122 = Math.max(this.recoveredField122, var1 + 100000000L);
         this.recoveredField75 = this.recoveredMethod45(0.04F, 0.13F);
         double var3 = this.recoveredField16.nextDouble();
         this.recoveredField76 = var3 < 0.63 ? this.recoveredMethod46(0.12F, 0.42F) : (var3 < 0.83 ? -this.recoveredMethod46(0.08F, 0.28F) : 0.0F);
         boolean var5 = this.recoveredField139 == FuntimeRotation.PathModel.EMPIRICAL
            && this.recoveredField144
            && var1 >= this.recoveredField109
            && var1 < this.recoveredField110;
         if (!var5) {
            this.recoveredMethod13(var1, true);
         }

         this.recoveredField22 = FuntimeRotation.AimPhase.RECOVER;
      }
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (mc.player != null && target != null) {
         long var2 = System.nanoTime();
         RotationStorage var4 = RotationStorage.instance;
         if (var4 != null && var4.currentPriority() > 1) {
            this.recoveredMethod10(var2, true);
            this.recoveredField132 = true;
         } else {
            if (this.recoveredField21 != target) {
               this.recoveredMethod1(target, var2);
            }

            if (this.recoveredField132) {
               this.recoveredMethod10(var2, true);
               this.recoveredField132 = false;
            } else {
               long var5 = this.recoveredField106 == 0L ? 4166667L : var2 - this.recoveredField106;
               if (var5 >= 500000L) {
                  if (var5 > 200000000L) {
                     this.recoveredMethod10(var2, true);
                     this.recoveredField22 = FuntimeRotation.AimPhase.ACQUIRE;
                  } else {
                     this.recoveredField106 = var2;
                     double var7 = Math.min(var5 / 1.0E9, 0.05);
                     boolean var9 = this.recoveredField131 || var2 < this.recoveredField122;
                     boolean var10 = this.recoveredMethod21(var2, var9 || var2 < this.recoveredField107);
                     boolean var11 = !mc.player.isOnGround();
                     if (var11 && !this.recoveredField133 && mc.player.getVelocity().y > 0.035) {
                        this.recoveredMethod14(var2, var9);
                     }

                     if (var11 && this.recoveredField147 && mc.player.getVelocity().y < -0.07) {
                        this.recoveredField147 = false;
                        this.recoveredMethod13(var2, var9);
                     } else if (!var11) {
                        this.recoveredField147 = false;
                     }

                     this.recoveredField133 = var11;
                     this.recoveredMethod2(var2, var7, var9);
                     Vec3d var12 = this.recoveredMethod4(target, var7, var9);
                     Vec3d var13 = var12.subtract(mc.player.getEyePos());
                     float var14 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var13.z, var13.x)) - 90.0);
                     float var15 = (float)(-Math.toDegrees(Math.atan2(var13.y, var13.horizontalLength())));
                     float var16 = this.recoveredMethod42(target, var13.length());
                     float var17 = this.recoveredMethod43(target, var13.length());
                     float var18 = MathHelper.clamp(this.recoveredField148, -var16 * (var9 ? 0.272F : 0.85F), var16 * (var9 ? 0.272F : 0.85F));
                     float var19 = MathHelper.clamp(this.recoveredField149, -var17 * (var9 ? 0.224F : 0.72F), var17 * (var9 ? 0.224F : 0.72F));
                     float var20 = MathHelper.clamp(this.recoveredMethod15(var2), -var17 * (var9 ? 0.176F : 0.78F), var17 * (var9 ? 0.176F : 0.78F));
                     this.recoveredMethod22(var2, var9, var16, var17);
                     float var21 = this.recoveredMethod23(var2) * (var9 ? 0.14400001F : 1.0F);
                     float var22 = (var9 ? 0.176F : (this.recoveredField22 == FuntimeRotation.AimPhase.ACQUIRE ? 0.35F : 1.0F)) * this.recoveredField83;
                     float var23 = MathHelper.clamp(var16 * 0.14F, 0.035F, 0.28F) * var22;
                     float var24 = MathHelper.clamp(var16 * 0.08F, 0.025F, 0.18F) * var22;
                     this.recoveredField75 = this.recoveredField75 * (float)Math.exp(-var7 / 0.075);
                     this.recoveredField76 = this.recoveredField76 * (float)Math.exp(-var7 / 0.09);
                     float var25 = var9 ? 0.52F : 1.0F;
                     var14 = MathHelper.wrapDegrees(
                        var14
                           + var18
                           + this.recoveredField71 * var23
                           + this.recoveredField77 * this.recoveredField81 * var25
                           + this.recoveredField92 * var21
                           + this.recoveredField75
                     );
                     var15 = MathHelper.clamp(
                        var15
                           + var19
                           + var20
                           + this.recoveredField72 * var24
                           + this.recoveredField78 * this.recoveredField81 * var25
                           + this.recoveredField93 * var21
                           + this.recoveredField76,
                        -89.0F,
                        89.0F
                     );
                     this.recoveredMethod6(var14, var15, var7);
                     float var26 = MathHelper.wrapDegrees(var14 - this.recoveredField24);
                     float var27 = var15 - this.recoveredField25;
                     float var28 = (float)Math.hypot(var26, var27);
                     if (var2 < this.recoveredField107 && !var9) {
                        this.recoveredField22 = FuntimeRotation.AimPhase.REACTION;
                        this.recoveredField26 = this.recoveredMethod49(this.recoveredField26, 0.0F, this.recoveredField102 * (float)var7);
                        this.recoveredField27 = this.recoveredMethod49(this.recoveredField27, 0.0F, this.recoveredField103 * (float)var7);
                     } else {
                        if (!var9) {
                           this.recoveredField22 = var28 > 10.0F ? FuntimeRotation.AimPhase.ACQUIRE : FuntimeRotation.AimPhase.TRACK;
                        } else {
                           this.recoveredField22 = var2 >= this.recoveredField122
                                 || !(Math.abs(this.recoveredField75) > 0.01F) && !(Math.abs(this.recoveredField76) > 0.01F)
                              ? FuntimeRotation.AimPhase.FOCUS
                              : FuntimeRotation.AimPhase.RECOVER;
                        }

                        this.recoveredMethod5(var26, var27, var7, var9, mc.player.isGliding());
                     }

                     if (var10) {
                        this.recoveredField26 = 0.0F;
                        this.recoveredField27 = 0.0F;
                        this.recoveredField28 = 0.0F;
                        this.recoveredField29 = 0.0F;
                     }

                     float var29 = (float)Math.min(1.5, 1.0 + mc.player.getVelocity().horizontalLength() * 0.5);
                     if (!mc.player.isOnGround()) {
                        var29 *= MathHelper.clamp(1.0F + mc.player.fallDistance * 0.015F, 1.0F, 1.25F);
                     }

                     double var30 = this.recoveredField110 > this.recoveredField109
                        ? this.recoveredMethod50((double)(var2 - this.recoveredField109) / (this.recoveredField110 - this.recoveredField109), 0.0, 1.0)
                        : 0.5;
                     float var32 = (float)this.recoveredMethod40(0.55F, 1.25, this.recoveredField141.evaluate(var30, this)) * var29;
                     float var33 = var10 ? 0.0F : this.recoveredField26 * (float)var7 * var32;
                     float var34 = var10 ? 0.0F : this.recoveredField27 * (float)var7 * var32;
                     if (var33 * var26 > 0.0F && Math.abs(var33) > Math.abs(var26)) {
                        if (Math.abs(this.recoveredField30) > 8.0F) {
                           var33 = var26 + MathHelper.clamp(this.recoveredField30 * (float)var7 * 0.58F, -0.45F, 0.45F);
                           this.recoveredField26 = this.recoveredField30 * 0.58F;
                           this.recoveredField28 = 0.0F;
                        } else {
                           var33 = var26;
                           this.recoveredField26 = 0.0F;
                           this.recoveredField28 = 0.0F;
                        }
                     }

                     if (var34 * var27 > 0.0F && Math.abs(var34) > Math.abs(var27)) {
                        if (Math.abs(this.recoveredField31) > 6.0F) {
                           var34 = var27 + MathHelper.clamp(this.recoveredField31 * (float)var7 * 0.52F, -0.32F, 0.32F);
                           this.recoveredField27 = this.recoveredField31 * 0.52F;
                           this.recoveredField29 = 0.0F;
                        } else {
                           var34 = var27;
                           this.recoveredField27 = 0.0F;
                           this.recoveredField29 = 0.0F;
                        }
                     }

                     var33 = Math.min(Math.abs(var33), 60.0F) * Math.signum(var33);
                     var34 = Math.min(Math.abs(var34), 24.0F) * Math.signum(var34);
                     this.recoveredField24 = MathHelper.wrapDegrees(this.recoveredField24 + var33);
                     this.recoveredField25 = MathHelper.clamp(this.recoveredField25 + var34, -89.0F, 89.0F);
                     if (Float.isFinite(this.recoveredField24) && Float.isFinite(this.recoveredField25)) {
                        this.recoveredMethod9(var2);
                     } else {
                        this.recoveredMethod10(var2, true);
                     }
                  }
               }
            }
         }
      }
   }

   private void recoveredMethod1(LivingEntity target, long now) {
      this.recoveredField21 = target;
      this.recoveredMethod10(now, true);
      this.recoveredField126 = now;
      this.recoveredField127 = 0L;
      this.recoveredField128 = 0L;
      this.recoveredField129 = 20150;
      this.recoveredField130 = 20150;
      this.recoveredField23 = target.getVelocity();
      this.recoveredField35 = this.recoveredField38 = this.recoveredMethod47(0.4, 0.6);
      this.recoveredField36 = this.recoveredField39 = this.recoveredMethod47(0.64, 0.82);
      this.recoveredField37 = this.recoveredField40 = this.recoveredMethod47(0.4, 0.6);
      this.recoveredField41 = this.recoveredField44 = this.recoveredField47 = this.recoveredField35;
      this.recoveredField42 = this.recoveredField45 = this.recoveredField48 = this.recoveredField36;
      this.recoveredField43 = this.recoveredField46 = this.recoveredField49 = this.recoveredField37;
      this.recoveredField50 = this.recoveredField53 = this.recoveredField35;
      this.recoveredField51 = this.recoveredField54 = this.recoveredField36;
      this.recoveredField52 = this.recoveredField55 = this.recoveredField37;
      this.recoveredMethod31();
      this.recoveredField71 = this.recoveredField72 = 0.0F;
      this.recoveredField73 = this.recoveredField74 = 0.0F;
      this.recoveredField75 = this.recoveredField76 = 0.0F;
      this.recoveredField77 = this.recoveredField78 = 0.0F;
      this.recoveredField79 = this.recoveredField80 = 0.0F;
      this.recoveredField81 = 0.0F;
      this.recoveredField82 = this.recoveredMethod46(0.03F, 0.09F);
      this.recoveredField83 = 0.1F;
      this.recoveredField84 = this.recoveredField85 = this.recoveredMethod46(0.86F, 1.16F);
      this.recoveredField86 = this.recoveredField87 = 1.0F;
      this.recoveredField88 = this.recoveredField89 = this.recoveredMethod46(0.72F, 1.12F);
      this.recoveredField90 = this.recoveredField91 = this.recoveredMethod46(-0.12F, 0.08F);
      this.recoveredField92 = this.recoveredField93 = 0.0F;
      this.recoveredField94 = this.recoveredField95 = 1.0F;
      this.recoveredField96 = 0.28F;
      this.recoveredField97 = 2.4F;
      this.recoveredField68 = this.recoveredField69 = this.recoveredField70 = 0.0;
      this.recoveredField111 = now;
      this.recoveredField98 = this.recoveredMethod46(610.0F, 830.0F);
      this.recoveredField99 = this.recoveredMethod46(280.0F, 410.0F);
      this.recoveredField100 = this.recoveredMethod46(2800.0F, 5200.0F);
      this.recoveredField101 = this.recoveredMethod46(1200.0F, 2400.0F);
      this.recoveredField102 = this.recoveredMethod46(3600.0F, 6500.0F);
      this.recoveredField103 = this.recoveredMethod46(1800.0F, 3400.0F);
      this.recoveredField104 = this.recoveredMethod46(36000.0F, 72000.0F);
      this.recoveredField105 = this.recoveredMethod46(16000.0F, 36000.0F);
      float var4 = this.recoveredMethod11(target);
      long var5;
      if (!this.recoveredField131 && !(var4 < 6.0F)) {
         var5 = Math.round(MathHelper.clamp(18.0F + var4 * 0.43F + (float)this.recoveredField16.nextGaussian() * 7.0F, 16.0F, 105.0F));
      } else {
         var5 = Math.round(this.recoveredMethod46(0.0F, 12.0F));
      }

      this.recoveredField107 = now + var5 * 1000000L;
      this.recoveredMethod12(now, this.recoveredField131, false);
      this.recoveredField112 = now + this.recoveredMethod48(260L, 720L);
      this.recoveredField113 = now + this.recoveredMethod48(480L, 1200L);
      this.recoveredField115 = now + this.recoveredMethod48(150L, 480L);
      this.recoveredField116 = now + this.recoveredMethod48(320L, 920L);
      this.recoveredField117 = now + this.recoveredMethod48(650L, 1900L);
      this.recoveredField120 = now + this.recoveredMethod48(650L, 1900L);
      this.recoveredField124 = now + this.recoveredMethod48(850L, 2600L);
      this.recoveredField125 = 0L;
      this.recoveredField133 = !mc.player.isOnGround();
      this.recoveredField22 = var5 > 0L ? FuntimeRotation.AimPhase.REACTION : FuntimeRotation.AimPhase.ACQUIRE;
   }

   private void recoveredMethod2(long now, double deltaTime, boolean precision) {
      this.recoveredMethod3(deltaTime);
      this.recoveredMethod18(now);
      if (now >= this.recoveredField108) {
         this.recoveredMethod12(now, precision, false);
         this.recoveredMethod18(now);
      }

      if (now >= this.recoveredField112) {
         float var6 = this.recoveredMethod44();
         this.recoveredField73 = MathHelper.clamp(this.recoveredMethod44() * 0.78F + var6 * 0.22F, -1.0F, 1.0F);
         this.recoveredField74 = MathHelper.clamp(this.recoveredMethod44() * 0.82F + var6 * 0.18F, -1.0F, 1.0F);
         this.recoveredField112 = now + this.recoveredMethod48(260L, 760L);
      }

      double var18 = 1.0 - Math.exp(-deltaTime / 0.38);
      double var8 = 1.0 - Math.exp(-deltaTime / 0.48);
      this.recoveredField71 = this.recoveredField71 + (this.recoveredField73 - this.recoveredField71) * (float)var18;
      this.recoveredField72 = this.recoveredField72 + (this.recoveredField74 - this.recoveredField72) * (float)var8;
      if (now >= this.recoveredField115) {
         this.recoveredField85 = this.recoveredMethod46(0.68F, 1.36F);
         this.recoveredField115 = now + this.recoveredMethod48(150L, 520L);
      }

      double var10 = 1.0 - Math.exp(-deltaTime / 0.13);
      this.recoveredField84 = this.recoveredField84 + (this.recoveredField85 - this.recoveredField84) * (float)var10;
      double var12 = 1.0 - Math.exp(-deltaTime / 0.09);
      this.recoveredField86 = this.recoveredField86 + (this.recoveredField87 - this.recoveredField86) * (float)var12;
      if (now >= this.recoveredField116) {
         this.recoveredField89 = this.recoveredMethod46(0.48F, 1.24F);
         this.recoveredField91 = this.recoveredMethod46(-0.22F, 0.13F);
         this.recoveredField116 = now + this.recoveredMethod48(340L, 1150L);
      }

      double var14 = 1.0 - Math.exp(-deltaTime / 0.22);
      this.recoveredField88 = this.recoveredField88 + (this.recoveredField89 - this.recoveredField88) * (float)var14;
      this.recoveredField90 = this.recoveredField90 + (this.recoveredField91 - this.recoveredField90) * (float)var14;
      if (precision) {
         this.recoveredField95 = 1.0F;
         if (this.recoveredField121 > 0L) {
            this.recoveredField121 = 0L;
            this.recoveredField120 = now + this.recoveredMethod48(700L, 2400L);
         }
      } else if (this.recoveredField121 > 0L && now >= this.recoveredField121) {
         this.recoveredField95 = 1.0F;
         this.recoveredField121 = 0L;
         this.recoveredField120 = now + this.recoveredMethod48(700L, 2400L);
      } else if (this.recoveredField121 == 0L && now >= this.recoveredField120) {
         this.recoveredField95 = this.recoveredMethod46(0.62F, 0.9F);
         this.recoveredField121 = now + this.recoveredMethod48(20L, 85L);
      }

      double var16 = 1.0 - Math.exp(-deltaTime / (this.recoveredField95 < this.recoveredField94 ? 0.022 : 0.045));
      this.recoveredField94 = this.recoveredField94 + (this.recoveredField95 - this.recoveredField94) * (float)var16;
      this.recoveredMethod19(now, deltaTime);
   }

   private void recoveredMethod3(double deltaTime) {
      float var3;
      if (mc.player.isOnGround()) {
         var3 = 1.0F;
      } else {
         double var4 = mc.player.getVelocity().y;
         if (var4 > 0.04) {
            var3 = 0.82F;
         } else if (var4 > -0.1) {
            var3 = 0.74F;
         } else {
            var3 = MathHelper.clamp(1.0F + (float)Math.abs(var4) * 0.34F, 1.04F, 1.18F);
         }
      }

      float var6 = (float)(1.0 - Math.exp(-deltaTime / 0.065));
      this.recoveredField158 = this.recoveredField158 + (var3 - this.recoveredField158) * var6;
   }

   private Vec3d recoveredMethod4(LivingEntity target, double deltaTime, boolean precision) {
      Vec3d var5 = target.getVelocity();
      if (var5.lengthSquared() > 2.25) {
         var5 = var5.normalize().multiply(1.5);
      }

      double var6 = 1.0 - Math.exp(-deltaTime / 0.12);
      this.recoveredField23 = this.recoveredField23.lerp(var5, var6);
      Box var8 = target.getBoundingBox();
      Vec3d var9 = new Vec3d(
         var8.minX + var8.getLengthX() * this.recoveredField35,
         var8.minY + var8.getLengthY() * this.recoveredField36,
         var8.minZ + var8.getLengthZ() * this.recoveredField37
      );
      double var10 = mc.player.getEyePos().distanceTo(var9);
      double var12;
      if (precision) {
         var12 = 0.05;
      } else if (!mc.player.isGliding() && !target.isGliding()) {
         var12 = this.recoveredMethod50((0.52 + var10 * 0.09) * this.recoveredField88 + this.recoveredField90, -0.2, 1.35);
      } else {
         var12 = this.recoveredMethod50((0.95 + var10 * 0.08) * this.recoveredField88 + this.recoveredField90, 0.15, 1.85);
      }

      Vec3d var14 = var9.add(this.recoveredField23.multiply(var12));
      if (!precision) {
         return var14;
      }

      double var15 = Math.min(var8.getLengthX(), var8.getLengthZ()) * 0.12;
      double var17 = var8.getLengthY() * 0.1;
      return new Vec3d(
         MathHelper.clamp(var14.x, var8.minX + var15, var8.maxX - var15),
         MathHelper.clamp(var14.y, var8.minY + var17, var8.maxY - var17),
         MathHelper.clamp(var14.z, var8.minZ + var15, var8.maxZ - var15)
      );
   }

   private void recoveredMethod5(float yawError, float pitchError, double deltaTime, boolean precision, boolean flying) {
      float var7 = this.recoveredField84 * this.recoveredField86 * this.recoveredField94 * this.recoveredField158 * (flying ? 1.12F : 1.0F);
      if (precision) {
         var7 *= 1.272F;
      }

      float var8 = Math.abs(yawError);
      float var9 = Math.abs(pitchError);
      float var10 = flying ? 1100.0F : (var8 < 18.0F ? 520.0F : (var8 < 65.0F ? 720.0F : 980.0F));
      float var11 = flying ? 520.0F : (var9 < 8.0F ? 230.0F : (var9 < 24.0F ? 330.0F : 430.0F));
      float var12 = Math.min(var10, this.recoveredField98 * var7);
      float var13 = Math.min(var11, this.recoveredField99 * var7);
      float var14 = (float)Math.exp(-Math.abs(yawError) / 18.0F);
      float var15 = (float)Math.exp(-Math.abs(pitchError) / 13.0F);
      float var16 = this.recoveredMethod8(yawError, this.recoveredField102, var12) + this.recoveredField30 * (precision ? 0.86F : 0.7F) * var14;
      float var17 = this.recoveredMethod8(pitchError, this.recoveredField103, var13) + this.recoveredField31 * (precision ? 0.82F : 0.66F) * var15;
      var16 = MathHelper.clamp(var16, -var12, var12);
      var17 = MathHelper.clamp(var17, -var13, var13);
      float var18 = 1.0F + MathHelper.clamp((var8 - 35.0F) / 90.0F, 0.0F, 0.45F);
      float var19 = 1.0F + MathHelper.clamp((var9 - 18.0F) / 42.0F, 0.0F, 0.3F);
      float var20 = (this.recoveredMethod7(this.recoveredField26, var16) ? this.recoveredField102 : this.recoveredField100) * var18;
      float var21 = (this.recoveredMethod7(this.recoveredField27, var17) ? this.recoveredField103 : this.recoveredField101) * var19;
      float var22 = precision ? 1.3440001F : 1.0F;
      float var23 = precision ? 1.32F : 1.0F;
      float var24 = MathHelper.clamp((var16 - this.recoveredField26) / (float)deltaTime, -var20 * var22, var20 * var22);
      float var25 = MathHelper.clamp((var17 - this.recoveredField27) / (float)deltaTime, -var21 * var23, var21 * var23);
      this.recoveredField28 = this.recoveredMethod49(this.recoveredField28, var24, this.recoveredField104 * var22 * (float)Math.sqrt(var18) * (float)deltaTime);
      this.recoveredField29 = this.recoveredMethod49(this.recoveredField29, var25, this.recoveredField105 * var23 * (float)Math.sqrt(var19) * (float)deltaTime);
      float var26 = this.recoveredField26 + this.recoveredField28 * (float)deltaTime;
      float var27 = this.recoveredField27 + this.recoveredField29 * (float)deltaTime;
      if ((var16 - this.recoveredField26) * (var16 - var26) <= 0.0F) {
         var26 = var16;
         this.recoveredField28 *= 0.22F;
      }

      if ((var17 - this.recoveredField27) * (var17 - var27) <= 0.0F) {
         var27 = var17;
         this.recoveredField29 *= 0.22F;
      }

      this.recoveredField26 = MathHelper.clamp(var26, -var12, var12);
      this.recoveredField27 = MathHelper.clamp(var27, -var13, var13);
   }

   private void recoveredMethod6(float wantedYaw, float wantedPitch, double deltaTime) {
      if (!this.recoveredField34) {
         this.recoveredField32 = wantedYaw;
         this.recoveredField33 = wantedPitch;
         this.recoveredField30 = 0.0F;
         this.recoveredField31 = 0.0F;
         this.recoveredField34 = true;
      } else {
         float var5 = MathHelper.wrapDegrees(wantedYaw - this.recoveredField32) / (float)deltaTime;
         float var6 = (wantedPitch - this.recoveredField33) / (float)deltaTime;
         var5 = MathHelper.clamp(var5, -1100.0F, 1100.0F);
         var6 = MathHelper.clamp(var6, -620.0F, 620.0F);
         float var7 = (float)(1.0 - Math.exp(-deltaTime / 0.055));
         this.recoveredField30 = this.recoveredField30 + (var5 - this.recoveredField30) * var7;
         this.recoveredField31 = this.recoveredField31 + (var6 - this.recoveredField31) * var7;
         this.recoveredField32 = wantedYaw;
         this.recoveredField33 = wantedPitch;
      }
   }

   private boolean recoveredMethod7(float currentVelocity, float desiredVelocity) {
      return Math.abs(currentVelocity) < 0.001F ? false : currentVelocity * desiredVelocity <= 0.0F || Math.abs(desiredVelocity) < Math.abs(currentVelocity);
   }

   private float recoveredMethod8(float error, float deceleration, float maximumSpeed) {
      if (Math.abs(error) < 0.001F) {
         return 0.0F;
      }

      float var4 = (float)Math.sqrt(2.0F * deceleration * Math.abs(error));
      return Math.copySign(Math.min(maximumSpeed, var4), error);
   }

   private void recoveredMethod9(long now) {
      long var3 = this.recoveredField128 == 0L ? Long.MAX_VALUE : now - this.recoveredField128;
      this.recoveredField128 = now;
      long var5 = this.recoveredField127 == 0L ? Long.MAX_VALUE : now - this.recoveredField127;
      if (var3 >= 12500000L || now >= this.recoveredField126) {
         if (this.recoveredField127 != 0L) {
            int var7 = (int)this.recoveredMethod50(var5 / 1000.0, 12500.0, 35000.0);
            this.recoveredField130 = this.recoveredField129;
            this.recoveredField129 = var7;
         }

         int var17 = FuntimeMotorProfile.mouseReportIntervalMicros(this.recoveredField16);
         double var8 = 20150.0
            - (this.recoveredField129 - 20150) * 0.19
            + (this.recoveredField130 - 20150) * 0.275
            + (var17 - 20150) * 0.95
            + this.recoveredMethod47(-260.0, 260.0);
         int var10 = (int)this.recoveredMethod50(var8, 11500.0, 35000.0);
         double var11 = var3 < 12500000L ? var3 / 2000.0 : 0.0;
         long var13 = (long)Math.max(8000.0, var10 - var11);
         this.recoveredField127 = now;
         this.recoveredField126 = now + var13 * 1000L;
         RotationStorage.update(new Rotation(this.recoveredField24, this.recoveredField25), 360.0F, 360.0F, 45.0F, 45.0F, 0, 1, this.recoveredField159);
         float var15 = Math.abs(MathHelper.wrapDegrees(this.recoveredField24 - mc.player.getYaw()));
         float var16 = Math.abs(this.recoveredField25 - mc.player.getPitch());
         if (var15 > 4.0F || var16 > 4.0F) {
            this.recoveredMethod10(now, true);
         }
      }
   }

   private void recoveredMethod10(long now, boolean clearVelocity) {
      if (mc.player != null) {
         this.recoveredField24 = MathHelper.wrapDegrees(mc.player.getYaw());
         this.recoveredField25 = MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F);
      }

      if (clearVelocity) {
         this.recoveredField26 = 0.0F;
         this.recoveredField27 = 0.0F;
         this.recoveredField28 = 0.0F;
         this.recoveredField29 = 0.0F;
         this.recoveredField30 = 0.0F;
         this.recoveredField31 = 0.0F;
         this.recoveredField34 = false;
         this.recoveredField148 = 0.0F;
         this.recoveredField149 = 0.0F;
         this.recoveredField150 = 0.0F;
         this.recoveredField151 = 0.0F;
      }

      this.recoveredField106 = now;
   }

   private float recoveredMethod11(LivingEntity target) {
      Vec3d var2 = target.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
      float var3 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
      float var4 = (float)(-Math.toDegrees(Math.atan2(var2.y, var2.horizontalLength())));
      return Math.abs(MathHelper.wrapDegrees(var3 - mc.player.getYaw())) + Math.abs(var4 - mc.player.getPitch());
   }

   private void recoveredMethod12(long now, boolean precision, boolean action) {
      boolean var5 = this.recoveredField146;
      this.recoveredField146 = false;
      this.recoveredField150 = this.recoveredField148;
      this.recoveredField151 = this.recoveredField149;
      this.recoveredField41 = this.recoveredField35;
      this.recoveredField42 = this.recoveredField36;
      this.recoveredField43 = this.recoveredField37;
      Vec3d var6 = this.recoveredMethod26(precision);
      this.recoveredField38 = var6.x;
      this.recoveredField39 = var6.y;
      this.recoveredField40 = var6.z;
      double var7 = this.recoveredMethod32(
         this.recoveredField41, this.recoveredField42, this.recoveredField43, this.recoveredField38, this.recoveredField39, this.recoveredField40
      );
      if (var5 && this.recoveredField16.nextDouble() < 0.96) {
         this.recoveredField139 = FuntimeRotation.PathModel.EMPIRICAL;
         if (this.recoveredField140 == FuntimeRotation.PathModel.EMPIRICAL) {
            this.recoveredField138++;
         } else {
            this.recoveredField140 = FuntimeRotation.PathModel.EMPIRICAL;
            this.recoveredField138 = 1;
         }
      } else {
         this.recoveredField139 = this.recoveredMethod24(action, var7);
      }

      if (this.recoveredField139 == FuntimeRotation.PathModel.EMPIRICAL) {
         this.recoveredField144 = action;
         this.recoveredField145 = var5;
         this.recoveredField143 = var5 ? FuntimeMotorProfile.chooseJump(this.recoveredField16) : FuntimeMotorProfile.choose(this.recoveredField16, action);
      } else {
         this.recoveredField144 = false;
         this.recoveredField145 = false;
      }

      this.recoveredField59 = this.recoveredMethod47(-0.28, 0.34);
      this.recoveredField141 = this.recoveredMethod25();
      this.recoveredField60 = this.recoveredMethod47(1.65, 2.9);
      this.recoveredField61 = this.recoveredMethod47(1.65, 2.9);
      this.recoveredField62 = this.recoveredMethod47(2.15, 3.75);
      this.recoveredField82 = this.recoveredMethod46(0.03F, 0.09F);
      double var9 = precision ? this.recoveredMethod47(0.025, 0.075) : this.recoveredMethod47(0.045, 0.14);
      this.recoveredField56 = this.recoveredMethod41(var9);
      this.recoveredField57 = this.recoveredMethod41(var9 * 0.78);
      this.recoveredField58 = this.recoveredMethod41(var9);
      if (this.recoveredField139 == FuntimeRotation.PathModel.EMPIRICAL) {
         this.recoveredField56 = 0.0;
         this.recoveredField57 = 0.0;
         this.recoveredField58 = 0.0;
         this.recoveredMethod16();
      } else {
         this.recoveredField152 = 0.0F;
         this.recoveredField153 = 0.0F;
         this.recoveredField154 = 0.0F;
      }

      this.recoveredField44 = this.recoveredMethod50(
         this.recoveredMethod40(this.recoveredField41, this.recoveredField38, 0.28) + this.recoveredMethod41(var9), 0.14, 0.86
      );
      this.recoveredField45 = this.recoveredMethod50(
         this.recoveredMethod40(this.recoveredField42, this.recoveredField39, 0.28) + this.recoveredMethod41(var9), 0.24, 0.9
      );
      this.recoveredField46 = this.recoveredMethod50(
         this.recoveredMethod40(this.recoveredField43, this.recoveredField40, 0.28) + this.recoveredMethod41(var9), 0.14, 0.86
      );
      double var11 = this.recoveredMethod47(0.36, 0.67);
      this.recoveredField50 = this.recoveredMethod50(
         this.recoveredMethod40(this.recoveredField41, this.recoveredField38, var11) + this.recoveredField56, 0.14, 0.86
      );
      this.recoveredField51 = this.recoveredMethod50(
         this.recoveredMethod40(this.recoveredField42, this.recoveredField39, var11) + this.recoveredField57, 0.24, 0.9
      );
      this.recoveredField52 = this.recoveredMethod50(
         this.recoveredMethod40(this.recoveredField43, this.recoveredField40, var11) + this.recoveredField58, 0.14, 0.86
      );
      this.recoveredField47 = this.recoveredMethod50(this.recoveredField50 + this.recoveredField50 - this.recoveredField44, 0.14, 0.86);
      this.recoveredField48 = this.recoveredMethod50(this.recoveredField51 + this.recoveredField51 - this.recoveredField45, 0.24, 0.9);
      this.recoveredField49 = this.recoveredMethod50(this.recoveredField52 + this.recoveredField52 - this.recoveredField46, 0.14, 0.86);
      double var13 = this.recoveredField139 == FuntimeRotation.PathModel.FLICK_SETTLE
         ? this.recoveredMethod47(0.025, 0.085)
         : this.recoveredMethod47(0.008, 0.045);
      this.recoveredField53 = this.recoveredMethod50(
         this.recoveredField38 + (this.recoveredField38 - this.recoveredField41) * var13 + this.recoveredMethod41(var9 * 0.32), 0.16, 0.84
      );
      this.recoveredField54 = this.recoveredMethod50(
         this.recoveredField39 + (this.recoveredField39 - this.recoveredField42) * var13 + this.recoveredMethod41(var9 * 0.22), 0.27, 0.88
      );
      this.recoveredField55 = this.recoveredMethod50(
         this.recoveredField40 + (this.recoveredField40 - this.recoveredField43) * var13 + this.recoveredMethod41(var9 * 0.32), 0.16, 0.84
      );
      if (this.recoveredField139 == FuntimeRotation.PathModel.EMPIRICAL) {
         this.recoveredField63 = 0.75;
         this.recoveredField64 = 0.58;
         this.recoveredField87 = this.recoveredMethod46(0.9F, 1.12F);
      } else if (this.recoveredField139 == FuntimeRotation.PathModel.FLICK_SETTLE) {
         this.recoveredField63 = this.recoveredMethod47(0.42, 0.6);
         this.recoveredField64 = this.recoveredMethod47(0.31, 0.48);
         this.recoveredField87 = this.recoveredMethod46(1.3F, 1.72F);
      } else if (this.recoveredField139 == FuntimeRotation.PathModel.CORRECTIVE) {
         this.recoveredField63 = this.recoveredMethod47(0.7, 0.86);
         this.recoveredField64 = this.recoveredMethod47(0.52, 0.69);
         this.recoveredField87 = this.recoveredMethod46(0.78F, 1.24F);
      } else {
         this.recoveredField63 = 0.75;
         this.recoveredField64 = 0.58;
         this.recoveredField87 = this.recoveredMethod46(0.74F, 1.3F);
      }

      double var15 = this.recoveredMethod33(
         this.recoveredField41, this.recoveredField42, this.recoveredField43, this.recoveredField38, this.recoveredField39, this.recoveredField40
      );
      double var17 = this.recoveredMethod50(0.72 + var15 * 0.9, 0.78, 1.28);
      long var19;
      if (this.recoveredField139 == FuntimeRotation.PathModel.EMPIRICAL) {
         long var21 = this.recoveredField145
            ? FuntimeMotorProfile.jumpDuration(this.recoveredField143)
            : FuntimeMotorProfile.duration(this.recoveredField144, this.recoveredField143);
         var19 = (long)(var21 * var17 * this.recoveredMethod47(0.92, 1.08));
      } else {
         long var23;
         long var27;
         if (this.recoveredField139 == FuntimeRotation.PathModel.FLICK_SETTLE) {
            var27 = 85L;
            var23 = 190L;
         } else if (this.recoveredField139 == FuntimeRotation.PathModel.RANDOM_SPLINE) {
            var27 = 170L;
            var23 = 390L;
         } else if (this.recoveredField139 == FuntimeRotation.PathModel.CORRECTIVE) {
            var27 = 145L;
            var23 = 350L;
         } else {
            var27 = 110L;
            var23 = 290L;
         }

         var19 = (long)(this.recoveredMethod47(var27, var23) * var17);
      }

      if (precision && this.recoveredField139 != FuntimeRotation.PathModel.EMPIRICAL) {
         var19 = Math.min(var19, action ? 245L : 310L);
      }

      double var28 = var19 / 1000.0;
      this.recoveredField65 = this.recoveredMethod50(this.recoveredField68 * var28 * 0.24, -0.12, 0.12);
      this.recoveredField66 = this.recoveredMethod50(this.recoveredField69 * var28 * 0.24, -0.09, 0.09);
      this.recoveredField67 = this.recoveredMethod50(this.recoveredField70 * var28 * 0.24, -0.12, 0.12);
      this.recoveredField109 = now;
      this.recoveredField110 = now + var19 * 1000000L;
      double var29 = this.recoveredField145 ? 1.0 : (precision ? (action ? 0.9 : 0.78) : (action ? 0.93 : 0.84));
      if (this.recoveredField16.nextDouble() < var29) {
         double var25 = this.recoveredField139 == FuntimeRotation.PathModel.EMPIRICAL
            ? (
               this.recoveredField145
                  ? this.recoveredMethod47(0.92, 0.985)
                  : (action ? this.recoveredMethod47(0.78, 0.94) : this.recoveredMethod47(0.76, 0.93))
            )
            : (action ? this.recoveredMethod47(0.55, 0.84) : this.recoveredMethod47(0.62, 0.89));
         this.recoveredField108 = this.recoveredField109 + (long)((this.recoveredField110 - this.recoveredField109) * var25);
      } else {
         long var30 = this.recoveredField16.nextDouble() < 0.08
            ? this.recoveredMethod48(140L, 620L)
            : (precision ? this.recoveredMethod48(17L, 88L) : this.recoveredMethod48(20L, 125L));
         this.recoveredField108 = this.recoveredField110 + var30;
      }
   }

   private void recoveredMethod13(long now, boolean precision) {
      if (now - this.recoveredField123 >= 30000000L) {
         this.recoveredField123 = now;
         this.recoveredMethod18(now);
         this.recoveredMethod12(now, precision, true);
      }
   }

   private void recoveredMethod14(long now, boolean precision) {
      this.recoveredMethod18(now);
      this.recoveredField146 = true;
      this.recoveredField147 = true;
      this.recoveredMethod12(now, precision, false);
      this.recoveredField155 = now;
      this.recoveredField156 = now + this.recoveredMethod48(435L, 525L);
      double var4 = this.recoveredField16.nextDouble();
      if (var4 < 0.52) {
         this.recoveredField157 = this.recoveredMethod46(6.0F, 18.0F);
      } else if (var4 < 0.75) {
         this.recoveredField157 = this.recoveredMethod46(1.5F, 6.0F);
      } else if (var4 < 0.9) {
         this.recoveredField157 = 0.0F;
      } else {
         this.recoveredField157 = -this.recoveredMethod46(1.5F, 6.0F);
      }
   }

   private float recoveredMethod15(long now) {
      if (this.recoveredField156 > this.recoveredField155 && now > this.recoveredField155 && now < this.recoveredField156 && this.recoveredField157 != 0.0F) {
         double var3 = (double)(now - this.recoveredField155) / (this.recoveredField156 - this.recoveredField155);
         double var5;
         if (var3 < 0.2) {
            var5 = this.recoveredMethod38(var3 / 0.2) * 0.58;
         } else if (var3 < 0.44) {
            var5 = this.recoveredMethod40(0.58, 1.0, this.recoveredMethod38((var3 - 0.2) / 0.24));
         } else if (var3 < 0.63) {
            var5 = this.recoveredMethod40(1.0, 0.84, this.recoveredMethod38((var3 - 0.44) / 0.19));
         } else if (var3 < 0.92) {
            var5 = this.recoveredMethod40(0.84, 0.0, this.recoveredMethod38((var3 - 0.63) / 0.29));
         } else {
            var5 = 0.0;
         }

         return -this.recoveredField157 * (float)var5;
      } else {
         return 0.0F;
      }
   }

   private void recoveredMethod16() {
      Vec3d var1 = this.recoveredMethod17(this.recoveredField41, this.recoveredField42, this.recoveredField43);
      Vec3d var2 = this.recoveredMethod17(this.recoveredField38, this.recoveredField39, this.recoveredField40);
      float var3 = MathHelper.wrapDegrees((float)(var2.x - var1.x));
      float var4 = (float)(var2.y - var1.y);
      float var5 = (float)Math.hypot(var3, var4);
      if (var5 < 0.001F) {
         this.recoveredField152 = 0.0F;
         this.recoveredField153 = 0.0F;
         this.recoveredField154 = 0.0F;
      } else {
         this.recoveredField152 = -var4 / var5;
         this.recoveredField153 = var3 / var5;
         this.recoveredField154 = Math.min(var5, 18.0F);
      }
   }

   private Vec3d recoveredMethod17(double x, double y, double z) {
      if (this.recoveredField21 != null && mc.player != null) {
         Box var7 = this.recoveredField21.getBoundingBox();
         Vec3d var8 = new Vec3d(var7.minX + var7.getLengthX() * x, var7.minY + var7.getLengthY() * y, var7.minZ + var7.getLengthZ() * z);
         Vec3d var9 = var8.subtract(mc.player.getEyePos());
         double var10 = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var9.z, var9.x)) - 90.0);
         double var12 = -Math.toDegrees(Math.atan2(var9.y, var9.horizontalLength()));
         return new Vec3d(var10, var12, 0.0);
      } else {
         return Vec3d.ZERO;
      }
   }

   private void recoveredMethod18(long now) {
      if (this.recoveredField110 > this.recoveredField109) {
         double var3 = this.recoveredMethod50((double)(now - this.recoveredField109) / (this.recoveredField110 - this.recoveredField109), 0.0, 1.0);
         double var5 = this.recoveredField35;
         double var7 = this.recoveredField36;
         double var9 = this.recoveredField37;
         double var11 = this.recoveredMethod50(var3 + this.recoveredField59 * var3 * (1.0 - var3), 0.0, 1.0);
         if (this.recoveredField139 == FuntimeRotation.PathModel.EMPIRICAL) {
            double var13 = this.recoveredField141.evaluate(var11, this);
            double var15 = this.recoveredField145 ? this.recoveredMethod40(var3, var13, 0.38) : var13;
            double var17 = this.recoveredField145
               ? FuntimeMotorProfile.jumpProgress(this.recoveredField143, var15)
               : FuntimeMotorProfile.progress(this.recoveredField144, this.recoveredField143, var15);
            this.recoveredField35 = this.recoveredMethod40(this.recoveredField41, this.recoveredField38, var17);
            this.recoveredField36 = this.recoveredMethod40(this.recoveredField42, this.recoveredField39, var17);
            this.recoveredField37 = this.recoveredMethod40(this.recoveredField43, this.recoveredField40, var17);
            double var19 = this.recoveredField145
               ? FuntimeMotorProfile.jumpLateral(this.recoveredField143, var15)
               : FuntimeMotorProfile.lateral(this.recoveredField144, this.recoveredField143, var15);
            float var21 = (float)(1.0 - this.recoveredMethod38(var15));
            this.recoveredField148 = this.recoveredField150 * var21 + this.recoveredField152 * this.recoveredField154 * (float)var19;
            this.recoveredField149 = this.recoveredField151 * var21 + this.recoveredField153 * this.recoveredField154 * (float)var19;
         } else if (this.recoveredField139 == FuntimeRotation.PathModel.RANDOM_SPLINE) {
            double var22 = this.recoveredField141.evaluate(var11, this);
            if (var22 < 0.5) {
               double var26 = var22 * 2.0;
               this.recoveredField35 = this.recoveredMethod39(this.recoveredField41, this.recoveredField44, this.recoveredField50, var26);
               this.recoveredField36 = this.recoveredMethod39(this.recoveredField42, this.recoveredField45, this.recoveredField51, var26);
               this.recoveredField37 = this.recoveredMethod39(this.recoveredField43, this.recoveredField46, this.recoveredField52, var26);
            } else {
               double var27 = (var22 - 0.5) * 2.0;
               this.recoveredField35 = this.recoveredMethod39(this.recoveredField50, this.recoveredField47, this.recoveredField38, var27);
               this.recoveredField36 = this.recoveredMethod39(this.recoveredField51, this.recoveredField48, this.recoveredField39, var27);
               this.recoveredField37 = this.recoveredMethod39(this.recoveredField52, this.recoveredField49, this.recoveredField40, var27);
            }
         } else if (this.recoveredField139 == FuntimeRotation.PathModel.FLICK_SETTLE) {
            this.recoveredField35 = this.recoveredMethod35(this.recoveredField41, this.recoveredField53, this.recoveredField38, var11);
            this.recoveredField36 = this.recoveredMethod35(this.recoveredField42, this.recoveredField54, this.recoveredField39, var11);
            this.recoveredField37 = this.recoveredMethod35(this.recoveredField43, this.recoveredField55, this.recoveredField40, var11);
         } else if (this.recoveredField139 == FuntimeRotation.PathModel.CORRECTIVE) {
            this.recoveredField35 = this.recoveredMethod34(this.recoveredField41, this.recoveredField53, this.recoveredField38, var11);
            this.recoveredField36 = this.recoveredMethod34(this.recoveredField42, this.recoveredField54, this.recoveredField39, var11);
            this.recoveredField37 = this.recoveredMethod34(this.recoveredField43, this.recoveredField55, this.recoveredField40, var11);
         } else {
            double var23 = this.recoveredField141.evaluate(var11, this);
            double var28 = 16.0 * var23 * var23 * (1.0 - var23) * (1.0 - var23);
            this.recoveredField35 = this.recoveredMethod40(this.recoveredField41, this.recoveredField38, var23) + this.recoveredField56 * var28;
            this.recoveredField36 = this.recoveredMethod40(this.recoveredField42, this.recoveredField39, var23) + this.recoveredField57 * var28;
            this.recoveredField37 = this.recoveredMethod40(this.recoveredField43, this.recoveredField40, var23) + this.recoveredField58 * var28;
         }

         if (this.recoveredField139 != FuntimeRotation.PathModel.EMPIRICAL) {
            float var24 = (float)(1.0 - this.recoveredMethod38(var3));
            this.recoveredField148 = this.recoveredField150 * var24;
            this.recoveredField149 = this.recoveredField151 * var24;
         }

         double var25 = 4.0 * var3 * Math.pow(1.0 - var3, 3.0);
         this.recoveredField35 = this.recoveredField35 + this.recoveredField65 * var25;
         this.recoveredField36 = this.recoveredField36 + this.recoveredField66 * var25;
         this.recoveredField37 = this.recoveredField37 + this.recoveredField67 * var25;
         this.recoveredField35 = this.recoveredMethod50(this.recoveredField35, 0.12, 0.88);
         this.recoveredField36 = this.recoveredMethod50(this.recoveredField36, 0.22, 0.92);
         this.recoveredField37 = this.recoveredMethod50(this.recoveredField37, 0.12, 0.88);
         if (this.recoveredField111 > 0L && now > this.recoveredField111) {
            double var29 = (now - this.recoveredField111) / 1.0E9;
            if (var29 <= 0.1) {
               double var30 = 1.0 - Math.exp(-var29 / 0.045);
               this.recoveredField68 = this.recoveredField68 + ((this.recoveredField35 - var5) / var29 - this.recoveredField68) * var30;
               this.recoveredField69 = this.recoveredField69 + ((this.recoveredField36 - var7) / var29 - this.recoveredField69) * var30;
               this.recoveredField70 = this.recoveredField70 + ((this.recoveredField37 - var9) / var29 - this.recoveredField70) * var30;
            }
         }

         this.recoveredField111 = now;
      }
   }

   private void recoveredMethod19(long now, double deltaTime) {
      Vec3d var5 = mc.player.getVelocity();
      boolean var6 = mc.player.isOnGround() && var5.x * var5.x + var5.z * var5.z < 0.0025;
      boolean var7 = this.recoveredMethod20(now);
      if (var6 && !var7) {
         if (this.recoveredField114 > 0L && now >= this.recoveredField114) {
            this.recoveredField79 = 0.0F;
            this.recoveredField80 = 0.0F;
            this.recoveredField114 = 0L;
            this.recoveredField113 = now + this.recoveredMethod48(480L, 1400L);
         }

         if (this.recoveredField114 == 0L && now >= this.recoveredField113) {
            float var8 = Math.max(0.001F, GCDUtil.getGCD());
            int var9 = this.recoveredField16.nextDouble() < 0.94 ? 1 : 2;
            int var10 = this.recoveredField16.nextDouble() < 0.78 ? 0 : 1;
            float var11 = this.recoveredField16.nextBoolean() ? 1.0F : -1.0F;
            float var12 = this.recoveredField16.nextBoolean() ? 1.0F : -1.0F;
            this.recoveredField79 = var9 * var8 * var11;
            this.recoveredField80 = var10 * var8 * var12;
            this.recoveredField114 = now + this.recoveredMethod48(18L, 44L);
         }
      } else {
         this.recoveredField79 = 0.0F;
         this.recoveredField80 = 0.0F;
         this.recoveredField114 = 0L;
         if (now >= this.recoveredField113) {
            this.recoveredField113 = now + this.recoveredMethod48(480L, 1200L);
         }
      }

      boolean var17 = this.recoveredField114 > now;
      double var18 = 1.0 - Math.exp(-deltaTime / (var17 ? 0.018 : 0.035));
      this.recoveredField77 = this.recoveredField77 + (this.recoveredField79 - this.recoveredField77) * (float)var18;
      this.recoveredField78 = this.recoveredField78 + (this.recoveredField80 - this.recoveredField78) * (float)var18;
      float var19 = var6 && !var7 ? 1.0F : 0.0F;
      double var20 = 1.0 - Math.exp(-deltaTime / (var19 > this.recoveredField81 ? 0.08 : 0.045));
      this.recoveredField81 = this.recoveredField81 + (var19 - this.recoveredField81) * (float)var20;
      double var14 = 1.0 - Math.exp(-deltaTime / (var7 ? 0.12 : 0.3));
      float var16 = var7 ? this.recoveredField82 : 0.1F;
      this.recoveredField83 = this.recoveredField83 + (var16 - this.recoveredField83) * (float)var14;
   }

   private boolean recoveredMethod20(long now) {
      double var3 = Math.sqrt(
         this.recoveredField68 * this.recoveredField68 + this.recoveredField69 * this.recoveredField69 + this.recoveredField70 * this.recoveredField70
      );
      double var5 = Math.hypot(this.recoveredField26, this.recoveredField27);
      return now < this.recoveredField110 && var3 > 0.12 || var5 > 24.0;
   }

   private boolean recoveredMethod21(long now, boolean protectedWindow) {
      if (protectedWindow) {
         this.recoveredField125 = 0L;
         if (now >= this.recoveredField124) {
            this.recoveredField124 = now + this.recoveredMethod48(650L, 1800L);
         }

         return false;
      } else {
         if (now < this.recoveredField125) {
            return true;
         }

         if (now < this.recoveredField124) {
            return false;
         }

         double var4 = this.recoveredField16.nextDouble();
         long var6 = var4 < 0.65
            ? this.recoveredMethod48(17L, 43L)
            : (var4 < 0.92 ? this.recoveredMethod48(44L, 90L) : (var4 < 0.985 ? this.recoveredMethod48(91L, 190L) : this.recoveredMethod48(220L, 620L)));
         this.recoveredField125 = now + var6;
         this.recoveredField124 = this.recoveredField125 + this.recoveredMethod48(750L, 2400L);
         return true;
      }
   }

   private void recoveredMethod22(long now, boolean precision, float yawMargin, float pitchMargin) {
      if (now >= this.recoveredField117 && now >= this.recoveredField119) {
         if (precision) {
            this.recoveredField117 = now + this.recoveredMethod48(260L, 720L);
         } else {
            float var6 = MathHelper.clamp(yawMargin * this.recoveredMethod46(0.78F, 1.24F), 0.85F, 12.0F);
            float var7 = MathHelper.clamp(pitchMargin * this.recoveredMethod46(0.58F, 0.96F), 1.2F, 16.0F);
            float var8 = this.recoveredField16.nextBoolean() ? 1.0F : -1.0F;
            float var9 = this.recoveredField16.nextBoolean() ? 1.0F : -1.0F;
            if (this.recoveredField21 != null && mc.player != null) {
               Vec3d var10 = this.recoveredField21.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
               float var11 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var10.z, var10.x)) - 90.0);
               float var12 = (float)(-Math.toDegrees(Math.atan2(var10.y, var10.horizontalLength())));
               float var13 = MathHelper.wrapDegrees(this.recoveredField24 - var11);
               float var14 = this.recoveredField25 - var12;
               if (Math.abs(var13) > 0.15F) {
                  var8 = Math.signum(var13);
               }

               if (Math.abs(var14) > 0.15F) {
                  var9 = Math.signum(var14);
               }
            }

            double var15 = this.recoveredField16.nextDouble();
            if (var15 < 0.46) {
               this.recoveredField92 = var8 * var6;
               this.recoveredField93 = this.recoveredMethod45(0.08F, 0.24F) * var7;
            } else if (var15 < 0.72) {
               this.recoveredField92 = this.recoveredMethod45(0.12F, 0.34F) * var6;
               this.recoveredField93 = var9 * var7;
            } else {
               this.recoveredField92 = var8 * var6 * this.recoveredMethod46(0.66F, 0.96F);
               this.recoveredField93 = var9 * var7 * this.recoveredMethod46(0.42F, 0.76F);
            }

            this.recoveredField96 = this.recoveredMethod46(0.18F, 0.38F);
            this.recoveredField97 = this.recoveredMethod46(2.0F, 2.8F);
            this.recoveredField118 = now;
            this.recoveredField119 = now + this.recoveredMethod48(125L, 340L);
            this.recoveredField117 = this.recoveredField119 + this.recoveredMethod48(900L, 3400L);
         }
      }
   }

   private float recoveredMethod23(long now) {
      if (this.recoveredField119 > this.recoveredField118 && now >= this.recoveredField118 && now < this.recoveredField119) {
         double var3 = this.recoveredMethod50((double)(now - this.recoveredField118) / (this.recoveredField119 - this.recoveredField118), 0.0, 1.0);
         if (var3 < this.recoveredField96) {
            double var7 = var3 / this.recoveredField96;
            return (float)(1.0 - Math.pow(1.0 - var7, this.recoveredField97));
         } else {
            double var5 = (var3 - this.recoveredField96) / (1.0 - this.recoveredField96);
            return (float)(1.0 - this.recoveredMethod38(var5));
         }
      } else {
         return 0.0F;
      }
   }

   private FuntimeRotation.PathModel recoveredMethod24(boolean action, double screenDelta) {
      FuntimeRotation.PathModel var4 = this.recoveredField140;

      for (int var5 = 0; var5 < 8; var5++) {
         double var6 = this.recoveredField16.nextDouble();
         if (action) {
            var4 = var6 < 0.9
               ? FuntimeRotation.PathModel.EMPIRICAL
               : (
                  var6 < 0.93
                     ? FuntimeRotation.PathModel.MINIMUM_JERK
                     : (
                        var6 < 0.97
                           ? FuntimeRotation.PathModel.CORRECTIVE
                           : (
                              var6 < 0.99
                                 ? FuntimeRotation.PathModel.RANDOM_SPLINE
                                 : (screenDelta >= 2.8 ? FuntimeRotation.PathModel.FLICK_SETTLE : FuntimeRotation.PathModel.CORRECTIVE)
                           )
                     )
               );
         } else {
            var4 = var6 < 0.92
               ? FuntimeRotation.PathModel.EMPIRICAL
               : (
                  var6 < 0.95
                     ? FuntimeRotation.PathModel.MINIMUM_JERK
                     : (var6 < 0.99 ? FuntimeRotation.PathModel.CORRECTIVE : FuntimeRotation.PathModel.RANDOM_SPLINE)
               );
         }

         if ((var4 != FuntimeRotation.PathModel.FLICK_SETTLE || this.recoveredField140 != FuntimeRotation.PathModel.FLICK_SETTLE)
            && (var4 != this.recoveredField140 || this.recoveredField138 < 2 || var4 == FuntimeRotation.PathModel.EMPIRICAL)) {
            break;
         }
      }

      if (var4 == this.recoveredField140) {
         this.recoveredField138++;
      } else {
         this.recoveredField140 = var4;
         this.recoveredField138 = 1;
      }

      return var4;
   }

   private FuntimeRotation.PathEase recoveredMethod25() {
      FuntimeRotation.PathEase[] var1 = FuntimeRotation.PathEase.values();
      FuntimeRotation.PathEase var2 = this.recoveredField142;

      for (int var3 = 0; var3 < 6 && var2 == this.recoveredField142; var3++) {
         var2 = var1[this.recoveredField16.nextInt(var1.length)];
      }

      this.recoveredField142 = var2;
      return var2;
   }

   private Vec3d recoveredMethod26(boolean precision) {
      int var2 = precision ? 44 : 56;
      Vec3d[] var3 = new Vec3d[var2];
      int[] var4 = new int[var2];
      double[] var5 = new double[var2];
      double[] var6 = new double[var2];
      double[] var7 = new double[var2];
      double var8 = precision ? 0.36 : 0.48;
      double var10 = precision ? 0.135 : 0.19;

      for (int var12 = 0; var12 < var2; var12++) {
         Vec3d var13 = this.recoveredMethod28(precision);
         int var14 = this.recoveredMethod29(var13);
         double var15 = this.recoveredField134 == 0
            ? this.recoveredMethod32(var13.x, var13.y, var13.z, this.recoveredField35, this.recoveredField36, this.recoveredField37)
            : Double.MAX_VALUE;
         double var17 = this.recoveredField134 == 0
            ? this.recoveredMethod33(var13.x, var13.y, var13.z, this.recoveredField35, this.recoveredField36, this.recoveredField37)
            : Double.MAX_VALUE;
         double var19 = 0.0;

         for (int var21 = 0; var21 < this.recoveredField134; var21++) {
            int var22 = (this.recoveredField135 - 1 - var21 + 12) % 12;
            var15 = Math.min(
               var15,
               this.recoveredMethod32(var13.x, var13.y, var13.z, this.recoveredField17[var22], this.recoveredField18[var22], this.recoveredField19[var22])
            );
            var17 = Math.min(
               var17,
               this.recoveredMethod33(var13.x, var13.y, var13.z, this.recoveredField17[var22], this.recoveredField18[var22], this.recoveredField19[var22])
            );
            if (this.recoveredField20[var22] == var14 && var21 < 6) {
               var19 += 0.08 - var21 * 0.01;
            }
         }

         double var27 = this.recoveredMethod32(var13.x, var13.y, var13.z, this.recoveredField35, this.recoveredField36, this.recoveredField37);
         double var23 = var15 * 1.35 + var17 * 1.1 + var27 * 0.42 + this.recoveredField16.nextDouble() * 0.08 - var19;
         if (var14 == this.recoveredField136) {
            var23 -= 0.28;
         }

         if (var14 == this.recoveredField137) {
            var23 -= 0.12;
         }

         var3[var12] = var13;
         var4[var12] = var14;
         var5[var12] = var23;
         var6[var12] = var15;
         var7[var12] = var17;
      }

      int var25 = this.recoveredMethod27(var5, var6, var7, var8, var10);
      if (var25 < 0) {
         var25 = this.recoveredMethod27(var5, var6, var7, var8 * 0.68, var10 * 0.7);
      }

      if (var25 < 0) {
         var25 = this.recoveredMethod27(var5, var6, var7, 0.24, 0.075);
      }

      if (var25 < 0) {
         return new Vec3d(this.recoveredField35, this.recoveredField36, this.recoveredField37);
      }

      Vec3d var26 = var3[var25];
      this.recoveredMethod30(var26, var4[var25]);
      return var26;
   }

   private int recoveredMethod27(
      double[] scores, double[] screenDistances, double[] normalizedDistances, double minimumScreenDistance, double minimumNormalizedDistance
   ) {
      double var8 = 1.1;
      double var10 = -Double.MAX_VALUE;

      for (int var12 = 0; var12 < scores.length; var12++) {
         if (!(screenDistances[var12] < minimumScreenDistance) && !(normalizedDistances[var12] < minimumNormalizedDistance)) {
            var10 = Math.max(var10, scores[var12]);
         }
      }

      if (var10 == -Double.MAX_VALUE) {
         return -1;
      }

      double var17 = 0.0;

      for (int var14 = 0; var14 < scores.length; var14++) {
         if (!(screenDistances[var14] < minimumScreenDistance) && !(normalizedDistances[var14] < minimumNormalizedDistance) && !(scores[var14] < var10 - var8)) {
            var17 += 0.16 + Math.exp((scores[var14] - var10) * 1.15);
         }
      }

      if (var17 <= 0.0) {
         return -1;
      }

      double var18 = this.recoveredField16.nextDouble() * var17;

      for (int var16 = 0; var16 < scores.length; var16++) {
         if (!(screenDistances[var16] < minimumScreenDistance) && !(normalizedDistances[var16] < minimumNormalizedDistance) && !(scores[var16] < var10 - var8)) {
            var18 -= 0.16 + Math.exp((scores[var16] - var10) * 1.15);
            if (var18 <= 0.0) {
               return var16;
            }
         }
      }

      return -1;
   }

   private Vec3d recoveredMethod28(boolean precision) {
      int var2 = this.recoveredField16.nextInt(8);
      double var3 = (var2 + this.recoveredMethod47(0.1, 0.9)) * Math.PI / 4.0;
      double var5 = precision ? this.recoveredMethod47(0.16, 0.34) : this.recoveredMethod47(0.18, 0.39);
      double var7 = this.recoveredField16.nextDouble();
      double var9;
      double var11;
      if (precision) {
         if (var7 < 0.1) {
            var9 = 0.36;
            var11 = 0.47;
         } else if (var7 < 0.37) {
            var9 = 0.47;
            var11 = 0.68;
         } else if (var7 < 0.89) {
            var9 = 0.68;
            var11 = 0.86;
         } else {
            var9 = 0.86;
            var11 = 0.9;
         }
      } else if (var7 < 0.15) {
         var9 = 0.29;
         var11 = 0.47;
      } else if (var7 < 0.43) {
         var9 = 0.47;
         var11 = 0.67;
      } else if (var7 < 0.89) {
         var9 = 0.67;
         var11 = 0.86;
      } else {
         var9 = 0.86;
         var11 = 0.92;
      }

      double var13 = 0.5 + Math.cos(var3) * var5;
      double var15 = this.recoveredMethod47(var9, var11);
      double var17 = 0.5 + Math.sin(var3) * var5;
      if (precision) {
         var13 = this.recoveredMethod50(var13, 0.16, 0.84);
         var15 = this.recoveredMethod50(var15, 0.36, 0.9);
         var17 = this.recoveredMethod50(var17, 0.16, 0.84);
      } else {
         var13 = this.recoveredMethod50(var13, 0.12, 0.88);
         var15 = this.recoveredMethod50(var15, 0.29, 0.92);
         var17 = this.recoveredMethod50(var17, 0.12, 0.88);
      }

      return new Vec3d(var13, var15, var17);
   }

   private int recoveredMethod29(Vec3d point) {
      int var2 = point.y < 0.49 ? 0 : (point.y < 0.68 ? 1 : 2);
      double var3 = Math.atan2(point.z - 0.5, point.x - 0.5);
      if (var3 < 0.0) {
         var3 += Math.PI * 2;
      }

      int var5 = Math.min(7, (int)(var3 / (Math.PI / 4)));
      return var2 * 8 + var5;
   }

   private void recoveredMethod30(Vec3d point, int zone) {
      this.recoveredField17[this.recoveredField135] = point.x;
      this.recoveredField18[this.recoveredField135] = point.y;
      this.recoveredField19[this.recoveredField135] = point.z;
      this.recoveredField20[this.recoveredField135] = zone;
      this.recoveredField135 = (this.recoveredField135 + 1) % 12;
      this.recoveredField134 = Math.min(this.recoveredField134 + 1, 12);
      this.recoveredField137 = this.recoveredField136;
      this.recoveredField136 = zone;
   }

   private void recoveredMethod31() {
      this.recoveredField134 = 0;
      this.recoveredField135 = 0;
      this.recoveredField136 = -1;
      this.recoveredField137 = -1;

      for (int var1 = 0; var1 < 12; var1++) {
         this.recoveredField17[var1] = 0.0;
         this.recoveredField18[var1] = 0.0;
         this.recoveredField19[var1] = 0.0;
         this.recoveredField20[var1] = -1;
      }
   }

   private double recoveredMethod32(double firstX, double firstY, double firstZ, double secondX, double secondY, double secondZ) {
      if (this.recoveredField21 != null && mc.player != null) {
         Box var13 = this.recoveredField21.getBoundingBox();
         Vec3d var14 = mc.player.getEyePos();
         double var15 = var13.minX + var13.getLengthX() * firstX - var14.x;
         double var17 = var13.minY + var13.getLengthY() * firstY - var14.y;
         double var19 = var13.minZ + var13.getLengthZ() * firstZ - var14.z;
         double var21 = var13.minX + var13.getLengthX() * secondX - var14.x;
         double var23 = var13.minY + var13.getLengthY() * secondY - var14.y;
         double var25 = var13.minZ + var13.getLengthZ() * secondZ - var14.z;
         float var27 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var19, var15)) - 90.0);
         float var28 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var25, var21)) - 90.0);
         float var29 = (float)(-Math.toDegrees(Math.atan2(var17, Math.hypot(var15, var19))));
         float var30 = (float)(-Math.toDegrees(Math.atan2(var23, Math.hypot(var21, var25))));
         double var31 = MathHelper.wrapDegrees(var27 - var28);
         double var33 = var29 - var30;
         return Math.hypot(var31, var33);
      } else {
         return this.recoveredMethod33(firstX, firstY, firstZ, secondX, secondY, secondZ);
      }
   }

   private double recoveredMethod33(double firstX, double firstY, double firstZ, double secondX, double secondY, double secondZ) {
      double var13 = firstX - secondX;
      double var15 = (firstY - secondY) * 1.25;
      double var17 = firstZ - secondZ;
      return Math.sqrt(var13 * var13 + var15 * var15 + var17 * var17);
   }

   private double recoveredMethod34(double start, double overshoot, double end, double progress) {
      double var9 = this.recoveredMethod37(this.recoveredMethod50(progress / this.recoveredField63, 0.0, 1.0));
      double var11 = this.recoveredMethod36(this.recoveredMethod50((progress - this.recoveredField64) / (1.0 - this.recoveredField64), 0.0, 1.0));
      return this.recoveredMethod40(start, overshoot, var9) + (end - overshoot) * var11;
   }

   private double recoveredMethod35(double start, double overshoot, double end, double progress) {
      double var9 = this.recoveredMethod50(progress / this.recoveredField63, 0.0, 1.0);
      double var11 = 1.0 - Math.pow(1.0 - var9, this.recoveredField62);
      double var13 = this.recoveredMethod38(this.recoveredMethod50((progress - this.recoveredField64) / (1.0 - this.recoveredField64), 0.0, 1.0));
      return this.recoveredMethod40(start, overshoot, var11) + (end - overshoot) * var13;
   }

   private double recoveredMethod36(double progress) {
      if (progress <= 0.0) {
         return 0.0;
      }

      if (progress >= 1.0) {
         return 1.0;
      }

      double var3 = Math.pow(progress, this.recoveredField60);
      double var5 = Math.pow(1.0 - progress, this.recoveredField61);
      return var3 / (var3 + var5);
   }

   private double recoveredMethod37(double progress) {
      return progress - Math.sin((Math.PI * 2) * progress) / (Math.PI * 2);
   }

   private double recoveredMethod38(double progress) {
      double var3 = progress * progress;
      double var5 = var3 * progress;
      return 10.0 * var5 - 15.0 * var5 * progress + 6.0 * var5 * var3;
   }

   private double recoveredMethod39(double start, double control, double end, double progress) {
      double var9 = 1.0 - progress;
      return var9 * var9 * start + 2.0 * var9 * progress * control + progress * progress * end;
   }

   private double recoveredMethod40(double start, double end, double progress) {
      return start + (end - start) * progress;
   }

   private double recoveredMethod41(double magnitude) {
      return this.recoveredMethod47(-magnitude, magnitude);
   }

   private float recoveredMethod42(LivingEntity target, double distance) {
      Box var4 = target.getBoundingBox();
      double var5 = Math.max(var4.getLengthX(), var4.getLengthZ()) * 0.5;
      return (float)Math.toDegrees(Math.atan2(var5, Math.max(0.35, distance)));
   }

   private float recoveredMethod43(LivingEntity target, double distance) {
      double var4 = target.getBoundingBox().getLengthY() * 0.5;
      return (float)Math.toDegrees(Math.atan2(var4, Math.max(0.35, distance)));
   }

   private float recoveredMethod44() {
      return MathHelper.clamp((float)this.recoveredField16.nextGaussian() * 0.48F, -1.0F, 1.0F);
   }

   private float recoveredMethod45(float minimum, float maximum) {
      return (this.recoveredField16.nextBoolean() ? 1.0F : -1.0F) * this.recoveredMethod46(minimum, maximum);
   }

   private float recoveredMethod46(float minimum, float maximum) {
      return minimum + this.recoveredField16.nextFloat() * (maximum - minimum);
   }

   private double recoveredMethod47(double minimum, double maximum) {
      return minimum + this.recoveredField16.nextDouble() * (maximum - minimum);
   }

   private long recoveredMethod48(long minimumMillis, long maximumMillis) {
      long var5 = minimumMillis + (long)(this.recoveredField16.nextDouble() * (maximumMillis - minimumMillis));
      return var5 * 1000000L;
   }

   private float recoveredMethod49(float current, float target, float maximumChange) {
      return current < target ? Math.min(current + maximumChange, target) : Math.max(current - maximumChange, target);
   }

   private double recoveredMethod50(double value, double minimum, double maximum) {
      return Math.max(minimum, Math.min(maximum, value));
   }

   private enum AimPhase {
      REACTION,
      ACQUIRE,
      TRACK,
      FOCUS,
      RECOVER;
   }

   private enum PathEase {
      MINIMUM_JERK,
      EASE_OUT_QUAD,
      EASE_OUT_CUBIC,
      EASE_OUT_QUART,
      EASE_IN_OUT_SINE,
      EASE_IN_OUT_CIRC,
      EASE_IN_OUT_CUBIC,
      EASE_IN_OUT_QUINT,
      EASE_IN_OUT_EXPO;

      double evaluate(double progress, FuntimeRotation owner) {
         switch (this) {
            case EASE_OUT_QUAD:
               return Easings.QUAD_OUT.ease(progress);
            case EASE_OUT_CUBIC:
               return Easings.CUBIC_OUT.ease(progress);
            case EASE_OUT_QUART:
               return Easings.QUART_OUT.ease(progress);
            case EASE_IN_OUT_SINE:
               return Easings.SINE_IN_OUT.ease(progress);
            case EASE_IN_OUT_CIRC:
               return Easings.CIRC_IN_OUT.ease(progress);
            case EASE_IN_OUT_CUBIC:
               return Easings.CUBIC_IN_OUT.ease(progress);
            case EASE_IN_OUT_QUINT:
               return Easings.QUINT_IN_OUT.ease(progress);
            case EASE_IN_OUT_EXPO:
               return Easings.EXPO_IN_OUT.ease(progress);
            default:
               return owner.recoveredMethod38(progress);
         }
      }
   }

   private enum PathModel {
      EMPIRICAL,
      MINIMUM_JERK,
      RANDOM_SPLINE,
      FLICK_SETTLE,
      CORRECTIVE;
   }
}
