package dev.bkclient.utility.game;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.world.ClientWorld;

public class LogoutGhostEntity extends FakePlayerEntity {
    public LogoutGhostEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }
}
