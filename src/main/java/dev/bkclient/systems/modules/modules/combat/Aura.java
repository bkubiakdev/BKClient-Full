package dev.bkclient.systems.modules.modules.combat;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.combatengine.bridge.SourceModuleAdapter;
import dev.bkclient.combatengine.bridge.AuraRuntime;
import dev.bkclient.combatengine.api.utils.math.TimerUtils;
import net.minecraft.entity.LivingEntity;
@ModuleInfo(name="Aura",category=ModuleCategory.COMBAT,desc="Automatically attacks nearby targets")
public final class Aura extends SourceModuleAdapter {
 private final dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura core;
 private final TimerUtils attackTimer = new TimerUtils();
 private dev.bkclient.systems.setting.settings.BooleanSetting webLagToggle;

 private final dev.bkclient.systems.setting.settings.RangeSetting webLagDelay =
     new dev.bkclient.systems.setting.settings.RangeSetting(this, "Web Lag Delay (ms)", () -> webLagToggle == null || !webLagToggle.isEnabled())
         .min(0).max(500).step(10).firstValue(100).secondValue(150);
 private final dev.bkclient.systems.event.EventListener<dev.bkclient.systems.event.impl.network.SendPacketEvent> webLagSend =
     event -> dev.bkclient.combatengine.bkengine.WebLagController.INSTANCE.send(event, webLagDelay.getFirstValue(), webLagDelay.getSecondValue());
 private final dev.bkclient.systems.event.EventListener<dev.bkclient.systems.event.impl.network.ReceivePacketEvent> webLagReceive = event -> {
     if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
         || event.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket)
         dev.bkclient.combatengine.bkengine.WebLagController.INSTANCE.correction();
 };
 private int attacks;
 public Aura() {
   super(dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura.INSTANCE);
   core=(dev.bkclient.combatengine.bkengine.module.impl.combat.AttackAura)source;
   webLagToggle = (dev.bkclient.systems.setting.settings.BooleanSetting) getSettings().stream()
       .filter(setting -> setting.getName().equals("Web Lag")).findFirst().orElseThrow();
   getSettings().remove(webLagDelay);
   getSettings().add(getSettings().indexOf(webLagToggle) + 1, webLagDelay);
   AuraRuntime.initialize();
 }
 public LivingEntity getTarget(){return core.getTarget();}
 public TimerUtils getAttackTimer(){attackTimer.setMillis(System.currentTimeMillis()-(long)dev.bkclient.combatengine.bkengine.utils.aura.UAttack.msCooldownReach());return attackTimer;}
 public boolean isClientLookEnabled(){return false;}
 public int getWhiteRiseTicksToAttack(){return isReadyToAttackNow()?0:1;}
 public float getAttackDistanceValue(){return core.attackRange.get();}
 public boolean isWallsEnabled(){return core.others.is("Through walls");}
 public boolean isNoHitInvEnabled(){return false;}
 public boolean isHitVectorModeEnabled(){return true;}
 public boolean isHolyWorldRotationSelected(){return false;}
 public boolean isSpookyTimeRotationSelected(){return false;}
 public boolean isFunTimeRotationSelected(){return false;}
 public boolean isReadyToAttackNow(){return mc.player!=null && dev.bkclient.combatengine.bkengine.utils.aura.UAttack.msCooldownReached();}
 public boolean isCooledDown(){return isReadyToAttackNow();}
 public boolean isRotationTargetValid(LivingEntity target){return target!=null && target==core.getTarget();}
 public boolean shouldPreventSprinting(){return core.preventSprint();}
 public int getAttacks(){return attacks;}
 public void noteAttack(){attacks++;}
 @Override public void onEnable() {
        AuraRuntime.selectAura(this);
        super.onEnable();
 }
 @Override public void onDisable(){super.onDisable();AuraRuntime.clearAura(this);BKClientInit.getInstance().getTargetManager().setCurrentTarget(null);}
}
