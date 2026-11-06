package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskController;

@ModuleInfo(name = "TinyTask", category = ModuleCategory.OTHER, desc = "Records and plays back input macros")
public class TinyTask extends BaseModule {
    private final TinyTaskController controller = new TinyTaskController(this);

    public TinyTaskController getController() {
        return this.controller;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.controller.stopAll();
    }
}
