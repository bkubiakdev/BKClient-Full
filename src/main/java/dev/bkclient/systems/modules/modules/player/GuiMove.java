package dev.bkclient.systems.modules.modules.player;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import dev.bkclient.mixin.accessors.CreativeInventoryScreenAccessor;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.network.SendPacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.player.InputEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.time.Timer;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "Gui Move", category = ModuleCategory.PLAYER, enabledByDefault = true)
public class GuiMove extends BaseModule {
   private final List<Packet<?>> packets = new ArrayList<>();
   private final List<ClickSlotC2SPacket> pickupPackets = new ArrayList<>();
   private final ModeSetting mode = new ModeSetting(this, "Mode");
   private final ModeSetting.Value noBypass = new ModeSetting.Value(this.mode, "No bypass");
   private final ModeSetting.Value auto = new ModeSetting.Value(this.mode, "Automatic").select();
   private final ModeSetting.Value custom = new ModeSetting.Value(this.mode, "Custom");
   private final ModeSetting containers = new ModeSetting(this, "In containers", this.auto::isSelected);
   private final ModeSetting.Value notWork = new ModeSetting.Value(this.containers, "Stand still");
   private final ModeSetting.Value vanillaCon = new ModeSetting.Value(this.containers, "Move").select();
   private final ModeSetting.Value shift = new ModeSetting.Value(this.containers, "Sneak");
   private final BooleanSetting cancelClose = new BooleanSetting(this, "Cancel closing", this.auto::isSelected).enable();
   private final BooleanSetting jump = new BooleanSetting(this, "Allow jumping", this.auto::isSelected);
   private final ModeSetting bypassMode = new ModeSetting(this, "Bypass", this.auto::isSelected);
   private final ModeSetting.Value vanilla = new ModeSetting.Value(this.bypassMode, "No bypass");
   private final ModeSetting.Value slow = new ModeSetting.Value(this.bypassMode, "Slowdown").select();
   private final ModeSetting.Value slowClose = new ModeSetting.Value(this.bypassMode, "On close");
   private final ModeSetting.Value close = new ModeSetting.Value(this.bypassMode, "Fake close");
   private final BooleanSetting ground = new BooleanSetting(this, "No bypass on ground", () -> this.vanilla.isSelected() || this.auto.isSelected());
   private final SliderSetting cooldown = new SliderSetting(
         this, "Delay", () -> this.vanilla.isSelected() || this.close.isSelected() || this.auto.isSelected()
      )
      .min(50.0F)
      .max(500.0F)
      .step(50.0F)
      .currentValue(100.0F)
      .suffix(" ms");
   private final Timer staying = new Timer();
   private final Timer grounding = new Timer();
   private boolean stay;
   private boolean sending;
   private int screenId = 0;
   private final EventListener<ClientPlayerTickEvent> onPlayerTick = event -> {
      if (this.auto.isSelected()) {
         if (ServerUtility.isST()) {
            this.notWork.select();
            this.cancelClose.enable();
            this.jump.enable();
            this.slow.select();
            this.ground.enable();
            this.cooldown.setCurrentValue(500.0F);
         } else if (ServerUtility.isFT()) {
            this.notWork.select();
            this.cancelClose.enable();
            this.jump.setEnabled(false);
            this.slowClose.select();
            this.ground.setEnabled(false);
            this.cooldown.setCurrentValue(100.0F);
         } else if (ServerUtility.isFT()) {
            this.notWork.select();
            this.cancelClose.enable();
            this.jump.setEnabled(false);
            this.slowClose.select();
            this.ground.setEnabled(false);
            this.cooldown.setCurrentValue(100.0F);
         } else if (ServerUtility.isIntave()) {
            this.notWork.select();
            this.cancelClose.enable();
            this.jump.setEnabled(false);
            this.slow.select();
            this.ground.setEnabled(false);
            this.cooldown.setCurrentValue(150.0F);
         } else if (ServerUtility.isHW()) {
            this.notWork.select();
            this.cancelClose.enable();
            this.jump.setEnabled(false);
            this.slow.select();
            this.ground.setEnabled(false);
            this.cooldown.setCurrentValue(100.0F);
         } else {
            this.notWork.select();
            this.cancelClose.enable();
            this.jump.setEnabled(false);
            this.slow.select();
            this.ground.setEnabled(false);
            this.cooldown.setCurrentValue(100.0F);
         }
      }

      if (mc.player.currentScreenHandler != null) {
         this.screenId = mc.player.currentScreenHandler.syncId;
      } else {
         this.screenId = 0;
      }

      if (this.slowClose.isSelected() && mc.currentScreen == null && !this.packets.isEmpty()) {
         this.stay = true;
      }

      if (this.canSend()) {
         this.sendPackets();
         this.stay = false;
      }

      if (!mc.player.isOnGround()) {
         this.grounding.reset();
      }

      if (mc.player.currentScreenHandler != null
         && (mc.player.currentScreenHandler.getCursorStack().isEmpty() || mc.player.currentScreenHandler.getCursorStack() == ItemStack.EMPTY)
         && !this.pickupPackets.isEmpty()
         && !this.sending) {
         this.stay = true;
         if (mc.player.isOnGround() && this.ground.isEnabled() || this.staying.finished((long)this.cooldown.getCurrentValue())) {
            this.sendPickupPackets();
         }
      }

      if (!this.isTyping() && this.invCheck()) {
         long windowHandle = mc.getWindow().getHandle();
         KeyBinding[] movementKeys = new KeyBinding[]{
            mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey
         };

         for (KeyBinding key : movementKeys) {
            int keyCode = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode();
            key.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
         }

         if (mc.player.getAbilities().flying) {
            int keyCode = InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode();
            mc.options.sneakKey.setPressed(InputUtil.isKeyPressed(windowHandle, keyCode));
         }
      } else if (this.isTyping()) {
         TextField.releaseMovementKeys();
      }
   };
   private final EventListener<SendPacketEvent> eventEventListener = event -> {
      if (!(mc.currentScreen instanceof GenericContainerScreen container && container.getTitle().getString().toLowerCase().contains("выбор"))
         && mc.player != null
         && !this.isTyping()
         && this.invCheck()
         && !this.vanilla.isSelected()
         && !this.sending
         && (!mc.player.isOnGround() || !this.ground.isEnabled())
         && !this.noBypass.isSelected()
         && event.getPacket() instanceof ClickSlotC2SPacket packet) {
         if (this.ground.isEnabled() && mc.player.isOnGround()) {
            this.grounding.reset();
         }

         if (packet.getActionType() == SlotActionType.PICKUP
            || packet.getActionType() == SlotActionType.PICKUP_ALL
            || packet.getActionType() == SlotActionType.CLONE
            || packet.getActionType() == SlotActionType.QUICK_CRAFT) {
            this.pickupPackets.add(packet);
            event.cancel();
            return;
         }

         if (this.slow.isSelected()) {
            this.packets.add(packet);
            event.cancel();
            this.stay = true;
         } else if (this.slowClose.isSelected()) {
            this.packets.add(packet);
            event.cancel();
            if (mc.currentScreen instanceof GenericContainerScreen
               || ServerUtility.isPastaFT() && packet.getStack().getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock
               || packet.getActionType() == SlotActionType.THROW) {
               this.stay = true;
            }
         } else if (this.close.isSelected()) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.screenId));
         }
      }
   };
   private final EventListener<InputEvent> onInput = event -> {
      if (this.isTyping()) {
         event.setForward(0.0F); event.setStrafe(0.0F); event.setJump(false); event.setSneak(false);
         return;
      }
      if (mc.currentScreen instanceof GenericContainerScreen && this.shift.isSelected()) {
         event.setSneak(true);
      }

      if (this.stay) {
         event.setForward(0.0F);
         event.setStrafe(0.0F);
         if (this.jump.isEnabled()) {
            event.setJump(false);
         }
      }

      if (this.jump.isEnabled() && event.isJump() || event.getStrafe() != 0.0F || event.getForward() != 0.0F) {
         this.staying.reset();
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (this.cancelClose.isEnabled() && event.getPacket() instanceof CloseScreenS2CPacket) {
         event.cancel();
      }
   };

   private boolean isTyping() {
      return mc.currentScreen instanceof ChatScreen
         || mc.currentScreen instanceof dev.bkclient.ui.menu.waypoints.WaypointScreen
         || TextField.hasFocusedField()
         || mc.currentScreen instanceof SignEditScreen
         || mc.currentScreen instanceof AnvilScreen
         || mc.currentScreen instanceof CreativeInventoryScreen && CreativeInventoryScreenAccessor.getSelectedTab() == ItemGroups.getSearchGroup();
   }

   private boolean invCheck() {
      return !this.notWork.isSelected()
         || this.ground.isEnabled() && mc.player.isOnGround()
         || mc.currentScreen instanceof InventoryScreen
         || mc.currentScreen instanceof CreativeInventoryScreen
         || mc.currentScreen == null;
   }

   @Override
   public void onDisable() {
      this.packets.clear();
      this.pickupPackets.clear();
      this.stay = false;
      this.sending = false;
      super.onDisable();
   }

   private void sendPackets() {
      if (!this.packets.isEmpty()) {
         this.sending = true;
         this.packets.forEach(mc.player.networkHandler::sendPacket);
         this.packets.clear();
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.screenId));
         this.sending = false;
      }
   }

   public boolean canSend() {
      return this.isEnabled()
         && this.stay
         && (
            mc.player.isOnGround() && this.ground.isEnabled() && this.grounding.finished(500L)
               || this.staying.finished((long)this.cooldown.getCurrentValue())
         );
   }

   private void sendPickupPackets() {
      if (!this.pickupPackets.isEmpty()) {
         this.sending = true;
         this.pickupPackets.forEach(mc.player.networkHandler::sendPacket);
         this.pickupPackets.clear();
         this.sending = false;
      }
   }

   public boolean slowing() {
      return this.slow.isSelected() || this.slowClose.isSelected();
   }

   @Generated
   public SliderSetting getCooldown() {
      return this.cooldown;
   }

   @Generated
   public void setStay(boolean stay) {
      this.stay = stay;
   }

   @Generated
   public void setSending(boolean sending) {
      this.sending = sending;
   }
}
