package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.systems.modules.modules.combat.KnockbackDisplacement;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.ui.hud.impl.island.IslandStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.math.KnockbackDirection;
import net.minecraft.util.math.RotationAxis;

/** Visualizes the configured direction; does not alter rotations or attacks. */
public final class DisplacementStatus extends IslandStatus implements IMinecraft {
    private float displayedAngle;
    private long lastFrame;

    public DisplacementStatus(SelectSetting setting) {
        super(setting, "displacement");
    }

    private KnockbackDisplacement module() {
        return BKClientInit.getInstance().getModuleManager().getModule(KnockbackDisplacement.class);
    }

    @Override
    public boolean canShow() {
        var module = module();
        boolean visible = mc.player != null && mc.world != null && module != null && module.isEnabled();
        if (!visible) lastFrame = 0;
        return visible;
    }

    @Override
    public void draw(CustomDrawContext context) {
        var module = module();
        if (module == null || mc.player == null) return;
        float target = module.getIndicatorAngle();
        long now = System.nanoTime();
        if (lastFrame == 0 || now - lastFrame > 500_000_000L) displayedAngle = target;
        else {
            float progress = (float)(1.0 - Math.exp(-(now - lastFrame) / 80_000_000.0));
            displayedAngle += KnockbackDirection.wrap(target - displayedAngle) * progress;
        }
        lastFrame = now;
        var font = Fonts.MEDIUM.getFont(7.0F);
        String label = "Displacement Angle";
        size.width = font.width(label) + 26.0F;
        size.height = 15.0F;
        var island = BKClientInit.getInstance().getHud().getIsland();
        float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
        float centerX = x + 10.0F;
        float centerY = 7.5F;
        float badgeHeight = HudTheme.isOld() ? 7.0F : 10.0F;
        dev.bkclient.ui.hud.components.HudValueRow.drawBadgeBackground(context,
                centerX - 7, centerY - badgeHeight / 2, 14, Colors.getAccent(), 1);
        context.getMatrices().push();
        try {
            context.getMatrices().translate(centerX, centerY, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(displayedAngle - 45.0F));
            LucideIcons.SELL.draw(context, -3.75F, -3.75F, 7.5F, ColorRGBA.WHITE);
        } finally {
            context.getMatrices().pop();
        }
        context.drawText(font, label, x + 21.0F, 5.0F, HudTheme.strongTextColor());
    }
}
