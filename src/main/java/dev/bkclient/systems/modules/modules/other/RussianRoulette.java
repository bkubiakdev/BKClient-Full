package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.game.MessageUtility;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.text.Text;

@ModuleInfo(name = "Russian Roulette", category = ModuleCategory.OTHER, desc = "Roll for a chat-only result")
public class RussianRoulette extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "Difficulty", "Select the chance of an unlucky roll");
   private final ModeSetting.Value easy = new ModeSetting.Value(this.mode, "Easy");
   private final ModeSetting.Value normal = new ModeSetting.Value(this.mode, "Medium");
   private final ModeSetting.Value veryHard = new ModeSetting.Value(this.mode, "Very Hard");

   @Override
   public void onEnable() {
      if (mc.world != null && mc.player != null && !isSilentToggle()) {
         int losingFaces = this.veryHard.isSelected() ? 3 : this.normal.isSelected() ? 2 : 1;
         boolean lucky = ThreadLocalRandom.current().nextInt(6) >= losingFaces;
         MessageUtility.info(Text.literal(Localizator.translate(
               lucky ? "roulette.result.lucky" : "roulette.result.unlucky")));
      }
      super.onEnable();
   }
}
