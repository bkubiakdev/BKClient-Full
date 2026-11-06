package dev.bkclient.mixin.minecraft.entity.projectile;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.PlayerVisibility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @ModifyVariable(method = {"raycast", "getEntityCollision"}, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Predicate<Entity> bkClient$filterHiddenPlayers(Predicate<Entity> original) {
        // The predicate is consumed synchronously inside this raycast, so a disabled module means the
        // wrapper would only ever delegate: hand back the original without allocating a lambda.
        var manager = BKClientInit.getInstance().getModuleManager();
        if (manager != null) {
            PlayerVisibility current;
            try {
                current = manager.getModule(PlayerVisibility.class);
            } catch (RuntimeException notRegistered) {
                current = null; // keep the old lazy behaviour: only a player test may throw
            }
            if (current != null && !current.isEnabled()) return original;
        }
        return entity -> {
            if (entity instanceof PlayerEntity) {
                PlayerVisibility module = BKClientInit.getInstance().getModuleManager().getModule(PlayerVisibility.class);
                if (module != null && module.isEnabled() && (BKClientInit.mc.player == null || !entity.getUuid().equals(BKClientInit.mc.player.getUuid())) && !PlayerVisibility.isNpc(entity)) return false;
            }
            return original.test(entity);
        };
    }
}
