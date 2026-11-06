package dev.bkclient.combatengine.client.modules.impl.player;

import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventUpdate;
import dev.bkclient.combatengine.api.utils.player.SwapManager;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.bridge.SprintPause;
import dev.bkclient.combatengine.client.modules.settings.implement.FloatSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class AutoEat extends Module {
   public static final AutoEat INSTANCE = new AutoEat();
   private static final String recoveredField1 = "baritone.api.BaritoneAPI";
   private final FloatSetting recoveredField2 = new FloatSetting("Плашки голода", 6.0F, 1.0F, 10.0F, 1.0F);
   private boolean recoveredField3;
   private boolean recoveredField4;
   private boolean recoveredField5;
   private int recoveredField6 = -1;
   private int recoveredField7 = -1;

   public AutoEat() {
      super("AutoEat", "Автоматически ест при низком голоде", Module.ModuleCategory.PLAYER);
      this.addSettings(this.recoveredField2);
   }

   public static boolean shouldSuppressCombat() {
      return INSTANCE != null && INSTANCE.isEnable() && INSTANCE.recoveredField3;
   }

   @Override
   public void onDisable() {
      this.recoveredMethod11();
      super.onDisable();
   }

   @EventLink
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         this.recoveredMethod11();
      } else if (mc.currentScreen != null) {
         this.recoveredMethod11();
      } else if (!mc.player.getAbilities().creativeMode && !mc.player.isSpectator()) {
         if (!this.recoveredField3) {
            if (!this.recoveredMethod2()) {
               return;
            }

            this.recoveredField3 = true;
            this.recoveredField6 = mc.player.getInventory().selectedSlot;
         }

         this.recoveredMethod1();
      } else {
         this.recoveredMethod11();
      }
   }

   private void recoveredMethod1() {
      ClientPlayerEntity var1 = mc.player;
      if (var1 == null) {
         this.recoveredMethod11();
      } else {
         this.recoveredMethod14();
         if (!this.recoveredField4) {
            SprintPause.pushPause(0L);
            this.recoveredField4 = true;
         }

         mc.options.attackKey.setPressed(false);
         if (!this.recoveredMethod3()) {
            if (!var1.isUsingItem()) {
               this.recoveredMethod11();
            }
         } else if (!this.recoveredMethod5()) {
            this.recoveredMethod11();
         } else {
            Hand var2 = this.recoveredMethod6(var1);
            if (var2 == null) {
               this.recoveredMethod11();
            } else {
               mc.options.useKey.setPressed(true);
               if (!var1.isUsingItem() || var1.getActiveHand() != var2) {
                  mc.interactionManager.interactItem(var1, var2);
               }
            }
         }
      }
   }

   private boolean recoveredMethod2() {
      return this.recoveredMethod3() && !mc.player.isUsingItem() && (this.recoveredMethod8(mc.player.getOffHandStack()) || this.recoveredMethod7() != -1);
   }

   private boolean recoveredMethod3() {
      return mc.player != null && mc.player.getHungerManager().getFoodLevel() < 20 && mc.player.getHungerManager().getFoodLevel() <= this.recoveredMethod4();
   }

   private int recoveredMethod4() {
      return Math.round(this.recoveredField2.get()) * 2;
   }

   private boolean recoveredMethod5() {
      ClientPlayerEntity var1 = mc.player;
      if (var1 == null) {
         return false;
      } else if (this.recoveredMethod8(var1.getOffHandStack())) {
         return true;
      } else if (this.recoveredMethod8(var1.getMainHandStack())) {
         return true;
      } else {
         int var2 = this.recoveredMethod7();
         if (var2 == -1) {
            return false;
         } else if (var2 < 9) {
            this.recoveredField5 = false;
            this.recoveredField7 = -1;
            this.recoveredMethod9(var2);
            return this.recoveredMethod8(var1.getMainHandStack());
         } else {
            this.recoveredMethod9(this.recoveredField6 == -1 ? var1.getInventory().selectedSlot : this.recoveredField6);
            this.recoveredMethod10(var2, var1.getInventory().selectedSlot);
            this.recoveredField5 = true;
            this.recoveredField7 = var2;
            return this.recoveredMethod8(var1.getMainHandStack());
         }
      }
   }

   private Hand recoveredMethod6(ClientPlayerEntity player) {
      if (player == null) {
         return null;
      } else if (this.recoveredMethod8(player.getOffHandStack())) {
         return Hand.OFF_HAND;
      } else {
         return this.recoveredMethod8(player.getMainHandStack()) ? Hand.MAIN_HAND : null;
      }
   }

   private int recoveredMethod7() {
      ClientPlayerEntity var1 = mc.player;
      if (var1 == null) {
         return -1;
      }

      int var2 = var1.getInventory().selectedSlot;
      if (this.recoveredMethod8(var1.getInventory().getStack(var2))) {
         return var2;
      }

      for (int var3 = 0; var3 < 9; var3++) {
         if (var3 != var2 && this.recoveredMethod8(var1.getInventory().getStack(var3))) {
            return var3;
         }
      }

      for (int var4 = 9; var4 < 36; var4++) {
         if (this.recoveredMethod8(var1.getInventory().getStack(var4))) {
            return var4;
         }
      }

      return -1;
   }

   private boolean recoveredMethod8(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else {
         return !stack.isOf(Items.GOLDEN_APPLE) && !stack.isOf(Items.ENCHANTED_GOLDEN_APPLE) && !stack.isOf(Items.CHORUS_FRUIT)
            ? stack.getUseAction() == UseAction.EAT
            : false;
      }
   }

   private void recoveredMethod9(int slot) {
      if (mc.player != null && slot >= 0 && slot <= 8 && mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
         if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   }

   private void recoveredMethod10(int inventorySlot, int hotbarSlot) {
      if (mc.player != null && mc.interactionManager != null && inventorySlot >= 9 && inventorySlot <= 35 && hotbarSlot >= 0 && hotbarSlot <= 8) {
         SwapManager.swapInventorySlotHidden(inventorySlot, hotbarSlot);
      }
   }

   private void recoveredMethod11() {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      if (this.recoveredField4) {
         SprintPause.popPause();
         this.recoveredField4 = false;
      }

      this.recoveredMethod12();
      this.recoveredField3 = false;
   }

   private void recoveredMethod12() {
      if (mc.player != null && mc.interactionManager != null) {
         if (this.recoveredField5 && this.recoveredField7 != -1) {
            int var1 = this.recoveredField6 == -1 ? mc.player.getInventory().selectedSlot : this.recoveredField6;
            this.recoveredMethod9(var1);
            this.recoveredMethod10(this.recoveredField7, var1);
         }

         if (this.recoveredField6 != -1) {
            this.recoveredMethod9(this.recoveredField6);
         }

         this.recoveredMethod13();
      } else {
         this.recoveredMethod13();
      }
   }

   private void recoveredMethod13() {
      this.recoveredField5 = false;
      this.recoveredField7 = -1;
      this.recoveredField6 = -1;
   }

   private void recoveredMethod14() {
      try {
         Object var1 = recoveredMethod16();
         if (var1 == null) {
            this.recoveredMethod15();
            return;
         }

         Object var2 = recoveredMethod17(var1, "getPathingBehavior");
         if (var2 == null || !Boolean.TRUE.equals(recoveredMethod17(var2, "hasPath"))) {
            this.recoveredMethod15();
            return;
         }

         Object var3 = recoveredMethod17(var1, "getInputOverrideHandler");
         if (var3 != null) {
            var3.getClass().getMethod("clearAllKeys").invoke(var3);
            Object var4 = var3.getClass().getMethod("getBlockBreakHelper").invoke(var3);
            if (var4 != null) {
               var4.getClass().getMethod("stopBreakingBlock").invoke(var4);
            }
         }

         var2.getClass().getMethod("requestPause").invoke(var2);
         this.recoveredMethod15();
      } catch (Throwable var5) {
         this.recoveredMethod15();
      }
   }

   private void recoveredMethod15() {
      try {
         if (mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
         }
      } catch (Throwable var2) {
      }
   }

   private static Object recoveredMethod16() throws ReflectiveOperationException {
      Class var0 = Class.forName("baritone.api.BaritoneAPI");
      Object var1 = var0.getMethod("getProvider").invoke(null);
      return var1 == null ? null : var1.getClass().getMethod("getPrimaryBaritone").invoke(var1);
   }

   private static Object recoveredMethod17(Object target, String methodName) throws ReflectiveOperationException {
      return target.getClass().getMethod(methodName).invoke(target);
   }
}
