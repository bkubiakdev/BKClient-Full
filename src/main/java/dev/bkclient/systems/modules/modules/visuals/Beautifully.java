package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.utility.animation.base.Easing;

@ModuleInfo(name = "Beautifully", category = ModuleCategory.VISUALS, enabledByDefault = true,
   desc = "modules.descriptions.beautifully")
public final class Beautifully extends BaseModule {
   private static Beautifully instance;
   private final SelectSetting animations = new SelectSetting(this, "modules.settings.beautifully.select");
   private final SelectSetting.Value camera = new SelectSetting.Value(animations, "modules.settings.beautifully.smooth_f5").select();
   private final SelectSetting.Value inventory = new SelectSetting.Value(animations, "modules.settings.beautifully.inventory_animation").select();
   private final SelectSetting.Value chat = new SelectSetting.Value(animations, "modules.settings.beautifully.chat_animation").select();
   private final SelectSetting.Value tab = new SelectSetting.Value(animations, "modules.settings.beautifully.tab_animation").select();
   public static final Easing OPEN = Easing.generate(0.45F, 1.45F, 0.43F, 0.91F);
   public static final Easing OUT_CUBIC = (t, b, c, d) -> { float p = t / d - 1; return c * (p*p*p + 1) + b; };
   public static final Easing IN_CUBIC = (t, b, c, d) -> { float p = t / d; return c * p*p*p + b; };

   public Beautifully() { instance = this; }
   private static boolean active() { return instance != null && instance.isEnabled(); }
   public static boolean cameraAnimation() { return active() && instance.camera.isSelected(); }
   public static boolean inventoryAnimation() { return active() && instance.inventory.isSelected(); }
   public static boolean chatAnimation() { return active() && instance.chat.isSelected(); }
   public static boolean tabAnimation() { return active() && instance.tab.isSelected(); }
   public static float progress(long openedAt) {
      return Math.clamp((System.currentTimeMillis() - openedAt) / 200.0F, 0, 1);
   }
}
