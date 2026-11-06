package dev.bkclient.combatengine.bkengine.module.impl.combat;
import dev.bkclient.combatengine.client.modules.Module;
import dev.bkclient.combatengine.client.modules.settings.implement.*;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.*;
import dev.bkclient.combatengine.bkengine.EventBKEngineTick;
import dev.bkclient.combatengine.bkengine.manager.rotation.*;
import dev.bkclient.combatengine.bkengine.module.impl.combat.aura.RotationType;
import dev.bkclient.combatengine.bkengine.utils.aura.*;
import dev.bkclient.combatengine.bkengine.utils.math.*;
import dev.bkclient.combatengine.bkengine.utils.other.TimerUtil;
import dev.bkclient.combatengine.bkengine.utils.player.MoveUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.*;
import java.util.concurrent.ThreadLocalRandom;
import static net.minecraft.util.Hand.MAIN_HAND;

/** BKEngine combat flow, adapted only at the host and Minecraft API boundaries. */
public final class AttackAura extends Module {
    private static final float SERVER_RANGE_MARGIN = 0.05F;
    public static final AttackAura INSTANCE = new AttackAura();
    public static AttackAura get() { return INSTANCE; }
    public final FloatSetting attackRange = new FloatSetting("Attack range",3,2.5f,6,.1f);
    public final FloatSetting preRange = new FloatSetting("Extra detection range",1,0,10,.1f);
    public final ModeSetting targetPriority = new ModeSetting("Target priority", "Distance", "Distance", "Health", "Angle / FOV", "None");
    public final ListSetting targets = new ListSetting("Targets",
        new BooleanSetting("Players",true),new BooleanSetting("Unarmored players",true),
        new BooleanSetting("Mobs",false),new BooleanSetting("Friends",false));
    public final ModeSetting typeRotation = new ModeSetting("Rotation","FunTime","FunTime","SpookyTime","Default","Snap","Ana");
    public final ModeSetting typeSnap = new ModeSetting("Snap mode","360","360","FOV").visible(() -> typeRotation.is("Snap"));
    public final FloatSetting fov = new FloatSetting("FOV",50,25,90,1).visible(() -> typeRotation.is("Snap") && typeSnap.is("FOV"));
    public final ModeSetting typeMove = new ModeSetting("Movement correction","Focused","Focused","Free");
    public final BooleanSetting onlyWeapon = new BooleanSetting("Only weapon",false);
    public final BooleanSetting webLag = new BooleanSetting("Web Lag",false);
    public final BooleanSetting onlyCriticals = new BooleanSetting("Only critical hits",true);
    public final ListSetting others = new ListSetting("Checks",onlyCriticals,new BooleanSetting("Smart criticals",false),new BooleanSetting("Through walls",false));
    public final ListSetting noattackto = new ListSetting("Pause when",new BooleanSetting("Using an item",false),new BooleanSetting("Screen open",false));
    public final BooleanSetting antiBot = new BooleanSetting("Anti-bot",false);
    /** When the target blocks with a shield: hit once with an axe from the hotbar (silent slot switch), then back. */
    public final BooleanSetting shieldBreaker = new BooleanSetting("Break shields",true);
    /** Every Aura attack plays the equipped Melee cosmetic's attack animation, as a left click would. */
    public final BooleanSetting cosmeticAttack = new BooleanSetting("Cosmetic attack animation",true);
    private final AntiBot bkengineAntiBot = new AntiBot();
    private net.minecraft.client.world.ClientWorld lastWorld;
    private String lastTargetPriority;
    private boolean sprintHold;
    private boolean weaponBlocked;
    public boolean preventSprint() { return isEnable() && sprintHold; }
    private AttackAura() {
        super("Aura","Automatically attacks nearby targets",ModuleCategory.COMBAT);
        addSettings(typeRotation,attackRange,preRange,targetPriority,targets,typeSnap,fov,typeMove,others,onlyWeapon,webLag,noattackto,antiBot,shieldBreaker,cosmeticAttack);
    }
    public LivingEntity getTarget() { return target; }
    @Override public void onEnable() {
        sprintHold = false;
        dev.bkclient.combatengine.api.storages.implement.RotationStorage.instance.stopRotation();
        RotationProcess.resetParentTimeout();
        lastWorld = mc.world;
        onResetOnWorld();
        bkengineAntiBot.clear();
        super.onEnable();
    }
    public static LivingEntity target = null;

    public float[] getRanges() {
        return new float[]{attackRange.get(), preRange.get()};
    }


    @EventLink(priority = -10)
    
    public void onEvent(EventUpdate e) {
        sprintHold = false;
        dev.bkclient.combatengine.bkengine.WebLagController.INSTANCE.tick();
        if (mc.world != lastWorld) {
            onResetOnWorld();
            RotationProcess.resetParentTimeout();
            bkengineAntiBot.clear();
            lastWorld = mc.world;
        }
        if (mc.player == null || mc.world == null || mc.interactionManager == null) { target = null; return; }
        // "Only weapon" without a weapon in hand: no attack and no rotation either, so Aura behaves as if there is no target.
        if (!weaponAllowed()) {
            if (target != null || weaponBlocked == false) {
                target = null;
                weaponBlocked = true;
                var storage = dev.bkclient.combatengine.api.storages.implement.RotationStorage.instance;
                if (storage != null) storage.stopRotation();
                RotationProcess.resetParentTimeout();
            }
            return;
        }
        weaponBlocked = false;
        if (antiBot.isState()) bkengineAntiBot.onTick(e); else bkengineAntiBot.clear();


        // Keep a valid target, but apply a changed priority on the next tick.
        if (target == null || !isValidTarget(target) || !targetPriority.getCurrent().equals(lastTargetPriority)) {
            updateTarget();
        }

        if(stoptick != 0) {
            stoptick--;
        }

        if (target != null && !checkToAttack() && !UAttack.canAttackWithoutCritical()
            && !AttackUtil.hasMovementRestrictions() && UAttack.resetSprintTick(target, getRanges())) {
            holdSprint();
        }

        if (!checkToAttack() && target != null) {
            attackEntity();
        }


    }

    private void holdSprint() {
        sprintHold = true;
        // Keep the user's held key intact so vanilla can resume sprint after the hit.
        mc.player.setSprinting(false);
    }
    /** Keep 1.21.4's signed axes: vanilla applies slowdown before normalizing velocity. */
    @EventLink(priority = -100)
    private void syncInputKeys(EventMoveInput event) {
        event.setJump(mc.options.jumpKey.isPressed());
        event.setSneak(mc.options.sneakKey.isPressed());
        event.setSprint(!preventSprint() && mc.options.sprintKey.isPressed());
    }
    public boolean isAttackPaused() { return mc.player == null || checkToAttack(); }
    private boolean weaponAllowed() {
        if (mc.player == null) return false;
        if (!onlyWeapon.isState()) return true;
        var item = mc.player.getMainHandStack().getItem();
        return item instanceof net.minecraft.item.SwordItem
            || item instanceof net.minecraft.item.AxeItem
            || item instanceof net.minecraft.item.TridentItem
            || item instanceof net.minecraft.item.MaceItem;
    }
    private boolean checkToAttack() {
        if (!weaponAllowed()) return true;
        var web = dev.bkclient.BKClientInit.getInstance().getModuleManager().getModule(
            dev.bkclient.systems.modules.modules.combat.AutoWeb.class);
        if (web != null && web.isPlacingBlock()) return true;


        boolean baseCheck = mc.player.isUsingItem() && noattackto.is("Using an item");

        boolean screenCheck = noattackto.is("Screen open") && mc.currentScreen != null  && !(mc.currentScreen instanceof dev.bkclient.ui.menu.MenuScreen);

        return baseCheck ||  screenCheck;
    }

    public void attackEntity() {
        if (target == null || !dev.bkclient.combatengine.bkengine.WebLagController.INSTANCE.readyToAttack()) return;
        float safeAttackRange = Math.max(0.0F, attackRange.get() - SERVER_RANGE_MARGIN);
        if (AuraUtil.getStrictDistance(target) >= safeAttackRange) {
            return;
        }
        float[] ranges = getRanges();
        ranges = new float[]{safeAttackRange, ranges[1], safeAttackRange + ranges[1]};

        if (target == null) {


            return;
        }

        UAttack.antiMissesHittingUpdate(target, false, true, false);


        boolean canAttack = UAttack.shouldAttack(target, !typeRotation.is("Ana"), true, true, 0L, ranges);


        if (canAttack) {
            boolean resetRequired = dev.bkclient.combatengine.bkengine.AttackTransaction.requiresSprintReset(
                    onlyCriticals.isState(), AttackUtil.isPlayerInCriticalState(),
                    AttackUtil.hasMovementRestrictions(), UAttack.canAttackWithoutCritical());
            boolean sentSprint = ((dev.bkclient.mixin.accessors.BKEngineClientPlayerAccessor)mc.player).bkengine$lastSentSprint();
            if (!dev.bkclient.combatengine.bkengine.AttackTransaction.ready(mc.player.isSprinting(), sentSprint, resetRequired)) {
                if (resetRequired) holdSprint();
                return;
            }

            final Runnable[] shieldBreak = UAttack.hitShieldBreakTaskForUse(target, shieldBreaker.isState()),
                    shieldPressBypass = UAttack.resetShieldSilentTaskForUse(true);
            final Runnable preHitSendCodeSingleTick = () -> {
                shieldPressBypass[0].run();
                shieldBreak[0].run();
            }, postHitSendCodeSingleTick = () -> {
                shieldBreak[1].run();
                shieldPressBypass[1].run();
            };




            if (!UAttack.useEntity(target, preHitSendCodeSingleTick, postHitSendCodeSingleTick, MAIN_HAND,
                             false)) return;


            justAttacked = true;
            if (cosmeticAttack.isState()) dev.bkclient.systems.cosmetics.render.MeleeCosmeticRenderer.attack(true);
            attackFlickAt = System.currentTimeMillis() + 250;

            count = (count + 1) % 2;

            hitCount++;
            if (hitCount >= pitchFlickThreshold) {
                hitCount = 0;
                pitchFlickThreshold = ThreadLocalRandom.current().nextInt(15,19);
                pitchFlickActive = true;
                pitchFlickEndTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(50, 70);
            }

        }


    }

    public static int stoptick = 0;

    @EventLink
    private void setCorrection(EventMoveInput eventMoveInput) {

        if (target != null && mc.player != null && mc.world != null && typeMove.is("Free")) {


            MoveUtil.fixMovement(eventMoveInput, mc.player.getYaw(), mc.gameRenderer.getCamera().getYaw());
        }


            if (target != null && mc.player != null && mc.world != null && typeMove.is("Focused") && ServerUtil.isFunTime()) {
                if (!mc.player.isSubmergedInWater()) {
                    if (ServerUtil.isCopyTime()) {

                        Vec3d vec3d = AuraUtil.getVector2(target);
                        float yaw = (float) Math.toDegrees(Math.atan2(-vec3d.x, vec3d.z));
                        MoveUtil.fixMovement(eventMoveInput, mc.player.getYaw(), yaw);


                    } else {
                        Vec3d vec = target.getPos().subtract(mc.player.getEyePos()).normalize();

                        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
                        MoveUtil.fixMovement(eventMoveInput, mc.player.getYaw(), yaw);

                    }
                }
            }
            if (target != null && mc.player != null && mc.world != null && typeMove.is("Focused") && !ServerUtil.isFunTime()) {

                if (ServerUtil.isCopyTime()) {

                    Vec3d vec3d = AuraUtil.getVector2(target);
                    float yaw = (float) Math.toDegrees(Math.atan2(-vec3d.x, vec3d.z));
                    MoveUtil.fixMovement(eventMoveInput, mc.player.getYaw(), yaw);


                } else {
                    Vec3d vec = target.getPos().subtract(mc.player.getEyePos()).normalize();

                    float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
                    MoveUtil.fixMovement(eventMoveInput, mc.player.getYaw(), yaw);

                }

        }
    }


public static long lastLookUpTime = 0;
public static long nextLookUpDelay = ThreadLocalRandom.current().nextLong(90000, 180000);
public static boolean isLookingUp = false;
public static long lookUpStartTime = 0;
public static int lookUpDuration = 0;
    public static long lastAttackTime = 0L;


    public float tick = 0;
    public static float lastYaw;
    public static float lastPitch;
    public Vec3d targetPoint;
    public Vec3d currentPoint;
    private int count;
    public boolean justAttacked = false;
    public long attackFlickAt = 0;

    private int hitCount = 0;
    private int pitchFlickThreshold = ThreadLocalRandom.current().nextInt(14,19);
    public boolean pitchFlickActive = false;
    public long pitchFlickEndTime = 0;

    public TimerUtil timeSped1 = new TimerUtil();
    public TimerUtil timeSped2 = new TimerUtil();


    @EventLink
    
    public void onRotate(EventBKEngineTick e) {
        if (!weaponAllowed()) return;
        if (target == null || mc.player == null || mc.world == null) {
            return;
        }
        float[] ranges = getRanges();
        ranges = new float[]{ranges[0], ranges[1], ranges[0] + ranges[1]};
        boolean canAttack = UAttack.shouldAttack(target, false, true, true, -350, ranges);

        for (RotationType type : RotationType.values()) {
            if (typeRotation.is(type.getName())) {
                type.getRotation().onRotation(this, target, ranges, canAttack);
                break;
            }
        }
        if (typeRotation.is("FunTime")) {

            Vec3d vec3d = UBoxPoints.getBestVector3dOnEntityBox(target.getBoundingBox(), false).add(0.2F * Math.sin(System.currentTimeMillis() / 250D), (target.getHeight() / 6)  * Math.cos(System.currentTimeMillis() / 850D), 0).subtract(mc.player.getEyePos());




            long currentTime = System.currentTimeMillis();

            if (!isLookingUp &&
                    currentTime - lastLookUpTime >= nextLookUpDelay) {

                isLookingUp = true;
                lookUpStartTime = currentTime;
                lookUpDuration = ThreadLocalRandom.current().nextInt(300, 400);
                lastLookUpTime = currentTime;
                nextLookUpDelay =
                        ThreadLocalRandom.current().nextLong(9100, 11200);
            }

            boolean fastspeed = false;
            if (isLookingUp && currentTime - lookUpStartTime >= lookUpDuration) {
                isLookingUp = false;
            }
            if(currentTime - lookUpStartTime >= lookUpDuration + 70L) {
                fastspeed =  true;
            }



            float yaw = (float) Math.toDegrees(Math.atan2(-vec3d.x, vec3d.z));
            float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec3d.y, Math.hypot(vec3d.x, vec3d.z))), -90, 90);

            float speedYAW =  MathUtil.randomLerp(44,66);
            float speedPit = !fastspeed ?  MathUtil.randomLerp(120,170) : MathUtil.randomLerp(4,7   );


            if(canAttack && fastspeed) {
                tick= MathUtil.random(0,2);
            }
            boolean attack = false;
            if(tick > 0) {
                attack = true;
                tick --;
            }
            float randomAttackShift = 0;
            float waveA = (float) Math.cos(System.currentTimeMillis() / 70D);
            float waveB = (float) Math.sin(System.currentTimeMillis() / 110D);

            if(attack) {
                lastYaw = yaw;
                lastPitch = pitch;
            }


            float yawJitter = waveA *  MathUtil.randomLerp(6, 15) ;
            float pitchJitter = waveB *  MathUtil.randomLerp(6,12) ;

            float finalPitch = isLookingUp ? -MathUtil.randomLerp(85,90) : (lastPitch);

            RotationProcess.update(new Rotation(lastYaw + yawJitter,finalPitch + pitchJitter), speedYAW,
                    speedPit, MathUtil.randomInt(35,45), MathUtil.randomInt(19,45), MathUtil.randomInt(0,3), 15, false);


        }
        
        
    }



    private void onResetOnWorld() {
        if(mc.player != null && mc.gameRenderer.getCamera() != null && mc.world != null) {
            lastPitch = mc.gameRenderer.getCamera().getPitch();
            lastYaw = mc.gameRenderer.getCamera().getYaw();

        }
        target = null;
        targetPoint = null;
        currentPoint = null;
        hitCount = 0;
        pitchFlickActive = false;


    }


    @Override
    
    public void onDisable() {
        sprintHold = false;
        dev.bkclient.combatengine.bkengine.WebLagController.INSTANCE.flush();
        super.onDisable();
        if(mc.player != null && mc.gameRenderer.getCamera() != null && mc.world != null) {
            lastPitch = mc.gameRenderer.getCamera().getPitch();
            lastYaw = mc.gameRenderer.getCamera().getYaw();
        }
        target = null;
        targetPoint = null;
        currentPoint = null;
        hitCount = 0;
        pitchFlickActive = false;


    }




    private void updateTarget() {
        String priority = targetPriority.getCurrent();
        lastTargetPriority = priority;
        LivingEntity bestTarget = null;
        double bestScore = Double.POSITIVE_INFINITY;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F).normalize();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!isValidTarget(living)) continue;
            if (priority.equals("None")) {
                bestTarget = living; // First valid entity in world iteration order, without sorting.
                break;
            }
            double score = switch (priority) {
                case "Health" -> living.getHealth();
                case "Angle / FOV" -> {
                    Vec3d targetPos = living.getPos().add(0, living.getHeight() * 0.5, 0);
                    Vec3d toTarget = targetPos.subtract(eyePos).normalize();
                    yield Math.acos(MathHelper.clamp(lookVec.dotProduct(toTarget), -1.0, 1.0));
                }
                default -> mc.player.squaredDistanceTo(living);
            };
            if (score < bestScore) {
                bestScore = score;
                bestTarget = living;
            }
        }
        target = bestTarget;
    }


    private float auraDist() {
        return attackRange.get() + preRange.get();
    }

    public double getTargetFov(LivingEntity entity) {
        Vec3d targetPos = entity.getPos().add(0, entity.getHeight() * 0.5, 0);
        Vec3d vec = targetPos.subtract(mc.player.getEyePos());

        float yaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

        float yawDelta = MathHelper.wrapDegrees(yaw - Rotation.cameraYaw());
        float pitchDelta = pitch - Rotation.cameraPitch();
        return Math.hypot(yawDelta, pitchDelta);
    }


    private boolean isValidTarget(LivingEntity entity) {
        if (entity.isRemoved() || entity.getWorld() != mc.world) return false;
        if (entity instanceof ClientPlayerEntity) return false;

        if (mc.player.distanceTo(entity) > auraDist()) return false;

        if (!others.is("Through walls")) {
            if (!mc.player.canSee(entity)) return false;
        }

        if (entity instanceof PlayerEntity p) {
            if (bkengineAntiBot.isBot(p)) return false;
            if (dev.bkclient.BKClientInit.getInstance().getFriendManager().isFriend(p.getName().getString()) && !targets.is("Friends")) return false;
        }
        if (entity instanceof PlayerEntity && !targets.is("Players")) return false;

        if (entity instanceof PlayerEntity && entity.getArmorVisibility() <= 0 && !targets.is("Unarmored players")) return false;

        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative()) return false;

        if ((entity instanceof Monster
                || entity instanceof SlimeEntity
                || entity instanceof VillagerEntity
                || entity instanceof AnimalEntity
                || entity instanceof FishEntity
                || entity instanceof SchoolingFishEntity
                || entity instanceof CodEntity
                || entity instanceof SalmonEntity
                || entity instanceof TropicalFishEntity
                || entity instanceof PufferfishEntity
                || entity instanceof SquidEntity
                || entity instanceof GlowSquidEntity
                || entity instanceof DolphinEntity
                || entity instanceof TurtleEntity
                || entity instanceof AxolotlEntity)
                && !targets.is("Mobs")) {
            return false;
        }


        return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStandEntity);
    }


}
