package dev.bkclient.combatengine.client.modules.impl.combat;

import dev.bkclient.combatengine.AuraServices;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.*;
import dev.bkclient.combatengine.api.storages.implement.RotationStorage;
import dev.bkclient.combatengine.api.utils.rotate.Rotation;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.client.modules.impl.player.AutoEat;
import dev.bkclient.combatengine.client.modules.impl.combat.bkadaptive.*;
import dev.bkclient.combatengine.client.modules.settings.implement.*;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.*;

/** BKAdaptive combat flow hosted by BK. Independent settings/state from Aura. */
public final class Aura2 extends Module {
    public static final Aura2 INSTANCE = new Aura2();
    public final ModeSetting rotation = new ModeSetting("Rotation", "Funtime", "Funtime", "Smooth", "Grim", "SlothAC", "HolyWorld", "Bezier", "NoRotate");
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting naked = new BooleanSetting("Unarmored", true);
    private final BooleanSetting invisible = new BooleanSetting("Invisible", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    public final FloatSetting range = new FloatSetting("Attack range", 3, 1, 6, .05f);
    private final ModeSetting sort = new ModeSetting("Priority", "Angle", "Angle", "Distance", "Health");
    private final BooleanSetting onlyCrit = new BooleanSetting("Only critical hits", true);
    private final BooleanSetting spaceCrit = new BooleanSetting("Smart crits", true);
    private final BooleanSetting walls = new BooleanSetting("Through walls", false);
    private final BooleanSetting releaseShield = new BooleanSetting("Release shield", true);
    private final BooleanSetting breakShield = new BooleanSetting("Break shield", true);
    private final BooleanSetting pauseEating = new BooleanSetting("Pause while eating", true);
    private final BooleanSetting tpsSync = new BooleanSetting("TPS sync", true);
    private final BooleanSetting visibleAim = new BooleanSetting("Visible aim", false);
    private final BooleanSetting correction = new BooleanSetting("Movement correction", true);
    private final BooleanSetting shieldMessage = new BooleanSetting("Shield confirmation", true);
    private boolean embeddedActive;
    private float acquisitionRange;
    private final BKAdaptiveRotations rotations = new BKAdaptiveRotations();
    private final boolean[] collisions = new boolean[8];
    private SimulatedPlayer[] predicted;
    private LivingEntity target;
    private int cooldown, hits, sprintUntil = -1, shieldResume = -1;
    private long lastAttack;
    private boolean serverSprinting;
    private String lastMode = "";
    private Vec3d jitter = Vec3d.ZERO;
    private int lastShieldTarget = -1, lastShieldAge = -100;
    private net.minecraft.client.world.ClientWorld lastWorld;

    private Aura2() {
        super("Aura2", "BKAdaptive combat with BK integration", ModuleCategory.COMBAT);
        addSettings(rotation, new ListSetting("Targets", players, naked, invisible, mobs, animals), range, sort,
            onlyCrit, spaceCrit, walls, releaseShield, breakShield, pauseEating, tpsSync, visibleAim, correction, shieldMessage);
    }
    public static Aura2 embedded() { return new Aura2(); }
    public void beginEmbedded() { reset(); embeddedActive=true; }
    public void endEmbedded() { resumeShield(); reset(); embeddedActive=false; }
    public void configure(String mode, float distance, float acquire, String priority, boolean[] targets,
                          boolean smart, boolean throughWalls, boolean release, boolean shieldBreak,
                          boolean pause, boolean visible, boolean syncTps) {
        rotation.set(mode); range.setValue(distance); acquisitionRange=Math.max(distance,acquire); sort.set(priority);
        players.setState(targets[0]); naked.setState(targets[1]); invisible.setState(targets[2]);
        animals.setState(targets[3]); mobs.setState(targets[4]);
        onlyCrit.setState(true); spaceCrit.setState(smart); walls.setState(throughWalls);
        releaseShield.setState(release); breakShield.setState(shieldBreak); pauseEating.setState(pause);
        visibleAim.setState(visible); tpsSync.setState(syncTps);
    }
    public boolean correctMovement() {
        return isEnable() && correction.isState() && !visibleAim.isState() && !rotation.is("NoRotate")
            && dev.bkclient.combatengine.api.storages.implement.FreeLookStorage.isActive();
    }
    public LivingEntity getTarget() { return target; }
    public boolean preventSprint() { return (isEnable() || embeddedActive) && mc.player != null && mc.player.age <= sprintUntil; }
    @Override public void onEnable() { reset(); super.onEnable(); }
    @Override public void onDisable() {
        resumeShield(); reset();
        if (RotationStorage.instance != null && !Aura.INSTANCE.isEnable()) RotationStorage.instance.stopRotation();
        super.onDisable();
    }
    private void reset() {
        target = null; predicted = null; cooldown = hits = 0; sprintUntil = shieldResume = -1;
        serverSprinting = mc.player != null && mc.player.isSprinting();
        lastWorld = mc.world; lastAttack = System.currentTimeMillis(); lastShieldTarget = -1;
        Arrays.fill(collisions, false); rotations.reset();
        lastMode = rotation.getCurrent();
        jitter = Vec3d.ZERO;
    }

    @EventLink(priority = -10)
    public void prepare(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.world != lastWorld) { reset(); return; }
        if (shieldResume >= 0 && mc.player.age >= shieldResume) resumeShield();
        cooldown = Math.max(0, cooldown - 1);
        for (int i = collisions.length - 1; i > 0; i--) collisions[i] = collisions[i-1];
        collisions[0] = mc.player.verticalCollision;
        predicted = null;
        if (!lastMode.equals(rotation.getCurrent())) {
            RotationStorage.instance.stopRotation(); rotations.reset(); lastMode = rotation.getCurrent();
        }
        LivingEntity old = target;
        if (!valid(target)) target = findTarget();
        if (old != target) { rotations.reset(); jitter = Vec3d.ZERO; }
        if (target == null || paused()) return;
        Vec3d point = point(target, embeddedActive ? acquisitionRange : range.get());
        if (point == null) return;
        if (rotation.is("SlothAC")) {
            var random = java.util.concurrent.ThreadLocalRandom.current();
            jitter = jitter.add(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).multiply(.1);
            Vec3d candidate = point.add(jitter);
            if (target.getBoundingBox().contains(candidate) && reachable(mc.player.getEyePos(), candidate)) point = candidate;
        }
        Rotation current = new Rotation(mc.player);
        Rotation desired = toward(point);
        if (!rotation.is("NoRotate")) {
            int horizon = rotation.is("Funtime") ? 5 : rotation.is("Grim") ? 4 : 0;
            if (horizon == 0 || canAttack(horizon)) {
                Rotation next = rotations.step(rotation.getCurrent(), current, desired, canCrit(0), canAttack(1),
                    rayHits(), mc.player.age, hits, System.currentTimeMillis() - lastAttack, point.distanceTo(mc.player.getEyePos()));
                RotationStorage.update(next, 360, 360, 180, 180, 1, 1, visibleAim.isState());
            }
        }
        if (canAttack(1)) {
            sprintUntil = mc.player.age + 1;
            mc.player.setSprinting(false);
            if (serverSprinting) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                serverSprinting = false;
            }
            if (releaseShield.isState() && mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.SHIELD)) {
                mc.interactionManager.stopUsingItem(mc.player);
                shieldResume = mc.player.age + 1;
            }
        }
    }

    @EventLink
    public void attack(EventUpdatePost event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || target == null) return;
        // Player physics advanced since PRE; regenerate forecasts for the actual attack instant.
        predicted = null;
        if (!valid(target) || !canAttack(0) || !rayHits() || mc.player.isSprinting() || serverSprinting) return;
        if (mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.SHIELD)) return;
        LivingEntity victim = target;
        mc.interactionManager.attackEntity(mc.player, victim);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastAttack = System.currentTimeMillis();
        breakShield(victim);
        hits++;
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getType() == EventPacket.Type.SEND) {
            if (event.getPacket() instanceof ClientCommandC2SPacket p) {
                if (p.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) serverSprinting = true;
                if (p.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) serverSprinting = false;
            }
            if (target != null && (event.getPacket() instanceof HandSwingC2SPacket || event.getPacket() instanceof UpdateSelectedSlotC2SPacket)) {
                int delay = rotation.is("Funtime") || rotation.is("SlothAC") ? new int[]{10,10,10,10,12}[hits%5]
                    : rotation.is("Smooth") ? new int[]{10,10,10,13}[hits%4] : 10;
                cooldown = Math.round(delay * (tpsSync.isState() ? 20f / MathHelper.clamp(AuraServices.INSTANCE.tpsCalc.getTPS(),1,20) : 1));
            }
        } else if (event.getPacket() instanceof EntityStatusS2CPacket p && p.getStatus() == 30 && shieldMessage.isState()) {
            Entity entity = p.getEntity(mc.world);
            if (entity != null && entity.getId() == lastShieldTarget && mc.player.age-lastShieldAge < 40) {
                mc.player.sendMessage(Text.literal((embeddedActive ? "[Aura]" : "[Aura2]")+" Shield broken: " + entity.getName().getString()), false);
                lastShieldTarget = -1;
            }
        }
    }
    @EventLink
    public void onMove(EventMoveInput event) {
        if (preventSprint()) { event.setSprint(false); mc.player.setSprinting(false); }
        if (correctMovement()) {
            var result=dev.bkclient.combatengine.api.utils.input.AuraMovement.correct(
                dev.bkclient.combatengine.api.storages.implement.FreeLookStorage.getFreeYaw(), mc.player.getYaw(), event.getForward(), event.getStrafe(), false);
            event.setForward(result.x);event.setStrafe(result.y);
        }
    }

    private boolean paused() {
        if(!mc.player.isAlive() || mc.player.isSpectator()) return true;
        if (mc.currentScreen instanceof HandledScreen<?> || AutoEat.shouldSuppressCombat()) return true;
        if (!mc.player.isUsingItem()) return false;
        UseAction use = mc.player.getActiveItem().getUseAction();
        boolean consuming = use == UseAction.EAT || use == UseAction.DRINK || use == UseAction.BOW || use == UseAction.CROSSBOW || use == UseAction.SPEAR;
        return consuming && (mc.player.getActiveHand() == Hand.MAIN_HAND || pauseEating.isState());
    }
    private boolean canAttack(int future) {
        if (target == null || !target.isAlive() || paused() || point(target) == null) return false;
        for (int i=0; i<=future; i++) if (canCrit(i)) return true;
        return false;
    }
    private SimulatedPlayer prediction(int ticks) {
        if (predicted == null) predicted = new SimulatedPlayer[8];
        if (predicted[ticks] == null) predicted[ticks] = SimulatedPlayer.simulateLocalPlayer(ticks);
        return predicted[ticks];
    }
    private boolean canCrit(int future) {
        if (mc.player.getAttackCooldownProgress(future + .5f) <= .9f || cooldown-future > 0) return false;
        if (!onlyCrit.isState()) return true;
        var player = mc.player;
        // Conditions where vanilla critical attacks are unavailable: BKAdaptive permits ordinary hits.
        if (player.hasStatusEffect(StatusEffects.LEVITATION) || player.hasStatusEffect(StatusEffects.BLINDNESS)
            || player.hasStatusEffect(StatusEffects.SLOW_FALLING) || player.isTouchingWater() || player.isInLava()
            || player.isClimbing() || player.getAbilities().flying || player.hasVehicle() || player.isGliding()) return true;
        for (BlockPos pos : BlockPos.iterate(player.getBoundingBox()))
            if (mc.world.getBlockState(pos).isOf(net.minecraft.block.Blocks.COBWEB)) return true;
        SimulatedPlayer state = prediction(future);
        if (spaceCrit.isState() && state.onGround && !mc.options.jumpKey.isPressed()) return true;
        if (player.getMainHandStack().getItem() instanceof MaceItem) return MaceItem.shouldDealAdditionalDamage(player);
        boolean nextGround = prediction(future+1).onGround, laterGround = prediction(future+2).onGround;
        return !state.onGround && state.fallDistance > 0
            && !(state.velocity.y < -.3 && laterGround && (collisions[4] || collisions[5] || collisions[6] || collisions[7]))
            && !(state.fallDistance == (float)state.velocity.y && nextGround)
            && !(state.fallDistance > 1 && nextGround);
    }
    private LivingEntity findTarget() {
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) if (e instanceof LivingEntity l && valid(l)) candidates.add(l);
        Comparator<LivingEntity> comparator = switch(sort.getCurrent()) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Distance" -> Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.player));
            default -> Comparator.comparingDouble(e -> new Rotation(mc.player).getDelta(toward(e.getBoundingBox().getCenter())));
        };
        return candidates.stream().min(comparator).orElse(null);
    }
    private boolean valid(LivingEntity e) {
        if (e == null || e == mc.player || !e.isAlive() || e.isRemoved() || e instanceof ArmorStandEntity) return false;
        if (e instanceof PlayerEntity) {
            if (!players.isState() || AuraServices.INSTANCE.friendStorage.isFriend(e.getName().getString())) return false;
            var antiBot = dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(dev.bkclient.systems.modules.modules.combat.AntiBot.class);
            if (antiBot != null && antiBot.isEnabled() && antiBot.isRWBot((PlayerEntity)e)) return false;
            if (!naked.isState() && e.getArmor() == 0 || !invisible.isState() && e.isInvisible()) return false;
        } else if (!(e instanceof HostileEntity && mobs.isState()) && !(e instanceof PassiveEntity && animals.isState())) return false;
        return point(e, embeddedActive ? acquisitionRange : range.get()) != null;
    }
    // BKAdaptive HitPointResolver: vertical samples ranked by angular cost, closest-box fallback.
    private Vec3d point(LivingEntity e) { return point(e, range.get()); }
    private Vec3d point(LivingEntity e, double distance) {
        Vec3d eye = mc.player.getEyePos(); Box box = e.getBoundingBox();
        Vec3d closest = new Vec3d(MathHelper.clamp(eye.x,box.minX,box.maxX),MathHelper.clamp(eye.y,box.minY,box.maxY),MathHelper.clamp(eye.z,box.minZ,box.maxZ));
        if (eye.squaredDistanceTo(closest) > distance*distance) return null;
        Vec3d best = null; double cost = Double.MAX_VALUE;
        Rotation current = new Rotation(mc.player);
        for (int i=0;i<=10;i++) {
            Vec3d p = new Vec3d(box.getCenter().x, MathHelper.lerp(i/10d,box.minY,box.maxY), box.getCenter().z);
            if (reachable(eye,p,distance)) { double c = current.getDelta(toward(p)); if (c < cost) { best=p; cost=c; } }
        }
        if (best == null) {
            Vec3d p = new Vec3d(MathHelper.clamp(eye.x,box.minX,box.maxX),MathHelper.clamp(eye.y,box.minY,box.maxY),MathHelper.clamp(eye.z,box.minZ,box.maxZ));
            if (reachable(eye,p,distance)) best=p;
        }
        return best;
    }
    private boolean reachable(Vec3d eye, Vec3d p) { return reachable(eye,p,range.get()); }
    private boolean reachable(Vec3d eye, Vec3d p, double distance) {
        return eye.squaredDistanceTo(p) <= distance*distance && (walls.isState() || mc.world.raycast(
            new RaycastContext(eye,p,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,mc.player)).getType() != HitResult.Type.BLOCK);
    }
    private Rotation toward(Vec3d point) {
        Vec3d v = point.subtract(mc.player.getEyePos());
        return new Rotation((float)Math.toDegrees(Math.atan2(v.z,v.x))-90,(float)-Math.toDegrees(Math.atan2(v.y,Math.hypot(v.x,v.z))));
    }
    private boolean rayHits() {
        if (target == null) return false;
        Vec3d eye = mc.player.getEyePos(), end = eye.add(new Rotation(mc.player).toVector().multiply(range.get()));
        if (target.getBoundingBox().contains(eye)) return true;
        return target.getBoundingBox().raycast(eye,end).filter(p -> reachable(eye,p)).isPresent();
    }
    private void breakShield(LivingEntity victim) {
        if (!breakShield.isState() || !victim.isBlocking() || victim.getRotationVec(1).normalize()
            .dotProduct(mc.player.getPos().subtract(victim.getPos()).normalize()) < Math.cos(Math.toRadians(110))) return;
        int axe=-1;
        for(int i=0;i<36;i++) if(mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {axe=i;break;}
        if(axe<0) return;
        int old=mc.player.getInventory().selectedSlot;
        boolean inventory=axe>=9;
        if(inventory && mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;
        try {
            if(inventory) mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,axe,old,SlotActionType.SWAP,mc.player);
            else {mc.player.getInventory().selectedSlot=axe;mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(axe));}
            lastShieldTarget=victim.getId(); lastShieldAge=mc.player.age;
            mc.interactionManager.attackEntity(mc.player,victim); mc.player.swingHand(Hand.MAIN_HAND);
        } finally {
            if(inventory) mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,axe,old,SlotActionType.SWAP,mc.player);
            else {mc.player.getInventory().selectedSlot=old;mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(old));}
        }
    }
    private void resumeShield() {
        if (shieldResume >= 0 && mc.player != null && mc.interactionManager != null && mc.options.useKey.isPressed()) {
            Hand hand = mc.player.getOffHandStack().isOf(Items.SHIELD) ? Hand.OFF_HAND : Hand.MAIN_HAND;
            if(mc.player.getStackInHand(hand).isOf(Items.SHIELD)) mc.interactionManager.interactItem(mc.player,hand);
        }
        shieldResume=-1;
    }
}
