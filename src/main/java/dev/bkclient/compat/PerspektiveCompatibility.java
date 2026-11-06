package dev.bkclient.compat;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import net.fabricmc.loader.api.FabricLoader;

/** Camera ownership only; combat/server rotations remain independent. */
public final class PerspektiveCompatibility {
    private PerspektiveCompatibility() {}

    public static boolean isFreeLookActive() { return Holder.ACTIVE.getAsBoolean(); }

    private static final class Holder {
        private static final BooleanSupplier ACTIVE = discover();
        private static BooleanSupplier discover() {
            if (!FabricLoader.getInstance().isModLoaded("perspektive")) return () -> false;
            try {
                return bind(Class.forName("de.royzer.perspektive.Perspektive"));
            } catch (ReflectiveOperationException | LinkageError e) {
                org.slf4j.LoggerFactory.getLogger("bkclient").warn("Perspektive compatibility unavailable", e);
                return () -> false;
            }
        }
    }

    static BooleanSupplier bind(Class<?> api) throws ReflectiveOperationException {
        Object instance = api.getField("INSTANCE").get(null);
        Method enabled = api.getMethod("getFreeLookEnabled");
        return () -> {
            try { return Boolean.TRUE.equals(enabled.invoke(instance)); }
            catch (ReflectiveOperationException | LinkageError e) { return false; }
        };
    }
}
