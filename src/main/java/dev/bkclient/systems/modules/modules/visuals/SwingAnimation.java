package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HandRenderEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.constructions.swinganim.SwingAnimScreen;
import dev.bkclient.systems.modules.constructions.swinganim.SwingTransformations;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.combat.Aura;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;

@ModuleInfo(name = "Swing Animation", category = ModuleCategory.VISUALS, desc = "Changes hand swing animations")
public class SwingAnimation extends BaseModule {
   private final BooleanSetting onlyAura = new BooleanSetting(this, "swing.only_aura");
   private final ButtonSetting button = new ButtonSetting(this, "swing.open_menu").action(() -> mc.setScreen(new SwingAnimScreen()));
   private final EventListener<HandRenderEvent> onHandRender = event -> {
      if (this.shouldApplyAnimation(event.getItemStack()) && event.getArm() == mc.options.getMainArm().getValue()) {
         MatrixStack matrices = event.getMatrices();
         float swingProgress = event.getSwingProgress();
         SwingTransformations trans = BKClientInit.getInstance().getSwingManager().transformations(swingProgress);
         trans.apply(matrices, event.getArm() == Arm.LEFT);
         event.cancel();
      }
   };

   public boolean shouldApplyAnimation(ItemStack itemStack) {
      Aura auraModule = BKClientInit.getInstance().getModuleManager().getModule(Aura.class);
      Entity target = BKClientInit.getInstance().getTargetManager().getCurrentTarget();
      Item item = itemStack.getItem();
      return !this.onlyAura.isEnabled() || dev.bkclient.combatengine.bridge.AuraRuntime.auraEnabled() && target != null
         ? item != Items.AIR
            && item != Items.FILLED_MAP
            && item != Items.CROSSBOW
            && item != Items.BOW
            && item != Items.TRIDENT
            && item.getUseAction(itemStack) != UseAction.DRINK
            && item.getUseAction(itemStack) != UseAction.EAT
         : false;
   }
}
