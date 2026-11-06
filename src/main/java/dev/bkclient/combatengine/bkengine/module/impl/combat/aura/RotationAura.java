package dev.bkclient.combatengine.bkengine.module.impl.combat.aura;

import net.minecraft.entity.LivingEntity;
import dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura;
import dev.bkclient.combatengine.bkengine.utils.annotation.IMinecraft;

/**
 * Базовый контракт ротации ауры. Каждая реализация получает всё необходимое
 * напрямую из {@link AttackAura}: саму ауру (для общего состояния), цель,
 * дистанции и признак возможности удара.
 */
public interface RotationAura extends IMinecraft {

    /**
     * @param aura        экземпляр ауры (общее состояние: tick, флики, таймеры, настройки)
     * @param target      текущая цель
     * @param ranges      [attackRange, preRange, attackRange + preRange]
     * @param canAttack   можно ли сейчас атаковать (рассчитано аурой)
     */
    void onRotation(AttackAura aura, LivingEntity target, float[] ranges, boolean canAttack);

}
