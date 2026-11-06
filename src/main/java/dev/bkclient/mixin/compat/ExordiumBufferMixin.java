package dev.bkclient.mixin.compat;

import dev.bkclient.systems.modules.modules.visuals.CustomHotbar;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Exordium 1.4.2 integration; never changes its saved configuration. */
@Pseudo
@Mixin(targets = "dev.tr7zw.exordium.components.BufferInstance", remap = false)
public abstract class ExordiumBufferMixin {
    @Shadow @Final private Identifier id;

    @Inject(method = "enabled()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void bkclient$renderCustomHotbarEveryFrame(CallbackInfoReturnable<Boolean> cir) {
        if (!"minecraft".equals(id.getNamespace())) return;
        String component = id.getPath();
        // Health and XP are drawn/replaced by the custom hotbar too. Bypass their
        // old cached vanilla images so enabling the module cannot duplicate them.
        if (("hotbar".equals(component) || "health".equals(component) || "experience_bar".equals(component))
                && CustomHotbar.replacesHud()) {
            cir.setReturnValue(false);
        }
    }
}
