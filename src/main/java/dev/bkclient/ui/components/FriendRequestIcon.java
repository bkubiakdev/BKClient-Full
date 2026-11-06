package dev.bkclient.ui.components;

import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.utility.colors.ColorRGBA;

/** Small circular add badge over the sender's avatar, matching AirDrop's layout. */
public final class FriendRequestIcon {
    private FriendRequestIcon() {}
    public static void draw(CustomDrawContext c, float x, float y, float size, ColorRGBA color) {
        // Same outer circle and one-unit inset as AirDrop's 11/9 avatar badge.
        c.drawSquircle(x, y, size, size, 2, BorderRadius.all(size / 2),
            dev.bkclient.utility.colors.Colors.getBackgroundColor().mulAlpha(color.getAlpha() / 255.0F));
        float inset = size / 11;
        float innerSize = size - inset * 2;
        c.drawSquircle(x + inset, y + inset, innerSize, innerSize, 2,
            BorderRadius.all(innerSize / 2), color);
        float centerX = x + size / 2, centerY = y + size / 2;
        float iconSize = size * .78F;
        c.getMatrices().push();
        try {
            c.getMatrices().translate(centerX, centerY, 0);
            // Lucide's MSDF cross rotated by 45 degrees is a crisp add symbol.
            c.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(45));
            LucideIcons.CROSS.draw(c, -iconSize / 2, -iconSize / 2, iconSize,
                ColorRGBA.WHITE.withAlpha(color.getAlpha()));
        } finally {
            c.getMatrices().pop();
        }
    }
}
