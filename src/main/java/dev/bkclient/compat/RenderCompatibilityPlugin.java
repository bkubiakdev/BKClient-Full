package dev.bkclient.compat;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Must remain available before protected client classes and Minecraft are loaded. */
public final class RenderCompatibilityPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("BKClient/RenderCompatibility");

    @Override public void onLoad(String mixinPackage) {
        if (!FabricLoader.getInstance().isModLoaded("immediatelyfast")) return;
        try {
            Class<?> entry = Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast");
            // Idempotent in 1.8.7: a later ImmediatelyFast plugin onLoad will not
            // reload config. All plugin onLoad calls precede mixin preparation.
            entry.getMethod("earlyInit").invoke(null);
            Object config = entry.getField("config").get(null);
            List<String> supported = ImmediatelyFastCompatibility.disableConflictingBatching(config);
            if (supported.isEmpty()) {
                LOGGER.warn("ImmediatelyFast compatibility unavailable: no supported batching settings found");
            } else {
                LOGGER.info("ImmediatelyFast compatibility active: {} disabled for this session; other optimizations unchanged", supported);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            LOGGER.warn("Could not apply ImmediatelyFast render compatibility", error);
        }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("dev.bkclient.mixin.compat.sodium."))
            return FabricLoader.getInstance().isModLoaded("sodium");
        boolean supported = supportsTerrainMixin(mixinClassName, FabricLoader.getInstance().isModLoaded("sodium"));
        if (!supported) LOGGER.info("Skipping vanilla terrain hook {}: Sodium uses a different terrain pipeline", mixinClassName);
        return supported;
    }

    static boolean supportsTerrainMixin(String mixinClassName, boolean sodiumLoaded) {
        // Sodium overwrites WorldRenderer.renderLayer and uses its own vertex
        // format/shaders. Neither the vanilla shader injection nor its UV tags
        // can be used there. Keep the hooks for vanilla/Indigo installations.
        if (!sodiumLoaded) return true;
        return !mixinClassName.equals("dev.bkclient.mixin.minecraft.client.render.TerrainWindMixin")
            && !mixinClassName.equals("dev.bkclient.mixin.minecraft.client.render.VegetationTagMixin")
            && !mixinClassName.equals("dev.bkclient.mixin.compat.IndigoVegetationMixin");
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
