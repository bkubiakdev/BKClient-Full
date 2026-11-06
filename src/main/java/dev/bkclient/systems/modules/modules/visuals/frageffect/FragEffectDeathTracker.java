package dev.bkclient.systems.modules.modules.visuals.frageffect;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FragEffectDeathTracker {
    private static final int MAX_MEMORIES = 12;
    private static final long MEMORY_LIFETIME_MS = 950L;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<DeathMemory> memories = new ArrayList<>();

    public synchronized void remember(LivingEntity entity, boolean allowNonPlayer) {
        if (entity == null || entity == mc.player || entity.age < 2 || !entity.isAlive()) return;
        if (!allowNonPlayer && !(entity instanceof PlayerEntity)) return;
        for (DeathMemory memory : memories) {
            if (memory.matches(entity)) {
                memory.refresh(entity);
                return;
            }
        }
        while (memories.size() >= MAX_MEMORIES) memories.remove(0);
        memories.add(new DeathMemory(entity));
    }

    public synchronized void tick() {
        if (memories.isEmpty()) return;
        Iterator<DeathMemory> iterator = memories.iterator();
        while (iterator.hasNext()) {
            DeathMemory memory = iterator.next();
            if (!memory.isActive() || memory.shouldDiscard(mc)) iterator.remove();
        }
    }

    public synchronized boolean wasRecentlyHit(LivingEntity entity) {
        if (entity == null) return false;
        for (DeathMemory memory : memories) {
            if (memory.matches(entity) && memory.isActive()) return true;
        }
        return false;
    }

    public synchronized void forget(LivingEntity entity) {
        if (entity != null) memories.removeIf(memory -> memory.matches(entity));
    }

    public synchronized void clear() {
        memories.clear();
    }

    private static final class DeathMemory {
        private final int entityId;
        private LivingEntity entity;
        private long expiresAt;

        private DeathMemory(LivingEntity entity) {
            this.entity = entity;
            this.entityId = entity.getId();
            this.expiresAt = System.currentTimeMillis() + MEMORY_LIFETIME_MS;
        }

        private boolean matches(LivingEntity other) {
            return other.getId() == entityId;
        }

        private void refresh(LivingEntity entity) {
            this.entity = entity;
            this.expiresAt = System.currentTimeMillis() + MEMORY_LIFETIME_MS;
        }

        private boolean shouldDiscard(MinecraftClient mc) {
            if (mc.world == null) return true;
            if (entity.getHealth() <= 0.0f || entity.isDead()) return false;
            return entity.isRemoved() || mc.world.getEntityById(entityId) == null;
        }

        private boolean isActive() {
            return System.currentTimeMillis() <= expiresAt;
        }
    }
}
