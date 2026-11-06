package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** Local camera adaptation of SystemSRC FreeLook, independent of Aura's rotation storage. */
@ModuleInfo(name="FreeLook", category=ModuleCategory.VISUALS, desc="Hold a key to look around independently of the player.")
public final class FreeLook extends BaseModule {
   private static FreeLook instance;
   public final BindSetting lookKey = new BindSetting(this,"Look key").key(GLFW.GLFW_KEY_LEFT_ALT);
   public final BooleanSetting autoThirdPerson = new BooleanSetting(this,"Auto third person").enabled(true);
   private boolean active, changedPerspective;
   private Perspective previousPerspective;
   private Entity entity;
   private float yaw, pitch;
   public FreeLook() { instance=this; }
   public static boolean active() { return instance!=null && instance.active; }
   public static float yaw() { return instance.yaw; }
   public static float pitch() { return instance.pitch; }
   public static boolean held(int key) {
      if(mc.getWindow()==null || !mc.isWindowFocused()) return false;
      long window=mc.getWindow().getHandle();
      if(key>=0 && key<=7) return GLFW.glfwGetMouseButton(window,key)==GLFW.GLFW_PRESS;
      if(key>=-100 && key<=-93) return GLFW.glfwGetMouseButton(window,key+100)==GLFW.GLFW_PRESS;
      return key>=GLFW.GLFW_KEY_SPACE && key<=GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(window,key)==GLFW.GLFW_PRESS;
   }
   @Override public void tick() { refresh(); }
   public static void refresh() {
      FreeLook self=instance; if(self==null) return;
      boolean allowed=self.isEnabled() && mc.player!=null && mc.world!=null && mc.currentScreen==null && !mc.isPaused()
         && !dev.bkclient.systems.modules.modules.player.FreecamUtil.isActive()
         && !dev.bkclient.compat.PerspektiveCompatibility.isFreeLookActive() && held(self.lookKey.getKey());
      if(!allowed || self.active && self.entity!=mc.getCameraEntity()) { self.stop(); return; }
      if(!self.active && mc.getCameraEntity()!=null) {
         self.entity=mc.getCameraEntity(); self.yaw=self.entity.getYaw(); self.pitch=self.entity.getPitch();
         self.previousPerspective=mc.options.getPerspective();
         self.changedPerspective=self.autoThirdPerson.isEnabled() && self.previousPerspective.isFirstPerson();
         if(self.changedPerspective) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         self.active=true;
      }
   }
   public static void look(double dx,double dy) {
      if(!active()) return;
      instance.yaw += (float)(dx*.15);
      instance.pitch = MathHelper.clamp(instance.pitch+(float)(dy*.15),-90,90);
   }
   private void stop() {
      if(!active) return;
      active=false;
      if(changedPerspective) mc.options.setPerspective(previousPerspective);
      changedPerspective=false;
      // The source turns the camera entity toward the final view on release.
      if(entity!=null && entity==mc.getCameraEntity() && mc.world!=null) {
         entity.setYaw(entity.getYaw()+MathHelper.wrapDegrees(yaw-entity.getYaw()));
         entity.setPitch(pitch);
      }
      entity=null;
   }
   @Override public void onDisable() { stop(); }
}
