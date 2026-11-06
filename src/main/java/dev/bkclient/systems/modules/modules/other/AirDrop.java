package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.supabase.SupabaseService;
import dev.bkclient.systems.notifications.AirdropNotification;

@ModuleInfo(name = "AirDrop", category = ModuleCategory.OTHER, enabledByDefault = false,
   desc = "Receive one config using a code valid for 5 minutes")
public final class AirDrop extends BaseModule {
   private final dev.bkclient.systems.setting.settings.ButtonSetting copyCode =
      new dev.bkclient.systems.setting.settings.ButtonSetting(this, "Copy receive code")
         .action(() -> SupabaseService.getInstance().copyReceiveCode());
   @Override
   public void onEnable() { SupabaseService.getInstance().startPolling(); }

   @Override
   public void onDisable() {
      SupabaseService.stopIfInitialized();
      AirdropNotification.dismissIfInitialized();
   }
}
