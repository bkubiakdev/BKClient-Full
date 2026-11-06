package dev.bkclient.systems.modules.modules.visuals;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.*;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.*;
import dev.bkclient.utility.colors.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(name="Chams", category=ModuleCategory.VISUALS, desc="Entity silhouettes with glow, fill and animated outlines")
public final class ShaderESP extends BaseModule {
    private static ShaderESP instance;
    public ShaderESP() {
        instance = this;
        // Keep the same stored settings; only simplify their presentation.
        var primary = java.util.List.of(recommended, subtle, players, mobs, colorMode, tintColor,
                glowStrength, fillOpacity, outline, advanced);
        var ordered = new java.util.ArrayList<dev.bkclient.systems.setting.Setting>(primary);
        for (var setting : getSettings()) if (!ordered.contains(setting)) ordered.add(setting);
        getSettings().clear(); getSettings().addAll(ordered);
    }
    public final BooleanSetting advanced = new BooleanSetting(this, "Advanced").enabled(false);
    public final ButtonSetting recommended = new ButtonSetting(this, "Apply Recommended").action(() -> applyPreset(false));
    public final ButtonSetting subtle = new ButtonSetting(this, "Apply Subtle").action(() -> applyPreset(true));
    private void applyPreset(boolean soft) {
        boolean old = dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave;
        dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave = true;
        try {
            players.enabled(true); mobs.enabled(true); localPlayer.enabled(true); ignoreNaked.enabled(false);
            theme.select(); glow.enabled(true); fill.enabled(true); outline.enabled(true); outside.select();
            opacity.currentValue(1); saturation.currentValue(1.15F);
            glowRadius.currentValue(soft ? .35F : .65F); glowStrength.currentValue(soft ? .9F : 2F);
            glowFalloff.currentValue(1.1F); fillOpacity.currentValue(soft ? .18F : .55F);
            innerGlow.currentValue(soft ? .3F : .65F); outlineWidth.currentValue(soft ? 1F : 2F);
            outlineStrength.currentValue(soft ? 1F : 1.8F); outlineWhite.currentValue(.35F);
            pulse.enabled(false); shimmer.enabled(false);
        } finally { dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave = old; }
        dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
    }
    public static ShaderESP get() { return instance; }
    public static boolean active() { return instance != null && instance.isEnabled() && mc.world != null && mc.player != null; }
    public final BooleanSetting localPlayer = new BooleanSetting(this, "Local Player", () -> !advanced.isEnabled()).enabled(true);
    public final BooleanSetting players = new BooleanSetting(this, "Players").enabled(true);
    public final BooleanSetting ignoreNaked = new BooleanSetting(this, "Ignore Unarmored", () -> !advanced.isEnabled()).enabled(false);
    public final BooleanSetting mobs = new BooleanSetting(this, "Mobs").enabled(true);
    public final BooleanSetting items = new BooleanSetting(this, "Items", () -> !advanced.isEnabled()).enabled(false);
    public final SliderSetting saturation = new SliderSetting(this, "Saturation", () -> !advanced.isEnabled()).min(0F).max(2F).step(0.05F).currentValue(1.15F);
    public final SliderSetting opacity = new SliderSetting(this, "Opacity", () -> !advanced.isEnabled()).min(0.05F).max(1F).step(0.01F).currentValue(1F);
    public final BooleanSetting friendColor = new BooleanSetting(this, "Friend Color", () -> !advanced.isEnabled()).enabled(true);
    public final BooleanSetting glow = new BooleanSetting(this, "Glow", () -> !advanced.isEnabled()).enabled(true);
    public final SliderSetting glowRadius = new SliderSetting(this, "Glow Radius", () -> !advanced.isEnabled() || !glow.isEnabled()).min(0F).max(1F).step(0.01F).currentValue(0.65F);
    public final SliderSetting glowStrength = new SliderSetting(this, "Glow Strength").min(0F).max(4F).step(0.05F).currentValue(2F);
    public final SliderSetting glowFalloff = new SliderSetting(this, "Glow Falloff", () -> !advanced.isEnabled() || !glow.isEnabled()).min(0.2F).max(4F).step(0.05F).currentValue(1.1F);
    public final BooleanSetting fill = new BooleanSetting(this, "Fill", () -> !advanced.isEnabled()).enabled(true);
    public final SliderSetting fillOpacity = new SliderSetting(this, "Fill Opacity").min(0F).max(1.5F).step(0.01F).currentValue(0.55F);
    public final SliderSetting innerGlow = new SliderSetting(this, "Inner Glow", () -> !advanced.isEnabled() || !fill.isEnabled()).min(0F).max(2F).step(0.05F).currentValue(0.6F);
    public final BooleanSetting outline = new BooleanSetting(this, "Outline").enabled(true);
    public final SliderSetting outlineWidth = new SliderSetting(this, "Outline Width", () -> !advanced.isEnabled() || !outline.isEnabled()).min(1F).max(5F).step(1F).currentValue(2F);
    public final SliderSetting outlineStrength = new SliderSetting(this, "Outline Strength", () -> !advanced.isEnabled() || !outline.isEnabled()).min(0F).max(3F).step(0.05F).currentValue(1.8F);
    public final SliderSetting outlineWhite = new SliderSetting(this, "Outline Whiteness", () -> !advanced.isEnabled() || !outline.isEnabled()).min(0F).max(1F).step(0.01F).currentValue(0.45F);
    public final BooleanSetting pulse = new BooleanSetting(this, "Pulse", () -> !advanced.isEnabled()).enabled(false);
    public final SliderSetting pulseSpeed = new SliderSetting(this, "Pulse Speed", () -> !advanced.isEnabled() || !pulse.isEnabled()).min(0.1F).max(5F).step(0.1F).currentValue(1F);
    public final SliderSetting pulseAmount = new SliderSetting(this, "Pulse Amount", () -> !advanced.isEnabled() || !pulse.isEnabled()).min(0F).max(1F).step(0.05F).currentValue(0.3F);
    public final BooleanSetting shimmer = new BooleanSetting(this, "Shimmer", () -> !advanced.isEnabled()).enabled(false);
    public final SliderSetting shimmerWidth = new SliderSetting(this, "Shimmer Width", () -> !advanced.isEnabled() || !shimmer.isEnabled()).min(0.01F).max(0.15F).step(0.01F).currentValue(0.04F);
    public final SliderSetting shimmerPeriod = new SliderSetting(this, "Shimmer Period", () -> !advanced.isEnabled() || !shimmer.isEnabled()).min(1F).max(15F).step(0.5F).currentValue(5F);
    public final SliderSetting shimmerBrightness = new SliderSetting(this, "Shimmer Brightness", () -> !advanced.isEnabled() || !shimmer.isEnabled()).min(0F).max(2F).step(0.05F).currentValue(0.8F);
    public final ModeSetting colorMode = new ModeSetting(this, "Color Mode");
    public final ModeSetting.Value theme = new ModeSetting.Value(colorMode, "Theme").select();
    public final ModeSetting.Value custom = new ModeSetting.Value(colorMode, "Custom");
    public final ColorSetting tintColor = new ColorSetting(this, "Color", () -> !custom.isSelected()).color(new ColorRGBA(255,51,85,255));
    public final ColorSetting friendTint = new ColorSetting(this, "Friend Tint", () -> !advanced.isEnabled() || !friendColor.isEnabled()).color(new ColorRGBA(85,255,85,255));
    public final ModeSetting outlineMode = new ModeSetting(this, "Outline Mode", () -> !advanced.isEnabled() || !outline.isEnabled());
    public final ModeSetting.Value outside = new ModeSetting.Value(outlineMode, "Outside").select();
    public final ModeSetting.Value inside = new ModeSetting.Value(outlineMode, "Inside");
    public final ModeSetting.Value both = new ModeSetting.Value(outlineMode, "Both");
    public ColorRGBA effectColor() { return theme.isSelected() ? Colors.getAccent() : tintColor.getColor(); }
    public static boolean targets(Entity entity) {
        if (!active() || entity == null || entity.isRemoved() || !entity.isAlive()) return false;
        if (entity == mc.player) return instance.localPlayer.isEnabled() && !mc.options.getPerspective().isFirstPerson();
        if (entity instanceof PlayerEntity p) {
            // Both checks return false, so test the cheap setting before building the custom-name string.
            if (!instance.players.isEnabled()) return false;
            net.minecraft.text.Text customName = p.getCustomName();
            if (customName != null && customName.getString().startsWith("Ghost_")) return false;
            return !instance.ignoreNaked.isEnabled() || p.getArmor() > 0;
        }
        if (entity instanceof LivingEntity) return instance.mobs.isEnabled();
        return entity instanceof ItemEntity && instance.items.isEnabled();
    }
    public static int maskColor(Entity e) {
        return e instanceof PlayerEntity && BKClientInit.getInstance().getFriendManager().isFriend(e.getName().getString())
            ? 0x55FF55 : 0xFFFFFF;
    }
    @Override public void onEnable() {
        // Repair the all-zero defaults saved by the first port, without overriding custom presets.
        if (glowStrength.getCurrentValue() == 0 && fillOpacity.getCurrentValue() == 0
                && outlineStrength.getCurrentValue() == 0 && saturation.getCurrentValue() == 0) {
            applyPreset(false);
        }
    }
    @Override public void onDisable() { dev.bkclient.utility.render.BKEngineShaderEspRenderer.release(); }
    public void applySettings(dev.bkclient.utility.render.BKEngineShaderEspRenderer.Params p) {
        p.color = effectColor().getRGB();
        p.friendEnabled = friendColor.isEnabled();
        p.friendColor = friendTint.getColor().getRGB();
        p.opacity = opacity.getCurrentValue();
        p.saturation = saturation.getCurrentValue();

        p.glowEnabled = glow.isEnabled();
        p.glowRadius = glowRadius.getCurrentValue();
        p.glowStrength = glowStrength.getCurrentValue();
        p.glowFalloff = glowFalloff.getCurrentValue();

        p.fillEnabled = fill.isEnabled();
        p.fillOpacity = fillOpacity.getCurrentValue();
        p.innerGlow = innerGlow.getCurrentValue();

        p.outlineEnabled = outline.isEnabled();
        p.outlineMode = (inside.isSelected() ? 1 : both.isSelected() ? 2 : 0);
        p.outlineWidth = (int) outlineWidth.getCurrentValue();
        p.outlineStrength = outlineStrength.getCurrentValue();
        p.outlineWhite = outlineWhite.getCurrentValue();

        p.pulseEnabled = pulse.isEnabled();
        p.pulseSpeed = pulseSpeed.getCurrentValue();
        p.pulseAmount = pulseAmount.getCurrentValue();

        p.shimmerEnabled = shimmer.isEnabled();
        p.shimmerWidth = shimmerWidth.getCurrentValue();
        p.shimmerPeriodSec = shimmerPeriod.getCurrentValue();
        p.shimmerBrightness = shimmerBrightness.getCurrentValue();
    }

}
