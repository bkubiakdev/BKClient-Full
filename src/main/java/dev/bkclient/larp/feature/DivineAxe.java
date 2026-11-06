package dev.bkclient.larp.feature;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.ClientClock;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public final class DivineAxe {
    private static final int COOLDOWN_TICKS = 1200;
    private static final double RADIUS = 5.0;
    private static final double VERTICAL_BOOST = 0.2;
    private static final double MIN_FORCE = 1.0;
    private static final double MAX_FORCE = 3.0;
    private static final double FORCE_SCALE = 0.22;
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;
    private static final double WALK_SPEED = 0.13;
    private static final int MAX_CONTROL_TICKS = 200;
    private static final double ARRIVED = 0.4;
    private static long readyAtTick;
    private static final Map<PlayerEntity, Flight> PUSHED;

    private DivineAxe() {
    }

    public static boolean onRightClick(ItemStack stack) {
        ClientWorld NarrationMessageBuilder;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || !((NarrationMessageBuilder = client.world) instanceof ClientWorld)) {
            return true;
        }
        ClientWorld world = NarrationMessageBuilder;
        long now = ClientClock.ticks();
        if (now < readyAtTick) {
            Titles.cooldown((int)Math.ceil((double)(readyAtTick - now) / 20.0));
            return true;
        }
        readyAtTick = now + 1200L;
        dev.bkclient.larp.LarpCooldown.set(stack, 1200);
        DivineAxe.push(world, player);
        Titles.playSound(SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
        Titles.show((Text)Text.empty(), LegacyText.parse("&bAktywowa\u0142e\u015b boski top\u00f3r&7!"));
        return true;
    }

    private static void push(ClientWorld world, ClientPlayerEntity player) {
        long releaseAt = ClientClock.ticks() + 200L;
        for (PlayerEntity target : world.getPlayers()) {
            Vec3d away;
            double distance;
            if (target == player || (distance = (away = target.getPos().subtract(player.getPos())).horizontalLength()) > 5.0 || distance < 0.01) continue;
            double force = 3.0 - 2.0 * (distance / 5.0);
            Vec3d direction = new Vec3d(away.x, 0.0, away.z).normalize().multiply(force * 0.22);
            PUSHED.put(target, new Flight(new Vec3d(direction.x, force * 0.2, direction.z), releaseAt));
        }
    }

    public static boolean controls(PlayerEntity player) {
        return PUSHED.containsKey(player);
    }

    public static void tickPushed(PlayerEntity target) {
        Flight flight = PUSHED.get(target);
        if (flight == null) {
            return;
        }
        if (ClientClock.ticks() >= flight.releaseAtTick || target.isRemoved()) {
            PUSHED.remove(target);
            return;
        }
        if (flight.walking) {
            DivineAxe.walkBack(target, flight);
        } else {
            DivineAxe.fly(target, flight);
        }
    }

    private static void fly(PlayerEntity target, Flight flight) {
        flight.velocity = new Vec3d(flight.velocity.x, (flight.velocity.y - 0.08) * 0.98, flight.velocity.z);
        target.move(MovementType.SELF, flight.velocity);
        target.setVelocity(flight.velocity);
        if (target.isOnGround()) {
            flight.walking = true;
            flight.velocity = Vec3d.ZERO;
        }
    }

    private static void walkBack(PlayerEntity target, Flight flight) {
        Vec3d home = target.getTrackedPosition().getPos();
        Vec3d step = home.subtract(target.getPos());
        if (step.horizontalLength() < 0.4) {
            PUSHED.remove(target);
            return;
        }
        Vec3d walk = new Vec3d(step.x, 0.0, step.z).normalize().multiply(0.13);
        double fall = target.isOnGround() ? 0.0 : -0.08;
        target.move(MovementType.SELF, new Vec3d(walk.x, fall, walk.z));
        target.setVelocity(walk.x, fall, walk.z);
        float yaw = (float)Math.toDegrees(Math.atan2(-walk.x, walk.z));
        target.setYaw(yaw);
        target.setBodyYaw(yaw);
        target.setHeadYaw(yaw);
    }

    public static void tick(MinecraftClient client) {
        if (client.world != null && !PUSHED.isEmpty()) {
            Iterator<PlayerEntity> it = PUSHED.keySet().iterator();
            while (it.hasNext()) {
                if (!it.next().isRemoved()) continue;
                it.remove();
            }
            return;
        }
        PUSHED.clear();
    }

    static {
        PUSHED = new HashMap<PlayerEntity, Flight>();
    }

    private static final class Flight {
        Vec3d velocity;
        boolean walking;
        long releaseAtTick;

        Flight(Vec3d velocity, long releaseAtTick) {
            this.velocity = velocity;
            this.releaseAtTick = releaseAtTick;
        }
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { PUSHED.clear(); readyAtTick = 0; }
}
