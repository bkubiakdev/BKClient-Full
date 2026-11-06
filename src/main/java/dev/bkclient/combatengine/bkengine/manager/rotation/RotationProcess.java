package dev.bkclient.combatengine.bkengine.manager.rotation;
import dev.bkclient.combatengine.api.storages.implement.FreeLookStorage;
import dev.bkclient.combatengine.api.events.EventLink;
import dev.bkclient.combatengine.api.events.implement.EventMoveInput;
import dev.bkclient.combatengine.bkengine.EventBKEngineTick;
import dev.bkclient.combatengine.bkengine.utils.annotation.IMinecraft;
import dev.bkclient.combatengine.bkengine.utils.aura.GCDUtil;
import dev.bkclient.combatengine.bkengine.utils.math.ServerUtil;
import dev.bkclient.combatengine.bkengine.utils.player.MoveUtil;
import net.minecraft.util.math.MathHelper;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
public final class RotationProcess implements IMinecraft {
    public static RotationTask currentTask = RotationTask.IDLE;
    public static float currentYawSpeed;
    public static float currentPitchSpeed;
    public static float currentYawReturnSpeed;
    public static float currentPitchReturnSpeed;
    public static int currentPriority;
    public static int currentTimeout;
    public static int idleTicks;
    public static Rotation targetRotation;

    public static boolean isRotating() {
        return !currentTask.equals(currentTask.IDLE);
    }

    private void resetRotation() {
        Rotation targetRotation = new Rotation(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch());


        if (ServerUtil.isHolyWorld()) {
            stopRotation();
        } else {
            if (updateRotation(targetRotation, currentYawReturnSpeed, currentPitchReturnSpeed)) {
                stopRotation();
            }
        }
    }

    public static void resetParentTimeout() {
        currentTimeout = 0;
        currentTask = RotationTask.IDLE;
        currentPriority = 0;

        FreeLookStorage.setActive(false);
    }


    @EventLink(priority = 10)
    public void onEventMovement(EventMoveInput eventMoveInput) {
        if (mc.player == null || mc.world == null) return;
        if (currentTask.equals(RotationTask.RESET)) {
            MoveUtil.fixMovement(eventMoveInput, mc.player.getYaw(), mc.gameRenderer.getCamera().getYaw());
        }

    }


    @EventLink(priority = 10)
    public void onEvent(EventBKEngineTick event) {

        

        if (currentTask.equals(RotationTask.AIM) && idleTicks > currentTimeout) {
            currentTask = (RotationTask.RESET);
        }


        if (currentTask.equals(RotationTask.RESET)) {
            if (ServerUtil.isHolyWorld() || ServerUtil.isCopyTime()) {
                stopRotation();
            } else {
                resetRotation();
            }
        }
        idleTicks++;
    }



    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed,
                              float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        if (currentPriority > priority) {
            return;
        }

        if (currentTask.equals(RotationTask.IDLE) && !clientRotation) {
            FreeLookStorage.setActive(true);
        }

        currentYawSpeed = yawSpeed;
        currentPitchSpeed = pitchSpeed;
        currentYawReturnSpeed = yawReturnSpeed;
        currentPitchReturnSpeed = pitchReturnSpeed;
        currentTimeout = timeout;
        currentPriority = priority;
        currentTask = RotationTask.AIM;
        targetRotation = target;

        updateRotation(target, yawSpeed, pitchSpeed);
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }


    static boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null)
            return false;

        Rotation currentRotation = new Rotation(mc.player);


        float pitchDelta = targetRotation.pitch - currentRotation.pitch;
        float yawDelta = wrapDegrees(targetRotation.yaw - currentRotation.yaw);

        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);


        // Delta is measured from player yaw; head yaw may be interpolated independently.
        float nextYaw = wrapDegrees(currentRotation.yaw
                + GCDUtil.getSensitivity(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw)));
        mc.player.setYaw(nextYaw);
        mc.player.headYaw = nextYaw;

        mc.player.setPitch(MathHelper.clamp(
                mc.player.getPitch()
                        + GCDUtil.getSensitivity(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)),
                -90F, 90F));


        idleTicks = 0;
        return new Rotation(mc.player).getDelta(targetRotation) < 1F;
    }

    public void stopRotation() {
        currentTask = (RotationTask.IDLE);
        currentPriority = (0);
        FreeLookStorage.setActive(false);

    }


    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }


}
