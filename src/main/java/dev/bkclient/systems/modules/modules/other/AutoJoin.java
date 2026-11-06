package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.accessors.ClientPlayerInteractionManagerAccessor;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.notifications.NotificationType;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.game.server.ServerUtility;
import dev.bkclient.utility.inventory.group.SlotGroup;
import dev.bkclient.utility.inventory.group.SlotGroups;
import dev.bkclient.utility.inventory.slots.HotbarSlot;
import dev.bkclient.utility.time.Timer;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleInfo(name = "Auto Join", category = ModuleCategory.OTHER, desc = "Automatically joins the selected mode")
public class AutoJoin extends BaseModule {
   private final String griefString = "306";
   private final ModeSetting mode = new ModeSetting(this, "Mode");
   private final ModeSetting.Value duels = new ModeSetting.Value(this.mode, "SpookyTime Duels").select();
   private final ModeSetting.Value grief = new ModeSetting.Value(this.mode, "Grief RW/FT/Spooky");
   private final Timer timer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (ServerUtility.isST() && this.duels.isSelected()) {
         SlotGroup<HotbarSlot> search = SlotGroups.hotbar();
         HotbarSlot compass = search.findItem(Items.COMPASS);
         HotbarSlot sword = search.findItem(Items.DIAMOND_SWORD);
         if (compass != null && this.timer.finished(300L)) {
            mc.player.getInventory().selectedSlot = compass.getSlotId();
            ((ClientPlayerInteractionManagerAccessor)mc.interactionManager)
               .invokeSendSequencedPacket(
                  mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch())
               );
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && mc.currentScreen.getTitle().getString().contains("Выберите режим")) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 14, 0, SlotActionType.QUICK_MOVE, mc.player);
            }

            this.timer.reset();
         }

         if (sword != null) {
            BKClientInit.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS, "Joined successfully", "You successfully joined duels");
            this.toggle();
         }
      }

      if ((ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST()) && this.grief.isSelected()) {
         mc.player.networkHandler.sendChatCommand("an306");
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet && (ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST()) && this.grief.isSelected()) {
         String message = packet.content().getString().toLowerCase();
         if (ServerUtility.isFT() ? message.contains("вы уже подключены к этому серверву!") : message.contains("вы уже подключены на этот сервер!")) {
            this.toggle();
         }
      }
   };
}
