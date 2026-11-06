package dev.bkclient.mixin.minecraft.entity;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.Capes;
import dev.bkclient.utility.render.wavey.WaveyCapable;
import dev.bkclient.utility.render.wavey.WaveySimulation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class WaveyPlayerMixin implements WaveyCapable {

    private static final int PART_COUNT = 16;
    private static final float HEIGHT_MUL = 4.0f;

    @Unique private WaveySimulation bkclient_wavey;
    @Unique private boolean bkclient_dirty;

    @Override
    public WaveySimulation bkclient$getWaveySimulation() { return bkclient_wavey; }
    @Override
    public void bkclient$setWaveySimulation(WaveySimulation sim) { this.bkclient_wavey = sim; }
    @Override
    public boolean bkclient$isWaveyDirty() { return bkclient_dirty; }
    @Override
    public void bkclient$setWaveyDirty(boolean dirty) { this.bkclient_dirty = dirty; }

    @Inject(method = "tick", at = @At("TAIL"))
    private void bkclient_waveyTick(CallbackInfo ci) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity)(Object)this;
        // WaveyCapeFeatureRenderer reads only the local player's simulation; simulating every
        // other player each tick produced results nothing ever drew.
        if (self != MinecraftClient.getInstance().player) return;

        if (bkclient_wavey == null) {
            bkclient_wavey = new WaveySimulation();
        }
        boolean reinit = bkclient_wavey.init(PART_COUNT);
        if (reinit) bkclient_dirty = true;

        if (bkclient_wavey.isEmpty()) return;

        // gravity — from Capes module slider, fallback 2.0
        Capes capes = BKClientInit.getInstance().getModuleManager().getModule(Capes.class);
        float baseGravity = (capes != null) ? capes.waveyGravity.getCurrentValue() : 2.0f;
        boolean underwater = self.isSubmergedInWater();
        bkclient_wavey.gravity = underwater ? baseGravity / 10f : baseGravity;
        bkclient_wavey.gravX = 0f;
        bkclient_wavey.gravY = -1f;

        // movement deltas — same as WaveyCapes CapeHolder.simulate()
        double capeX = self.capeX - self.getX();
        double capeZ = self.capeZ - self.getZ();
        float bodyYaw = self.prevBodyYaw + (self.bodyYaw - self.prevBodyYaw);
        double sinYaw = MathHelper.sin(bodyYaw * 0.017453292F);
        double cosYaw = -MathHelper.cos(bodyYaw * 0.017453292F);

        double fallHack = MathHelper.clamp((self.prevY - self.getY()) * 10, 0, 1);
        double changeX = (capeX * sinYaw + capeZ * cosYaw) + fallHack
                + (self.isSneaking() && !bkclient_wavey.sneaking ? 3 : 0);
        double changeY = ((self.getY() - self.prevY) * HEIGHT_MUL)
                + (self.isSneaking() && !bkclient_wavey.sneaking ? 1 : 0);
        double changeZ = (capeX * cosYaw - capeZ * sinYaw);

        bkclient_wavey.sneaking = self.isSneaking();
        bkclient_wavey.applyMovement((float)changeX, (float)changeY, (float)changeZ);

        if (bkclient_dirty) {
            bkclient_dirty = false;
            bkclient_wavey.applyMovement(1f, 1f, 0f);
            for (int i = 0; i < 5; i++) bkclient_wavey.simulate();
        }
        bkclient_wavey.simulate();
    }
}
