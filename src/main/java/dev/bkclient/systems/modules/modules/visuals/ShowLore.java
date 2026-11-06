package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.StringSetting;

@ModuleInfo(name = "Show Lore", category = ModuleCategory.VISUALS, desc = "modules.descriptions.show_lore")
public class ShowLore extends BaseModule {
    private final StringSetting keyword = new StringSetting(this, "modules.settings.show_lore.keyword").text("additional damage");
    public final BooleanSetting fullLore = new BooleanSetting(this, "modules.settings.show_lore.full_lore");

    public StringSetting getKeyword() {
        return keyword;
    }
}
