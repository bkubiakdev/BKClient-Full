package dev.bkclient.combatengine.client.modules.impl.combat;

import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventAttackEntity;
import dev.bkclient.combatengine.api.events.implement.EventGameUpdate;
import dev.bkclient.combatengine.api.events.implement.EventMoveInput;
import dev.bkclient.combatengine.api.events.implement.EventPacket;
import dev.bkclient.combatengine.api.events.implement.EventUpdate;
import dev.bkclient.combatengine.api.events.implement.EventUpdatePost;
import dev.bkclient.combatengine.api.storages.implement.FreeLookStorage;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import dev.bkclient.combatengine.api.utils.combat.IdealHitUtils;
import dev.bkclient.combatengine.api.utils.combat.PredictUtils;
import dev.bkclient.combatengine.api.utils.input.MovingUtil;
import dev.bkclient.combatengine.api.utils.math.MathUtils;
import dev.bkclient.combatengine.api.utils.math.TimerUtils;
import dev.bkclient.combatengine.api.utils.player.HotbarUtil;
import dev.bkclient.combatengine.api.utils.player.SlotSearchResult;
import dev.bkclient.combatengine.api.utils.player.SwapManager;
import dev.bkclient.combatengine.api.utils.rotate.MultipointUtils;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.api.utils.rotate.RotationUtils;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.client.modules.impl.combat.components.RotationsSystem;
import dev.bkclient.combatengine.client.modules.impl.combat.components.interpolation.BestPoint;
import dev.bkclient.combatengine.client.modules.impl.combat.components.neuro.NeuroRotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.neuro.StyleRecorder;
import dev.bkclient.combatengine.client.modules.impl.combat.components.rotations.FuntimeRotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.rotations.Holy2Rotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.rotations.HolyWorldRotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.rotations.LegitRotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.rotations.SlothRotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.rotations.TestRotation;
import dev.bkclient.combatengine.client.modules.impl.combat.components.rotations.WhiteRiseRotation;
import dev.bkclient.combatengine.bridge.SprintPause;
import dev.bkclient.combatengine.client.modules.impl.player.AutoEat;
import dev.bkclient.combatengine.client.modules.settings.implement.BooleanSetting;
import dev.bkclient.combatengine.client.modules.settings.implement.FloatSetting;
import dev.bkclient.combatengine.client.modules.settings.implement.ListSetting;
import dev.bkclient.combatengine.client.modules.settings.implement.ModeSetting;
import dev.bkclient.combatengine.mixin.ILivingEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Generated;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class Aura extends Module {
   public static Aura INSTANCE = new Aura();
   public final ModeSetting rotationType = new ModeSetting("Ротация", "HolyLegit", "Smooth", "HolyLegit", "NoRotate", "Funtime", "HolyWorld", "SlothAC", "Grim");
   private final ListSetting recoveredField1 = new ListSetting(
      "Таргеты",
      new BooleanSetting("Игроки", true),
      new BooleanSetting("Голые", true),
      new BooleanSetting("Невидимки", true),
      new BooleanSetting("Мирные", false),
      new BooleanSetting("Мобы", true)
   );
   private final FloatSetting recoveredField2 = new FloatSetting("Дистанция атаки", 3.0F, 0.0F, 6.0F, 0.05F);
   private final FloatSetting recoveredField3 = new FloatSetting("Дистанция наводки", 3.0F, 0.0F, 6.0F, 0.05F);
   private final FloatSetting recoveredField4 = new FloatSetting("Дистанция на элитрах", 50.0F, 10.0F, 100.0F, 0.05F);
   public final BooleanSetting smartCrit = new BooleanSetting("Умные криты", false);
   private final BooleanSetting recoveredField5 = new BooleanSetting("Бить через стены", true);
   private final BooleanSetting recoveredField6 = new BooleanSetting("Отжимать щит", false);
   private final BooleanSetting recoveredField7 = new BooleanSetting("Ломать щит", true);
   private final BooleanSetting recoveredField8 = new BooleanSetting("Не бить когда ешь", true);
   public static BooleanSetting clientLook = new BooleanSetting("Наводка от первого лица", false);
   private final ModeSetting recoveredField9 = new ModeSetting("Коррекция", "Свободная", "Нет", "Свободная", "Сфокусированная", "Полная");
   private final ModeSetting recoveredField10 = new ModeSetting("Приоритет", "Дистанция", "Дистанция", "Здоровье", "Угол", "Никакой");
   private final BooleanSetting recoveredField11 = new BooleanSetting("Авто цербер", false);
   private final FloatSetting recoveredField12 = new FloatSetting("Задержка свапа", 4.0F, 2.0F, 10.0F, 1.0F).visible(() -> this.recoveredField11.isState());
   private LivingEntity recoveredField13;
   private Vec2f recoveredField14 = new Vec2f(0.0F, 0.0F);
   private Vec2f recoveredField15 = new Vec2f(0.0F, 0.0F);
   private final TimerUtils recoveredField16 = new TimerUtils();
   private final BooleanSetting recoveredField17 = new BooleanSetting("Обход рв стен", false);
   private final BooleanSetting recoveredField18 = new BooleanSetting("Смотреть вниз", false).visible(this.recoveredField17::isState);
   private final TestRotation recoveredField19 = new TestRotation();
   private final FuntimeRotation recoveredField20 = new FuntimeRotation();
   private final SlothRotation recoveredField21 = new SlothRotation();
   private final HolyWorldRotation recoveredField22 = new HolyWorldRotation();
   private final Holy2Rotation recoveredField23 = new Holy2Rotation();
   private final WhiteRiseRotation recoveredField24 = new WhiteRiseRotation(this);
   private final LegitRotation recoveredField25 = new LegitRotation();
   private final NeuroRotation recoveredField26 = new NeuroRotation();
   private boolean recoveredField27;
   private boolean recoveredField28 = false;
   private boolean recoveredField29 = false;
   private boolean recoveredField30 = false;
   private float recoveredField31;
   private float recoveredField32;
   private boolean recoveredField33;
   private LivingEntity recoveredField34;
   private boolean recoveredField35;
   private long recoveredField36;
   private final TimerUtils recoveredField37 = new TimerUtils();
   private TpsSync recoveredField38;
   private static final int recoveredField39 = 3;
   private static final int recoveredField40 = 13;
   private int recoveredField41 = 0;
   private int recoveredField42 = 0;
   private int recoveredField43 = -1;
   private boolean recoveredField44 = false;
   private LivingEntity recoveredField45 = null;
   private LivingEntity recoveredField46 = null;
   private boolean recoveredField47 = false;
   private int recoveredField48 = -1;
   private boolean recoveredField49 = false;
   private int recoveredField50 = 0;
   private boolean recoveredField51 = false;
   private boolean recoveredField52 = false;
   private int recoveredField53 = 0;
   private boolean recoveredField54 = false;
   private ItemStack recoveredField55 = ItemStack.EMPTY;
   private int recoveredField56 = 0;
   private float recoveredField57 = 0.0F;
   private float recoveredField58 = 0.0F;
   public static float adjYaw;
   public static float adjPitch;
   public static float otvodkaYaw;
   public static float otvodkaPitch;
   public boolean isRotated;

   private Aura2 bkAdaptive;
   private boolean bkAdaptiveActive;
   private final BooleanSetting bkAdaptiveTps = new BooleanSetting("TPS sync", true).visible(this::usesBKAdaptive);
   private final BooleanSetting bkAdaptiveMovement = new BooleanSetting("Movement correction", false).visible(this::usesBKAdaptive);
   public boolean usesBKAdaptive() { return rotationType.is("HolyWorld") || rotationType.is("SlothAC") || rotationType.is("Grim"); }
   public boolean preventSprint() { return isEnable() && bkAdaptiveActive && bkAdaptive != null && bkAdaptive.preventSprint(); }
   private void syncBKAdaptive() {
      if (usesBKAdaptive() && !bkAdaptiveActive) {
         recoveredMethod61();
         if(recoveredField54) {SprintPause.popPause();recoveredField54=false;}
         recoveredField53=0;recoveredField13=null;
         recoveredField20.reset();recoveredField21.reset();recoveredField25.reset();recoveredMethod51();
         if(RotationStorage.instance!=null) RotationStorage.instance.stopRotation();
         if(bkAdaptive==null) bkAdaptive=Aura2.embedded();
         bkAdaptive.beginEmbedded();bkAdaptiveActive=true;
      } else if(!usesBKAdaptive() && bkAdaptiveActive) {
         bkAdaptive.endEmbedded();bkAdaptiveActive=false;recoveredField13=null;
         recoveredField41=recoveredField42=0;
         if(RotationStorage.instance!=null) RotationStorage.instance.stopRotation();
      }
      if(bkAdaptiveActive) {
         boolean[] targets=new boolean[5];for(int i=0;i<5;i++)targets[i]=recoveredField1.getSettings().get(i).isState();
         String priority=switch(recoveredField10.getIndex()){case 1 -> "Health";case 2 -> "Angle";default -> "Distance";};
         bkAdaptive.configure(rotationType.getCurrent(),recoveredField2.get(),mc.player!=null && mc.player.isGliding()?recoveredField4.get():recoveredField2.get()+recoveredField3.get(),priority,targets,smartCrit.isState(),
            recoveredField5.isState(),recoveredField6.isState(),recoveredField7.isState(),recoveredField8.isState(),clientLook.isState(),bkAdaptiveTps.isState());
      }
   }
   public Float movementReferenceYaw() {
      if(!isEnable() || mc.player==null || rotationType.is("NoRotate") || (usesBKAdaptive() ? !bkAdaptiveMovement.isState() : recoveredField9.getIndex()==0) || clientLook.isState()
         || !FreeLookStorage.isActive()) return null;
      return FreeLookStorage.getFreeYaw();
   }
   public Aura() {
      super("AttackAura", "Автоматически наводиться и бьёт таргета", Module.ModuleCategory.COMBAT);
      recoveredField9.visible(() -> !usesBKAdaptive());
      recoveredField17.visible(() -> !usesBKAdaptive());
      recoveredField18.visible(() -> !usesBKAdaptive() && recoveredField17.isState());
      recoveredField11.visible(() -> !usesBKAdaptive());
      recoveredField12.visible(() -> !usesBKAdaptive() && recoveredField11.isState());
      this.addSettings(
         this.rotationType,
         this.recoveredField1,
         this.recoveredField2,
         this.recoveredField3,
         this.recoveredField4,
         this.smartCrit,
         this.recoveredField8,
         this.recoveredField5,
         this.recoveredField17,
         this.recoveredField18,
         this.recoveredField6,
         this.recoveredField7,
         this.recoveredField11,
         this.recoveredField12,
         clientLook,
         this.recoveredField9,
         this.recoveredField10,
         this.bkAdaptiveTps,
         this.bkAdaptiveMovement
      );
   }

   @EventLink
   public void onPlayerTick(EventUpdate e) {
      if(usesBKAdaptive() || bkAdaptiveActive) return;
      if (mc.player != null && mc.world != null) {
         this.recoveredField57++;
         this.recoveredMethod25();
         this.recoveredMethod55();
      }
   }

   @EventLink
   public void onAttackEntity(EventAttackEntity event) {
      if(usesBKAdaptive() || bkAdaptiveActive) return;
      if (mc.player != null && mc.world != null) {
         if (event.getPlayer() == mc.player) {
            if (event.getTarget() instanceof LivingEntity var2) {
               if (this.recoveredMethod39(var2)) {
                  this.recoveredField13 = var2;
               }
            }
         }
      }
   }

   @EventLink
   public void onMoveInput(EventMoveInput event) {
      if (this.recoveredField53 > 0) {
         if (mc.player != null) {
            mc.player.setSprinting(false);
         }

         event.setForward(0.0F);
         event.setStrafe(0.0F);
         event.setJump(false);
         event.setSneak(false);
         event.setSprint(false);
      } else {
         this.recoveredMethod1(event);
      }
   }

   private void recoveredMethod1(EventMoveInput event) {
      if(preventSprint()) {event.setSprint(false);mc.player.setSprinting(false);}
      Float yaw=movementReferenceYaw();
      if(yaw!=null) {
         var corrected=dev.bkclient.combatengine.api.utils.input.AuraMovement.correct(yaw,mc.player.getYaw(),event.getForward(),event.getStrafe(),true);
         event.setForward(corrected.x);event.setStrafe(corrected.y);
      }
   }

   private float recoveredMethod2() {
      return RotationStorage.instance != null && RotationStorage.instance.targetRotation() != null
         ? RotationStorage.instance.targetRotation().getYaw()
         : mc.player.getYaw();
   }

   private float recoveredMethod3() {
      return RotationUtils.getRotations(this.recoveredField13.getBoundingBox().getCenter()).x;
   }

   private void recoveredMethod4(EventMoveInput event, float yaw, float directionYaw) {
      float var4 = event.getForward();
      float var5 = event.getStrafe();
      if (var4 != 0.0F || var5 != 0.0F) {
         double var6 = MathHelper.wrapDegrees(Math.toDegrees(MovingUtil.direction(directionYaw, var4, var5)));
         float var8 = 0.0F;
         float var9 = 0.0F;
         float var10 = Float.MAX_VALUE;

         for (float var11 = -1.0F; var11 <= 1.0F; var11++) {
            for (float var12 = -1.0F; var12 <= 1.0F; var12++) {
               if (var11 != 0.0F || var12 != 0.0F) {
                  double var13 = MathHelper.wrapDegrees(Math.toDegrees(MovingUtil.direction(yaw, var11, var12)));
                  double var15 = Math.abs(var6 - var13);
                  if (var15 < var10) {
                     var10 = (float)var15;
                     var8 = var11;
                     var9 = var12;
                  }
               }
            }
         }

         event.setForward(var8);
         event.setStrafe(var9);
      }
   }

   @EventLink
   private void recoveredMethod5(EventGameUpdate e) {
      if (mc.player != null && mc.world != null && this.recoveredField13 != null) {
         this.recoveredMethod8();
      }
   }

   @EventLink(priority = -10)
   public void onTick(EventUpdate e) {
      syncBKAdaptive();
      if(bkAdaptiveActive) {bkAdaptive.prepare(e);recoveredField13=bkAdaptive.getTarget();
         if(mc.player!=null) {recoveredField14=new Vec2f(mc.player.getYaw(),mc.player.getPitch());recoveredField15=recoveredField14;}
         return;
      }
      if (mc.player != null && mc.world != null) {
         if (this.recoveredField42 > 0) {
            this.recoveredField42--;
         }

         if (this.recoveredField41 > 0) {
            this.recoveredField41--;
         }

         boolean var2 = ModuleClass.packetCriticals.isEnable() && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
         if (!var2) {
            this.recoveredMethod7();
         }
      }
   }

   @EventLink
   public void onPost(EventUpdatePost e) {
      if(usesBKAdaptive() || bkAdaptiveActive) {if(bkAdaptiveActive) bkAdaptive.attack(e);return;}
      if (mc.player != null && mc.world != null) {
         boolean var2 = ModuleClass.packetCriticals.isEnable() && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
         if (var2 && mc.player.fallDistance > 0.0F && mc.player.fallDistance < 1.0F) {
            this.recoveredMethod7();
         }
      }
   }

   @EventLink(priority = -200)
   public void onPacket(EventPacket e) {
      if(usesBKAdaptive() || bkAdaptiveActive) {if(bkAdaptiveActive) bkAdaptive.onPacket(e);return;}
      if (!e.isCancelled()) {
         if (e.getType() == EventPacket.Type.SEND && e.getPacket() instanceof PlayerMoveC2SPacket var2) {
            if (var2.changesLook() && mc.player != null) {
               this.recoveredField31 = var2.getYaw(mc.player.getYaw());
               this.recoveredField32 = var2.getPitch(mc.player.getPitch());
               this.recoveredField33 = true;
            }
         }
      }
   }

   private LivingEntity recoveredMethod6() {
      LivingEntity var1 = null;
      double var2 = 100.0;
      Vec3d var4 = mc.player.getEyePos();

      for (Entity var6 : mc.world.getEntities()) {
         if (var6 instanceof LivingEntity var7 && var7 != mc.player && var7.isAlive() && !(var7.getHealth() <= 0.0F) && !(var7 instanceof ArmorStandEntity)) {
            double var8 = var4.squaredDistanceTo(var7.getBoundingBox().getCenter());
            if (!(var8 > var2)) {
               var2 = var8;
               var1 = var7;
            }
         }
      }

      return var1;
   }

   private void recoveredMethod7() {
      this.recoveredMethod25();
      if (this.recoveredField13 != null) {
         boolean var1 = !this.recoveredMethod46() && this.recoveredMethod12();
         boolean var2 = !this.rotationType.is("Neuro") || this.recoveredField26.attackConsent();
         boolean var3 = this.recoveredField8.isState() && (this.recoveredMethod35() || AutoEat.shouldSuppressCombat());
         if (this.recoveredMethod46() && this.recoveredField41 <= 0 && var2 && !var3) {
            this.recoveredMethod48(this.recoveredField13);
         }

         if ((this.recoveredMethod42() || var1) && this.recoveredField41 <= 0 && var2) {
            if (var3) {
               return;
            }

            if (this.recoveredMethod10() && !this.recoveredMethod11()) {
               return;
            }

            this.recoveredMethod27();
            if (this.recoveredMethod46()) {
               this.recoveredMethod49();
            }

            this.recoveredMethod71();
         }
      } else {
         this.recoveredField41 = 0;
         this.recoveredField37.reset();
         adjPitch = 0.0F;
         adjYaw = 0.0F;
         this.recoveredField19.reset();
         this.recoveredMethod52();
         this.recoveredMethod45();
         this.recoveredMethod54();
         this.recoveredMethod53();
         this.recoveredField24.reset();
         this.recoveredField25.reset();
         this.recoveredField46 = null;
         this.recoveredField42 = 0;
         this.recoveredMethod71();
      }
   }

   public void Rotate() {
      this.recoveredMethod8();
   }

   private void recoveredMethod8() {
      if(usesBKAdaptive() || bkAdaptiveActive) return;
      if (this.recoveredField13 != null) {
         if (!this.rotationType.is("Funtime")) {
            this.recoveredMethod45();
         }

         if (!this.recoveredMethod46()) {
            this.recoveredMethod52();
         }

         if (!this.rotationType.is("Sloth")) {
            this.recoveredMethod54();
         }

         if (!this.rotationType.is("Neuro")) {
            this.recoveredMethod53();
         }

         if (this.recoveredMethod10()) {
            this.recoveredMethod9(this.recoveredField13);
         } else {
            RotationsSystem var1;
            if (this.rotationType.is("Smooth")) {
               var1 = new RotationsSystem() {
                  @Override
                  public void updateRotations(LivingEntity target) {
                     if (!mc.player.isGliding()) {
                        Vec3d var2 = target.getPos().add(0.0, target.getHeight() * 0.6F, 0.0).subtract(mc.player.getEyePos());
                        float var3 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var2.z, var2.x)) - 90.0);
                        float var4 = (float)(-Math.toDegrees(Math.atan2(var2.y, Math.hypot(var2.x, var2.z))));
                        RotationStorage.update(new Rotation(var3, var4), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, Aura.clientLook.isState());
                     } else {
                        Vec3d var8 = Vec3d.fromPolar(target.getLerpTargetPitch(), target.getLerpTargetYaw());
                        Vec3d var9 = target.getRotationVector();
                        Vec3d var10 = target.getPos().add(0.0, target.getHeight() * 0.6F, 0.0).subtract(mc.player.getEyePos());
                        Vec3d var5 = var8.normalize().lerp(var9, var8.length());
                        float var6 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(var10.z, var10.x)) - 90.0);
                        float var7 = (float)(-Math.toDegrees(Math.atan2(var10.y, Math.hypot(var10.x, var10.z))));
                        RotationStorage.update(new Rotation(var6, var7), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, Aura.clientLook.isState());
                     }
                  }
               };
            } else if (this.rotationType.is("HolyLegit")) {
               var1 = this.recoveredField25;
            } else if (this.recoveredMethod46()) {
               if (!this.recoveredField30) {
                  this.recoveredMethod51();
                  this.recoveredField30 = true;
               }

               if (this.recoveredField34 != this.recoveredField13) {
                  this.recoveredField34 = this.recoveredField13;
                  this.recoveredField33 = false;
                  this.recoveredField36 = 0L;
               }

               var1 = this.recoveredMethod47();
            } else if (this.rotationType.is("Test")) {
               var1 = this.recoveredField19;
            } else if (this.rotationType.is("Funtime")) {
               if (!this.recoveredField28) {
                  this.recoveredField20.reset();
                  this.recoveredField28 = true;
               }

               this.recoveredField20.setVisibleAim(clientLook.isState());
               this.recoveredField20.setPrecisionLock(this.recoveredMethod44(this.recoveredField13));
               var1 = this.recoveredField20;
            } else if (this.rotationType.is("Sloth")) {
               if (!this.recoveredField29) {
                  this.recoveredField21.reset();
                  this.recoveredField29 = true;
               }

               if (this.recoveredField34 != this.recoveredField13) {
                  this.recoveredField34 = this.recoveredField13;
                  this.recoveredField33 = false;
                  this.recoveredField36 = 0L;
               }

               this.recoveredField21.setVisibleAim(false);
               this.recoveredField21.setPrecisionLock(this.recoveredMethod44(this.recoveredField13));
               var1 = this.recoveredField21;
            } else if (this.rotationType.is("Neuro")) {
               if (!this.recoveredField27) {
                  this.recoveredField26.reset();
                  this.recoveredField27 = true;
               }

               if (!this.recoveredField26.isAvailable()) {
                  return;
               }

               var1 = this.recoveredField26;
            } else if (this.rotationType.is("NoRotate")) {
               var1 = new RotationsSystem() {
                  @Override
                  public void updateRotations(LivingEntity target) {
                     RotationStorage.update(
                        new Rotation(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch()),
                        MathUtils.random(100.0F, 170.0F),
                        MathUtils.random(100.0F, 170.0F),
                        MathUtils.random(100.0F, 170.0F),
                        MathUtils.random(100.0F, 170.0F),
                        1,
                        6,
                        false
                     );
                  }
               };
            } else if (this.rotationType.is("Data")) {
               var1 = new RotationsSystem() {
                  @Override
                  public void updateRotations(LivingEntity target) {
                     boolean var2 = Aura.this.recoveredMethod26();
                     Vec3d var3 = Aura.this.recoveredMethod21(target);
                     Vec2f var4 = RotationUtils.getRotations(var3);
                     float var5 = mc.player.getYaw();
                     float var6 = mc.player.getPitch();
                     float var7 = Math.abs(MathHelper.wrapDegrees(var4.x - var5));
                     float var8 = Math.abs(var4.y - var6);
                     boolean var9 = var7 > 70.0F || var7 > 42.0F && mc.player.squaredDistanceTo(target) < 9.0;
                     Rotation var10 = null;
                     if (var10 == null || var9) {
                        var10 = new Rotation(var4.x, var4.y);
                     }

                     Aura.this.recoveredField15 = new Vec2f(var10.getYaw(), var10.getPitch());
                     Aura.this.recoveredField14 = new Vec2f(var5, var6);
                     float var11 = var9 ? Math.max(95.0F, Math.min(180.0F, var7 * 1.45F)) : (var2 ? 24.0F : 11.5F);
                     float var12 = var9 ? Math.max(55.0F, Math.min(110.0F, Math.max(18.0F, var8 * 1.35F))) : (var2 ? 18.0F : 9.0F);
                     float var13 = var9 ? var11 : (var2 ? 18.0F : 9.0F);
                     float var14 = var9 ? var12 : (var2 ? 14.0F : 7.0F);
                     RotationStorage.update(var10, var11, var12, var13, var14, 1, 1, Aura.clientLook.isState());
                  }
               };
            } else {
               final Vec2f var2 = RotationUtils.getRotations(this.recoveredMethod20(this.recoveredField13, this.recoveredField13.getLeashPos(1.0F)));
               var1 = new RotationsSystem() {
                  @Override
                  public void updateRotations(LivingEntity target) {
                     Aura.this.recoveredField14 = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                     RotationStorage.update(new Rotation(var2.x, var2.y), 360.0F, 360.0F, 360.0F, 360.0F, 1, 1, Aura.clientLook.isState());
                  }
               };
            }

            var1.updateRotations(this.recoveredField13);
         }
      }
   }

   private void recoveredMethod9(LivingEntity target) {
      Vec3d var2 = MultipointUtils.getClosestPoint(target);
      if (var2 == null) {
         var2 = target.getBoundingBox().getCenter();
      }

      Vec3d var3 = this.recoveredMethod20(target, var2);
      Vec2f var4 = RotationUtils.getRotations(var3);
      this.recoveredField15 = var4;
      this.recoveredField14 = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
      boolean var5 = mc.player.age <= this.recoveredField43;
      float var6 = var4.x;
      float var7 = var4.y;
      if (!var5) {
         if (this.recoveredMethod15()) {
            var7 = 90.0F;
         } else {
            var6 = FreeLookStorage.getFreeYaw();
            var7 = FreeLookStorage.getFreePitch();
         }
      }

      RotationStorage.update(new Rotation(var6, var7), 360.0F, 360.0F, 360.0F, 360.0F, 0, 6, clientLook.isState());
   }

   private boolean recoveredMethod10() {
      return !this.rotationType.is("Sloth") && this.recoveredMethod14();
   }

   private boolean recoveredMethod11() {
      if (!this.recoveredField44) {
         this.recoveredField44 = true;
         this.recoveredField43 = mc.player.age + 1;
         this.recoveredField45 = this.recoveredField13;
         return false;
      } else if (mc.player.age > this.recoveredField43) {
         this.recoveredMethod71();
         return false;
      } else {
         return this.recoveredMethod13();
      }
   }

   private boolean recoveredMethod12() {
      if (!this.recoveredField44 || mc.player == null || this.recoveredField13 == null || this.recoveredField13 != this.recoveredField45) {
         return false;
      } else if (mc.player.age > this.recoveredField43 + 1) {
         this.recoveredMethod71();
         return false;
      } else {
         return mc.player.age >= this.recoveredField43;
      }
   }

   private boolean recoveredMethod13() {
      if (this.recoveredField13 != null && mc.player != null) {
         float var1 = Math.abs(MathHelper.wrapDegrees(this.recoveredField15.x - mc.player.getYaw()));
         float var2 = Math.abs(this.recoveredField15.y - mc.player.getPitch());
         boolean var3 = this.recoveredMethod14() || mc.player.isGliding() && this.recoveredField13.isGliding();
         if (!var3) {
            EntityHitResult var4 = this.recoveredMethod16();
            var3 = var4 != null && var4.getEntity() == this.recoveredField13;
         }

         return var1 <= 3.0F && var2 <= 2.5F && var3;
      } else {
         return false;
      }
   }

   private boolean recoveredMethod14() {
      return this.recoveredField17.isState() && this.recoveredField13 != null && this.recoveredMethod18(this.recoveredField13);
   }

   private boolean recoveredMethod15() {
      return this.recoveredMethod14() && this.recoveredField18.isState();
   }

   private EntityHitResult recoveredMethod16() {
      return this.recoveredMethod17(mc.player.getYaw(), mc.player.getPitch());
   }

   private EntityHitResult recoveredMethod17(float yaw, float pitch) {
      Vec3d var3 = mc.player.getCameraPosVec(1.0F);
      Vec3d var4 = Vec3d.fromPolar(pitch, yaw);
      float var5 = this.recoveredField2.getValue().floatValue() * 2.0F;
      Vec3d var6 = var3.add(var4.multiply(var5));
      return ProjectileUtil.raycast(mc.player, var3, var6, mc.player.getBoundingBox().expand(var5), ex -> ex != mc.player && ex.isAlive(), var5 * var5);
   }

   private boolean recoveredMethod18(LivingEntity entity) {
      return entity != null && mc.player != null && mc.world != null ? !mc.player.canSee(entity) || this.recoveredMethod19(entity) : false;
   }

   private boolean recoveredMethod19(LivingEntity entity) {
      if (entity != null && mc.player != null && mc.world != null && this.recoveredField17.isState()) {
         Vec3d var2 = mc.player.getEyePos();
         Box var3 = entity.getBoundingBox();
         double var4 = var3.getCenter().x;
         double var6 = var3.getCenter().z;
         Vec3d[] var8 = new Vec3d[]{
            var3.getCenter(),
            this.recoveredMethod23(entity),
            new Vec3d(var4, var3.maxY - 0.08, var6),
            new Vec3d(var4, var3.minY + 0.12, var6),
            new Vec3d(var3.minX + 0.04, var3.minY + var3.getLengthY() * 0.55, var6),
            new Vec3d(var3.maxX - 0.04, var3.minY + var3.getLengthY() * 0.55, var6),
            new Vec3d(var4, var3.minY + var3.getLengthY() * 0.55, var3.minZ + 0.04),
            new Vec3d(var4, var3.minY + var3.getLengthY() * 0.55, var3.maxZ - 0.04)
         };
         int var9 = 0;
         int var10 = 0;

         for (Vec3d var14 : var8) {
            BlockHitResult var15 = mc.world.raycast(new RaycastContext(var2, var14, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            if (var15 != null && var15.getType() == Type.BLOCK) {
               var9++;
            } else {
               var10++;
            }
         }

         return var10 > 0 && var9 >= var10;
      } else {
         return false;
      }
   }

   private Vec3d recoveredMethod20(LivingEntity target, Vec3d point) {
      return mc.player != null && target != null && mc.player.isGliding() && target.isGliding() ? PredictUtils.bypasselytrahacking(target) : point;
   }

   private Vec3d recoveredMethod21(LivingEntity target) {
      if (this.recoveredMethod22(target)) {
         return this.recoveredMethod20(target, this.recoveredMethod23(target));
      }

      Vec3d var2 = BestPoint.getNearestPoint(target);
      if (var2 == null) {
         var2 = MultipointUtils.getClosestPoint(target);
      }

      if (var2 == null) {
         var2 = this.recoveredMethod23(target);
      }

      return this.recoveredMethod20(target, var2);
   }

   private boolean recoveredMethod22(LivingEntity target) {
      if (mc.player != null && target != null) {
         Vec3d var2 = mc.player.getEyePos();
         Box var3 = target.getBoundingBox();
         if (var3.expand(0.12).contains(var2)) {
            return true;
         }

         Vec3d var4 = this.recoveredMethod23(target);
         return var2.squaredDistanceTo(var4) <= 2.25;
      } else {
         return false;
      }
   }

   private Vec3d recoveredMethod23(LivingEntity target) {
      Box var2 = target.getBoundingBox();
      return new Vec3d(var2.getCenter().x, var2.minY + var2.getLengthY() * 0.72, var2.getCenter().z);
   }

   private LivingEntity recoveredMethod24() {
      ArrayList<LivingEntity> var1 = new ArrayList<>();

      for (Entity var3 : mc.world.getEntities()) {
         if (var3 instanceof LivingEntity var4 && this.recoveredMethod39(var4)) {
            var1.add(var4);
         }
      }

      if (!var1.isEmpty() && this.isEnable()) {
         switch (this.recoveredField10.getCurrent()) {
            case "Дистанция":
               var1.sort(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().squaredDistanceTo(mc.player.getEyePos())));
               break;
            case "Здоровье":
               var1.sort(Comparator.comparingDouble(LivingEntity::getHealth));
               break;
            case "Угол":
               var1.sort(Comparator.comparingDouble(entity -> {
                  Vec2f var1x = RotationUtils.getRotations(entity.getBoundingBox().getCenter());
                  double var2 = Math.abs(MathHelper.wrapDegrees(var1x.x - mc.player.getYaw()));
                  double var4x = Math.abs(MathHelper.wrapDegrees(var1x.y - mc.player.getPitch()));
                  return var2 + var4x;
               }));
            case "Никакой":
         }

         return var1.isEmpty() ? null : (LivingEntity)var1.get(0);
      } else {
         return null;
      }
   }

   private void recoveredMethod25() {
      if (!this.isEnable()) {
         this.recoveredField13 = null;
         this.recoveredMethod59();
      } else if (this.recoveredField13 == null || !this.recoveredMethod39(this.recoveredField13)) {
         if (this.recoveredField13 != null && !this.recoveredMethod39(this.recoveredField13)) {
            this.recoveredMethod59();
         }

         this.recoveredField13 = this.recoveredMethod24();
         if (this.recoveredField13 == null) {
            this.recoveredMethod59();
         }
      }
   }

   private boolean recoveredMethod26() {
      float var1 = mc.player.getAttackCooldownProgress(1.5F);
      float var2 = Math.max(0.82F, IdealHitUtils.getAICooldown() - 0.08F);
      boolean var3 = var1 >= var2;
      boolean var4 = !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0 && mc.player.fallDistance > 0.0F;
      return var3 || var4;
   }

   private void recoveredMethod27() {
      Hand var1 = null;
      if (this.recoveredField6.isState()) {
         var1 = this.recoveredMethod34();
         if (var1 != null) {
            mc.interactionManager.stopUsingItem(mc.player);
         }
      }

      this.recoveredMethod29();
      StyleRecorder.get().noteModuleAttack();
      this.recoveredField35 = true;

      try {
         boolean var2 = false;
         if (this.recoveredField13 instanceof PlayerEntity var3 && var3.isBlocking() && this.recoveredField7.isState()) {
            var2 = this.recoveredMethod30(var3);
         }

         if (!var2) {
            mc.interactionManager.attackEntity(mc.player, this.recoveredField13);
         }
      } finally {
         this.recoveredField35 = false;
      }

      mc.player.swingHand(Hand.MAIN_HAND);
      if (var1 != null) {
         this.recoveredMethod36(var1);
      }

      if (this.rotationType.is("Funtime")) {
         this.recoveredField20.onAttack();
      }

      if (this.rotationType.is("WhiteRise")) {
         this.recoveredField24.onAttack();
      }

      this.recoveredField41 = this.recoveredMethod28();
      this.recoveredField42 = this.recoveredField41;
      this.recoveredField16.reset();
   }

   private int recoveredMethod28() {
      int var1 = ThreadLocalRandom.current().nextInt(3, 14);
      if (this.recoveredField38 != null && this.recoveredField38.isEnable()) {
         long var2 = (long)((float)this.recoveredField38.getAdjustedCooldown(var1 * 50L) * 1.1F);
         var1 = MathHelper.clamp(Math.round((float)var2 / 50.0F), 3, 13);
      }

      return var1;
   }

   private void recoveredMethod29() {
      if (this.recoveredField17.isState() && this.recoveredField13 != null && mc.player != null && mc.world != null) {
         if (!mc.player.canSee(this.recoveredField13)) {
            if (mc.player.networkHandler != null) {
               Vec3d var1 = mc.player.getEyePos();
               Vec3d var2 = this.recoveredField13.getBoundingBox().getCenter();
               BlockHitResult var3 = mc.world.raycast(new RaycastContext(var1, var2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
               if (var3 != null && var3.getType() == Type.BLOCK) {
                  BlockPos var4 = var3.getBlockPos();
                  if (!mc.world.getBlockState(var4).isAir()) {
                     if (!(mc.world.getBlockState(var4).getHardness(mc.world, var4) < 0.0F)) {
                        Direction var5 = var3.getSide() == null ? Direction.UP : var3.getSide();
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, var4, var5));
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, var4, var5));
                     }
                  }
               }
            }
         }
      }
   }

   private boolean recoveredMethod30(PlayerEntity entity) {
      SlotSearchResult var2 = HotbarUtil.getAxe();
      if (!var2.found()) {
         return false;
      }

      int var3 = mc.player.getInventory().selectedSlot;
      if (var2.slot() == var3) {
         mc.interactionManager.attackEntity(mc.player, entity);
         return true;
      }

      if (var2.isInHotBar()) {
         return this.recoveredMethod31(entity, var2.slot(), var3);
      }

      if (mc.player.currentScreenHandler.syncId != 0) {
         return false;
      }

      int var4 = this.recoveredMethod33(var3);
      if (var4 == -1) {
         return false;
      }

      this.recoveredMethod32(var2.slot(), var4);
      boolean var5 = false;

      try {
         var5 = this.recoveredMethod31(entity, var4, var3);
      } finally {
         this.recoveredMethod32(var2.slot(), var4);
      }

      return var5;
   }

   private boolean recoveredMethod31(PlayerEntity entity, int attackSlot, int previousSlot) {
      if (mc.player != null && mc.player.networkHandler != null && mc.interactionManager != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(attackSlot));

         try {
            mc.interactionManager.attackEntity(mc.player, entity);
            return true;
         } finally {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
         }
      } else {
         return false;
      }
   }

   private void recoveredMethod32(int inventorySlot, int hotbarSlot) {
      if (mc.player != null && mc.interactionManager != null && mc.player.networkHandler != null) {
         SwapManager.swapInventorySlotHidden(inventorySlot, hotbarSlot);
      }
   }

   private int recoveredMethod33(int previousSlot) {
      for (int var2 = 8; var2 >= 0; var2--) {
         if (var2 != previousSlot) {
            return var2;
         }
      }

      return previousSlot >= 0 && previousSlot < 9 ? previousSlot : -1;
   }

   private Hand recoveredMethod34() {
      if (mc.player != null && mc.player.isBlocking()) {
         Hand var1 = mc.player.getActiveHand();
         if (var1 == null) {
            return null;
         } else {
            return this.recoveredMethod37(mc.player.getStackInHand(var1)) ? var1 : null;
         }
      } else {
         return null;
      }
   }

   private boolean recoveredMethod35() {
      return mc.player != null && mc.player.isUsingItem() ? !this.recoveredField6.isState() || this.recoveredMethod34() == null : false;
   }

   private void recoveredMethod36(Hand hand) {
      if (mc.player != null && mc.interactionManager != null && hand != null) {
         if (this.recoveredMethod37(mc.player.getStackInHand(hand)) && !mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, hand);
         }
      }
   }

   private boolean recoveredMethod37(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof ShieldItem;
   }

   private boolean recoveredMethod38() {
      Item var1 = mc.player.getMainHandStack().getItem();
      return var1 != Items.AIR
         && (
            var1 instanceof SwordItem
               || var1 instanceof PickaxeItem
               || var1 instanceof AxeItem
               || var1 instanceof HoeItem
               || var1 instanceof ShovelItem
               || var1 instanceof MaceItem
               || var1 == Items.MACE
         );
   }

   private boolean recoveredMethod39(LivingEntity entity) {
      if (entity != null && entity != mc.player) {
         if (!entity.isAlive() || entity.getHealth() <= 0.0F) {
            return false;
         }

         if (entity instanceof ArmorStandEntity) {
            return false;
         }

         if (!(entity instanceof IronGolemEntity) && !(entity instanceof BatEntity)) {
            if (entity instanceof PlayerEntity var2) {
               if (!this.recoveredField1.is("Игроки")) {
                  return false;
               }

               if (this.recoveredMethod40(var2)) {
                  return false;
               }

               if (this.recoveredMethod41(var2) && !this.recoveredField1.is("Голые")) {
                  return false;
               }

               if (var2.hasStatusEffect(StatusEffects.INVISIBILITY) && !this.recoveredField1.is("Невидимки")) {
                  return false;
               }
            } else if (!(entity instanceof PassiveEntity) && !(entity instanceof CodEntity)) {
               if (!(entity instanceof HostileEntity)) {
                  return false;
               }

               if (!this.recoveredField1.is("Мобы")) {
                  return false;
               }
            } else if (!this.recoveredField1.is("Мирные")) {
               return false;
            }

            Vec3d var5 = BestPoint.getNearestPoint(entity);
            if (var5 == null) {
               var5 = MultipointUtils.getClosestPoint(entity);
            }

            if (mc.player.getEyePos().distanceTo(var5) > this.recoveredMethod70()) {
               return false;
            }

            if (this.recoveredMethod46()) {
               if (!this.recoveredMethod50(entity)) {
                  return false;
               }

               if (mc.player.canSee(entity)) {
                  if (entity == this.recoveredField13) {
                     this.recoveredField36 = 0L;
                  }
               } else {
                  if (entity != this.recoveredField13) {
                     return false;
                  }

                  long var3 = System.nanoTime();
                  if (this.recoveredField36 == 0L) {
                     this.recoveredField36 = var3;
                  }

                  if (var3 - this.recoveredField36 > 180000000L) {
                     return false;
                  }
               }
            }

            return this.recoveredField5.isState() || this.recoveredField17.isState() || mc.player.canSee(entity);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean recoveredMethod40(PlayerEntity player) {
      if (player != null && AuraServices.INSTANCE.friendStorage != null) {
         return player.getName() != null && AuraServices.INSTANCE.friendStorage.isFriend(player.getName().getString())
            ? true
            : player.getGameProfile() != null && AuraServices.INSTANCE.friendStorage.isFriend(player.getGameProfile().getName());
      } else {
         return false;
      }
   }

   private boolean recoveredMethod41(PlayerEntity player) {
      for (ItemStack var3 : player.getArmorItems()) {
         if (!var3.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private boolean recoveredMethod42() {
      if (mc.player.getAttackCooldownProgress(1.5F) < IdealHitUtils.getAICooldown()) {
         return false;
      }

      EntityHitResult var1 = this.recoveredMethod16();
      if (this.recoveredMethod14() || var1 != null && var1.getEntity() == this.recoveredField13) {
         if (this.rotationType.is("Data") && !this.recoveredMethod14() && !this.recoveredMethod69(var1)) {
            return false;
         }

         if (mc.player.isGliding() && this.recoveredField13.isGliding()) {
            Vec3d var5 = this.recoveredField13.getPos().add(0.0, this.recoveredField13.getHeight() / 2.0, 0.0);
            byte var3 = 0;
            Vec3d var6 = PredictUtils.predict(this.recoveredField13, var5, var3);
            if (mc.player.getEyePos().distanceTo(var6) > 5.0) {
               return false;
            }
         } else {
            double var2 = mc.player.getEyePos().distanceTo(this.recoveredField13.getBoundingBox().getCenter());
            Vec3d var4 = var2 > 3.0 ? BestPoint.getNearestPoint(this.recoveredField13) : this.recoveredField13.getBoundingBox().getCenter();
            if (var4 == null) {
               var4 = MultipointUtils.getClosestPoint(this.recoveredField13);
            }

            if (mc.player.getEyePos().distanceTo(var4) > this.recoveredField2.getValue().floatValue()) {
               return false;
            }
         }

         return IdealHitUtils.canCritical(this.recoveredField13);
      } else {
         return false;
      }
   }

   private boolean recoveredMethod43() {
      EntityHitResult var1 = this.recoveredMethod17(this.recoveredField31, this.recoveredField32);
      if (var1 != null && var1.getEntity() == this.recoveredField13) {
         Vec3d var2 = mc.player.getEyePos();
         Vec3d var3 = this.recoveredField13.getBoundingBox().getCenter().subtract(var2);
         if (var3.lengthSquared() < 1.0E-8) {
            return false;
         }

         Vec3d var4 = Vec3d.fromPolar(this.recoveredField32, this.recoveredField31);
         double var5 = MathHelper.clamp(var4.normalize().dotProduct(var3.normalize()), -1.0, 1.0);
         return Math.toDegrees(Math.acos(var5)) <= 22.0;
      } else {
         return false;
      }
   }

   private boolean recoveredMethod44(LivingEntity entity) {
      if (entity != null && entity == this.recoveredField13 && mc.player != null) {
         float var2 = Math.max(0.62F, IdealHitUtils.getAICooldown() - 0.18F);
         if (this.recoveredField41 > 3 && mc.player.getAttackCooldownProgress(1.5F) < var2) {
            return false;
         }

         Box var3 = entity.getBoundingBox();
         Vec3d var4 = mc.player.getEyePos();
         double var5 = MathHelper.clamp(var4.x, var3.minX, var3.maxX);
         double var7 = MathHelper.clamp(var4.y, var3.minY, var3.maxY);
         double var9 = MathHelper.clamp(var4.z, var3.minZ, var3.maxZ);
         double var11 = var4.x - var5;
         double var13 = var4.y - var7;
         double var15 = var4.z - var9;
         double var17 = this.recoveredField2.getValue().floatValue() + 0.4F;
         return var11 * var11 + var13 * var13 + var15 * var15 <= var17 * var17;
      } else {
         return false;
      }
   }

   private void recoveredMethod45() {
      if (this.recoveredField28) {
         this.recoveredField28 = false;
         this.recoveredField20.reset();
      }
   }

   private boolean recoveredMethod46() {
      return this.rotationType.is("Holy2");
   }

   private RotationsSystem recoveredMethod47() {
      return this.rotationType.is("Holy2") ? this.recoveredField23 : this.recoveredField22;
   }

   private void recoveredMethod48(LivingEntity entity) {
      if (this.rotationType.is("Holy2")) {
         this.recoveredField23.prepareAttack(entity);
      } else {
         this.recoveredField22.prepareAttack(entity);
      }
   }

   private void recoveredMethod49() {
      if (this.rotationType.is("Holy2")) {
         this.recoveredField23.onAttack();
      } else {
         this.recoveredField22.onAttack();
      }
   }

   private boolean recoveredMethod50(LivingEntity entity) {
      return this.rotationType.is("Holy2") ? this.recoveredField23.isInFov(entity) : this.recoveredField22.isInFov(entity);
   }

   private void recoveredMethod51() {
      this.recoveredField22.reset();
      this.recoveredField23.reset();
   }

   private void recoveredMethod52() {
      if (this.recoveredField30) {
         this.recoveredField30 = false;
         this.recoveredMethod51();
         this.recoveredField34 = null;
         this.recoveredField33 = false;
         this.recoveredField36 = 0L;
         this.recoveredField35 = false;
      }
   }

   private void recoveredMethod53() {
      if (this.recoveredField27) {
         this.recoveredField27 = false;
         this.recoveredField26.reset();
      }
   }

   private void recoveredMethod54() {
      if (this.recoveredField29) {
         this.recoveredField29 = false;
         this.recoveredField21.reset();
      }

      this.recoveredField34 = null;
      this.recoveredField33 = false;
      this.recoveredField36 = 0L;
   }

   public boolean isHolyWorldActive() {
      return this.isEnable() && this.recoveredMethod46();
   }

   public boolean isHolyWorldAttackInProgress() {
      return this.isHolyWorldActive() && this.recoveredField35;
   }

   private void recoveredMethod55() {
      if (mc.player != null && mc.interactionManager != null) {
         this.recoveredMethod62();
         this.recoveredMethod63();
         if (this.recoveredField52) {
            this.recoveredField52 = false;
            SwapManager.closeHiddenInventory();
         } else if (this.recoveredField49) {
            if (this.recoveredField48 >= 0 && this.recoveredField48 < 36) {
               this.recoveredField50--;
               if (this.recoveredField50 <= 0) {
                  this.recoveredField49 = false;
                  this.recoveredMethod60();
               }
            } else {
               this.recoveredMethod61();
            }
         } else if (!this.recoveredField11.isState()) {
            this.recoveredMethod59();
         } else if (this.recoveredField13 != null && this.recoveredMethod39(this.recoveredField13)) {
            if (this.recoveredMethod56()) {
               this.recoveredMethod58();
            } else {
               this.recoveredMethod59();
            }
         } else {
            this.recoveredMethod59();
         }
      } else {
         this.recoveredMethod61();
      }
   }

   private boolean recoveredMethod56() {
      if (mc.player != null && mc.world != null) {
         double var1 = 25.0;
         int var3 = 0;
         boolean var4 = false;

         for (PlayerEntity var6 : mc.world.getPlayers()) {
            if (var6 != mc.player && var6.isAlive() && !var6.isSpectator() && !(mc.player.squaredDistanceTo(var6) > var1)) {
               var3++;
               if (this.recoveredMethod57(var6)) {
                  var4 = true;
               }
            }
         }

         return (var3 == 1 || var3 == 2) && !var4;
      } else {
         return false;
      }
   }

   private boolean recoveredMethod57(PlayerEntity player) {
      ItemStack var2 = player.getMainHandStack();
      return var2 != null && !var2.isEmpty() && Registries.ITEM.getId(var2.getItem()).getPath().endsWith("_sword");
   }

   private void recoveredMethod58() {
      if (this.recoveredField13 == null || !this.recoveredMethod39(this.recoveredField13)) {
         this.recoveredMethod59();
      } else if (!this.recoveredMethod68(this.recoveredMethod66())) {
         if (!this.recoveredField47 || this.recoveredField48 < 0) {
            int var1 = this.recoveredMethod67();
            if (var1 != -1) {
               this.recoveredField49 = true;
               this.recoveredField50 = (int)this.recoveredField12.get();
               this.recoveredField51 = false;
               this.recoveredField48 = var1;
               this.recoveredMethod64();
            }
         }
      }
   }

   private void recoveredMethod59() {
      if (this.recoveredField47) {
         if (mc.player == null || mc.interactionManager == null) {
            this.recoveredMethod61();
         } else if (this.recoveredField48 >= 0 && this.recoveredField48 < 36 && this.recoveredMethod68(this.recoveredMethod66())) {
            this.recoveredField49 = true;
            this.recoveredField50 = (int)this.recoveredField12.get();
            this.recoveredField51 = true;
            this.recoveredMethod64();
         } else {
            this.recoveredMethod61();
         }
      }
   }

   private void recoveredMethod60() {
      if (mc.player == null || mc.interactionManager == null) {
         this.recoveredMethod61();
      } else if (this.recoveredField48 < 0 || this.recoveredField48 >= 36) {
         this.recoveredMethod61();
      } else if (!SwapManager.openHiddenInventory()) {
         this.recoveredMethod61();
      } else {
         mc.interactionManager
            .clickSlot(mc.player.currentScreenHandler.syncId, SwapManager.toContainerSlot(this.recoveredField48), 40, SlotActionType.SWAP, mc.player);
         this.recoveredField52 = true;
         this.recoveredField53 = Math.max(this.recoveredField53, 2);
         this.recoveredField55 = mc.player.getInventory().getStack(this.recoveredField48).copy();
         this.recoveredField56 = 10;
         if (this.recoveredField51) {
            this.recoveredField47 = false;
            this.recoveredField48 = -1;
            this.recoveredField51 = false;
            this.recoveredField49 = false;
            this.recoveredField50 = 0;
         } else {
            this.recoveredField47 = true;
         }
      }
   }

   private void recoveredMethod61() {
      this.recoveredField47 = false;
      this.recoveredField48 = -1;
      this.recoveredField51 = false;
      this.recoveredField49 = false;
      this.recoveredField50 = 0;
      this.recoveredField52 = false;
      this.recoveredField55 = ItemStack.EMPTY;
      this.recoveredField56 = 0;
   }

   private void recoveredMethod62() {
      if (this.recoveredField56 > 0) {
         this.recoveredField56--;
         if (SwapManager.matches(mc.player.getOffHandStack(), this.recoveredField55)) {
            this.recoveredField56 = 0;
         }

         if (this.recoveredField56 == 0) {
            this.recoveredField55 = ItemStack.EMPTY;
         }
      }
   }

   private void recoveredMethod63() {
      if (this.recoveredField53 > 0) {
         this.recoveredField53--;
         if (this.recoveredField53 == 0) {
            this.recoveredMethod65();
         }
      }
   }

   private void recoveredMethod64() {
      this.recoveredField53 = Math.max(this.recoveredField53, 2);
      if (!this.recoveredField54) {
         SprintPause.pushPause(0L);
         this.recoveredField54 = true;
      }
   }

   private void recoveredMethod65() {
      if (this.recoveredField54) {
         SprintPause.popPause();
         this.recoveredField54 = false;
      }
   }

   private ItemStack recoveredMethod66() {
      if (!this.recoveredField55.isEmpty()) {
         return this.recoveredField55;
      } else {
         return mc.player == null ? ItemStack.EMPTY : mc.player.getOffHandStack();
      }
   }

   private int recoveredMethod67() {
      for (int var1 = 0; var1 < 36; var1++) {
         ItemStack var2 = mc.player.getInventory().getStack(var1);
         if (this.recoveredMethod68(var2)) {
            return var1;
         }
      }

      return -1;
   }

   private boolean recoveredMethod68(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.isOf(Items.PLAYER_HEAD) && stack.getName().getString().contains("Сфера Цербера");
   }

   public int getWhiteRiseTicksToAttack() {
      return this.recoveredField42;
   }

   private boolean recoveredMethod69(EntityHitResult result) {
      float var2 = Math.abs(MathHelper.wrapDegrees(this.recoveredField15.x - mc.player.getYaw()));
      float var3 = Math.abs(this.recoveredField15.y - mc.player.getPitch());
      boolean var4 = var2 <= 1.15F && var3 <= 0.9F;
      boolean var5 = result != null && result.getEntity() == this.recoveredField13;
      return var4 && var5;
   }

   public boolean isAboveWater() {
      BlockPos var1 = BlockPos.ofFloored(mc.player.getPos().add(0.0, -0.4, 0.0));
      return !mc.player.isSubmergedInWater() && mc.world.getBlockState(var1).isOf(Blocks.WATER);
   }

   public float getAttackCooldown() {
      return MathHelper.clamp(((ILivingEntity)mc.player).getLastAttackedTicks() / this.getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
   }

   public float getAttackCooldownProgressPerTick() {
      return (float)(1.0 / mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED) * 20.0);
   }

   private float recoveredMethod70() {
      return mc.player.isGliding()
         ? this.recoveredField4.getValue().floatValue()
         : this.recoveredField2.getValue().floatValue() + this.recoveredField3.getValue().floatValue();
   }

   @Override
   public void onDisable() {
      if(bkAdaptiveActive) {bkAdaptive.endEmbedded();bkAdaptiveActive=false;}
      if(RotationStorage.instance!=null) RotationStorage.instance.stopRotation();
      super.onDisable();
      if (this.recoveredField47
         && mc.player != null
         && mc.interactionManager != null
         && mc.player.networkHandler != null
         && this.recoveredField48 >= 0
         && this.recoveredField48 < 36
         && this.recoveredMethod68(mc.player.getOffHandStack())
         && mc.player.currentScreenHandler.syncId == 0) {
         SwapManager.swapInventorySlotHidden(this.recoveredField48, 40);
      }

      this.recoveredMethod61();
      this.recoveredField53 = 0;
      if (this.recoveredField54) {
         SprintPause.popPause();
         this.recoveredField54 = false;
      }

      if (this.recoveredField13 != null) {
         this.recoveredField37.reset();
      }

      this.recoveredField13 = null;
      this.recoveredField19.reset();
      this.recoveredMethod51();
      this.recoveredField30 = false;
      this.recoveredField20.reset();
      this.recoveredField28 = false;
      this.recoveredField21.reset();
      this.recoveredField29 = false;
      this.recoveredField33 = false;
      this.recoveredField34 = null;
      this.recoveredField35 = false;
      this.recoveredField36 = 0L;
      this.recoveredField24.reset();
      this.recoveredField25.reset();
      this.recoveredField46 = null;
      this.recoveredField42 = 0;
      this.recoveredField41 = 0;
      this.recoveredMethod71();
   }

   @Override
   public void onEnable() {
      bkAdaptiveActive=false;
      super.onEnable();
      this.recoveredMethod61();
      this.recoveredField19.reset();
      this.recoveredMethod51();
      this.recoveredField30 = false;
      this.recoveredField20.reset();
      this.recoveredField28 = false;
      this.recoveredField21.reset();
      this.recoveredField29 = false;
      this.recoveredField33 = false;
      this.recoveredField34 = null;
      this.recoveredField35 = false;
      this.recoveredField36 = 0L;
      this.recoveredField24.reset();
      this.recoveredField25.reset();
      this.recoveredField46 = null;
      this.recoveredField42 = 0;
      this.recoveredField41 = 0;
      this.recoveredMethod71();
      if (mc.player != null) {
         this.recoveredField14 = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   private void recoveredMethod71() {
      this.recoveredField43 = -1;
      this.recoveredField44 = false;
      this.recoveredField45 = null;
   }

   @Generated
   public LivingEntity getTarget() {
      return this.recoveredField13;
   }

   @Generated
   public Vec2f getCurrentRotations() {
      return this.recoveredField14;
   }

   @Generated
   public Vec2f getTargetRotations() {
      return this.recoveredField15;
   }

   @Generated
   public TimerUtils getAttackTimer() {
      return this.recoveredField16;
   }
}
