package dev.bkclient.systems.modules.modules.player;

import dev.bkclient.BKClientInit;
import dev.bkclient.mixin.accessors.EntityAccessor;
import dev.bkclient.mixin.accessors.LivingEntityAccessor;
import dev.bkclient.mixin.minecraft.client.IMinecraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class FreecamUtil {
    private static boolean active = false;
    private static Vec3d originPos = Vec3d.ZERO;
    private static float originYaw = 0.0f;
    private static float originPitch = 0.0f;
    private static float originBodyYaw = 0.0f;
    private static float originHeadYaw = 0.0f;
    private static OtherClientPlayerEntity ghost = null;
    private static final int ghostId = -7300;
    private static final double freecamBlockRange = 4.0;
    private static final double freecamEntityRange = 3.0;
    private static Vec3d freecamPos = Vec3d.ZERO;
    private static Vec3d prevFreecamPos = Vec3d.ZERO;
    private static float freecamYaw = 0.0f;
    private static float freecamPitch = 0.0f;
    private static float prevFreecamYaw = 0.0f;
    private static float prevFreecamPitch = 0.0f;
    private static boolean lookDirty = false;
    private static boolean prevPlayerNoClip = false;
    private static boolean prevPlayerNoGravity = false;

    private FreecamUtil() {
    }

    public static boolean isEnabled() {
        FreeCam mod = BKClientInit.getInstance().getModuleManager().getModule(FreeCam.class);
        return mod != null && mod.isEnabled();
    }

    public static boolean isActive() {
        return active;
    }

    public static OtherClientPlayerEntity ghost() {
        return ghost;
    }

    public static OtherClientPlayerEntity cameraAnchor() {
        return null;
    }

    public static Vec3d originPos() {
        return originPos;
    }

    public static Vec3d freecamPos() {
        return freecamPos;
    }

    public static float freecamYaw() {
        return freecamYaw;
    }

    public static float freecamPitch() {
        return freecamPitch;
    }

    public static Vec3d getCameraPos(float tickDelta) {
        return new Vec3d(
            MathHelper.lerp(tickDelta, prevFreecamPos.x, freecamPos.x),
            MathHelper.lerp(tickDelta, prevFreecamPos.y, freecamPos.y),
            MathHelper.lerp(tickDelta, prevFreecamPos.z, freecamPos.z)
        );
    }

    public static float getCameraYaw(float tickDelta) {
        return MathHelper.lerpAngleDegrees(tickDelta, prevFreecamYaw, freecamYaw);
    }

    public static float getCameraPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevFreecamPitch, freecamPitch);
    }

    public static void applyLookDelta(double deltaX, double deltaY) {
        prevFreecamYaw = freecamYaw;
        prevFreecamPitch = freecamPitch;
        freecamYaw += (float) deltaX * 0.15f;
        freecamPitch += (float) deltaY * 0.15f;
        freecamPitch = MathHelper.clamp(freecamPitch, -90.0f, 90.0f);
        freecamYaw = MathHelper.wrapDegrees(freecamYaw);
        lookDirty = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            syncLocalPlayerToFreecam(client.player);
        }
    }

    public static float originYaw() {
        return originYaw;
    }

    public static float originPitch() {
        return originPitch;
    }

    public static boolean shouldCancelPackets() {
        return isActive() && cancelMoveEnabled();
    }

    public static boolean cancelMoveEnabled() {
        FreeCam freeCam = BKClientInit.getInstance().getModuleManager().getModule(FreeCam.class);
        return freeCam == null || freeCam.getCancelMove().isEnabled();
    }

    public static void tick(MinecraftClient client) {
        boolean enabled = isEnabled();
        if (!enabled) {
            if (active) {
                disable(client);
            }
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            if (active) {
                active = false;
            }
            return;
        }
        if (!active) {
            enable(client);
        }
        syncLocalPlayerToFreecam(client.player);
        if (ghost != null) {
            syncGhost(client.player);
        }
    }

    public static boolean handleMovement(ClientPlayerEntity player) {
        if (player == null || !isEnabled()) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return false;
        }
        if (!active) {
            enable(client);
        }
        if (!active) {
            return false;
        }
        prevFreecamPos = freecamPos;
        if (lookDirty) {
            lookDirty = false;
        } else {
            prevFreecamYaw = freecamYaw;
            prevFreecamPitch = freecamPitch;
        }
        if (player.input != null) {
            player.input.movementForward = 0.0f;
            player.input.movementSideways = 0.0f;
            player.input.playerInput = PlayerInput.DEFAULT;
        }
        FreeCam freeCam = BKClientInit.getInstance().getModuleManager().getModule(FreeCam.class);
        float speedX = freeCam != null ? clampSpeed(freeCam.getSpeedX().getCurrentValue()) : 0.35f;
        float speedY = freeCam != null ? clampSpeed(freeCam.getSpeedY().getCurrentValue()) : 0.35f;
        
        long win = client.getWindow().getHandle();
        float forward = 0.0f;
        if (isKeyDown(win, client.options.forwardKey)) {
            forward += 1.0f;
        }
        if (isKeyDown(win, client.options.backKey)) {
            forward -= 1.0f;
        }
        float strafe = 0.0f;
        if (isKeyDown(win, client.options.leftKey)) {
            strafe += 1.0f;
        }
        if (isKeyDown(win, client.options.rightKey)) {
            strafe -= 1.0f;
        }
        float len = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (len > 0.001f) {
            forward /= len;
            strafe /= len;
        }
        double rad = Math.toRadians(freecamYaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double dx = (forward * -sin + strafe * cos) * speedX;
        double dz = (forward * cos + strafe * sin) * speedX;
        double dy = 0.0;
        if (isKeyDown(win, client.options.jumpKey)) {
            dy += speedY;
        }
        if (isKeyDown(win, client.options.sneakKey)) {
            dy -= speedY;
        }
        if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
            freecamPos = freecamPos.add(dx, dy, dz);
        }
        syncLocalPlayerToFreecam(player);
        return true;
    }

    public static void updateCrosshair(MinecraftClient client, float tickDelta) {
        if (client == null || !isEnabled() || client.world == null) {
            return;
        }
        boolean activeState = isActive();
        Entity camEntity = client.getCameraEntity();
        double blockRange = activeState ? freecamBlockRange : getPlayerBlockRange(client);
        double entityRange = activeState ? freecamEntityRange : getPlayerEntityRange(client);
        Vec3d start;
        Vec3d dir;
        if (activeState) {
            start = getCameraPos(tickDelta);
            dir = Vec3d.fromPolar(getCameraPitch(tickDelta), getCameraYaw(tickDelta));
        } else {
            if (camEntity == null) {
                return;
            }
            start = new Vec3d(camEntity.getX(), camEntity.getEyeY(), camEntity.getZ());
            dir = camEntity.getRotationVec(tickDelta);
        }
        Vec3d end = start.add(dir.multiply(blockRange));
        EntityHitResult ehr = raycastEntities(client, start, dir, entityRange);
        BlockHitResult blockHit = client.world.raycast(new RaycastContext(
            start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (camEntity == null ? client.player : camEntity)
        ));
        double entityDistSq = Double.POSITIVE_INFINITY;
        if (ehr != null) {
            entityDistSq = start.squaredDistanceTo(ehr.getPos());
        }
        double blockDistSq = blockHit == null ? Double.POSITIVE_INFINITY : start.squaredDistanceTo(blockHit.getPos());
        
            IMinecraftClient mcAccessor = (IMinecraftClient) client;
        if (ehr != null && entityDistSq <= blockDistSq) {
            mcAccessor.setCrosshairTarget(ehr);
            mcAccessor.setTargetedEntity(ehr.getEntity());
        } else if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
            mcAccessor.setCrosshairTarget(blockHit);
            mcAccessor.setTargetedEntity(null);
        }
    }

    private static double getPlayerBlockRange(MinecraftClient client) {
        return client != null && client.player != null ? client.player.getBlockInteractionRange() : 4.5;
    }

    private static double getPlayerEntityRange(MinecraftClient client) {
        return client != null && client.player != null ? client.player.getEntityInteractionRange() : 3.0;
    }

    private static EntityHitResult raycastEntities(MinecraftClient client, Vec3d start, Vec3d dir, double range) {
        if (client == null || client.player == null) {
            return null;
        }
        Vec3d end = start.add(dir.x * range, dir.y * range, dir.z * range);
        Box box = new Box(start, start).stretch(dir.multiply(range)).expand(1.0);
        return ProjectileUtil.raycast(
            client.player, start, end, box, entity -> !entity.isSpectator() && entity.canHit(), range * range
        );
    }

    public static void enable(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        ClientPlayerEntity player = client.player;
        active = true;
        originPos = player.getPos();
        originYaw = player.getYaw();
        originPitch = player.getPitch();
        originBodyYaw = player.getBodyYaw();
        originHeadYaw = player.getHeadYaw();
        prevPlayerNoClip = player.noClip;
        prevPlayerNoGravity = player.hasNoGravity();
        prevFreecamPos = freecamPos = player.getEyePos();
        freecamYaw = player.getYaw();
        freecamPitch = player.getPitch();
        prevFreecamYaw = freecamYaw;
        prevFreecamPitch = freecamPitch;
        ghost = buildGhost(client.world, player);
    }

    public static void disable(MinecraftClient client) {
        active = false;
        if (client != null && client.player != null) {
            ClientPlayerEntity player = client.player;
            player.setVelocity(Vec3d.ZERO);
            player.setPos(originPos.x, originPos.y, originPos.z);
            player.setYaw(originYaw);
            player.setPitch(originPitch);
            player.setBodyYaw(originBodyYaw);
            player.setHeadYaw(originHeadYaw);
            player.noClip = prevPlayerNoClip;
            player.setNoGravity(prevPlayerNoGravity);

            EntityAccessor ea = (EntityAccessor) player;
            ea.setPrevX(originPos.x);
            ea.setPrevY(originPos.y);
            ea.setPrevZ(originPos.z);
            ea.setLastRenderX(originPos.x);
            ea.setLastRenderY(originPos.y);
            ea.setLastRenderZ(originPos.z);
            ea.setPrevYaw(originYaw);
            ea.setPrevPitch(originPitch);
        }
        ghost = null;
    }

    private static void syncLocalPlayerToFreecam(ClientPlayerEntity player) {
        if (player == null) {
            return;
        }
        double y = freecamPos.y - player.getEyeHeight(player.getPose());
        player.noClip = true;
        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.setPos(freecamPos.x, y, freecamPos.z);
        player.setYaw(freecamYaw);
        player.setPitch(freecamPitch);
        player.setBodyYaw(freecamYaw);
        player.setHeadYaw(freecamYaw);

        EntityAccessor ea = (EntityAccessor) player;
        ea.setPrevX(freecamPos.x);
        ea.setPrevY(y);
        ea.setPrevZ(freecamPos.z);
        ea.setLastRenderX(freecamPos.x);
        ea.setLastRenderY(y);
        ea.setLastRenderZ(freecamPos.z);
        ea.setPrevYaw(freecamYaw);
        ea.setPrevPitch(freecamPitch);
    }

    private static OtherClientPlayerEntity buildGhost(ClientWorld world, PlayerEntity player) {
        if (world == null || player == null) {
            return null;
        }
        OtherClientPlayerEntity g = new OtherClientPlayerEntity(world, player.getGameProfile());
        g.copyPositionAndRotation(player);
        g.setYaw(originYaw);
        g.setPitch(originPitch);
        g.setBodyYaw(originBodyYaw);
        g.setHeadYaw(originHeadYaw);
        g.setPose(player.getPose());
        g.setOnGround(player.isOnGround());
        g.setVelocity(Vec3d.ZERO);
        g.noClip = true;
        g.setNoGravity(true);
        g.setId(ghostId);
        copyEquipment(player, g);
        return g;
    }

    private static void syncGhost(PlayerEntity from) {
        if (ghost == null) {
            return;
        }
        ghost.setPos(originPos.x, originPos.y, originPos.z);
        ghost.setYaw(originYaw);
        ghost.setPitch(originPitch);
        ghost.setBodyYaw(originBodyYaw);
        ghost.setHeadYaw(originHeadYaw);
        
        EntityAccessor ea = (EntityAccessor) ghost;
        ea.setPrevX(originPos.x);
        ea.setPrevY(originPos.y);
        ea.setPrevZ(originPos.z);
        ea.setLastRenderX(originPos.x);
        ea.setLastRenderY(originPos.y);
        ea.setLastRenderZ(originPos.z);
        ea.setPrevYaw(originYaw);
        ea.setPrevPitch(originPitch);
        
        LivingEntityAccessor lea = (LivingEntityAccessor) ghost;
        lea.setPrevBodyYaw(originBodyYaw);
        lea.setPrevHeadYaw(originHeadYaw);
        
        copyEquipment(from, ghost);
    }

    private static void copyEquipment(PlayerEntity from, OtherClientPlayerEntity to) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = from.getEquippedStack(slot);
            to.equipStack(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    private static boolean isKeyDown(long window, net.minecraft.client.option.KeyBinding binding) {
        if (binding == null) {
            return false;
        }
        net.minecraft.client.util.InputUtil.Key key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(binding);
        if (key == null || key.getCategory() != net.minecraft.client.util.InputUtil.Type.KEYSYM) {
            return binding.isPressed();
        }
        int code = key.getCode();
        if (code == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            return false;
        }
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    private static float clampSpeed(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v)) {
            return 0.35f;
        }
        return MathHelper.clamp(v, 0.05f, 2.0f);
    }

    public static void renderGhost(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (!active) {
            return;
        }
        OtherClientPlayerEntity ghostEntity = ghost;
        if (ghostEntity == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ClientWorld world = context.world();
        if (world == null) {
            return;
        }
        net.minecraft.client.render.Camera camera = context.camera();
        if (camera == null) {
            return;
        }
        net.minecraft.client.util.math.MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            matrices = new net.minecraft.client.util.math.MatrixStack();
        }
        Vec3d camPos = camera.getPos();
        net.minecraft.client.render.entity.EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        net.minecraft.client.render.VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Vec3d pos = ghostEntity.getPos();
        int light = net.minecraft.client.render.WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(pos));
        dispatcher.render(ghostEntity, pos.x, pos.y, pos.z, ghostEntity.getYaw(), matrices, consumers, light);
        consumers.draw();
        matrices.pop();
    }
}
