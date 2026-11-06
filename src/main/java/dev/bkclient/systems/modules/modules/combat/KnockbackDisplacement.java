package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.window.KeyPressEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.math.KnockbackDirection;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/** Changes only the attacker's facing around an existing attack; never adds an attack or velocity. */
@ModuleInfo(name = "Knockback Displacement", category = ModuleCategory.COMBAT,
        desc = "Redirects directional attack knockback. Requires server-side knockback; Dynamic points toward you.")
public final class KnockbackDisplacement extends BaseModule {
    private final ModeSetting direction = new ModeSetting(this, "Knockback Direction");
    private final ModeSetting.Value dynamic = new ModeSetting.Value(direction, "Dynamic").select();
    private final ModeSetting.Value fixed = new ModeSetting.Value(direction, "Direction");
    private final BooleanSetting sprintOnly = new BooleanSetting(this, "Only Sprint Knockback").enabled(false);
    private final SliderSetting angle = new SliderSetting(this, "Direction Angle")
            .min(-180).max(180).step(1).currentValue(0);
    private final BindSetting changeAngle = new BindSetting(this, "Change Angle Key");
    private final SliderSetting angleStep = new SliderSetting(this, "Angle Step")
            .min(15).max(180).step(15).currentValue(90);
    private final BooleanSetting hideRotation = new BooleanSetting(this, "Hide Camera Rotation").enabled(false);
    private LivingEntity pending;
    private net.minecraft.client.world.ClientWorld pendingWorld;
    private int pendingAge;
    private float forcedYaw;
    private boolean prepared, replaying, accepted;
    private ClientPlayerEntity visualPlayer;
    private float visualHeadYaw, visualBodyYaw;
    private long visualUntil;
    private final EventListener<dev.bkclient.systems.event.impl.network.SendPacketEvent> onSend = event -> {
        // The caller swings even when attackEntity is deferred. Replay one swing with the actual attack.
        if (validPending() && !replaying && event.getPacket() instanceof net.minecraft.network.packet.c2s.play.HandSwingC2SPacket) event.cancel();
    };

    private final EventListener<KeyPressEvent> onKey = event -> {
        if (isEnabled() && mc.currentScreen == null && event.getAction() == 1 && changeAngle.isKey(event.getKey())) {
            angle.setCurrentValue(KnockbackDirection.wrap(angle.getCurrentValue() + angleStep.getCurrentValue()));
            dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
        }
    };

    public boolean deferAttack(net.minecraft.entity.player.PlayerEntity attacker, Entity target) {
        if (!isEnabled() || replaying || attacker != mc.player || mc.player == null || mc.world == null
                || !(target instanceof LivingEntity living) || !living.isAlive() || mc.player.hasVehicle()) return false;
        // Aura has its own critical window and rotation transaction; never postpone its attacks.
        if (dev.bkclient.combatengine.bridge.AuraRuntime.auraEnabled()) {
            clearPending();
            restoreVisual();
            return false;
        }
        if (sprintOnly.isEnabled() && (!mc.player.isSprinting() || mc.player.getAttackCooldownProgress(.5F) <= .9F)) return false;
        if (pending != null && !validPending()) clearPending();
        if (pending != null) return true;
        restoreVisual();
        pending = living;
        pendingWorld = mc.world;
        pendingAge = mc.player.age;
        prepared = false;
        float base = dynamic.isSelected() ? KnockbackDirection.towardAttacker(
                mc.player.getX() - target.getX(), mc.player.getZ() - target.getZ(), mc.player.getYaw() + 180F)
                : mc.player.getYaw() + 180F;
        forcedYaw = KnockbackDirection.nearest(mc.player.getYaw(), base + angle.getCurrentValue());
        return true;
    }

    /** Called only after the normal attack listeners allow this attack. */
    public void attackAccepted() { if (replaying) accepted = true; }

    /** Read-only HUD direction, relative to the current camera. */
    public float getIndicatorAngle() {
        if (mc.player == null) return 180F + angle.getCurrentValue();
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();
        if (validPending() || visualPlayer == mc.player) {
            return KnockbackDirection.wrap(forcedYaw - cameraYaw);
        }
        float yaw = mc.player.getYaw() + 180F;
        if (dynamic.isSelected() && mc.targetedEntity instanceof LivingEntity target && target.isAlive()) {
            yaw = KnockbackDirection.towardAttacker(mc.player.getX() - target.getX(),
                    mc.player.getZ() - target.getZ(), yaw);
        }
        return KnockbackDirection.wrap(yaw + angle.getCurrentValue() - cameraYaw);
    }

    public float movementYaw(float original) {
        return validPending() ? forcedYaw : original;
    }

    public void movementSent() {
        if (validPending()) prepared = true;
    }

    /** Runs at the start of the next player tick, before its movement packet, not after it. */
    public void flushAttack() {
        if (pending == null) return;
        if (!validPending()) { clearPending(); return; }
        // A missing movement callback must not swallow clicks indefinitely.
        if (!prepared && mc.player.age - pendingAge < 2) return;
        boolean redirected = prepared && (!sprintOnly.isEnabled() || mc.player.isSprinting());
        LivingEntity target = pending;
        clearPending();
        float cameraYaw = mc.player.getYaw();
        replaying = true;
        accepted = false;
        try {
            if (redirected) mc.player.setYaw(forcedYaw);
            mc.interactionManager.attackEntity(mc.player, target);
            if (accepted) mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        } finally {
            mc.player.setYaw(cameraYaw);
            replaying = false;
        }
        if (accepted && redirected && !hideRotation.isEnabled()) {
            visualPlayer = mc.player;
            visualHeadYaw = mc.player.headYaw;
            visualBodyYaw = mc.player.bodyYaw;
            mc.player.headYaw = forcedYaw;
            mc.player.bodyYaw = forcedYaw;
            visualUntil = System.nanoTime() + 180_000_000L;
        }
    }

    private boolean validPending() {
        return isEnabled() && pending != null && mc.player != null && mc.interactionManager != null
                && !dev.bkclient.combatengine.bridge.AuraRuntime.auraEnabled()
                && mc.world == pendingWorld && pending.isAlive() && !pending.isRemoved()
                && !mc.player.hasVehicle() && mc.player.age - pendingAge >= 0 && mc.player.age - pendingAge <= 5
                && pending.getBoundingBox().squaredMagnitude(mc.player.getEyePos())
                   <= Math.pow(mc.player.getEntityInteractionRange(), 2);
    }
    private void clearPending() { pending = null; pendingWorld = null; prepared = false; }
    private void restoreVisual() {
        if (visualPlayer == null) return;
        // Never restore player yaw: the camera belongs to the user's mouse.
        if (visualPlayer.headYaw == forcedYaw) visualPlayer.headYaw = visualHeadYaw;
        if (visualPlayer.bodyYaw == forcedYaw) visualPlayer.bodyYaw = visualBodyYaw;
        visualPlayer = null;
    }
    @Override public void tick() {
        if (pending != null && !validPending()) clearPending();
        if (visualPlayer != null && (visualPlayer != mc.player || System.nanoTime() >= visualUntil)) restoreVisual();
    }
    @Override public void onDisable() { clearPending(); restoreVisual(); replaying = false; }
}
