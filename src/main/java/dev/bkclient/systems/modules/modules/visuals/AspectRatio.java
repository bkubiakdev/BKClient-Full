package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;

/**
 * Stretches or squashes the world's projection without touching the window.
 *
 * <p>Only the horizontal term of the projection matrix is scaled, so the interface, the inventory
 * and everything else drawn in screen space stay exactly where they were. Below 1 the view narrows
 * and things look taller; above 1 it widens and you see more to the sides.
 */
@ModuleInfo(name = "Aspect Ratio", category = ModuleCategory.VISUALS, desc = "modules.descriptions.aspect_ratio")
public class AspectRatio extends BaseModule {

    public final SliderSetting ratio = new SliderSetting((SettingsContainer) this, "modules.settings.aspect_ratio.ratio")
            .min(0.1F).max(2.0F).currentValue(1.0F).step(0.01F);

    public static AspectRatio getInstance() {
        return BKClientInit.getInstance().getModuleManager().getModule(AspectRatio.class);
    }

    /**
     * Applies the stretch to a projection matrix in place.
     *
     * <p>Returns the matrix untouched whenever the numbers cannot be trusted — a zero-sized
     * framebuffer during a resize, or a non-finite ratio — rather than writing a broken projection
     * that would blank the world for the rest of the frame.
     */
    public static void apply(Matrix4f projection) {
        AspectRatio module = getInstance();
        if (module == null || !module.isEnabled() || projection == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        float width = mc.getWindow().getFramebufferWidth();
        float height = mc.getWindow().getFramebufferHeight();
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float actual = width / height;
        float wanted = resolveRatio(width, height) / height;
        if (!Float.isFinite(actual) || !Float.isFinite(wanted) || wanted <= 0.0F) {
            return;
        }
        projection.m00(projection.m00() * (actual / wanted));
    }

    /** The width the projection should pretend the window has, given its real dimensions. */
    public static float resolveRatio(float width, float height) {
        AspectRatio module = getInstance();
        if (module == null || !module.isEnabled() || height <= 0.0F) {
            return width;
        }
        return height * module.ratio.getCurrentValue();
    }
}
