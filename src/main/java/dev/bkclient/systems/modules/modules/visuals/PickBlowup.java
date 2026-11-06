package dev.bkclient.systems.modules.modules.visuals;
import dev.bkclient.systems.modules.api.*;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.utility.colors.Colors;

import java.util.*;
import net.minecraft.util.math.*;
import net.minecraft.entity.ItemEntity;
import dev.bkclient.utility.render.EffectLines;
@ModuleInfo(name="PickBlowup",category=ModuleCategory.VISUALS,desc="Energy burst when picking up items")
public final class PickBlowup extends BaseModule {
 private final ColorSetting color=new ColorSetting(this,"Color").color(Colors.ACCENT);
 private final ButtonSetting test=new ButtonSetting(this,"Test").action(()->{if(mc.player!=null){if(!isEnabled())enable();spawn(mc.player.getPos().add(mc.player.getRotationVec(1).multiply(2)));}});
 private final List<ColorModuleSupport> effects=new ArrayList<>();
 private final Random nu0=new Random();
 private EffectLines lines;
 private Object world;
 private void spawn(Vec3d pos){if(world!=mc.world){effects.clear();world=mc.world;}if(effects.size()>=16)effects.remove(0);effects.add(new ColorModuleSupport(this,pos,System.currentTimeMillis(),color.getColor().getRGB()));}
 public void pickup(int entityId,int collectorId){if(!isEnabled()||mc.player==null||mc.world==null||collectorId!=mc.player.getId())return;if(mc.world.getEntityById(entityId) instanceof ItemEntity item && !item.getStack().isEmpty())spawn(item.getPos().add(0,.01,0));}
 private final EventListener<Render3DEvent> render=event->{
   if(world!=mc.world){effects.clear();world=mc.world;}if(mc.world==null||effects.isEmpty())return;
   long now=System.currentTimeMillis();effects.removeIf(e->now-e.dgsgb>=1650);
   try(EffectLines batch=new EffectLines(event)){lines=batch;for(ColorModuleSupport e:effects)olqifi(e,MathHelper.clamp((now-e.dgsgb)/1650f,0,1));}finally{lines=null;}
 };
 @Override public void onDisable(){effects.clear();world=null;}
      public void olqifi(ColorModuleSupport effect, float progress) {
      float value = this.ccqlow(Math.min(1F, progress * 8.0F));
      float value2 = 1F - this.ccqlow(progress);
      float value3 = value * value2;
      float value4 = this.stopMethod97(progress);
      float value5 = MathHelper.lerp(value4, 0.07F, 0.68F);
      Vec3d position = effect.bdvaa.add(0, 0.025 + Math.sin(progress * 3.141592653589793) * 0.11, 0);
      this.czac(position, value5 * 1.08F, effect.stopField12, value3 * 0.16F, 5.5F);
      this.czac(position, value5, effect.stopField12, value3 * 0.88F, 1.75F);
      this.czac(position.add(0, 0.035, 0), value5 * 0.78F, EffectLines.mix(effect.stopField12, -1, 0.62F), value3 * 0.72F, 1.15F);
      this.cyw(effect, progress, value3);

      for (PickFragment moduleRange7 : effect.manageField750) {
         float value6 = MathHelper.clamp((progress - moduleRange7.delay()) / (1F - moduleRange7.delay()), 0F, 1F);
         if (!(value6 <= 0F) && !(value6 >= 1F)) {
            float value7 = (1F - this.ccqlow(value6)) * value3;
            Vec3d position2 = this.initMethod77(effect.bdvaa, moduleRange7, value6);
            Vec3d position3 = this.initMethod77(effect.bdvaa, moduleRange7, Math.max(0F, value6 - 0.075F));
            int index = EffectLines.mix(effect.stopField12, -1, moduleRange7.whiteMix());
            lines.line(position3, position2, EffectLines.alpha(index, value7 * 0.2F), 4.0F, true);
            lines.line(position3, position2, EffectLines.alpha(index, value7), 1.25F, true);
         }
      }
   }

      public void cyw(ColorModuleSupport effect, float progress, float alpha) {
      float value = progress * 3.1415927F * 3.4F;

      for (int index = 0; index < 3; index++) {
         Vec3d position = null;

         for (int index2 = 0; index2 <= 28; index2++) {
            float value2 = index2 / 28.0F;
            float value3 = (float)Math.sin(value2 * 3.141592653589793);
            float value4 = MathHelper.lerp(value2, 0.31F, 0.035F) * (0.88F + 0.12F * (float)Math.sin(value2 * 3.141592653589793 * 4.0 + value));
            double value5 = value + index * 3.141592653589793 * 2.0 / 3.0 + value2 * 3.141592653589793 * 3.25;
            Vec3d position2 = effect.bdvaa.add(Math.sin(value5) * value4, 0.04 + value2 * 1.08, Math.cos(value5) * value4);
            if (position != null) {
               int index3 = EffectLines.mix(effect.stopField12, -1, value2 * 0.72F);
               float value6 = alpha * value3;
               lines.line(position, position2, EffectLines.alpha(index3, value6 * 0.14F), 5.0F, true);
               lines.line(position, position2, EffectLines.alpha(index3, value6 * 0.82F), 1.35F, true);
            }

            position = position2;
         }
      }
   }

      public Vec3d initMethod77(Vec3d origin, PickFragment spark, float progress) {
      double value = spark.angle() + spark.turn() * progress;
      double value2 = MathHelper.lerp(progress, spark.startRadius(), spark.endRadius());
      double value3 = spark.lift() * this.stopMethod97(progress) + Math.sin(progress * 3.141592653589793) * 0.08;
      return origin.add(Math.sin(value) * value2, value3, Math.cos(value) * value2);
   }

      public float ccqlow(float value) {
      value = MathHelper.clamp(value, 0F, 1F);
      return value * value * (3.0F - 2.0F * value);
   }

      public float stopMethod97(float value) {
      value = MathHelper.clamp(value, 0F, 1F);
      float value2 = 1F - value;
      return 1F - value2 * value2 * value2;
   }

      public void czac(Vec3d origin, float radius, int color, float alpha, float width) {
      Vec3d position = null;
      int index = EffectLines.alpha(color, alpha);

      for (int index2 = 0; index2 <= 48; index2++) {
         double value = 6.283185307179586 * index2 / 48.0;
         Vec3d position2 = origin.add(Math.sin(value) * radius, 0, Math.cos(value) * radius);
         if (position != null) {
            lines.line(position, position2, index, width, true);
         }

         position = position2;
      }
   }

      public int sdinxh() {
      return this.color.getColor().getRGB();
   }

private static final class ColorModuleSupport {
      public final Vec3d bdvaa;
      public final long dgsgb;
      public final int stopField12;
      public final List<PickFragment> manageField750 = new ArrayList<>(48);

   public ColorModuleSupport(final PickBlowup pickBlowupModule, Vec3d origin, long startedAt, int color) {
      this.bdvaa = origin;
      this.dgsgb = startedAt;
      this.stopField12 = color;

      for (int index = 0; index < 48; index++) {
         double value = pickBlowupModule.nu0.nextDouble() * 3.141592653589793 * 2.0;
         double value2 = 0.025 + pickBlowupModule.nu0.nextDouble() * 0.08;
         double value3 = 0.28 + pickBlowupModule.nu0.nextDouble() * 0.4;
         double value4 = 0.24 + pickBlowupModule.nu0.nextDouble() * 0.72;
         double value5 = (pickBlowupModule.nu0.nextDouble() - 0.5) * 2.2;
         float value6 = pickBlowupModule.nu0.nextFloat() * 0.24F;
         float value7 = 0.2F + pickBlowupModule.nu0.nextFloat() * 0.65F;
         this.manageField750.add(new PickFragment(value, value2, value3, value4, value5, value6, value7));
      }
   }

}
private record PickFragment(
   double angle,
   double startRadius,
   double endRadius,
   double lift,
   double turn,
   float delay,
   float whiteMix
) {

      public double angle() {
      return this.angle;
   }

      public double startRadius() {
      return this.startRadius;
   }

      public double endRadius() {
      return this.endRadius;
   }

      public double lift() {
      return this.lift;
   }

      public double turn() {
      return this.turn;
   }

      public float delay() {
      return this.delay;
   }

      public float whiteMix() {
      return this.whiteMix;
   }

}

}
