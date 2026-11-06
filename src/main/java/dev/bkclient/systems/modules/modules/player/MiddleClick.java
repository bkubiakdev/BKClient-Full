package dev.bkclient.systems.modules.modules.player;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventIntegration;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.event.impl.window.MouseEvent;
import dev.bkclient.systems.friends.FriendManager;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BindSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

@ModuleInfo(name = "Middle Click", category = ModuleCategory.PLAYER, desc = "Performs actions with the middle mouse button")
public class MiddleClick extends BaseModule {
   private final SelectSetting actions = new SelectSetting(this, "Actions").min(1);
   private final SelectSetting.Value clickPearl = new SelectSetting.Value(this.actions, "Throw pearl").select();
   private final SelectSetting.Value clickFriend = new SelectSetting.Value(this.actions, "Add friends");
   private final BindSetting clickFriendKey = new BindSetting(this, "Friend key", () -> !this.clickFriend.isSelected());
   private final BindSetting clickPearlKey = new BindSetting(this, "Pearl key", () -> !this.clickPearl.isSelected());
   private final EventListener<KeyPressEvent> onKeyPressEvent = event -> this.handleKey(event.getKey(), event.getAction());
   private final EventListener<MouseEvent> onMouseEvent = event -> this.handleKey(event.getButton(), event.getAction());

   private void handleKey(int key, int action) {
      if (mc.currentScreen == null && action == 1) {
         if (this.clickFriend.isSelected() && this.clickFriendKey.isKey(key) && mc.targetedEntity instanceof PlayerEntity) {
            String nick = mc.targetedEntity.getName().getString();
            FriendManager friend = BKClientInit.getInstance().getFriendManager();
            if (friend.isFriend(nick)) {
               friend.remove(nick);
            } else {
               friend.add(nick);
            }
         }

         if (this.clickPearl.isSelected() && this.clickPearlKey.isKey(key)) {
            EventIntegration.SWAP_INTEGRATION.useItem(Items.ENDER_PEARL);
         }
      }
   }
}
