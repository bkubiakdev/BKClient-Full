package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;

/** Source settings with a BK render hook; native Zoom interpolation was not included in SystemSRC. */
@ModuleInfo(name="Zoom", category=ModuleCategory.VISUALS, desc="Hold a key to zoom; optionally adjust FOV with the mouse wheel.")
public final class Zoom extends BaseModule {
   private static Zoom instance;
   public final BindSetting zoomKey=new BindSetting(this,"Zoom key").key(-1);
   public final SliderSetting zoomFov=new SliderSetting(this,"Zoom FOV").min(1).max(110).currentValue(30).step(1);
   public final SliderSetting speed=new SliderSetting(this,"Speed").min(.1F).max(10).currentValue(1).step(.1F);
   public final BooleanSetting scrollControl=new BooleanSetting(this,"Scroll control");
   public final SliderSetting scrollStep=new SliderSetting(this,"Scroll step",()->!scrollControl.isEnabled()).min(1).max(30).currentValue(5).step(1);
   private boolean zooming;
   private double wheelFov, currentFov;
   private long lastFrame;
   public Zoom() { instance=this; }
   private boolean allowed() { return isEnabled() && mc.player!=null && mc.world!=null && mc.currentScreen==null && !mc.isPaused() && mc.isWindowFocused(); }
   public static double interpolate(double current,double target,double speed,double seconds) {
      return current+(target-current)*(1-Math.exp(-Math.max(0,seconds)*speed*12));
   }
   public static float apply(float original) {
      Zoom self=instance;
      if(self==null) return original;
      if(!self.allowed()) { self.reset(); return original; }
      boolean pressed=FreeLook.held(self.zoomKey.getKey());
      if(pressed && !self.zooming) self.wheelFov=self.zoomFov.getCurrentValue();
      self.zooming=pressed;
      long now=System.nanoTime();
      double dt=self.lastFrame==0 ? 0 : Math.min(.25,(now-self.lastFrame)/1_000_000_000.0);
      if(self.lastFrame==0) self.currentFov=original;
      self.lastFrame=now;
      double target=pressed ? (self.scrollControl.isEnabled()?self.wheelFov:self.zoomFov.getCurrentValue()) : original;
      self.currentFov=interpolate(self.currentFov,target,self.speed.getCurrentValue(),dt);
      if(!pressed && Math.abs(self.currentFov-original)<.01) self.currentFov=original;
      return (float)self.currentFov;
   }
   public static boolean scroll(double amount) {
      Zoom self=instance;
      if(self==null || !self.allowed() || !self.scrollControl.isEnabled() || !FreeLook.held(self.zoomKey.getKey())) return false;
      if(!self.zooming) {self.wheelFov=self.zoomFov.getCurrentValue();self.zooming=true;}
      self.wheelFov=Math.clamp(self.wheelFov-amount*self.scrollStep.getCurrentValue(),1,110);
      return true;
   }
   private void reset() { zooming=false; currentFov=0; lastFrame=0; }
   @Override public void onDisable() { reset(); }
}
