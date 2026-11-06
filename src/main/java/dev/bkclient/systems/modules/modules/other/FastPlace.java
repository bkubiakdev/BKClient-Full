package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;

@ModuleInfo(name = "Fast Place", category = ModuleCategory.OTHER, desc = "Places blocks repeatedly at the selected CPS")
public class FastPlace extends BaseModule {
    public final SliderSetting cps = new SliderSetting(this, "CPS")
        .min(5.0F).max(25.0F).step(1.0F).currentValue(15.0F);
}
