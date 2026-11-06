package dev.bkclient.systems.modules.modules.visuals;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class LogoutPos {
    private final UUID uuid;
    private final String name;
    private final Vec3d pos;
    private final float health;
    private final long timestamp;
    private final PlayerEntity entity;
    private final net.minecraft.util.Identifier skinTexture;
    private final dev.bkclient.utility.game.LogoutGhostEntity ghost;

    public LogoutPos(UUID uuid, String name, Vec3d pos, float health, long timestamp, PlayerEntity entity,
            net.minecraft.util.Identifier skinTexture, dev.bkclient.utility.game.LogoutGhostEntity ghost) {
        this.uuid = uuid;
        this.name = name;
        this.pos = pos;
        this.health = health;
        this.timestamp = timestamp;
        this.entity = entity;
        this.skinTexture = skinTexture;
        this.ghost = ghost;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Vec3d getPos() {
        return pos;
    }

    public float getHealth() {
        return health;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PlayerEntity getEntity() {
        return entity;
    }

    public net.minecraft.util.Identifier getSkinTexture() {
        return skinTexture;
    }

    public dev.bkclient.utility.game.LogoutGhostEntity getGhost() {
        return ghost;
    }
}
