package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.player.ClientPlayerTickEvent;
import dev.bkclient.systems.event.impl.render.Render3DEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ColorSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.render.DashTrailRenderer;
import net.minecraft.entity.player.PlayerEntity;


@ModuleInfo(name = "Trails", category = ModuleCategory.VISUALS, desc = "Renders movement trails behind players")
public class Trails extends BaseModule {

    public Trails() {
        // Keep the Dash source selector directly before its dependent color picker.
        this.getSettings().remove(this.dashColorMode);
        this.getSettings().add(this.getSettings().indexOf(this.color1), this.dashColorMode);
    }


    public final ModeSetting style = new ModeSetting(this, "modules.settings.trails.style");
    public final ModeSetting.Value sDash = new ModeSetting.Value(style, "Dash").select();
    public final ColorSetting color1 = new ColorSetting(this, "modules.settings.trails.color1",
            () -> !this.dashColorMode.is(this.dcCustom)).color(new ColorRGBA(233, 30, 99));
    public final BooleanSetting showFriends = new BooleanSetting(this, "modules.settings.trails.show_friends");

    public final ModeSetting dashColorMode = new ModeSetting((SettingsContainer) this,
            "modules.settings.trails.dash_color_mode", () -> !style.is(sDash));
    public final ModeSetting.Value dcPalette = new ModeSetting.Value(dashColorMode, "Random Palette");
    public final ModeSetting.Value dcCustom  = new ModeSetting.Value(dashColorMode, "Custom");
    public final ModeSetting.Value dcClient  = new ModeSetting.Value(dashColorMode, "Client");
    public final ModeSetting.Value dcRainbow = new ModeSetting.Value(dashColorMode, "Rainbow");

    public final BooleanSetting dashDots = new BooleanSetting(this, "modules.settings.trails.dash_dots",
            () -> !style.is(sDash)).enable();
    public final BooleanSetting dashSegments = new BooleanSetting(this, "modules.settings.trails.dash_segments",
            () -> !style.is(sDash)).enable();
    public final BooleanSetting dashMotionSmoothing = new BooleanSetting(this, "modules.settings.trails.dash_smoothing",
            () -> !style.is(sDash)).enable();
    public final SliderSetting dashLength = new SliderSetting((SettingsContainer) this,
            "modules.settings.trails.dash_length", () -> !style.is(sDash))
            .min(0.5f).max(6f).currentValue(3f).step(0.05f);
    public final SliderSetting dashMaxDistance = new SliderSetting((SettingsContainer) this,
            "modules.settings.trails.dash_max_distance", () -> !style.is(sDash))
            .min(15f).max(100f).currentValue(25f).step(1f);

    private final DashTrailRenderer dashRenderer = new DashTrailRenderer();

    private net.minecraft.client.world.ClientWorld trailWorld;
    private final EventListener<dev.bkclient.systems.event.impl.game.WorldChangeEvent> onWorldChange = event -> { dashRenderer.clear(); trailWorld = null; };
    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (trailWorld != mc.world) { dashRenderer.clear(); trailWorld = mc.world; }
        if (mc.world == null || mc.player == null) { dashRenderer.clear(); return; }
        updateDash();
    };

    private final EventListener<Render3DEvent> on3DRender = event -> {
        if (mc.world != null && mc.player != null && trailWorld == mc.world)
            dashRenderer.render(event.getMatrices(), event.getTickDelta());
    };

    @Override public void onDisable() { dashRenderer.clear(); trailWorld = null; }

    private void updateDash() {
        dashRenderer.configure(
                dashMotionSmoothing.isEnabled(),
                dashSegments.isEnabled(),
                dashDots.isEnabled(),
                dashLength.getCurrentValue()
        );

        java.util.Set<Integer> activePlayers = new java.util.HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldDrawTrailFor(player) || player.squaredDistanceTo(mc.player) > Math.pow(dashMaxDistance.getCurrentValue(), 2)) continue;
            activePlayers.add(player.getId());
            ColorRGBA col = getDashColor();
            dashRenderer.trackPlayer(player, col);
        }
        dashRenderer.retainOnly(activePlayers);
        dashRenderer.tick();
    }

    private ColorRGBA getDashColor() {
        if (dashColorMode.is(dcCustom)) {
            return color1.getColor();
        }
        if (dashColorMode.is(dcRainbow)) {
            float hue = ((System.currentTimeMillis() % 4000L) / 4000.0f) % 1.0f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f);
            return new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        }
        return Colors.getAccentColor();
    }

    private boolean shouldDrawTrailFor(PlayerEntity player) {
        if (player == mc.player) return true;
        return showFriends.isEnabled() && BKClientInit.getInstance().getFriendManager().isFriend(player.getName().getString());
    }

}
