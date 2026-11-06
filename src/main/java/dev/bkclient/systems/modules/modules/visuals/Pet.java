package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Pet", category = ModuleCategory.VISUALS, desc = "Renders a cosmetic pet that follows you")
public class Pet extends BaseModule {


    public final ModeSetting animal = new ModeSetting(this, "Animal");
    public final ModeSetting.Value aCat     = new ModeSetting.Value(animal, "Cat");
    public final ModeSetting.Value aWolf    = new ModeSetting.Value(animal, "Dog");
    public final ModeSetting.Value aFox     = new ModeSetting.Value(animal, "Fox");
    public final ModeSetting.Value aParrot  = new ModeSetting.Value(animal, "Parrot");
    public final ModeSetting.Value aRabbit  = new ModeSetting.Value(animal, "Rabbit");
    public final ModeSetting.Value aFrog    = new ModeSetting.Value(animal, "Frog");
    public final ModeSetting.Value aAxolotl = new ModeSetting.Value(animal, "Axolotl");
    public final ModeSetting.Value aBee     = new ModeSetting.Value(animal, "Bee");
    public final ModeSetting.Value aPanda   = new ModeSetting.Value(animal, "Panda");
    public final ModeSetting.Value aBat     = new ModeSetting.Value(animal, "Bat");
    public final ModeSetting.Value aChicken = new ModeSetting.Value(animal, "Chicken");
    public final ModeSetting.Value aTurtle  = new ModeSetting.Value(animal, "Turtle");


    public final ModeSetting catVariant = new ModeSetting(this, "Cat Breed", "", () -> !animal.is(aCat));
    public final ModeSetting.Value vCalico   = new ModeSetting.Value(catVariant, "Calico");
    public final ModeSetting.Value vTabby    = new ModeSetting.Value(catVariant, "Tabby");
    public final ModeSetting.Value vBlack    = new ModeSetting.Value(catVariant, "Black");
    public final ModeSetting.Value vRed      = new ModeSetting.Value(catVariant, "Red");
    public final ModeSetting.Value vSiamese  = new ModeSetting.Value(catVariant, "Siamese");
    public final ModeSetting.Value vBritish  = new ModeSetting.Value(catVariant, "British Shorthair");
    public final ModeSetting.Value vJellie   = new ModeSetting.Value(catVariant, "Jellie");
    public final ModeSetting.Value vPersian  = new ModeSetting.Value(catVariant, "Persian");
    public final ModeSetting.Value vRagdoll  = new ModeSetting.Value(catVariant, "Ragdoll");
    public final ModeSetting.Value vWhite    = new ModeSetting.Value(catVariant, "White");
    public final ModeSetting.Value vAllBlack = new ModeSetting.Value(catVariant, "All Black");


    public final ModeSetting foxVariant = new ModeSetting(this, "Fox Type", "", () -> !animal.is(aFox));
    public final ModeSetting.Value vRedFox  = new ModeSetting.Value(foxVariant, "Red");
    public final ModeSetting.Value vSnowFox = new ModeSetting.Value(foxVariant, "Snow");


    public final ModeSetting parrotVariant = new ModeSetting(this, "Parrot Color", "", () -> !animal.is(aParrot));
    public final ModeSetting.Value vRedP    = new ModeSetting.Value(parrotVariant, "Red");
    public final ModeSetting.Value vBlueP   = new ModeSetting.Value(parrotVariant, "Blue");
    public final ModeSetting.Value vGreenP  = new ModeSetting.Value(parrotVariant, "Green");
    public final ModeSetting.Value vCyanP   = new ModeSetting.Value(parrotVariant, "Cyan");
    public final ModeSetting.Value vGrayP   = new ModeSetting.Value(parrotVariant, "Gray");


    public final ModeSetting axolotlVariant = new ModeSetting(this, "Axolotl Color", "", () -> !animal.is(aAxolotl));
    public final ModeSetting.Value vAxLucy    = new ModeSetting.Value(axolotlVariant, "Lucy (Pink)");
    public final ModeSetting.Value vAxWild    = new ModeSetting.Value(axolotlVariant, "Wild (Brown)");
    public final ModeSetting.Value vAxGold    = new ModeSetting.Value(axolotlVariant, "Gold");
    public final ModeSetting.Value vAxCyan    = new ModeSetting.Value(axolotlVariant, "Cyan");
    public final ModeSetting.Value vAxBlue    = new ModeSetting.Value(axolotlVariant, "Blue");


    public final ModeSetting pandaGene = new ModeSetting(this, "Panda Gene", "", () -> !animal.is(aPanda));
    public final ModeSetting.Value vPaNormal    = new ModeSetting.Value(pandaGene, "Normal");
    public final ModeSetting.Value vPaLazy      = new ModeSetting.Value(pandaGene, "Lazy");
    public final ModeSetting.Value vPaWorried   = new ModeSetting.Value(pandaGene, "Worried");
    public final ModeSetting.Value vPaPlayful   = new ModeSetting.Value(pandaGene, "Playful");
    public final ModeSetting.Value vPaBrown     = new ModeSetting.Value(pandaGene, "Brown");
    public final ModeSetting.Value vPaWeak      = new ModeSetting.Value(pandaGene, "Weak");
    public final ModeSetting.Value vPaAggressive= new ModeSetting.Value(pandaGene, "Aggressive");


    public final SliderSetting offsetX = new SliderSetting(this, "Offset X")
            .min(-3f).max(3f).step(0.1f).currentValue(1.2f);
    public final SliderSetting offsetZ = new SliderSetting(this, "Offset Z")
            .min(-3f).max(3f).step(0.1f).currentValue(0.5f);
    public final BooleanSetting sitting = new BooleanSetting(this, "Sitting");


    private LivingEntity fakeEntity;

    // Parrot flight state
    private double parrotAngle = 0;
    private double parrotRadius = 1.8;
    private double parrotHeightOffset = 0;
    private double parrotHeightPhase = 0;


    @Override
    public void onEnable() {
        parrotAngle = 0;
        spawnPet();
    }

    @Override
    public void onDisable() {
        fakeEntity = null;
    }


    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (fakeEntity == null || fakeEntity.getWorld() != mc.world || !isCorrectType()) {
            spawnPet();
        }
        if (fakeEntity == null) return;

        fakeEntity.prevX = fakeEntity.getX();
        fakeEntity.prevY = fakeEntity.getY();
        fakeEntity.prevZ = fakeEntity.getZ();
        fakeEntity.prevYaw = fakeEntity.getYaw();
        fakeEntity.prevPitch = fakeEntity.getPitch();
        fakeEntity.prevHeadYaw = fakeEntity.getHeadYaw();

        if (animal.is(aParrot)) {
            tickParrot(mc);
        } else if (animal.is(aBee) || animal.is(aBat)) {
            tickFloating(mc);
        } else {
            tickGrounded(mc);
        }

        applyVariant();
    };

    // Ground-based pets: cat, wolf, fox, rabbit, frog, axolotl, panda, chicken, turtle
    private void tickGrounded(MinecraftClient mc) {
        double ox = offsetX.getCurrentValue();
        double oz = offsetZ.getCurrentValue();
        float yaw = mc.player.getYaw();
        double rad = Math.toRadians(yaw);
        double rx = ox * Math.cos(rad) + oz * Math.sin(rad);
        double rz = -ox * Math.sin(rad) + oz * Math.cos(rad);

        Vec3d target = mc.player.getPos().add(rx, 0, rz);
        Vec3d cur = fakeEntity.getPos();

        double nx = MathHelper.lerp(0.18, cur.x, target.x);
        double ny = mc.player.getY();
        double nz = MathHelper.lerp(0.18, cur.z, target.z);
        fakeEntity.setPos(nx, ny, nz);

        // Walking animation — based on actual movement this tick
        double dx = nx - cur.x;
        double dz = nz - cur.z;
        float speed = (float) Math.sqrt(dx * dx + dz * dz) * 6f;
        speed = Math.min(speed, 1f);
        fakeEntity.limbAnimator.updateLimbs(speed, 0.4f, 1.0f);

        // Always compute direction toward player (for head)
        double tdx = mc.player.getX() - fakeEntity.getX();
        double tdz = mc.player.getZ() - fakeEntity.getZ();
        float towardPlayer = (float) Math.toDegrees(Math.atan2(-tdx, tdz));

        float targetBodyYaw;
        float bodyLerpSpeed;
        if (speed > 0.003f) {
            // Moving: body faces movement direction
            targetBodyYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            bodyLerpSpeed = 0.20f;
        } else {
            // Idle: body slowly turns toward player
            targetBodyYaw = towardPlayer;
            bodyLerpSpeed = 0.08f;
        }

        float newBodyYaw = MathHelper.lerpAngleDegrees(bodyLerpSpeed, fakeEntity.getBodyYaw(), targetBodyYaw);
        // Head always looks at player, faster than body
        float newHeadYaw = MathHelper.lerpAngleDegrees(0.35f, fakeEntity.getHeadYaw(), towardPlayer);

        fakeEntity.prevBodyYaw = fakeEntity.getBodyYaw();
        fakeEntity.setBodyYaw(newBodyYaw);
        fakeEntity.setYaw(newBodyYaw);
        fakeEntity.setHeadYaw(newHeadYaw);

        if (fakeEntity instanceof TameableEntity tameable) {
            boolean s = sitting.isEnabled();
            tameable.setSitting(s);
            tameable.setInSittingPose(s);
            fakeEntity.setPose(s ? net.minecraft.entity.EntityPose.SITTING : net.minecraft.entity.EntityPose.STANDING);
        }
    }

    // Parrot orbits around player in a smooth circle + bobbing
    private void tickParrot(MinecraftClient mc) {
        parrotAngle += 0.05;
        parrotHeightPhase += 0.08;

        parrotRadius = 1.6 + Math.sin(parrotAngle * 0.3) * 0.3;
        parrotHeightOffset = 0.8 + Math.sin(parrotHeightPhase) * 0.25;

        double px = mc.player.getX() + Math.cos(parrotAngle) * parrotRadius;
        double py = mc.player.getY() + mc.player.getStandingEyeHeight() + parrotHeightOffset - 0.4;
        double pz = mc.player.getZ() + Math.sin(parrotAngle) * parrotRadius;

        Vec3d cur = fakeEntity.getPos();
        fakeEntity.setPos(
                MathHelper.lerp(0.3, cur.x, px),
                MathHelper.lerp(0.3, cur.y, py),
                MathHelper.lerp(0.3, cur.z, pz)
        );

        // Face the direction of travel (tangent to orbit circle)
        // orbit direction: dx/dt = -sin(angle), dz/dt = cos(angle)
        // yaw = atan2(-dx, dz) where dx=-sin, dz=cos â†’ atan2(sin, cos) = angle
        float orbitYaw = (float) Math.toDegrees(parrotAngle);
        float newBodyYaw = MathHelper.lerpAngleDegrees(0.25f, fakeEntity.getBodyYaw(), orbitYaw);
        fakeEntity.prevBodyYaw = fakeEntity.getBodyYaw();
        fakeEntity.setBodyYaw(newBodyYaw);
        fakeEntity.setYaw(newBodyYaw);
        fakeEntity.setHeadYaw(newBodyYaw); // head same as body — no spinning head

        // Wing flap
        fakeEntity.limbAnimator.updateLimbs(1f, 0.5f, 1.0f);
        if (fakeEntity instanceof ParrotEntity parrot) {
            parrot.prevFlapProgress = parrot.flapProgress;
            parrot.prevMaxWingDeviation = parrot.maxWingDeviation;
            parrot.flapProgress += 1f;
            parrot.maxWingDeviation = 1.0f;
        }
    }

    // Floating pets: bee, bat — orbit + hover + proper animation state
    private void tickFloating(MinecraftClient mc) {
        parrotAngle += 0.035;
        parrotHeightPhase += 0.07;

        double r = 1.5;
        double px = mc.player.getX() + Math.cos(parrotAngle) * r;
        double py = mc.player.getY() + 1.3 + Math.sin(parrotHeightPhase) * 0.2;
        double pz = mc.player.getZ() + Math.sin(parrotAngle) * r;

        Vec3d cur = fakeEntity.getPos();
        fakeEntity.setPos(
                MathHelper.lerp(0.25, cur.x, px),
                MathHelper.lerp(0.25, cur.y, py),
                MathHelper.lerp(0.25, cur.z, pz)
        );

        float orbitYaw = (float) Math.toDegrees(parrotAngle);
        float newBodyYaw = MathHelper.lerpAngleDegrees(0.25f, fakeEntity.getBodyYaw(), orbitYaw);
        fakeEntity.prevBodyYaw = fakeEntity.getBodyYaw();
        fakeEntity.setBodyYaw(newBodyYaw);
        fakeEntity.setYaw(newBodyYaw);
        fakeEntity.setHeadYaw(newBodyYaw);

        fakeEntity.limbAnimator.updateLimbs(1f, 0.5f, 1.0f);

        // Bat: keep flyingAnimationState running, roosting off
        if (fakeEntity instanceof BatEntity bat) {
            bat.setRoosting(false);
            bat.roostingAnimationState.stop();
            bat.flyingAnimationState.startIfNotRunning(bat.age);
            bat.age++;
        }
    }


    private final EventListener<Render3DEvent> onRender = event -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || fakeEntity == null) return;

        float delta = event.getTickDelta();
        Vec3d cam = event.getCamera().getPos();

        // Interpolated render position
        double rx = MathHelper.lerp(delta, fakeEntity.prevX, fakeEntity.getX()) - cam.x;
        double ry = MathHelper.lerp(delta, fakeEntity.prevY, fakeEntity.getY()) - cam.y;
        double rz = MathHelper.lerp(delta, fakeEntity.prevZ, fakeEntity.getZ()) - cam.z;

        MatrixStack matrices = event.getMatrices();
        matrices.push();
        matrices.translate(rx, ry, rz);

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        mc.getEntityRenderDispatcher().render(
                fakeEntity,
                0, 0, 0,
                delta,
                matrices,
                immediate,
                mc.getEntityRenderDispatcher().getLight(fakeEntity, delta)
        );

        immediate.draw();
        matrices.pop();
    };


    private void spawnPet() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        EntityType<? extends LivingEntity> type = getSelectedType();
        fakeEntity = type.create(mc.world, SpawnReason.NATURAL);
        if (fakeEntity == null) return;

        Vec3d spawnPos = mc.player != null
                ? mc.player.getPos().add(offsetX.getCurrentValue(), 0, offsetZ.getCurrentValue())
                : Vec3d.ZERO;
        fakeEntity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        fakeEntity.prevX = spawnPos.x;
        fakeEntity.prevY = spawnPos.y;
        fakeEntity.prevZ = spawnPos.z;

        if (fakeEntity instanceof TameableEntity tameable) {
            boolean s = sitting.isEnabled();
            tameable.setTamed(true, false);
            tameable.setSitting(s);
            tameable.setInSittingPose(s);
            fakeEntity.setPose(s ? net.minecraft.entity.EntityPose.SITTING : net.minecraft.entity.EntityPose.STANDING);
        }

        if (fakeEntity instanceof BatEntity bat) {
            bat.setRoosting(false);
            bat.flyingAnimationState.start(0);
        }

        applyVariant();
    }

    private boolean isCorrectType() {
        return fakeEntity != null && fakeEntity.getType() == getSelectedType();
    }

    @SuppressWarnings("unchecked")
    private EntityType<? extends LivingEntity> getSelectedType() {
        if (animal.is(aWolf))    return EntityType.WOLF;
        if (animal.is(aFox))     return EntityType.FOX;
        if (animal.is(aParrot))  return EntityType.PARROT;
        if (animal.is(aRabbit))  return EntityType.RABBIT;
        if (animal.is(aFrog))    return EntityType.FROG;
        if (animal.is(aAxolotl)) return EntityType.AXOLOTL;
        if (animal.is(aBee))     return EntityType.BEE;
        if (animal.is(aPanda))   return EntityType.PANDA;
        if (animal.is(aBat))     return EntityType.BAT;
        if (animal.is(aChicken)) return EntityType.CHICKEN;
        if (animal.is(aTurtle))  return EntityType.TURTLE;
        return EntityType.CAT;
    }

    private void applyVariant() {
        if (fakeEntity == null) return;

        if (fakeEntity instanceof CatEntity cat) {
            String[] ids = {"calico","tabby","black","red","siamese","british_shorthair","jellie","persian","ragdoll","white","all_black"};
            int idx = getModeIndex(catVariant);
            Registries.CAT_VARIANT.getEntry(Identifier.of("minecraft:" + ids[Math.min(idx, ids.length - 1)]))
                    .ifPresent(cat::setVariant);

        } else if (fakeEntity instanceof FoxEntity fox) {
            fox.setVariant(foxVariant.is(vSnowFox) ? FoxEntity.Type.SNOW : FoxEntity.Type.RED);

        } else if (fakeEntity instanceof ParrotEntity parrot) {
            ParrotEntity.Variant v = ParrotEntity.Variant.RED_BLUE;
            if (parrotVariant.is(vBlueP))  v = ParrotEntity.Variant.BLUE;
            else if (parrotVariant.is(vGreenP)) v = ParrotEntity.Variant.GREEN;
            else if (parrotVariant.is(vCyanP))  v = ParrotEntity.Variant.YELLOW_BLUE;
            else if (parrotVariant.is(vGrayP))  v = ParrotEntity.Variant.GRAY;
            parrot.setVariant(v);

        } else if (fakeEntity instanceof AxolotlEntity axolotl) {
            AxolotlEntity.Variant v = AxolotlEntity.Variant.LUCY;
            if (axolotlVariant.is(vAxWild)) v = AxolotlEntity.Variant.WILD;
            else if (axolotlVariant.is(vAxGold)) v = AxolotlEntity.Variant.GOLD;
            else if (axolotlVariant.is(vAxCyan)) v = AxolotlEntity.Variant.CYAN;
            else if (axolotlVariant.is(vAxBlue)) v = AxolotlEntity.Variant.BLUE;
            axolotl.setVariant(v);

        } else if (fakeEntity instanceof RabbitEntity rabbit) {
            rabbit.setVariant(RabbitEntity.RabbitType.WHITE);

        } else if (fakeEntity instanceof PandaEntity panda) {
            PandaEntity.Gene g = PandaEntity.Gene.NORMAL;
            if (pandaGene.is(vPaLazy))       g = PandaEntity.Gene.LAZY;
            else if (pandaGene.is(vPaWorried))    g = PandaEntity.Gene.WORRIED;
            else if (pandaGene.is(vPaPlayful))    g = PandaEntity.Gene.PLAYFUL;
            else if (pandaGene.is(vPaBrown))      g = PandaEntity.Gene.BROWN;
            else if (pandaGene.is(vPaWeak))       g = PandaEntity.Gene.WEAK;
            else if (pandaGene.is(vPaAggressive)) g = PandaEntity.Gene.AGGRESSIVE;
            panda.setMainGene(g);
            panda.setHiddenGene(g);
        }
    }

    private int getModeIndex(ModeSetting setting) {
        for (int i = 0; i < setting.getValues().size(); i++) {
            if (setting.is(setting.getValues().get(i))) return i;
        }
        return 0;
    }
}
