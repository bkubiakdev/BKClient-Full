package dev.bkclient.systems.modules.modules.player;

import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

@ModuleInfo(name = "FreeCam", category = ModuleCategory.PLAYER, desc = "Lataj kamerą niezależnie od ciała gracza")
public class FreeCam extends BaseModule {
    private final SliderSetting speedX = new SliderSetting(this, "Speed X").min(0.05f).max(2.0f).currentValue(0.35f).step(0.05f);
    private final SliderSetting speedY = new SliderSetting(this, "Speed Y").min(0.05f).max(2.0f).currentValue(0.35f).step(0.05f);
    private final BooleanSetting cancelMove = new BooleanSetting(this, "Cancel Move").enabled(true);
    private final BooleanSetting display = new BooleanSetting(this, "Display coordinates");

    private int x;
    private int y;
    private int z;

    private final EventListener<HudRenderEvent> render2d = event -> {
        if (this.display.isEnabled() && mc.player != null) {
            BlockPos diff = mc.player.getBlockPos().subtract(new Vec3i(this.x, this.y, this.z));
            String pos = "X: " + diff.getX() + " Y: " + diff.getY() + " Z: " + diff.getZ();
            Font bold = Fonts.BOLD.getFont(8.0f);
            event.getContext().drawText(bold, Text.of(pos),
                    sr.getScaledWidth() / 2.0f - bold.width(pos) / 2.0f + 8.0f, sr.getScaledHeight() / 2.0f - 20.0f);
        }
    };

    @Override
    public void tick() {
        if (mc.player == null) {
            return;
        }
        FreecamUtil.tick(mc);
        super.tick();
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        this.x = mc.player.getBlockX();
        this.y = mc.player.getBlockY();
        this.z = mc.player.getBlockZ();
        FreecamUtil.enable(mc);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.player == null) {
            return;
        }
        FreecamUtil.disable(mc);
        super.onDisable();
    }

    public SliderSetting getSpeedX() {
        return speedX;
    }

    public SliderSetting getSpeedY() {
        return speedY;
    }

    public BooleanSetting getCancelMove() {
        return cancelMove;
    }

    public BooleanSetting getDisplay() {
        return display;
    }
}
