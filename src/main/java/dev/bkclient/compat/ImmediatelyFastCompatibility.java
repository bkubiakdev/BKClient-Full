package dev.bkclient.compat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** No Minecraft classes: this runs during mixin selection, before preLaunch. */
public final class ImmediatelyFastCompatibility {
    private ImmediatelyFastCompatibility() {}

    public static List<String> disableConflictingBatching(Object config) throws ReflectiveOperationException {
        List<String> supported = new ArrayList<>();
        for (String name : new String[]{"enhanced_batching", "hud_batching", "experimental_screen_batching"}) {
            try {
                Field field = config.getClass().getField(name);
                if (field.getType() == boolean.class) {
                    field.setBoolean(config, false);
                    supported.add(name);
                }
            } catch (NoSuchFieldException ignored) {
                // ImmediatelyFast uses different field names across releases.
            }
        }
        return supported;
    }
}
