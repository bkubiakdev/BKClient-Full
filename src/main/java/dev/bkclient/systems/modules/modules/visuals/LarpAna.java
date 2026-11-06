package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.larp.LarpRuntime;
import dev.bkclient.larp.LarpScreen;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ButtonSetting;

@ModuleInfo(name = "Larp Ana", desc = "Local event item appearances and ability simulations", category = ModuleCategory.VISUALS)
public final class LarpAna extends BaseModule {
    public static LarpAna instance;
    public final BooleanSetting simulate = new BooleanSetting(this, "Simulate abilities").enable();
    public final BooleanSetting effects = new BooleanSetting(this, "Sound and effects").enable();
    private final ButtonSetting editor = new ButtonSetting(this, "Open editor").action(() -> {
        if (!dev.bkclient.BKClientInit.INSTANCE.isPanic()) mc.setScreen(new LarpScreen(mc.currentScreen));
    });
    public LarpAna() { instance = this; LarpRuntime.init(); }
    @Override public void onDisable() { LarpRuntime.reset(); super.onDisable(); }
}
