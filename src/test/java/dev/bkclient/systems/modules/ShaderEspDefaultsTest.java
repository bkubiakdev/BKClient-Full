package dev.bkclient.systems.modules;
import dev.bkclient.systems.modules.modules.visuals.ShaderESP;
import dev.bkclient.systems.setting.settings.SliderSetting;
import com.google.gson.JsonPrimitive;
public final class ShaderEspDefaultsTest {
 public static void main(String[] args) {
  var module = new ShaderESP();
  check(module.opacity.getCurrentValue() > .9F, "Default opacity must be visible");
  check(module.glowStrength.getCurrentValue() > 1F, "Default glow must be visible");
  check(module.fillOpacity.getCurrentValue() > .2F, "Default fill must be visible");
  for (var setting : module.getSettings()) if (setting instanceof SliderSetting slider) {
   check(Float.isFinite(slider.getCurrentValue()) && slider.getCurrentValue() >= slider.getMin()
      && slider.getCurrentValue() <= slider.getMax(), "Invalid default: " + slider.getName());
   slider.load(new JsonPrimitive(0));
  }
  module.onEnable();
  check(module.glowStrength.getCurrentValue() > 1F && module.saturation.getCurrentValue() > 1F, "Broken configuration repaired");
  module.subtle.getAction().run();
  float soft = module.glowStrength.getCurrentValue();
  module.onEnable();
  check(module.glowStrength.getCurrentValue() == soft, "Existing preset preserved");
  module.recommended.getAction().run();
  check(module.glowStrength.getCurrentValue() > soft, "Recommended preset applied");
  System.out.println("Shader ESP: visible defaults, saved-zero recovery and presets passed");
 }
 private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
