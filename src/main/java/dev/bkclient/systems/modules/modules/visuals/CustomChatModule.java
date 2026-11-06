package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;

@ModuleInfo(name = "Custom Chat", category = ModuleCategory.VISUALS, enabledByDefault = true)
public class CustomChatModule extends BaseModule {
   private final BooleanSetting directMessages = new BooleanSetting(this, "custom_chat.direct_messages").enable();

   public boolean areDirectMessagesEnabled() {
      return this.isEnabled() && this.directMessages.isEnabled();
   }
}
