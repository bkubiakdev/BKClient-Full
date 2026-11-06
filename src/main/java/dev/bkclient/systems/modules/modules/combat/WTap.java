package dev.bkclient.systems.modules.modules.combat;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.AttackEvent;
import dev.bkclient.systems.event.impl.game.PostAttackEvent;
import dev.bkclient.systems.event.impl.game.WorldChangeEvent;
import dev.bkclient.systems.event.impl.player.InputEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.modules.modules.combat.wtap.TapRules;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.concurrent.ThreadLocalRandom;

/** W-Tap, S-Tap and Sprint Reset after every sprint hit, so each hit keeps sprint knockback. */
@ModuleInfo(name = "W-Tap", category = ModuleCategory.COMBAT, desc = "modules.descriptions.w_tap")
public final class WTap extends BaseModule {
    private static final String KEY = "modules.settings.w_tap.";

    private final ModeSetting mode = new ModeSetting(this, KEY + "mode");
    private final ModeSetting.Value smart = new ModeSetting.Value(mode, KEY + "smart").select();
    private final ModeSetting.Value wTap = new ModeSetting.Value(mode, KEY + "w_tap");
    private final ModeSetting.Value sTap = new ModeSetting.Value(mode, KEY + "s_tap");
    private final ModeSetting.Value sprintReset = new ModeSetting.Value(mode, KEY + "sprint_reset");
    private final SliderSetting closeDistance = new SliderSetting(this, KEY + "close_distance", () -> !smart.isSelected())
            .min(1.5F).max(3.5F).step(0.1F).currentValue(2.5F).suffix(" m");
    private final SliderSetting delay = new SliderSetting(this, KEY + "delay")
            .min(0).max(3).step(1).currentValue(0).suffix(" t");
    private final SliderSetting duration = new SliderSetting(this, KEY + "duration")
            .min(1).max(5).step(1).currentValue(1).suffix(" t");
    private final BooleanSetting randomize = new BooleanSetting(this, KEY + "randomize").enable();
    private final SliderSetting chance = new SliderSetting(this, KEY + "chance")
            .min(10).max(100).step(5).currentValue(100).suffix("%");
    private final BooleanSetting onlyPlayers = new BooleanSetting(this, KEY + "only_players").enable();
    private final BooleanSetting resumeSprint = new BooleanSetting(this, KEY + "resume_sprint").enable();

    private final TapRules.Schedule schedule = new TapRules.Schedule();
    private long inputTick;
    private boolean sprintingBeforeHit;

    private final EventListener<AttackEvent> onAttack = event -> {
        // Vanilla clears sprinting during a sprint hit, so remember it before the attack.
        sprintingBeforeHit = mc.player != null && mc.player.isSprinting();
    };

    private final EventListener<PostAttackEvent> onPostAttack = event -> {
        if (mc.player == null || !sprintingBeforeHit || BKClientInit.getInstance().isPanic()) return;
        sprintingBeforeHit = false;
        Entity target = event.getEntity();
        if (target == null || onlyPlayers.isEnabled() && !(target instanceof PlayerEntity)) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextInt(100) >= (int) chance.getCurrentValue()) return;
        boolean forwardHeld = mc.options.forwardKey.isPressed();
        TapRules.Action action = TapRules.choose(currentMode(), forwardHeld,
                mc.player.distanceTo(target), closeDistance.getCurrentValue());
        int ticks = (int) duration.getCurrentValue() + (randomize.isEnabled() ? random.nextInt(2) : 0);
        // The attack runs before this tick's movement input, so the tap starts on the next input tick.
        schedule.schedule(inputTick + 1, (int) delay.getCurrentValue(), ticks, action);
    };

    private final EventListener<InputEvent> onInput = event -> {
        if (mc.player == null) return;
        inputTick++;
        TapRules.Action action = schedule.actionAt(inputTick);
        switch (action) {
            case RELEASE_W -> {
                if (event.getForward() > 0.0F) event.setForward(0.0F);
                stopSprint(event);
            }
            case PRESS_S -> {
                event.setForward(-1.0F);
                stopSprint(event);
            }
            case STOP_SPRINT -> stopSprint(event);
            case NONE -> {
                if (schedule.justEnded(inputTick) && resumeSprint.isEnabled() && canResprint(event)) {
                    mc.player.setSprinting(true);
                }
            }
        }
    };

    private final EventListener<WorldChangeEvent> onWorld = event -> reset();

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void reset() {
        schedule.reset();
        inputTick = 0;
        sprintingBeforeHit = false;
    }

    private TapRules.Mode currentMode() {
        if (wTap.isSelected()) return TapRules.Mode.W_TAP;
        if (sTap.isSelected()) return TapRules.Mode.S_TAP;
        if (sprintReset.isSelected()) return TapRules.Mode.SPRINT_RESET;
        return TapRules.Mode.SMART;
    }

    /** The sprint key is cleared too, otherwise vanilla restarts sprinting in the same tick and nothing is sent. */
    private void stopSprint(InputEvent event) {
        event.setSprint(false);
        mc.player.setSprinting(false);
    }

    private boolean canResprint(InputEvent event) {
        return event.getForward() > 0.0F && !event.isSneak() && !mc.player.isUsingItem()
                && !mc.player.isSprinting() && !mc.player.isTouchingWater()
                && (mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.getAbilities().allowFlying);
    }
}
