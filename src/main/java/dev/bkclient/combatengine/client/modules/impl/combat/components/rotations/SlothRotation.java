package dev.bkclient.combatengine.client.modules.impl.combat.components.rotations;

import dev.bkclient.combatengine.api.QClient;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.RotationsSystem;
import dev.bkclient.combatengine.client.modules.impl.combat.components.gcd.GCDUtil;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SlothRotation extends RotationsSystem implements QClient {
   private static final long recoveredField1 = 500000L;
   private static final long recoveredField2 = 200000000L;
   private static final long recoveredField3 = 120000000L;
   private static final long recoveredField4 = 30000000L;
   private static final long recoveredField5 = 150000000L;
   private static final double recoveredField6 = 0.05;
   private static final int recoveredField7 = 1;
   private static final int recoveredField8 = 4;
   private static final int recoveredField9 = 20150;
   private static final float recoveredField10 = 0.8F;
   private static final float recoveredField11 = 1.2F;
   private final Random recoveredField12 = new Random();
   private final double[] recoveredField13 = new double[4];
   private final double[] recoveredField14 = new double[4];
   private final double[] recoveredField15 = new double[4];
   private final int[] recoveredField16 = new int[4];
   private LivingEntity recoveredField17;
   private SlothRotation.AimPhase recoveredField18 = SlothRotation.AimPhase.REACTION;
   private Vec3d recoveredField19 = Vec3d.ZERO;
   private float recoveredField20;
   private float recoveredField21;
   private float recoveredField22;
   private float recoveredField23;
   private float recoveredField24;
   private float recoveredField25;
   private float recoveredField26;
   private float recoveredField27;
   private float recoveredField28;
   private float recoveredField29;
   private boolean recoveredField30;
   private double recoveredField31 = 0.5;
   private double recoveredField32 = 0.62;
   private double recoveredField33 = 0.5;
   private double recoveredField34 = 0.5;
   private double recoveredField35 = 0.62;
   private double recoveredField36 = 0.5;
   private double recoveredField37 = 0.5;
   private double recoveredField38 = 0.62;
   private double recoveredField39 = 0.5;
   private double recoveredField40 = 0.5;
   private double recoveredField41 = 0.62;
   private double recoveredField42 = 0.5;
   private double recoveredField43 = 0.5;
   private double recoveredField44 = 0.62;
   private double recoveredField45 = 0.5;
   private double recoveredField46 = 0.5;
   private double recoveredField47 = 0.62;
   private double recoveredField48 = 0.5;
   private double recoveredField49 = 0.5;
   private double recoveredField50 = 0.62;
   private double recoveredField51 = 0.5;
   private double recoveredField52;
   private double recoveredField53;
   private double recoveredField54;
   private double recoveredField55;
   private double recoveredField56 = 2.0;
   private double recoveredField57 = 2.0;
   private double recoveredField58 = 2.8;
   private double recoveredField59 = 0.75;
   private double recoveredField60 = 0.58;
   private double recoveredField61;
   private double recoveredField62;
   private double recoveredField63;
   private double recoveredField64;
   private double recoveredField65;
   private double recoveredField66;
   private float recoveredField67;
   private float recoveredField68;
   private float recoveredField69;
   private float recoveredField70;
   private float recoveredField71;
   private float recoveredField72;
   private float recoveredField73;
   private float recoveredField74;
   private float recoveredField75;
   private float recoveredField76;
   private float recoveredField77;
   private float recoveredField78 = 0.06F;
   private float recoveredField79 = 0.1F;
   private float recoveredField80 = 1.0F;
   private float recoveredField81 = 1.0F;
   private float recoveredField82 = 1.0F;
   private float recoveredField83 = 1.0F;
   private float recoveredField84 = 1.0F;
   private float recoveredField85 = 1.0F;
   private float recoveredField86;
   private float recoveredField87;
   private float recoveredField88;
   private float recoveredField89;
   private float recoveredField90 = 1.0F;
   private float recoveredField91 = 1.0F;
   private float recoveredField92 = 0.28F;
   private float recoveredField93 = 2.4F;
   private float recoveredField94;
   private float recoveredField95;
   private float recoveredField96;
   private float recoveredField97;
   private float recoveredField98;
   private float recoveredField99;
   private float recoveredField100;
   private float recoveredField101;
   private long recoveredField102;
   private long recoveredField103;
   private long recoveredField104;
   private long recoveredField105;
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
   private int recoveredField124 = 20150;
   private int recoveredField125 = 20150;
   private int recoveredField126 = Integer.MIN_VALUE;
   private int recoveredField127;
   private int recoveredField128;
   private int recoveredField129;
   private int recoveredField130;
   private int recoveredField131 = Integer.MIN_VALUE;
   private int recoveredField132 = Integer.MIN_VALUE;
   private boolean recoveredField133;
   private float recoveredField134 = 1.0F;
   private boolean recoveredField135;
   private boolean recoveredField136;
   private boolean recoveredField137;
   private int recoveredField138;
   private int recoveredField139;
   private int recoveredField140 = -1;
   private int recoveredField141 = -1;
   private int recoveredField142;
   private SlothRotation.PathModel recoveredField143 = SlothRotation.PathModel.MINIMUM_JERK;
   private SlothRotation.PathModel recoveredField144 = SlothRotation.PathModel.MINIMUM_JERK;
   private int recoveredField145;
   private boolean recoveredField146;
   private boolean recoveredField147;
   private boolean recoveredField148;
   private boolean recoveredField149;
   private float recoveredField150;
   private float recoveredField151;
   private float recoveredField152;
   private float recoveredField153;
   private float recoveredField154;
   private float recoveredField155;
   private float recoveredField156;
   private long recoveredField157;
   private long recoveredField158;
   private float recoveredField159;
   private float recoveredField160 = 1.0F;
   private boolean recoveredField161;
   private int recoveredField162 = Integer.MIN_VALUE;
   private int recoveredField163;
   private boolean recoveredField164;
   private int recoveredField165 = Integer.MIN_VALUE;
   private float recoveredField166 = 1.0F;
   private float recoveredField167 = 1.0F;
   private static final int recoveredField168 = 6;
   private int recoveredField169 = Integer.MIN_VALUE;
   private int recoveredField170;
   private Vec3d recoveredField171 = Vec3d.ZERO;
   private float recoveredField172 = 1.0F;
   private float recoveredField173;
   private long recoveredField174;
   private final SlothPatternEasing.PatternDriver recoveredField175 = new SlothPatternEasing.PatternDriver();
   private float recoveredField176 = 1.0F;

   public void setSpeedBoost(float boost) {
      this.recoveredField176 = Math.max(0.1F, boost);
   }

   public void reset() {
      this.recoveredField17 = null;
      this.recoveredField18 = SlothRotation.AimPhase.REACTION;
      this.recoveredField19 = Vec3d.ZERO;
      this.recoveredField22 = 0.0F;
      this.recoveredField23 = 0.0F;
      this.recoveredField24 = 0.0F;
      this.recoveredField25 = 0.0F;
      this.recoveredField26 = 0.0F;
      this.recoveredField27 = 0.0F;
      this.recoveredField28 = 0.0F;
      this.recoveredField29 = 0.0F;
      this.recoveredField30 = false;
      this.recoveredField31 = this.recoveredField34 = 0.5;
      this.recoveredField32 = this.recoveredField35 = 0.62;
      this.recoveredField33 = this.recoveredField36 = 0.5;
      this.recoveredField37 = this.recoveredField40 = this.recoveredField43 = 0.5;
      this.recoveredField38 = this.recoveredField41 = this.recoveredField44 = 0.62;
      this.recoveredField39 = this.recoveredField42 = this.recoveredField45 = 0.5;
      this.recoveredField46 = this.recoveredField49 = 0.5;
      this.recoveredField47 = this.recoveredField50 = 0.62;
      this.recoveredField48 = this.recoveredField51 = 0.5;
      this.recoveredField52 = this.recoveredField53 = this.recoveredField54 = 0.0;
      this.recoveredField55 = 0.0;
      this.recoveredField56 = this.recoveredField57 = 2.0;
      this.recoveredField58 = 2.8;
      this.recoveredField59 = 0.75;
      this.recoveredField60 = 0.58;
      this.recoveredField61 = this.recoveredField62 = this.recoveredField63 = 0.0;
      this.recoveredField64 = this.recoveredField65 = this.recoveredField66 = 0.0;
      this.recoveredField67 = this.recoveredField68 = 0.0F;
      this.recoveredField69 = this.recoveredField70 = 0.0F;
      this.recoveredField71 = this.recoveredField72 = 0.0F;
      this.recoveredField73 = this.recoveredField74 = 0.0F;
      this.recoveredField75 = this.recoveredField76 = 0.0F;
      this.recoveredField77 = 0.0F;
      this.recoveredField78 = 0.06F;
      this.recoveredField79 = 0.1F;
      this.recoveredField80 = this.recoveredField81 = 1.0F;
      this.recoveredField82 = this.recoveredField83 = 1.0F;
      this.recoveredField84 = this.recoveredField85 = 1.0F;
      this.recoveredField86 = this.recoveredField87 = 0.0F;
      this.recoveredField88 = this.recoveredField89 = 0.0F;
      this.recoveredField90 = this.recoveredField91 = 1.0F;
      this.recoveredField92 = 0.28F;
      this.recoveredField93 = 2.4F;
      this.recoveredField102 = 0L;
      this.recoveredField103 = 0L;
      this.recoveredField104 = 0L;
      this.recoveredField105 = 0L;
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
      this.recoveredField124 = 20150;
      this.recoveredField125 = 20150;
      this.recoveredField126 = Integer.MIN_VALUE;
      this.recoveredField127 = 0;
      this.recoveredField128 = 0;
      this.recoveredField129 = 0;
      this.recoveredField130 = 0;
      this.recoveredField131 = Integer.MIN_VALUE;
      this.recoveredField132 = Integer.MIN_VALUE;
      this.recoveredField133 = false;
      this.recoveredField134 = 1.0F;
      this.recoveredField135 = false;
      this.recoveredField136 = false;
      this.recoveredField137 = mc.player != null && !mc.player.isOnGround();
      this.recoveredMethod41();
      this.recoveredField142 = 0;
      this.recoveredField143 = SlothRotation.PathModel.MINIMUM_JERK;
      this.recoveredField144 = SlothRotation.PathModel.MINIMUM_JERK;
      this.recoveredField145 = 0;
      this.recoveredField146 = false;
      this.recoveredField147 = false;
      this.recoveredField148 = false;
      this.recoveredField149 = false;
      this.recoveredField150 = 0.0F;
      this.recoveredField151 = 0.0F;
      this.recoveredField152 = 0.0F;
      this.recoveredField153 = 0.0F;
      this.recoveredField154 = 0.0F;
      this.recoveredField155 = 0.0F;
      this.recoveredField156 = 0.0F;
      this.recoveredField157 = 0L;
      this.recoveredField158 = 0L;
      this.recoveredField159 = 0.0F;
      this.recoveredField160 = 1.0F;
      this.recoveredField162 = Integer.MIN_VALUE;
      this.recoveredField163 = 0;
      this.recoveredField164 = false;
      this.recoveredField165 = Integer.MIN_VALUE;
      this.recoveredField166 = 1.0F;
      this.recoveredField167 = 1.0F;
      this.recoveredField169 = Integer.MIN_VALUE;
      this.recoveredField170 = 0;
      this.recoveredField171 = Vec3d.ZERO;
      this.recoveredField172 = 1.0F;
      this.recoveredField173 = 0.0F;
      this.recoveredField174 = 0L;
      this.recoveredField175.reset();
      if (mc.player != null) {
         this.recoveredField20 = mc.player.getYaw();
         this.recoveredField21 = mc.player.getPitch();
      } else {
         this.recoveredField20 = 0.0F;
         this.recoveredField21 = 0.0F;
      }
   }

   public void setPrecisionLock(boolean precision) {
      long var2 = System.nanoTime();
      if (precision && !this.recoveredField135) {
         this.recoveredMethod32(var2);
         if (this.recoveredField17 != null && var2 - this.recoveredField119 >= 150000000L) {
            this.recoveredMethod23(var2, true);
         }

         this.recoveredField119 = var2;
      }

      this.recoveredField135 = precision;
      if (precision) {
         this.recoveredField118 = Math.max(this.recoveredField118, var2 + 120000000L);
      }
   }

   public void setVisibleAim(boolean visible) {
      this.recoveredField161 = visible;
   }

   public void onAttack() {
      if (mc.player != null && this.recoveredField17 != null) {
         long var1 = System.nanoTime();
         this.recoveredField118 = Math.max(this.recoveredField118, var1 + 100000000L);
         if (this.recoveredField12.nextDouble() < 0.32) {
            this.recoveredField71 = this.recoveredMethod55(1.5F, 4.5F);
         } else {
            this.recoveredField71 = this.recoveredMethod55(0.04F, 0.13F);
         }

         double var3 = this.recoveredField12.nextDouble();
         this.recoveredField72 = var3 < 0.25
            ? this.recoveredMethod56(0.8F, 2.2F)
            : (var3 < 0.63 ? this.recoveredMethod56(0.12F, 0.42F) : (var3 < 0.83 ? -this.recoveredMethod56(0.08F, 0.28F) : 0.0F));
         boolean var5 = this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL
            && this.recoveredField146
            && var1 >= this.recoveredField105
            && var1 < this.recoveredField106;
         if (!var5) {
            this.recoveredMethod23(var1, true);
         }

         this.recoveredField18 = SlothRotation.AimPhase.RECOVER;
      }
   }

   @Override
   public void updateRotations(LivingEntity target) {
      if (mc.player != null && target != null) {
         long var2 = System.nanoTime();
         RotationStorage var4 = RotationStorage.instance;
         if (var4 != null && var4.currentPriority() > 1) {
            this.recoveredMethod20(var2, true);
            this.recoveredField136 = true;
         } else {
            if (this.recoveredField17 != target) {
               this.recoveredMethod1(target, var2);
            }

            if (this.recoveredField136) {
               this.recoveredMethod20(var2, true);
               this.recoveredField136 = false;
            } else {
               long var5 = this.recoveredField102 == 0L ? 4166667L : var2 - this.recoveredField102;
               if (var5 >= 500000L) {
                  if (var5 > 200000000L) {
                     this.recoveredMethod20(var2, true);
                     this.recoveredField18 = SlothRotation.AimPhase.ACQUIRE;
                  } else {
                     this.recoveredField102 = var2;
                     double var7 = Math.min(var5 / 1.0E9, 0.05);
                     boolean var9 = this.recoveredField135 || var2 < this.recoveredField118;
                     boolean var10 = !mc.player.isOnGround();
                     if (var10 && !this.recoveredField137 && mc.player.getVelocity().y > 0.035) {
                        this.recoveredMethod24(var2, var9);
                     }

                     if (var10 && this.recoveredField149 && mc.player.getVelocity().y < -0.07) {
                        this.recoveredField149 = false;
                        this.recoveredMethod23(var2, var9);
                     } else if (!var10) {
                        this.recoveredField149 = false;
                     }

                     this.recoveredField137 = var10;
                     this.recoveredMethod2(var2, var7, var9);
                     this.recoveredMethod4(target, var2, var7);
                     Vec3d var11 = this.recoveredMethod5(target, var7, var9);
                     Vec3d var12 = var11.subtract(mc.player.getEyePos());
                     float var13 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var12.z, var12.x)) - 90.0);
                     float var14 = (float)(-Math.toDegrees(Math.atan2(var12.y, var12.horizontalLength())));
                     float var15 = this.recoveredMethod52(target, var12.length());
                     float var16 = this.recoveredMethod53(target, var12.length());
                     float var17 = MathHelper.clamp(this.recoveredField150, -var15 * (var9 ? 0.272F : 0.85F), var15 * (var9 ? 0.272F : 0.85F));
                     float var18 = MathHelper.clamp(this.recoveredField151, -var16 * (var9 ? 0.224F : 0.72F), var16 * (var9 ? 0.224F : 0.72F));
                     float var19 = MathHelper.clamp(this.recoveredMethod25(var2), -var16 * (var9 ? 0.176F : 0.78F), var16 * (var9 ? 0.176F : 0.78F));
                     this.recoveredMethod31(var2, var9, var15, var16);
                     float var20 = this.recoveredMethod33(var2);
                     float var21 = (var9 ? 0.176F : (this.recoveredField18 == SlothRotation.AimPhase.ACQUIRE ? 0.35F : 1.0F)) * this.recoveredField79;
                     float var22 = MathHelper.clamp(var15 * 0.35F, 0.12F, 1.05F) * var21;
                     float var23 = MathHelper.clamp(var15 * 0.22F, 0.09F, 0.75F) * var21;
                     this.recoveredField71 = this.recoveredField71 * (float)Math.exp(-var7 / 0.075);
                     this.recoveredField72 = this.recoveredField72 * (float)Math.exp(-var7 / 0.09);
                     float var24 = var9 ? 0.52F : 1.0F;
                     var13 = MathHelper.wrapDegrees(
                        var13
                           + var17
                           + this.recoveredField67 * var22
                           + this.recoveredField73 * this.recoveredField77 * var24
                           + this.recoveredField88 * var20
                           + this.recoveredField71
                     );
                     var14 = MathHelper.clamp(
                        var14
                           + var18
                           + var19
                           + this.recoveredField68 * var23
                           + this.recoveredField74 * this.recoveredField77 * var24
                           + this.recoveredField89 * var20
                           + this.recoveredField72,
                        -89.0F,
                        89.0F
                     );
                     float var25 = MathHelper.wrapDegrees(var13 - this.recoveredField20);
                     float var26 = var14 - this.recoveredField21;
                     float var27 = (float)Math.hypot(var25, var26);
                     if (this.recoveredMethod12(var27, var9, var2 < this.recoveredField103)) {
                        this.recoveredMethod16(var13, var14, var7);
                     } else if (this.recoveredMethod17(var2, var9, var2 < this.recoveredField103)) {
                        this.recoveredMethod16(var13, var14, var7);
                     } else {
                        this.recoveredMethod8(var13, var14, var7);
                        if (var2 < this.recoveredField103 && !var9) {
                           this.recoveredField18 = SlothRotation.AimPhase.REACTION;
                           this.recoveredField22 = this.recoveredMethod59(this.recoveredField22, 0.0F, this.recoveredField98 * (float)var7);
                           this.recoveredField23 = this.recoveredMethod59(this.recoveredField23, 0.0F, this.recoveredField99 * (float)var7);
                        } else {
                           if (!var9) {
                              this.recoveredField18 = var27 > 10.0F ? SlothRotation.AimPhase.ACQUIRE : SlothRotation.AimPhase.TRACK;
                           } else {
                              this.recoveredField18 = var2 >= this.recoveredField118
                                    || !(Math.abs(this.recoveredField71) > 0.01F) && !(Math.abs(this.recoveredField72) > 0.01F)
                                 ? SlothRotation.AimPhase.FOCUS
                                 : SlothRotation.AimPhase.RECOVER;
                           }

                           this.recoveredMethod7(var25, var26, var7, var9, mc.player.isGliding());
                        }

                        float var28 = mc.player.age == this.recoveredField131 ? this.recoveredField134 : 1.0F;
                        this.recoveredMethod18(var9);
                        float var29 = this.recoveredField22 * (float)var7 * var28;
                        float var30 = this.recoveredField23 * (float)var7 * var28;
                        float var31 = var29 * this.recoveredField166;
                        float var32 = var30 * this.recoveredField167;
                        if (var31 * var25 > 0.0F && Math.abs(var31) > Math.abs(var25)) {
                           boolean var33 = Math.abs(var29) > Math.abs(var25);
                           if (Math.abs(this.recoveredField26) > 8.0F) {
                              var31 = var25 + MathHelper.clamp(this.recoveredField26 * (float)var7 * 0.58F, -0.45F, 0.45F);
                              if (var33) {
                                 this.recoveredField22 = this.recoveredField26 * 0.58F;
                                 this.recoveredField24 = 0.0F;
                              }
                           } else {
                              var31 = var25 * this.recoveredMethod56(0.82F, 0.98F);
                              if (var33) {
                                 this.recoveredField22 = 0.0F;
                                 this.recoveredField24 = 0.0F;
                              }
                           }
                        }

                        if (var32 * var26 > 0.0F && Math.abs(var32) > Math.abs(var26)) {
                           boolean var46 = Math.abs(var30) > Math.abs(var26);
                           if (Math.abs(this.recoveredField27) > 6.0F) {
                              var32 = var26 + MathHelper.clamp(this.recoveredField27 * (float)var7 * 0.52F, -0.32F, 0.32F);
                              if (var46) {
                                 this.recoveredField23 = this.recoveredField27 * 0.52F;
                                 this.recoveredField25 = 0.0F;
                              }
                           } else {
                              var32 = var26 * this.recoveredMethod56(0.82F, 0.98F);
                              if (var46) {
                                 this.recoveredField23 = 0.0F;
                                 this.recoveredField25 = 0.0F;
                              }
                           }
                        }

                        float var47;
                        if (this.recoveredField106 > this.recoveredField105) {
                           var47 = (float)this.recoveredMethod60(
                              (double)(var2 - this.recoveredField105) / (this.recoveredField106 - this.recoveredField105), 0.0, 1.0
                           );
                        } else {
                           var47 = 0.5F;
                        }

                        SlothPatternEasing.PatternOffset var34 = this.recoveredField175.advance(var7, var47);
                        var31 += var34.yawOffset();
                        var32 += var34.pitchOffset();
                        float var35 = MathHelper.clamp(0.85F + SlothPatternEasing.finalSpeed(mc.player, var47) * 0.35F, 0.85F, 1.35F);
                        var31 = SlothPatternEasing.clampAndScale(var31, 60.0F, var35);
                        var32 = SlothPatternEasing.clampAndScale(var32, 24.0F, var35);
                        float var36 = this.recoveredMethod56(62.0F, 70.0F);
                        float var37 = this.recoveredMethod56(9.0F, 11.0F);
                        var31 = MathHelper.clamp(var31, -var36, var36);
                        var32 = MathHelper.clamp(var32, -var37, var37);
                        if (Math.abs(var31) + Math.abs(var32) >= GCDUtil.getGCDValue()) {
                           this.recoveredField170 = 0;
                        }

                        this.recoveredField20 = MathHelper.wrapDegrees(this.recoveredField20 + var31);
                        this.recoveredField21 = MathHelper.clamp(this.recoveredField21 + var32, -89.0F, 89.0F);
                        if (Float.isFinite(this.recoveredField20) && Float.isFinite(this.recoveredField21)) {
                           this.recoveredMethod11(var2, var9);
                        } else {
                           this.recoveredMethod20(var2, true);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void recoveredMethod1(LivingEntity target, long now) {
      this.recoveredField17 = target;
      this.recoveredMethod20(now, true);
      this.recoveredField121 = now;
      this.recoveredField122 = 0L;
      this.recoveredField123 = 0L;
      this.recoveredField124 = 20150;
      this.recoveredField125 = 20150;
      this.recoveredField126 = Integer.MIN_VALUE;
      this.recoveredField128 = this.recoveredMethod13();
      this.recoveredField127 = this.recoveredField128 - 1;
      this.recoveredField129 = 0;
      this.recoveredField130 = 0;
      this.recoveredField131 = Integer.MIN_VALUE;
      this.recoveredField132 = Integer.MIN_VALUE;
      this.recoveredField133 = false;
      this.recoveredField134 = 1.0F;
      this.recoveredField162 = Integer.MIN_VALUE;
      this.recoveredField163 = 0;
      this.recoveredField164 = false;
      this.recoveredField165 = Integer.MIN_VALUE;
      this.recoveredField169 = Integer.MIN_VALUE;
      this.recoveredField170 = 0;
      this.recoveredField171 = target.getVelocity();
      this.recoveredField172 = 1.0F;
      this.recoveredField173 = 0.0F;
      this.recoveredField174 = 0L;
      this.recoveredField175.reset();
      this.recoveredField19 = target.getVelocity();
      this.recoveredField31 = this.recoveredField34 = this.recoveredMethod57(0.4, 0.6);
      this.recoveredField32 = this.recoveredField35 = this.recoveredMethod57(0.64, 0.82);
      this.recoveredField33 = this.recoveredField36 = this.recoveredMethod57(0.4, 0.6);
      this.recoveredField37 = this.recoveredField40 = this.recoveredField43 = this.recoveredField31;
      this.recoveredField38 = this.recoveredField41 = this.recoveredField44 = this.recoveredField32;
      this.recoveredField39 = this.recoveredField42 = this.recoveredField45 = this.recoveredField33;
      this.recoveredField46 = this.recoveredField49 = this.recoveredField31;
      this.recoveredField47 = this.recoveredField50 = this.recoveredField32;
      this.recoveredField48 = this.recoveredField51 = this.recoveredField33;
      this.recoveredMethod41();
      this.recoveredField67 = this.recoveredField68 = 0.0F;
      this.recoveredField69 = this.recoveredField70 = 0.0F;
      this.recoveredField71 = this.recoveredField72 = 0.0F;
      this.recoveredField73 = this.recoveredField74 = 0.0F;
      this.recoveredField75 = this.recoveredField76 = 0.0F;
      this.recoveredField77 = 0.0F;
      this.recoveredField78 = SlothDatasetProfile.pathNoiseScale(this.recoveredField12);
      this.recoveredField79 = 0.1F;
      this.recoveredField80 = this.recoveredField81 = SlothDatasetProfile.initialSpeedScale(this.recoveredField12);
      this.recoveredField82 = this.recoveredField83 = 1.0F;
      this.recoveredField84 = this.recoveredField85 = SlothDatasetProfile.perceptionScale(this.recoveredField12);
      this.recoveredField86 = this.recoveredField87 = SlothDatasetProfile.perceptionBias(this.recoveredField12);
      this.recoveredField88 = this.recoveredField89 = 0.0F;
      this.recoveredField90 = this.recoveredField91 = 1.0F;
      this.recoveredField92 = 0.28F;
      this.recoveredField93 = 2.4F;
      this.recoveredField64 = this.recoveredField65 = this.recoveredField66 = 0.0;
      this.recoveredField107 = now;
      this.recoveredField94 = SlothDatasetProfile.maximumYawSpeed(this.recoveredField12) * 2.0F;
      this.recoveredField95 = SlothDatasetProfile.maximumPitchSpeed(this.recoveredField12);
      this.recoveredField96 = SlothDatasetProfile.yawAcceleration(this.recoveredField12) * 1.35F;
      this.recoveredField97 = SlothDatasetProfile.pitchAcceleration(this.recoveredField12);
      this.recoveredField98 = SlothDatasetProfile.yawDeceleration(this.recoveredField12) * 1.35F;
      this.recoveredField99 = SlothDatasetProfile.pitchDeceleration(this.recoveredField12);
      this.recoveredField100 = SlothDatasetProfile.yawJerkLimit(this.recoveredField12) * 1.35F;
      this.recoveredField101 = SlothDatasetProfile.pitchJerkLimit(this.recoveredField12);
      float var4 = this.recoveredMethod21(target);
      long var5 = SlothDatasetProfile.reactionMillis(this.recoveredField12, var4, this.recoveredField135);
      this.recoveredField103 = now + var5 * 1000000L;
      this.recoveredMethod22(now, this.recoveredField135, false);
      this.recoveredField108 = now + this.recoveredMethod58(260L, 720L);
      this.recoveredField109 = now + this.recoveredMethod58(240L, 800L);
      this.recoveredField111 = now + this.recoveredMethod58(110L, 340L);
      this.recoveredField112 = now + this.recoveredMethod58(320L, 920L);
      this.recoveredField113 = now + this.recoveredMethod58(500L, 1300L);
      this.recoveredField116 = now + this.recoveredMethod58(1000L, 2800L);
      this.recoveredField137 = !mc.player.isOnGround();
      this.recoveredField18 = var5 > 0L ? SlothRotation.AimPhase.REACTION : SlothRotation.AimPhase.ACQUIRE;
   }

   private void recoveredMethod2(long now, double deltaTime, boolean precision) {
      this.recoveredMethod3(deltaTime);
      this.recoveredMethod28(now);
      if (now >= this.recoveredField104) {
         this.recoveredMethod22(now, precision, false);
         this.recoveredMethod28(now);
      }

      if (now >= this.recoveredField108) {
         float var6 = this.recoveredMethod54();
         this.recoveredField69 = MathHelper.clamp(this.recoveredMethod54() * 0.78F + var6 * 0.22F, -1.0F, 1.0F);
         this.recoveredField70 = MathHelper.clamp(this.recoveredMethod54() * 0.82F + var6 * 0.18F, -1.0F, 1.0F);
         this.recoveredField108 = now + this.recoveredMethod58(260L, 760L);
      }

      double var18 = 1.0 - Math.exp(-deltaTime / 0.38);
      double var8 = 1.0 - Math.exp(-deltaTime / 0.48);
      this.recoveredField67 = this.recoveredField67 + (this.recoveredField69 - this.recoveredField67) * (float)var18;
      this.recoveredField68 = this.recoveredField68 + (this.recoveredField70 - this.recoveredField68) * (float)var8;
      if (now >= this.recoveredField111) {
         this.recoveredField81 = SlothDatasetProfile.driftingSpeedScale(this.recoveredField12);
         this.recoveredField111 = now + this.recoveredMethod58(110L, 360L);
      }

      double var10 = 1.0 - Math.exp(-deltaTime / 0.16);
      this.recoveredField80 = this.recoveredField80 + (this.recoveredField81 - this.recoveredField80) * (float)var10;
      double var12 = 1.0 - Math.exp(-deltaTime / 0.09);
      this.recoveredField82 = this.recoveredField82 + (this.recoveredField83 - this.recoveredField82) * (float)var12;
      if (now >= this.recoveredField112) {
         this.recoveredField85 = SlothDatasetProfile.perceptionScale(this.recoveredField12);
         this.recoveredField87 = SlothDatasetProfile.perceptionBias(this.recoveredField12);
         this.recoveredField112 = now + this.recoveredMethod58(340L, 1150L);
      }

      double var14 = 1.0 - Math.exp(-deltaTime / 0.22);
      this.recoveredField84 = this.recoveredField84 + (this.recoveredField85 - this.recoveredField84) * (float)var14;
      this.recoveredField86 = this.recoveredField86 + (this.recoveredField87 - this.recoveredField86) * (float)var14;
      if (precision) {
         this.recoveredField91 = 1.0F;
         if (this.recoveredField117 > 0L) {
            this.recoveredField117 = 0L;
            this.recoveredField116 = now + this.recoveredMethod58(700L, 2400L);
         }
      } else if (this.recoveredField117 > 0L && now >= this.recoveredField117) {
         this.recoveredField91 = 1.0F;
         this.recoveredField117 = 0L;
         this.recoveredField116 = now + this.recoveredMethod58(700L, 2400L);
      } else if (this.recoveredField117 == 0L && now >= this.recoveredField116) {
         this.recoveredField91 = this.recoveredMethod56(0.78F, 0.94F);
         this.recoveredField117 = now + this.recoveredMethod58(15L, 54L);
      }

      double var16 = 1.0 - Math.exp(-deltaTime / (this.recoveredField91 < this.recoveredField90 ? 0.022 : 0.045));
      this.recoveredField90 = this.recoveredField90 + (this.recoveredField91 - this.recoveredField90) * (float)var16;
      this.recoveredMethod29(now, deltaTime);
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
      this.recoveredField160 = this.recoveredField160 + (var3 - this.recoveredField160) * var6;
   }

   private void recoveredMethod4(LivingEntity target, long now, double deltaTime) {
      Vec3d var6 = target.getVelocity();
      double var7 = var6.horizontalLength();
      double var9 = this.recoveredField171.horizontalLength();
      if (var7 > 0.08 && var9 > 0.08) {
         double var11 = (var6.x * this.recoveredField171.x + var6.z * this.recoveredField171.z) / (var7 * var9);
         long var13 = this.recoveredField173 > 0.6F ? 110000000L : 250000000L;
         if (var11 < 0.5 && now - this.recoveredField174 > var13) {
            this.recoveredField174 = now;
            this.recoveredField173 = Math.min(1.0F, this.recoveredField173 + 0.45F);
            if (this.recoveredField12.nextDouble() < 0.85) {
               this.recoveredField172 = Math.max(this.recoveredField172, this.recoveredMethod56(1.28F, 1.68F));
            }
         }
      }

      this.recoveredField171 = var6;
      float var15 = (float)(1.0 - Math.exp(-deltaTime / 0.3));
      this.recoveredField172 = this.recoveredField172 + (1.0F - this.recoveredField172) * var15;
      float var12 = (float)(1.0 - Math.exp(-deltaTime / 1.1));
      this.recoveredField173 = this.recoveredField173 + (0.0F - this.recoveredField173) * var12;
      if (this.recoveredField173 > 0.35F) {
         this.recoveredField172 = Math.max(this.recoveredField172, 1.0F + 0.28F * this.recoveredField173);
      }
   }

   private Vec3d recoveredMethod5(LivingEntity target, double deltaTime, boolean precision) {
      Vec3d var5 = target.getVelocity();
      if (var5.lengthSquared() > 2.25) {
         var5 = var5.normalize().multiply(1.5);
      }

      double var6 = var5.subtract(this.recoveredField19).horizontalLength();
      double var8 = this.recoveredMethod60(0.12 - var6 * 0.35, 0.045, 0.12);
      double var10 = 1.0 - Math.exp(-deltaTime / var8);
      this.recoveredField19 = this.recoveredField19.lerp(var5, var10);
      Box var12 = target.getBoundingBox();
      Vec3d var13 = new Vec3d(
         var12.minX + var12.getLengthX() * this.recoveredField31,
         var12.minY + var12.getLengthY() * this.recoveredField32,
         var12.minZ + var12.getLengthZ() * this.recoveredField33
      );
      double var14 = mc.player.getEyePos().distanceTo(var13);
      double var16;
      if (precision) {
         var16 = this.recoveredMethod60(
            (0.16 + var14 * 0.025 + this.recoveredField19.horizontalLength() * 0.09) * this.recoveredField84 + this.recoveredField86 * 0.18, 0.1, 0.42
         );
      } else if (!mc.player.isGliding() && !target.isGliding()) {
         var16 = this.recoveredMethod60((0.52 + var14 * 0.09) * this.recoveredField84 + this.recoveredField86, -0.2, 1.35);
      } else {
         var16 = this.recoveredMethod60((0.95 + var14 * 0.08) * this.recoveredField84 + this.recoveredField86, 0.15, 1.85);
      }

      if (this.recoveredField173 > 0.2F) {
         var16 *= 1.0 - 0.45 * this.recoveredField173;
      }

      Vec3d var18 = this.recoveredField19.multiply(var16);
      Vec3d var19 = var13.add(var18);
      if (!precision) {
         return var19;
      }

      double var20 = Math.min(var12.getLengthX(), var12.getLengthZ()) * 0.12;
      double var22 = var12.getLengthY() * 0.1;
      double var24 = Math.min(
         this.recoveredMethod6(var13.x, var18.x, var12.minX + var20, var12.maxX - var20),
         Math.min(
            this.recoveredMethod6(var13.y, var18.y, var12.minY + var22, var12.maxY - var22),
            this.recoveredMethod6(var13.z, var18.z, var12.minZ + var20, var12.maxZ - var20)
         )
      );
      return var13.add(var18.multiply(var24));
   }

   private double recoveredMethod6(double value, double delta, double minimum, double maximum) {
      if (Math.abs(delta) < 1.0E-8) {
         return 1.0;
      }

      double var9 = delta > 0.0 ? (maximum - value) / delta : (minimum - value) / delta;
      return this.recoveredMethod60(var9, 0.0, 1.0);
   }

   private void recoveredMethod7(float yawError, float pitchError, double deltaTime, boolean precision, boolean flying) {
      float var7 = this.recoveredField80
         * this.recoveredField82
         * this.recoveredField90
         * this.recoveredField160
         * this.recoveredField172
         * (flying ? 1.12F : 1.0F);
      if (precision) {
         var7 *= 1.272F;
      }

      float var8 = Math.abs(yawError);
      float var9 = Math.abs(pitchError);
      float var10 = flying ? 1400.0F : (var8 < 18.0F ? 800.0F : (var8 < 65.0F ? 1150.0F : 1400.0F));
      float var11 = flying ? 520.0F : (var9 < 8.0F ? 230.0F : (var9 < 24.0F ? 330.0F : 430.0F));
      float var12 = this.recoveredField176;
      float var13 = Math.min(var10, this.recoveredField94 * var7 * var12);
      float var14 = Math.min(var11, this.recoveredField95 * var7 * var12);
      float var15 = 0.22F + 0.78F * (float)Math.exp(-Math.abs(yawError) / 28.0F);
      float var16 = 0.2F + 0.8F * (float)Math.exp(-Math.abs(pitchError) / 16.0F);
      float var17 = this.recoveredMethod10(yawError, this.recoveredField98, var13) + this.recoveredField26 * (precision ? 0.92F : 0.78F) * var15;
      float var18 = this.recoveredMethod10(pitchError, this.recoveredField99, var14) + this.recoveredField27 * (precision ? 0.82F : 0.66F) * var16;
      var17 = MathHelper.clamp(var17, -var13, var13);
      var18 = MathHelper.clamp(var18, -var14, var14);
      float var19 = 1.0F + MathHelper.clamp((var8 - 30.0F) / 80.0F, 0.0F, 0.55F);
      float var20 = 1.0F + MathHelper.clamp((var9 - 18.0F) / 42.0F, 0.0F, 0.3F);
      boolean var21 = this.recoveredField22 * yawError < 0.0F && Math.abs(this.recoveredField22) > 12.0F;
      float var22 = var21 ? 1.55F : 1.0F;
      float var23 = (this.recoveredMethod9(this.recoveredField22, var17) ? this.recoveredField98 : this.recoveredField96) * var19 * var22;
      float var24 = (this.recoveredMethod9(this.recoveredField23, var18) ? this.recoveredField99 : this.recoveredField97) * var20;
      float var25 = precision ? 1.3440001F : 1.0F;
      float var26 = precision ? 1.32F : 1.0F;
      float var27 = MathHelper.clamp((var17 - this.recoveredField22) / (float)deltaTime, -var23 * var25, var23 * var25);
      float var28 = MathHelper.clamp((var18 - this.recoveredField23) / (float)deltaTime, -var24 * var26, var24 * var26);
      this.recoveredField24 = this.recoveredMethod59(
         this.recoveredField24, var27, this.recoveredField100 * var25 * var22 * (float)Math.sqrt(var19) * (float)deltaTime
      );
      this.recoveredField25 = this.recoveredMethod59(this.recoveredField25, var28, this.recoveredField101 * var26 * (float)Math.sqrt(var20) * (float)deltaTime);
      float var29 = this.recoveredField22 + this.recoveredField24 * (float)deltaTime;
      float var30 = this.recoveredField23 + this.recoveredField25 * (float)deltaTime;
      if ((var17 - this.recoveredField22) * (var17 - var29) <= 0.0F) {
         var29 = var17;
         this.recoveredField24 *= 0.22F;
      }

      if ((var18 - this.recoveredField23) * (var18 - var30) <= 0.0F) {
         var30 = var18;
         this.recoveredField25 *= 0.22F;
      }

      this.recoveredField22 = MathHelper.clamp(var29, -var13, var13);
      this.recoveredField23 = MathHelper.clamp(var30, -var14, var14);
   }

   private void recoveredMethod8(float wantedYaw, float wantedPitch, double deltaTime) {
      if (!this.recoveredField30) {
         this.recoveredField28 = wantedYaw;
         this.recoveredField29 = wantedPitch;
         this.recoveredField26 = 0.0F;
         this.recoveredField27 = 0.0F;
         this.recoveredField30 = true;
      } else {
         float var5 = MathHelper.wrapDegrees(wantedYaw - this.recoveredField28) / (float)deltaTime;
         float var6 = (wantedPitch - this.recoveredField29) / (float)deltaTime;
         var5 = MathHelper.clamp(var5, -1100.0F, 1100.0F);
         var6 = MathHelper.clamp(var6, -620.0F, 620.0F);
         float var7 = (float)(1.0 - Math.exp(-deltaTime / 0.055));
         this.recoveredField26 = this.recoveredField26 + (var5 - this.recoveredField26) * var7;
         this.recoveredField27 = this.recoveredField27 + (var6 - this.recoveredField27) * var7;
         this.recoveredField28 = wantedYaw;
         this.recoveredField29 = wantedPitch;
      }
   }

   private boolean recoveredMethod9(float currentVelocity, float desiredVelocity) {
      return Math.abs(currentVelocity) < 0.001F ? false : currentVelocity * desiredVelocity <= 0.0F || Math.abs(desiredVelocity) < Math.abs(currentVelocity);
   }

   private float recoveredMethod10(float error, float deceleration, float maximumSpeed) {
      if (Math.abs(error) < 0.001F) {
         return 0.0F;
      }

      float var4 = (float)Math.sqrt(2.0F * deceleration * Math.abs(error));
      return Math.copySign(Math.min(maximumSpeed, var4), error);
   }

   private void recoveredMethod11(long now, boolean precision) {
      boolean var4 = precision && mc.player.age != this.recoveredField132;
      boolean var5 = var4 || mc.player.age == this.recoveredField131;
      long var6 = this.recoveredField123 == 0L ? Long.MAX_VALUE : now - this.recoveredField123;
      this.recoveredField123 = now;
      long var8 = this.recoveredField122 == 0L ? Long.MAX_VALUE : now - this.recoveredField122;
      if (var5 || var6 >= 12500000L || now >= this.recoveredField121) {
         if (this.recoveredField122 != 0L) {
            int var10 = (int)this.recoveredMethod60(var8 / 1000.0, 12500.0, 150000.0);
            this.recoveredField125 = this.recoveredField124;
            this.recoveredField124 = var10;
         }

         int var20 = SlothDatasetProfile.mouseReportIntervalMicros(this.recoveredField12);
         double var11 = 20150.0
            - (this.recoveredField124 - 20150) * 0.19
            + (this.recoveredField125 - 20150) * 0.275
            + (var20 - 20150) * 0.95
            + this.recoveredMethod57(-260.0, 260.0);
         int var13 = (int)this.recoveredMethod60(var11, 11500.0, 75000.0);
         double var14 = var6 < 12500000L ? var6 / 2000.0 : 0.0;
         long var16 = (long)Math.max(8000.0, var13 - var14);
         this.recoveredField122 = now;
         this.recoveredField121 = now + var16 * 1000L;
         if (precision) {
            this.recoveredField132 = mc.player.age;
         }

         RotationStorage.update(new Rotation(this.recoveredField20, this.recoveredField21), 360.0F, 360.0F, 30.0F, 22.0F, 2, 1, this.recoveredField161);
         float var18 = Math.abs(MathHelper.wrapDegrees(this.recoveredField20 - mc.player.getYaw()));
         float var19 = Math.abs(this.recoveredField21 - mc.player.getPitch());
         if (var18 > 4.0F || var19 > 4.0F) {
            this.recoveredMethod20(now, true);
         }
      }
   }

   private boolean recoveredMethod12(float totalError, boolean precision, boolean reaction) {
      int var4 = mc.player.age;
      if (var4 == this.recoveredField126) {
         return this.recoveredField133;
      }

      this.recoveredField126 = var4;
      if (this.recoveredField170 >= 6) {
         boolean var12 = this.recoveredField133;
         this.recoveredField133 = false;
         this.recoveredField128 = Math.max(3, this.recoveredMethod13());
         this.recoveredField127 = this.recoveredField128 - 1;
         this.recoveredField131 = var12 ? var4 : Integer.MIN_VALUE;
         this.recoveredField134 = 1.0F;
         if (var12) {
            this.recoveredMethod15();
         }

         return false;
      } else {
         float var5 = (float)Math.hypot(this.recoveredField26, this.recoveredField27);
         float var6 = (float)Math.hypot(this.recoveredField22, this.recoveredField23);
         float var7 = (float)Math.hypot(MathHelper.wrapDegrees(this.recoveredField20 - mc.player.getYaw()), this.recoveredField21 - mc.player.getPitch());
         boolean var8 = !precision && !reaction && totalError < 1.6F && var5 < 38.0F && var6 < 50.0F && var7 < 0.6F;
         boolean var9 = precision && !reaction && totalError < 0.48F && var5 < 18.0F && var6 < 26.0F && var7 < 0.22F;
         boolean var10 = var8 || var9;
         if (!var10) {
            boolean var11 = this.recoveredField133;
            this.recoveredField133 = false;
            this.recoveredField128 = Math.max(3, this.recoveredMethod13());
            this.recoveredField127 = this.recoveredField128 - 1;
            this.recoveredField131 = var11 ? var4 : Integer.MIN_VALUE;
            this.recoveredField134 = var11 ? (precision ? this.recoveredMethod56(0.76F, 0.9F) : this.recoveredMethod56(0.58F, 0.82F)) : 1.0F;
            if (var11) {
               this.recoveredMethod15();
            }

            return false;
         } else {
            if (precision && !this.recoveredField133 && this.recoveredField127 > 1) {
               this.recoveredField127 = this.recoveredField12.nextBoolean() ? 0 : 1;
            }

            if (this.recoveredField127 > 0) {
               this.recoveredField127--;
               return this.recoveredField133;
            }

            if (this.recoveredField133) {
               this.recoveredField129 = this.recoveredField128;
               this.recoveredField133 = false;
               this.recoveredField128 = this.recoveredMethod13();
               if (this.recoveredField129 == 1 && this.recoveredField130 == 1 && this.recoveredField128 == 1) {
                  this.recoveredField128 = 2;
               }

               this.recoveredField127 = this.recoveredField128 - 1;
               this.recoveredField131 = var4;
               this.recoveredField134 = precision ? this.recoveredMethod56(0.82F, 0.95F) : this.recoveredMethod56(0.58F, 0.82F);
               this.recoveredMethod15();
               return false;
            } else {
               this.recoveredField130 = this.recoveredField128;
               this.recoveredField133 = true;
               this.recoveredField128 = precision ? 1 : this.recoveredMethod14();
               if (this.recoveredField129 == 1 && this.recoveredField130 == 1 && this.recoveredField128 == 1) {
                  this.recoveredField128 = 2;
               }

               this.recoveredField127 = this.recoveredField128 - 1;
               return true;
            }
         }
      }
   }

   private int recoveredMethod13() {
      int var1 = (int)Math.round(Math.exp(1.05 + this.recoveredField12.nextGaussian() * 0.52));
      if (this.recoveredField12.nextDouble() < 0.08) {
         var1 += this.recoveredField12.nextInt(4, 13);
      }

      return MathHelper.clamp(var1, 2, 18);
   }

   private int recoveredMethod14() {
      double var1 = this.recoveredField12.nextDouble();
      return var1 < 0.55 ? 1 : (var1 < 0.9 ? 2 : 3);
   }

   private void recoveredMethod15() {
      this.recoveredField121 = 0L;
      this.recoveredField122 = 0L;
      this.recoveredField123 = 0L;
   }

   private void recoveredMethod16(float wantedYaw, float wantedPitch, double deltaTime) {
      if (mc.player.age != this.recoveredField169) {
         this.recoveredField169 = mc.player.age;
         this.recoveredField170++;
      }

      this.recoveredField28 = wantedYaw;
      this.recoveredField29 = wantedPitch;
      this.recoveredField30 = true;
      this.recoveredField22 = this.recoveredMethod59(this.recoveredField22, 0.0F, this.recoveredField98 * (float)deltaTime);
      this.recoveredField23 = this.recoveredMethod59(this.recoveredField23, 0.0F, this.recoveredField99 * (float)deltaTime);
      this.recoveredField24 = this.recoveredMethod59(this.recoveredField24, 0.0F, this.recoveredField100 * (float)deltaTime);
      this.recoveredField25 = this.recoveredMethod59(this.recoveredField25, 0.0F, this.recoveredField101 * (float)deltaTime);
      this.recoveredMethod19();
   }

   private boolean recoveredMethod17(long now, boolean precision, boolean reaction) {
      int var5 = mc.player.age;
      if (var5 == this.recoveredField162) {
         return this.recoveredField164;
      } else {
         this.recoveredField162 = var5;
         if (this.recoveredField170 >= 6) {
            this.recoveredField163 = 0;
            this.recoveredField164 = false;
            return false;
         } else if (this.recoveredField163 > 0) {
            this.recoveredField163--;
            this.recoveredField164 = true;
            return true;
         } else {
            boolean var6 = this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL && this.recoveredField147
               || this.recoveredField159 != 0.0F && now >= this.recoveredField157 && now < this.recoveredField158;
            if (!precision && !reaction && !var6 && this.recoveredField172 < 1.15F && this.recoveredField12.nextDouble() < 0.09) {
               this.recoveredField163 = this.recoveredField12.nextDouble() < 0.75 ? 0 : 1;
               this.recoveredField164 = true;
               return true;
            } else {
               this.recoveredField164 = false;
               return false;
            }
         }
      }
   }

   private void recoveredMethod18(boolean precision) {
      int var2 = mc.player.age;
      if (var2 != this.recoveredField165) {
         this.recoveredField165 = var2;
         float var3 = precision ? 0.16F : 0.55F;
         this.recoveredField166 = MathHelper.clamp((float)Math.exp(this.recoveredField12.nextGaussian() * var3), 0.35F, 2.3F);
         this.recoveredField167 = MathHelper.clamp((float)Math.exp(this.recoveredField12.nextGaussian() * var3 * 0.9F), 0.35F, 2.15F);
      }
   }

   private void recoveredMethod19() {
      RotationStorage.update(new Rotation(mc.player.getYaw(), mc.player.getPitch()), 360.0F, 360.0F, 30.0F, 22.0F, 2, 1, this.recoveredField161);
   }

   private void recoveredMethod20(long now, boolean clearVelocity) {
      if (mc.player != null) {
         this.recoveredField20 = MathHelper.wrapDegrees(mc.player.getYaw());
         this.recoveredField21 = MathHelper.clamp(mc.player.getPitch(), -89.0F, 89.0F);
      }

      if (clearVelocity) {
         this.recoveredField22 = 0.0F;
         this.recoveredField23 = 0.0F;
         this.recoveredField24 = 0.0F;
         this.recoveredField25 = 0.0F;
         this.recoveredField26 = 0.0F;
         this.recoveredField27 = 0.0F;
         this.recoveredField30 = false;
         this.recoveredField150 = 0.0F;
         this.recoveredField151 = 0.0F;
         this.recoveredField152 = 0.0F;
         this.recoveredField153 = 0.0F;
         this.recoveredField71 = 0.0F;
         this.recoveredField72 = 0.0F;
      }

      this.recoveredField102 = now;
   }

   private float recoveredMethod21(LivingEntity target) {
      Vec3d var2 = target.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
      float var3 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
      float var4 = (float)(-Math.toDegrees(Math.atan2(var2.y, var2.horizontalLength())));
      return Math.abs(MathHelper.wrapDegrees(var3 - mc.player.getYaw())) + Math.abs(var4 - mc.player.getPitch());
   }

   private void recoveredMethod22(long now, boolean precision, boolean action) {
      boolean var5 = this.recoveredField148;
      this.recoveredField148 = false;
      this.recoveredField152 = this.recoveredField150;
      this.recoveredField153 = this.recoveredField151;
      this.recoveredField37 = this.recoveredField31;
      this.recoveredField38 = this.recoveredField32;
      this.recoveredField39 = this.recoveredField33;
      Vec3d var6 = this.recoveredMethod35(precision);
      this.recoveredField34 = var6.x;
      this.recoveredField35 = var6.y;
      this.recoveredField36 = var6.z;
      if (var5 && this.recoveredField12.nextDouble() < 0.6) {
         this.recoveredField143 = SlothRotation.PathModel.EMPIRICAL;
         if (this.recoveredField144 == SlothRotation.PathModel.EMPIRICAL) {
            this.recoveredField142++;
         } else {
            this.recoveredField144 = SlothRotation.PathModel.EMPIRICAL;
            this.recoveredField142 = 1;
         }
      } else {
         this.recoveredField143 = this.recoveredMethod34(action);
      }

      if (this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL) {
         this.recoveredField146 = action;
         this.recoveredField147 = var5;
         this.recoveredField145 = var5 ? SlothDatasetProfile.chooseJump(this.recoveredField12) : SlothDatasetProfile.choose(this.recoveredField12, action);
      } else {
         this.recoveredField146 = false;
         this.recoveredField147 = false;
      }

      this.recoveredField55 = this.recoveredMethod57(-0.16, 0.2);
      this.recoveredField56 = this.recoveredMethod57(1.82, 2.48);
      this.recoveredField57 = this.recoveredMethod57(1.78, 2.42);
      this.recoveredField58 = this.recoveredMethod57(2.45, 3.25);
      this.recoveredField78 = SlothDatasetProfile.pathNoiseScale(this.recoveredField12);
      double var7 = SlothDatasetProfile.pathCurve(this.recoveredField12, precision);
      this.recoveredField52 = this.recoveredMethod51(var7);
      this.recoveredField53 = this.recoveredMethod51(var7 * 0.78);
      this.recoveredField54 = this.recoveredMethod51(var7);
      if (this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL) {
         this.recoveredField52 = 0.0;
         this.recoveredField53 = 0.0;
         this.recoveredField54 = 0.0;
         this.recoveredMethod26();
      } else {
         this.recoveredField154 = 0.0F;
         this.recoveredField155 = 0.0F;
         this.recoveredField156 = 0.0F;
      }

      this.recoveredField40 = this.recoveredMethod60(
         this.recoveredMethod50(this.recoveredField37, this.recoveredField34, 0.28) + this.recoveredMethod51(var7), 0.14, 0.86
      );
      this.recoveredField41 = this.recoveredMethod60(
         this.recoveredMethod50(this.recoveredField38, this.recoveredField35, 0.28) + this.recoveredMethod51(var7), 0.24, 0.9
      );
      this.recoveredField42 = this.recoveredMethod60(
         this.recoveredMethod50(this.recoveredField39, this.recoveredField36, 0.28) + this.recoveredMethod51(var7), 0.14, 0.86
      );
      double var9 = this.recoveredMethod57(0.36, 0.67);
      this.recoveredField46 = this.recoveredMethod60(
         this.recoveredMethod50(this.recoveredField37, this.recoveredField34, var9) + this.recoveredField52, 0.14, 0.86
      );
      this.recoveredField47 = this.recoveredMethod60(
         this.recoveredMethod50(this.recoveredField38, this.recoveredField35, var9) + this.recoveredField53, 0.24, 0.9
      );
      this.recoveredField48 = this.recoveredMethod60(
         this.recoveredMethod50(this.recoveredField39, this.recoveredField36, var9) + this.recoveredField54, 0.14, 0.86
      );
      this.recoveredField43 = this.recoveredMethod60(this.recoveredField46 + this.recoveredField46 - this.recoveredField40, 0.14, 0.86);
      this.recoveredField44 = this.recoveredMethod60(this.recoveredField47 + this.recoveredField47 - this.recoveredField41, 0.24, 0.9);
      this.recoveredField45 = this.recoveredMethod60(this.recoveredField48 + this.recoveredField48 - this.recoveredField42, 0.14, 0.86);
      double var11 = this.recoveredField143 == SlothRotation.PathModel.FLICK_SETTLE
         ? this.recoveredMethod57(0.025, 0.085)
         : this.recoveredMethod57(0.008, 0.045);
      this.recoveredField49 = this.recoveredMethod60(
         this.recoveredField34 + (this.recoveredField34 - this.recoveredField37) * var11 + this.recoveredMethod51(var7 * 0.32), 0.16, 0.84
      );
      this.recoveredField50 = this.recoveredMethod60(
         this.recoveredField35 + (this.recoveredField35 - this.recoveredField38) * var11 + this.recoveredMethod51(var7 * 0.22), 0.27, 0.88
      );
      this.recoveredField51 = this.recoveredMethod60(
         this.recoveredField36 + (this.recoveredField36 - this.recoveredField39) * var11 + this.recoveredMethod51(var7 * 0.32), 0.16, 0.84
      );
      if (this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL) {
         this.recoveredField59 = 0.75;
         this.recoveredField60 = 0.58;
         this.recoveredField83 = this.recoveredMethod56(0.9F, 1.12F);
      } else if (this.recoveredField143 == SlothRotation.PathModel.FLICK_SETTLE) {
         this.recoveredField59 = this.recoveredMethod57(0.42, 0.6);
         this.recoveredField60 = this.recoveredMethod57(0.31, 0.48);
         this.recoveredField83 = this.recoveredMethod56(1.3F, 1.72F);
      } else if (this.recoveredField143 == SlothRotation.PathModel.CORRECTIVE) {
         this.recoveredField59 = this.recoveredMethod57(0.7, 0.86);
         this.recoveredField60 = this.recoveredMethod57(0.52, 0.69);
         this.recoveredField83 = this.recoveredMethod56(0.78F, 1.24F);
      } else {
         this.recoveredField59 = 0.75;
         this.recoveredField60 = 0.58;
         this.recoveredField83 = this.recoveredMethod56(0.74F, 1.3F);
      }

      double var13 = this.recoveredMethod43(
         this.recoveredField37, this.recoveredField38, this.recoveredField39, this.recoveredField34, this.recoveredField35, this.recoveredField36
      );
      double var15 = this.recoveredMethod60(0.72 + var13 * 0.9, 0.78, 1.28);
      long var17;
      if (this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL) {
         long var19 = this.recoveredField147
            ? SlothDatasetProfile.jumpDuration(this.recoveredField145)
            : SlothDatasetProfile.duration(this.recoveredField146, this.recoveredField145);
         var17 = (long)(var19 * var15 * this.recoveredMethod57(0.9, 1.12));
      } else {
         long var21;
         long var25;
         if (this.recoveredField143 == SlothRotation.PathModel.FLICK_SETTLE) {
            var25 = 85L;
            var21 = 190L;
         } else if (this.recoveredField143 == SlothRotation.PathModel.RANDOM_SPLINE) {
            var25 = 170L;
            var21 = 390L;
         } else if (this.recoveredField143 == SlothRotation.PathModel.CORRECTIVE) {
            var25 = 145L;
            var21 = 350L;
         } else {
            var25 = 110L;
            var21 = 290L;
         }

         var17 = (long)(this.recoveredMethod57(var25, var21) * var15);
      }

      if (precision && this.recoveredField143 != SlothRotation.PathModel.EMPIRICAL) {
         var17 = Math.min(var17, action ? 245L : 310L);
      }

      double var26 = var17 / 1000.0;
      this.recoveredField61 = this.recoveredMethod60(this.recoveredField64 * var26 * 0.24, -0.12, 0.12);
      this.recoveredField62 = this.recoveredMethod60(this.recoveredField65 * var26 * 0.24, -0.09, 0.09);
      this.recoveredField63 = this.recoveredMethod60(this.recoveredField66 * var26 * 0.24, -0.12, 0.12);
      this.recoveredField105 = now;
      this.recoveredField106 = now + var17 * 1000000L;
      double var27 = this.recoveredField147 ? 1.0 : (precision ? (action ? 0.9 : 0.78) : (action ? 0.93 : 0.84));
      if (this.recoveredField12.nextDouble() < var27) {
         double var23 = this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL
            ? (
               this.recoveredField147
                  ? this.recoveredMethod57(0.92, 0.985)
                  : (action ? this.recoveredMethod57(0.78, 0.94) : this.recoveredMethod57(0.76, 0.93))
            )
            : (action ? this.recoveredMethod57(0.55, 0.84) : this.recoveredMethod57(0.62, 0.89));
         this.recoveredField104 = this.recoveredField105 + (long)((this.recoveredField106 - this.recoveredField105) * var23);
      } else {
         long var28 = this.recoveredField12.nextDouble() < 0.08
            ? this.recoveredMethod58(140L, 620L)
            : (precision ? this.recoveredMethod58(17L, 88L) : this.recoveredMethod58(20L, 125L));
         this.recoveredField104 = this.recoveredField106 + var28;
      }
   }

   private void recoveredMethod23(long now, boolean precision) {
      if (now - this.recoveredField120 >= 30000000L) {
         this.recoveredField120 = now;
         this.recoveredMethod28(now);
         this.recoveredMethod22(now, precision, true);
      }
   }

   private void recoveredMethod24(long now, boolean precision) {
      this.recoveredMethod28(now);
      this.recoveredField148 = true;
      this.recoveredField149 = true;
      this.recoveredMethod22(now, precision, false);
      this.recoveredField157 = now;
      this.recoveredField158 = now + this.recoveredMethod58(435L, 525L);
      double var4 = this.recoveredField12.nextDouble();
      if (var4 < 0.52) {
         this.recoveredField159 = this.recoveredMethod56(6.0F, 18.0F);
      } else if (var4 < 0.75) {
         this.recoveredField159 = this.recoveredMethod56(1.5F, 6.0F);
      } else if (var4 < 0.9) {
         this.recoveredField159 = 0.0F;
      } else {
         this.recoveredField159 = -this.recoveredMethod56(1.5F, 6.0F);
      }
   }

   private float recoveredMethod25(long now) {
      if (this.recoveredField158 > this.recoveredField157 && now > this.recoveredField157 && now < this.recoveredField158 && this.recoveredField159 != 0.0F) {
         double var3 = (double)(now - this.recoveredField157) / (this.recoveredField158 - this.recoveredField157);
         double var5;
         if (var3 < 0.2) {
            var5 = this.recoveredMethod48(var3 / 0.2) * 0.58;
         } else if (var3 < 0.44) {
            var5 = this.recoveredMethod50(0.58, 1.0, this.recoveredMethod48((var3 - 0.2) / 0.24));
         } else if (var3 < 0.63) {
            var5 = this.recoveredMethod50(1.0, 0.84, this.recoveredMethod48((var3 - 0.44) / 0.19));
         } else if (var3 < 0.92) {
            var5 = this.recoveredMethod50(0.84, 0.0, this.recoveredMethod48((var3 - 0.63) / 0.29));
         } else {
            var5 = 0.0;
         }

         return -this.recoveredField159 * (float)var5;
      } else {
         return 0.0F;
      }
   }

   private void recoveredMethod26() {
      Vec3d var1 = this.recoveredMethod27(this.recoveredField37, this.recoveredField38, this.recoveredField39);
      Vec3d var2 = this.recoveredMethod27(this.recoveredField34, this.recoveredField35, this.recoveredField36);
      float var3 = MathHelper.wrapDegrees((float)(var2.x - var1.x));
      float var4 = (float)(var2.y - var1.y);
      float var5 = (float)Math.hypot(var3, var4);
      if (var5 < 0.001F) {
         this.recoveredField154 = 0.0F;
         this.recoveredField155 = 0.0F;
         this.recoveredField156 = 0.0F;
      } else {
         this.recoveredField154 = -var4 / var5;
         this.recoveredField155 = var3 / var5;
         this.recoveredField156 = Math.min(var5, 18.0F);
      }
   }

   private Vec3d recoveredMethod27(double x, double y, double z) {
      if (this.recoveredField17 != null && mc.player != null) {
         Box var7 = this.recoveredField17.getBoundingBox();
         Vec3d var8 = new Vec3d(var7.minX + var7.getLengthX() * x, var7.minY + var7.getLengthY() * y, var7.minZ + var7.getLengthZ() * z);
         Vec3d var9 = var8.subtract(mc.player.getEyePos());
         double var10 = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var9.z, var9.x)) - 90.0);
         double var12 = -Math.toDegrees(Math.atan2(var9.y, var9.horizontalLength()));
         return new Vec3d(var10, var12, 0.0);
      } else {
         return Vec3d.ZERO;
      }
   }

   private void recoveredMethod28(long now) {
      if (this.recoveredField106 > this.recoveredField105) {
         double var3 = this.recoveredMethod60((double)(now - this.recoveredField105) / (this.recoveredField106 - this.recoveredField105), 0.0, 1.0);
         double var5 = this.recoveredField31;
         double var7 = this.recoveredField32;
         double var9 = this.recoveredField33;
         double var11 = this.recoveredMethod60(var3 + this.recoveredField55 * var3 * (1.0 - var3), 0.0, 1.0);
         if (this.recoveredField143 == SlothRotation.PathModel.EMPIRICAL) {
            double var13 = this.recoveredField147 ? this.recoveredMethod50(var3, var11, 0.38) : var11;
            double var15 = this.recoveredField147
               ? SlothDatasetProfile.jumpProgress(this.recoveredField145, var13)
               : SlothDatasetProfile.progress(this.recoveredField146, this.recoveredField145, var13);
            this.recoveredField31 = this.recoveredMethod50(this.recoveredField37, this.recoveredField34, var15);
            this.recoveredField32 = this.recoveredMethod50(this.recoveredField38, this.recoveredField35, var15);
            this.recoveredField33 = this.recoveredMethod50(this.recoveredField39, this.recoveredField36, var15);
            double var17 = this.recoveredField147
               ? SlothDatasetProfile.jumpLateral(this.recoveredField145, var13)
               : SlothDatasetProfile.lateral(this.recoveredField146, this.recoveredField145, var13);
            float var19 = (float)(1.0 - this.recoveredMethod48(var13));
            this.recoveredField150 = this.recoveredField152 * var19 + this.recoveredField154 * this.recoveredField156 * (float)var17;
            this.recoveredField151 = this.recoveredField153 * var19 + this.recoveredField155 * this.recoveredField156 * (float)var17;
         } else if (this.recoveredField143 == SlothRotation.PathModel.RANDOM_SPLINE) {
            double var20 = this.recoveredMethod46(var11);
            if (var20 < 0.5) {
               double var24 = var20 * 2.0;
               this.recoveredField31 = this.recoveredMethod49(this.recoveredField37, this.recoveredField40, this.recoveredField46, var24);
               this.recoveredField32 = this.recoveredMethod49(this.recoveredField38, this.recoveredField41, this.recoveredField47, var24);
               this.recoveredField33 = this.recoveredMethod49(this.recoveredField39, this.recoveredField42, this.recoveredField48, var24);
            } else {
               double var25 = (var20 - 0.5) * 2.0;
               this.recoveredField31 = this.recoveredMethod49(this.recoveredField46, this.recoveredField43, this.recoveredField34, var25);
               this.recoveredField32 = this.recoveredMethod49(this.recoveredField47, this.recoveredField44, this.recoveredField35, var25);
               this.recoveredField33 = this.recoveredMethod49(this.recoveredField48, this.recoveredField45, this.recoveredField36, var25);
            }
         } else if (this.recoveredField143 == SlothRotation.PathModel.FLICK_SETTLE) {
            this.recoveredField31 = this.recoveredMethod45(this.recoveredField37, this.recoveredField49, this.recoveredField34, var11);
            this.recoveredField32 = this.recoveredMethod45(this.recoveredField38, this.recoveredField50, this.recoveredField35, var11);
            this.recoveredField33 = this.recoveredMethod45(this.recoveredField39, this.recoveredField51, this.recoveredField36, var11);
         } else if (this.recoveredField143 == SlothRotation.PathModel.CORRECTIVE) {
            this.recoveredField31 = this.recoveredMethod44(this.recoveredField37, this.recoveredField49, this.recoveredField34, var11);
            this.recoveredField32 = this.recoveredMethod44(this.recoveredField38, this.recoveredField50, this.recoveredField35, var11);
            this.recoveredField33 = this.recoveredMethod44(this.recoveredField39, this.recoveredField51, this.recoveredField36, var11);
         } else {
            double var21 = this.recoveredMethod48(var11);
            double var26 = 16.0 * var21 * var21 * (1.0 - var21) * (1.0 - var21);
            this.recoveredField31 = this.recoveredMethod50(this.recoveredField37, this.recoveredField34, var21) + this.recoveredField52 * var26;
            this.recoveredField32 = this.recoveredMethod50(this.recoveredField38, this.recoveredField35, var21) + this.recoveredField53 * var26;
            this.recoveredField33 = this.recoveredMethod50(this.recoveredField39, this.recoveredField36, var21) + this.recoveredField54 * var26;
         }

         if (this.recoveredField143 != SlothRotation.PathModel.EMPIRICAL) {
            float var22 = (float)(1.0 - this.recoveredMethod48(var3));
            this.recoveredField150 = this.recoveredField152 * var22;
            this.recoveredField151 = this.recoveredField153 * var22;
         }

         double var23 = 4.0 * var3 * Math.pow(1.0 - var3, 3.0);
         this.recoveredField31 = this.recoveredField31 + this.recoveredField61 * var23;
         this.recoveredField32 = this.recoveredField32 + this.recoveredField62 * var23;
         this.recoveredField33 = this.recoveredField33 + this.recoveredField63 * var23;
         this.recoveredField31 = this.recoveredMethod60(this.recoveredField31, 0.12, 0.88);
         this.recoveredField32 = this.recoveredMethod60(this.recoveredField32, 0.22, 0.92);
         this.recoveredField33 = this.recoveredMethod60(this.recoveredField33, 0.12, 0.88);
         if (this.recoveredField107 > 0L && now > this.recoveredField107) {
            double var27 = (now - this.recoveredField107) / 1.0E9;
            if (var27 <= 0.1) {
               double var28 = 1.0 - Math.exp(-var27 / 0.045);
               this.recoveredField64 = this.recoveredField64 + ((this.recoveredField31 - var5) / var27 - this.recoveredField64) * var28;
               this.recoveredField65 = this.recoveredField65 + ((this.recoveredField32 - var7) / var27 - this.recoveredField65) * var28;
               this.recoveredField66 = this.recoveredField66 + ((this.recoveredField33 - var9) / var27 - this.recoveredField66) * var28;
            }
         }

         this.recoveredField107 = now;
      }
   }

   private void recoveredMethod29(long now, double deltaTime) {
      Vec3d var5 = mc.player.getVelocity();
      boolean var6 = mc.player.isOnGround() && var5.x * var5.x + var5.z * var5.z < 0.0025;
      boolean var7 = this.recoveredMethod30(now);
      if (var6 && !var7) {
         if (this.recoveredField110 > 0L && now >= this.recoveredField110) {
            this.recoveredField75 = 0.0F;
            this.recoveredField76 = 0.0F;
            this.recoveredField110 = 0L;
            this.recoveredField109 = now + this.recoveredMethod58(200L, 700L);
         }

         if (this.recoveredField110 == 0L && now >= this.recoveredField109) {
            float var8 = Math.max(0.001F, GCDUtil.getGCDValue());
            double var9 = this.recoveredField12.nextDouble();
            int var11 = var9 < 0.72 ? 1 : (var9 < 0.96 ? 2 : 3);
            double var12 = this.recoveredField12.nextDouble();
            int var14 = var12 < 0.6 ? 0 : (var12 < 0.92 ? 1 : 2);
            float var15 = this.recoveredField12.nextBoolean() ? 1.0F : -1.0F;
            float var16 = this.recoveredField12.nextBoolean() ? 1.0F : -1.0F;
            this.recoveredField75 = var11 * var8 * var15;
            this.recoveredField76 = var14 * var8 * var16;
            this.recoveredField110 = now + this.recoveredMethod58(24L, 70L);
         }
      } else {
         this.recoveredField75 = 0.0F;
         this.recoveredField76 = 0.0F;
         this.recoveredField110 = 0L;
         if (now >= this.recoveredField109) {
            this.recoveredField109 = now + this.recoveredMethod58(240L, 800L);
         }
      }

      boolean var17 = this.recoveredField110 > now;
      double var18 = 1.0 - Math.exp(-deltaTime / (var17 ? 0.018 : 0.035));
      this.recoveredField73 = this.recoveredField73 + (this.recoveredField75 - this.recoveredField73) * (float)var18;
      this.recoveredField74 = this.recoveredField74 + (this.recoveredField76 - this.recoveredField74) * (float)var18;
      float var19 = var6 && !var7 ? 1.0F : 0.0F;
      double var20 = 1.0 - Math.exp(-deltaTime / (var19 > this.recoveredField77 ? 0.08 : 0.045));
      this.recoveredField77 = this.recoveredField77 + (var19 - this.recoveredField77) * (float)var20;
      double var21 = 1.0 - Math.exp(-deltaTime / (var7 ? 0.12 : 0.3));
      float var22 = var7 ? this.recoveredField78 : 0.1F;
      this.recoveredField79 = this.recoveredField79 + (var22 - this.recoveredField79) * (float)var21;
   }

   private boolean recoveredMethod30(long now) {
      double var3 = Math.sqrt(
         this.recoveredField64 * this.recoveredField64 + this.recoveredField65 * this.recoveredField65 + this.recoveredField66 * this.recoveredField66
      );
      double var5 = Math.hypot(this.recoveredField22, this.recoveredField23);
      return now < this.recoveredField106 && var3 > 0.12 || var5 > 24.0;
   }

   private void recoveredMethod31(long now, boolean precision, float yawMargin, float pitchMargin) {
      if (now >= this.recoveredField113 && now >= this.recoveredField115) {
         if (precision) {
            this.recoveredField113 = now + this.recoveredMethod58(700L, 1800L);
         } else if (this.recoveredField172 > 1.2F) {
            this.recoveredField113 = now + this.recoveredMethod58(150L, 320L);
         } else {
            float var6 = Math.min(yawMargin + MathHelper.clamp(yawMargin * this.recoveredMethod56(0.25F, 1.1F), 1.5F, 8.0F), 24.0F);
            float var7 = Math.min(pitchMargin * 0.5F + MathHelper.clamp(pitchMargin * this.recoveredMethod56(0.35F, 0.8F), 1.2F, 6.0F), 14.0F);
            float var8 = this.recoveredField12.nextBoolean() ? 1.0F : -1.0F;
            float var9 = this.recoveredField12.nextBoolean() ? 1.0F : -1.0F;
            if (this.recoveredField17 != null && mc.player != null) {
               Vec3d var10 = this.recoveredField17.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
               float var11 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var10.z, var10.x)) - 90.0);
               float var12 = (float)(-Math.toDegrees(Math.atan2(var10.y, var10.horizontalLength())));
               float var13 = MathHelper.wrapDegrees(this.recoveredField20 - var11);
               float var14 = this.recoveredField21 - var12;
               if (Math.abs(var13) > 0.15F) {
                  var8 = Math.signum(var13);
               }

               if (Math.abs(var14) > 0.15F) {
                  var9 = Math.signum(var14);
               }
            }

            double var15 = this.recoveredField12.nextDouble();
            if (var15 < 0.46) {
               this.recoveredField88 = var8 * var6;
               this.recoveredField89 = this.recoveredMethod55(0.08F, 0.24F) * var7;
            } else if (var15 < 0.72) {
               this.recoveredField88 = this.recoveredMethod55(0.12F, 0.34F) * var6;
               this.recoveredField89 = var9 * var7;
            } else {
               this.recoveredField88 = var8 * var6 * this.recoveredMethod56(0.66F, 0.96F);
               this.recoveredField89 = var9 * var7 * this.recoveredMethod56(0.42F, 0.76F);
            }

            this.recoveredField92 = this.recoveredMethod56(0.28F, 0.45F);
            this.recoveredField93 = this.recoveredMethod56(2.0F, 2.8F);
            this.recoveredField114 = now;
            this.recoveredField115 = now + this.recoveredMethod58(240L, 480L);
            this.recoveredField113 = this.recoveredField115 + this.recoveredMethod58(480L, 1500L);
         }
      }
   }

   private void recoveredMethod32(long now) {
      float var3 = this.recoveredMethod33(now);
      if (var3 <= 0.0F) {
         this.recoveredField114 = 0L;
         this.recoveredField115 = 0L;
         this.recoveredField88 = 0.0F;
         this.recoveredField89 = 0.0F;
      } else {
         this.recoveredField88 *= var3;
         this.recoveredField89 *= var3;
         this.recoveredField92 = 0.0F;
         this.recoveredField114 = now;
         this.recoveredField115 = now + this.recoveredMethod58(70L, 105L);
      }

      this.recoveredField113 = now + this.recoveredMethod58(900L, 2400L);
   }

   private float recoveredMethod33(long now) {
      if (this.recoveredField115 > this.recoveredField114 && now >= this.recoveredField114 && now < this.recoveredField115) {
         double var3 = this.recoveredMethod60((double)(now - this.recoveredField114) / (this.recoveredField115 - this.recoveredField114), 0.0, 1.0);
         if (var3 < this.recoveredField92) {
            double var7 = var3 / this.recoveredField92;
            return (float)(1.0 - Math.pow(1.0 - var7, this.recoveredField93));
         } else {
            double var5 = (var3 - this.recoveredField92) / (1.0 - this.recoveredField92);
            return (float)(1.0 - this.recoveredMethod48(var5));
         }
      } else {
         return 0.0F;
      }
   }

   private SlothRotation.PathModel recoveredMethod34(boolean action) {
      SlothRotation.PathModel var2 = this.recoveredField144;

      for (int var3 = 0; var3 < 8; var3++) {
         double var4 = this.recoveredField12.nextDouble();
         if (action) {
            var2 = var4 < 0.5
               ? SlothRotation.PathModel.EMPIRICAL
               : (
                  var4 < 0.68
                     ? SlothRotation.PathModel.MINIMUM_JERK
                     : (var4 < 0.85 ? SlothRotation.PathModel.CORRECTIVE : SlothRotation.PathModel.RANDOM_SPLINE)
               );
         } else {
            var2 = var4 < 0.48
               ? SlothRotation.PathModel.EMPIRICAL
               : (
                  var4 < 0.69
                     ? SlothRotation.PathModel.MINIMUM_JERK
                     : (var4 < 0.86 ? SlothRotation.PathModel.CORRECTIVE : SlothRotation.PathModel.RANDOM_SPLINE)
               );
         }

         if ((var2 != SlothRotation.PathModel.FLICK_SETTLE || this.recoveredField144 != SlothRotation.PathModel.FLICK_SETTLE)
            && (var2 != this.recoveredField144 || this.recoveredField142 < 2 || var2 == SlothRotation.PathModel.EMPIRICAL)) {
            break;
         }
      }

      if (var2 == this.recoveredField144) {
         this.recoveredField142++;
      } else {
         this.recoveredField144 = var2;
         this.recoveredField142 = 1;
      }

      return var2;
   }

   private Vec3d recoveredMethod35(boolean precision) {
      int var2 = precision ? 24 : 28;
      Vec3d[] var3 = new Vec3d[var2];
      int[] var4 = new int[var2];
      double[] var5 = new double[var2];
      double[] var6 = new double[var2];
      double[] var7 = new double[var2];
      double var8 = precision ? 0.05 : 0.07;
      double var10 = precision ? 0.02 : 0.025;

      for (int var12 = 0; var12 < var2; var12++) {
         Vec3d var13 = this.recoveredMethod37(precision);
         int var14 = this.recoveredMethod39(var13);
         double var15 = this.recoveredField138 == 0
            ? this.recoveredMethod42(var13.x, var13.y, var13.z, this.recoveredField31, this.recoveredField32, this.recoveredField33)
            : Double.MAX_VALUE;
         double var17 = this.recoveredField138 == 0
            ? this.recoveredMethod43(var13.x, var13.y, var13.z, this.recoveredField31, this.recoveredField32, this.recoveredField33)
            : Double.MAX_VALUE;
         double var19 = 0.0;

         for (int var21 = 0; var21 < this.recoveredField138; var21++) {
            int var22 = (this.recoveredField139 - 1 - var21 + 4) % 4;
            var15 = Math.min(
               var15,
               this.recoveredMethod42(var13.x, var13.y, var13.z, this.recoveredField13[var22], this.recoveredField14[var22], this.recoveredField15[var22])
            );
            var17 = Math.min(
               var17,
               this.recoveredMethod43(var13.x, var13.y, var13.z, this.recoveredField13[var22], this.recoveredField14[var22], this.recoveredField15[var22])
            );
            if (this.recoveredField16[var22] == var14) {
               var19 += 0.025 / (var21 + 1.0);
            }
         }

         double var27 = this.recoveredMethod42(var13.x, var13.y, var13.z, this.recoveredField31, this.recoveredField32, this.recoveredField33);
         double var23 = var15 * 0.22 + var17 * 0.35 + var27 * 0.06 + this.recoveredField12.nextDouble() * 0.75 - var19;
         if (var14 == this.recoveredField140) {
            var23 -= 0.08;
         }

         if (var14 == this.recoveredField141) {
            var23 -= 0.035;
         }

         var3[var12] = var13;
         var4[var12] = var14;
         var5[var12] = var23;
         var6[var12] = var15;
         var7[var12] = var17;
      }

      int var25 = this.recoveredMethod36(var5, var6, var7, var8, var10);
      if (var25 < 0) {
         var25 = this.recoveredMethod36(var5, var6, var7, var8 * 0.55, var10 * 0.55);
      }

      if (var25 < 0) {
         var25 = this.recoveredMethod36(var5, var6, var7, 0.018, 0.008);
      }

      if (var25 < 0) {
         return new Vec3d(this.recoveredField31, this.recoveredField32, this.recoveredField33);
      }

      Vec3d var26 = var3[var25];
      this.recoveredMethod40(var26, var4[var25]);
      return var26;
   }

   private int recoveredMethod36(
      double[] scores, double[] screenDistances, double[] normalizedDistances, double minimumScreenDistance, double minimumNormalizedDistance
   ) {
      double var8 = 2.0;
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
            var17 += 0.3 + Math.exp((scores[var14] - var10) * 0.6);
         }
      }

      if (var17 <= 0.0) {
         return -1;
      }

      double var18 = this.recoveredField12.nextDouble() * var17;

      for (int var16 = 0; var16 < scores.length; var16++) {
         if (!(screenDistances[var16] < minimumScreenDistance) && !(normalizedDistances[var16] < minimumNormalizedDistance) && !(scores[var16] < var10 - var8)) {
            var18 -= 0.3 + Math.exp((scores[var16] - var10) * 0.6);
            if (var18 <= 0.0) {
               return var16;
            }
         }
      }

      return -1;
   }

   private Vec3d recoveredMethod37(boolean precision) {
      double var2 = this.recoveredField12.nextDouble();
      double var4;
      double var6;
      double var8;
      if (var2 < 0.7) {
         double var10 = this.recoveredField12.nextGaussian();
         double var12 = this.recoveredField12.nextGaussian();
         double var14 = this.recoveredField31 * 0.68 + 0.16;
         double var16 = this.recoveredField32 * 0.62 + 0.2622;
         double var18 = this.recoveredField33 * 0.68 + 0.16;
         double var20 = precision ? 0.075 : 0.11;
         var4 = var14 + var10 * var20;
         var8 = var18 + var12 * var20;
         var6 = var16 + this.recoveredField12.nextGaussian() * (precision ? 0.075 : 0.11);
      } else if (var2 < 0.92) {
         double var25 = this.recoveredField12.nextGaussian();
         double var27 = this.recoveredField12.nextGaussian();
         var4 = 0.5 + var25 * (precision ? 0.14 : 0.19);
         var8 = 0.5 + var27 * (precision ? 0.14 : 0.19);
         var6 = 0.66 + this.recoveredField12.nextGaussian() * (precision ? 0.12 : 0.17);
      } else {
         double var26 = this.recoveredField12.nextDouble() * Math.PI * 2.0;
         double var28 = precision ? this.recoveredMethod57(0.2, 0.34) : this.recoveredMethod57(0.24, 0.4);
         var4 = 0.5 + Math.cos(var26) * var28;
         var8 = 0.5 + Math.sin(var26) * var28;
         var6 = this.recoveredMethod57(precision ? 0.42 : 0.32, precision ? 0.88 : 0.92);
      }

      if (precision) {
         var4 = this.recoveredMethod38(var4, 0.16, 0.84);
         var6 = this.recoveredMethod38(var6, 0.36, 0.9);
         var8 = this.recoveredMethod38(var8, 0.16, 0.84);
      } else {
         var4 = this.recoveredMethod38(var4, 0.12, 0.88);
         var6 = this.recoveredMethod38(var6, 0.29, 0.92);
         var8 = this.recoveredMethod38(var8, 0.12, 0.88);
      }

      return new Vec3d(var4, var6, var8);
   }

   private double recoveredMethod38(double value, double minimum, double maximum) {
      double var7 = value;

      for (int var9 = 0; var9 < 3 && (var7 < minimum || var7 > maximum); var9++) {
         var7 = var7 < minimum ? minimum + (minimum - var7) : maximum - (var7 - maximum);
      }

      return this.recoveredMethod60(var7, minimum, maximum);
   }

   private int recoveredMethod39(Vec3d point) {
      int var2 = point.y < 0.49 ? 0 : (point.y < 0.68 ? 1 : 2);
      double var3 = Math.atan2(point.z - 0.5, point.x - 0.5);
      if (var3 < 0.0) {
         var3 += Math.PI * 2;
      }

      int var5 = Math.min(7, (int)(var3 / (Math.PI / 4)));
      return var2 * 8 + var5;
   }

   private void recoveredMethod40(Vec3d point, int zone) {
      this.recoveredField13[this.recoveredField139] = point.x;
      this.recoveredField14[this.recoveredField139] = point.y;
      this.recoveredField15[this.recoveredField139] = point.z;
      this.recoveredField16[this.recoveredField139] = zone;
      this.recoveredField139 = (this.recoveredField139 + 1) % 4;
      this.recoveredField138 = Math.min(this.recoveredField138 + 1, 4);
      this.recoveredField141 = this.recoveredField140;
      this.recoveredField140 = zone;
   }

   private void recoveredMethod41() {
      this.recoveredField138 = 0;
      this.recoveredField139 = 0;
      this.recoveredField140 = -1;
      this.recoveredField141 = -1;

      for (int var1 = 0; var1 < 4; var1++) {
         this.recoveredField13[var1] = 0.0;
         this.recoveredField14[var1] = 0.0;
         this.recoveredField15[var1] = 0.0;
         this.recoveredField16[var1] = -1;
      }
   }

   private double recoveredMethod42(double firstX, double firstY, double firstZ, double secondX, double secondY, double secondZ) {
      if (this.recoveredField17 != null && mc.player != null) {
         Box var13 = this.recoveredField17.getBoundingBox();
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
         return this.recoveredMethod43(firstX, firstY, firstZ, secondX, secondY, secondZ);
      }
   }

   private double recoveredMethod43(double firstX, double firstY, double firstZ, double secondX, double secondY, double secondZ) {
      double var13 = firstX - secondX;
      double var15 = (firstY - secondY) * 1.25;
      double var17 = firstZ - secondZ;
      return Math.sqrt(var13 * var13 + var15 * var15 + var17 * var17);
   }

   private double recoveredMethod44(double start, double overshoot, double end, double progress) {
      double var9 = this.recoveredMethod47(this.recoveredMethod60(progress / this.recoveredField59, 0.0, 1.0));
      double var11 = this.recoveredMethod46(this.recoveredMethod60((progress - this.recoveredField60) / (1.0 - this.recoveredField60), 0.0, 1.0));
      return this.recoveredMethod50(start, overshoot, var9) + (end - overshoot) * var11;
   }

   private double recoveredMethod45(double start, double overshoot, double end, double progress) {
      double var9 = this.recoveredMethod60(progress / this.recoveredField59, 0.0, 1.0);
      double var11 = 1.0 - Math.pow(1.0 - var9, this.recoveredField58);
      double var13 = this.recoveredMethod48(this.recoveredMethod60((progress - this.recoveredField60) / (1.0 - this.recoveredField60), 0.0, 1.0));
      return this.recoveredMethod50(start, overshoot, var11) + (end - overshoot) * var13;
   }

   private double recoveredMethod46(double progress) {
      if (progress <= 0.0) {
         return 0.0;
      }

      if (progress >= 1.0) {
         return 1.0;
      }

      double var3 = Math.pow(progress, this.recoveredField56);
      double var5 = Math.pow(1.0 - progress, this.recoveredField57);
      return var3 / (var3 + var5);
   }

   private double recoveredMethod47(double progress) {
      return progress - Math.sin((Math.PI * 2) * progress) / (Math.PI * 2);
   }

   private double recoveredMethod48(double progress) {
      double var3 = progress * progress;
      double var5 = var3 * progress;
      return 10.0 * var5 - 15.0 * var5 * progress + 6.0 * var5 * var3;
   }

   private double recoveredMethod49(double start, double control, double end, double progress) {
      double var9 = 1.0 - progress;
      return var9 * var9 * start + 2.0 * var9 * progress * control + progress * progress * end;
   }

   private double recoveredMethod50(double start, double end, double progress) {
      return start + (end - start) * progress;
   }

   private double recoveredMethod51(double magnitude) {
      return this.recoveredMethod57(-magnitude, magnitude);
   }

   private float recoveredMethod52(LivingEntity target, double distance) {
      Box var4 = target.getBoundingBox();
      double var5 = Math.max(var4.getLengthX(), var4.getLengthZ()) * 0.5;
      return (float)Math.toDegrees(Math.atan2(var5, Math.max(0.35, distance)));
   }

   private float recoveredMethod53(LivingEntity target, double distance) {
      double var4 = target.getBoundingBox().getLengthY() * 0.5;
      return (float)Math.toDegrees(Math.atan2(var4, Math.max(0.35, distance)));
   }

   private float recoveredMethod54() {
      return MathHelper.clamp((float)this.recoveredField12.nextGaussian() * 0.48F, -1.0F, 1.0F);
   }

   private float recoveredMethod55(float minimum, float maximum) {
      return (this.recoveredField12.nextBoolean() ? 1.0F : -1.0F) * this.recoveredMethod56(minimum, maximum);
   }

   private float recoveredMethod56(float minimum, float maximum) {
      return minimum + this.recoveredField12.nextFloat() * (maximum - minimum);
   }

   private double recoveredMethod57(double minimum, double maximum) {
      return minimum + this.recoveredField12.nextDouble() * (maximum - minimum);
   }

   private long recoveredMethod58(long minimumMillis, long maximumMillis) {
      long var5 = minimumMillis + (long)(this.recoveredField12.nextDouble() * (maximumMillis - minimumMillis));
      return var5 * 1000000L;
   }

   private float recoveredMethod59(float current, float target, float maximumChange) {
      return current < target ? Math.min(current + maximumChange, target) : Math.max(current - maximumChange, target);
   }

   private double recoveredMethod60(double value, double minimum, double maximum) {
      return Math.max(minimum, Math.min(maximum, value));
   }

   private enum AimPhase {
      REACTION,
      ACQUIRE,
      TRACK,
      FOCUS,
      RECOVER;
   }

   private enum PathModel {
      EMPIRICAL,
      MINIMUM_JERK,
      RANDOM_SPLINE,
      FLICK_SETTLE,
      CORRECTIVE;
   }
}
