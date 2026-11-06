package dev.bkclient.combatengine.bridge;

import dev.bkclient.combatengine.api.events.implement.EventPacket;
import dev.bkclient.combatengine.api.storages.implement.FreeLookStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded, local diagnostics. A position packet may also be a legitimate teleport. */
public final class AuraMovementTrace {
    private record Sample(long time, String value) {}
    private static volatile Sample latest;
    private static volatile String sent="none";
    private static final AtomicLong nextLog=new AtomicLong();
    private static long nextAttackLog;
    public static void beforeBKEngineAttack() {
        var mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (mc.player == null || now < nextAttackLog) return;
        nextAttackLog = now + 2000;
        var p = mc.player;
        boolean sentSprint = ((dev.bkclient.mixin.accessors.BKEngineClientPlayerAccessor)p).bkengine$lastSentSprint();
        LoggerFactory.getLogger("AuraMovement").info(
            "[AuraMovement] bkengine-hit sync=v3 age={} sprint={} sent-sprint={} yaw={} velocity={} ground={}",
            p.age, p.isSprinting(), sentSprint, p.getYaw(), p.getVelocity(), p.isOnGround());
    }
    public static void capture() {
        var mc=MinecraftClient.getInstance();
        if(mc.player==null || !AuraRuntime.auraEnabled()) {latest=null;return;}
        var p=mc.player;
        var aura=dev.bkclient.combatengine.client.modules.impl.combat.Aura.INSTANCE;
        var bkengine=dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura.INSTANCE;
        var adaptive=dev.bkclient.combatengine.client.modules.impl.combat.AdaptiveAura.INSTANCE;
        String mode=bkengine.isEnable()?"Aura/"+bkengine.typeRotation.getCurrent()
            :adaptive.isEnable()?"AuraAdaptive/"+adaptive.rotation.getCurrent()
            :aura.isEnable()?"Aura/"+aura.rotationType.getCurrent()
            :"Aura2/V3/"+dev.bkclient.combatengine.client.modules.impl.combat.Aura2.INSTANCE.rotation.getCurrent();
        latest=new Sample(System.currentTimeMillis(),mode+" age="+p.age+" body="+p.getYaw()+" view="+FreeLookStorage.getFreeYaw()
            +" f="+p.input.movementForward+" s="+p.input.movementSideways+" keys="+p.input.playerInput
            +" sprint="+p.isSprinting()+" ground="+p.isOnGround()+" velocity="+p.getVelocity());
    }
    public static void observe(EventPacket event) {
        Sample sample=latest;
        if(sample==null || System.currentTimeMillis()-sample.time()>1000) return;
        if(event.getType()==EventPacket.Type.SEND && event.getPacket() instanceof PlayerMoveC2SPacket move) {
            sent="yaw="+move.getYaw(Float.NaN)+" pitch="+move.getPitch(Float.NaN)+" ground="+move.isOnGround()+" cancelled="+event.isCancelled();
        } else if(event.getType()==EventPacket.Type.RECEIVE && event.getPacket() instanceof PlayerPositionLookS2CPacket correction) {
            long now=System.currentTimeMillis(),previous=nextLog.get();
            if(now<previous || !nextLog.compareAndSet(previous,now+2000)) return;
            LoggerFactory.getLogger("AuraMovement").info("[AuraMovement] server-position id={} flags={} change={} input={} last-send={}",
                correction.teleportId(),correction.relatives(),correction.change(),sample.value(),sent);
        }
    }
}
