package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.PanicSession;

/** Stops BKClient for this game session while preserving the installed mod and settings. */
@ModuleInfo(name = "Panic", category = ModuleCategory.OTHER, desc = "modules.descriptions.panic")
public class Panic extends BaseModule {
    @Override public void onEnable() { PanicSession.activate(); }
}
