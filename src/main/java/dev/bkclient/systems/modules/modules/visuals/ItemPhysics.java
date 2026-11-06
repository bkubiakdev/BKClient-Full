package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;

/**
 * Dropped items lie flat on the ground and tumble while falling instead of
 * hovering upright and spinning. Ported from BKClient's Item Physics.
 *
 * <p>All of the work happens in {@code ItemEntityRendererMixin}; this class only
 * carries the toggle. BKClient's version exposes a "Physics" mode setting with a
 * single value ("Normal"), so there is nothing to configure.
 */
@ModuleInfo(name = "Item Physics", category = ModuleCategory.VISUALS)
public class ItemPhysics extends BaseModule {
    private static ItemPhysics INSTANCE;

    public ItemPhysics() {
        INSTANCE = this;
    }

    public static ItemPhysics getInstance() {
        return INSTANCE;
    }
}
