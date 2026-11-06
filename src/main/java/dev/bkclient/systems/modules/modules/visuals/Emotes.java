package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.emotes.EmoteManager;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.emotes.EmoteWheelScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Emotes and dances on the local player: a wheel of emotes opens on a key, the chosen animation plays on the player model.
 * Only this client shows them; other players see nothing unless they run the same client.
 */
@ModuleInfo(name = "Emotes", category = ModuleCategory.VISUALS, desc = "Emote wheel with dances and gestures for your player model")
public final class Emotes extends BaseModule {
   private static Emotes instance;

   private final BindSetting wheelKey = new BindSetting(this, "Wheel Key").key(GLFW.GLFW_KEY_B);
   private final BooleanSetting autoThirdPerson = new BooleanSetting(this, "Auto Third Person").enabled(true);
   private final BooleanSetting cancelOnMove = new BooleanSetting(this, "Cancel On Move").enabled(true);
   private final SliderSetting speed = new SliderSetting(this, "Speed").min(0.5F).max(2.5F).step(0.1F).currentValue(1.0F);
   private final BooleanSetting nearbyDance = new BooleanSetting(this, "Nearby Players Dance");
   private final SliderSetting nearbyRange = new SliderSetting(this, "Nearby Range", () -> !this.nearbyDance.isEnabled())
      .min(3.0F).max(40.0F).step(1.0F).currentValue(12.0F).suffix("blocks");

   public Emotes() {
      instance = this;
   }

   public static Emotes getInstance() {
      return instance;
   }

   public boolean autoThirdPerson() {
      return this.autoThirdPerson.isEnabled();
   }

   public boolean cancelOnMove() {
      return this.cancelOnMove.isEnabled();
   }

   public float speed() {
      return this.speed.getCurrentValue();
   }

   public boolean nearbyDance() {
      return this.nearbyDance.isEnabled();
   }

   public float nearbyRange() {
      return this.nearbyRange.getCurrentValue();
   }

   public int wheelKeyCode() {
      return this.wheelKey.getKey();
   }

   @Override
   public void onDisable() {
      EmoteManager.reset();
   }

   @Override
   public void tick() {
      EmoteManager.tick();
   }

   private final EventListener<KeyPressEvent> onKey = event -> {
      if (event.getAction() != GLFW.GLFW_PRESS || mc.player == null || mc.currentScreen != null) return;
      int key = this.wheelKey.getKey();
      if (key > 0 && event.getKey() == key) mc.setScreen(new EmoteWheelScreen(key));
   };
}
