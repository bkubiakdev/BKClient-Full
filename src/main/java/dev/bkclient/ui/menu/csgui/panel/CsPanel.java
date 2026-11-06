/*
 * CsPanel — bazowy, przeciągalny panel CsGui (rysowany ZA głównym oknem).
 */
package dev.bkclient.ui.menu.csgui.panel;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.gui.GuiUtility;

public abstract class CsPanel {

    protected static final float CORNER_RADIUS = CsTheme.Window.RADIUS;
    protected static final float BORDER_THICKNESS = CsTheme.Window.BORDER;

    protected static final ColorRGBA BACKGROUND = CsTheme.Color.WINDOW_FILL;
    protected static final ColorRGBA BACKGROUND_GLASS = CsTheme.Color.WINDOW_GLASS;
    protected static final ColorRGBA BORDER = CsTheme.Color.WINDOW_BORDER;

    protected float x;
    protected float y;
    protected float width;
    protected float height;

    private boolean dragging;
    private float dragX;
    private float dragY;

    public CsPanel(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Rysowanie zawartości panelu (po tle). */
    protected void renderContent(UIContext context, float alpha) {
    }

    public void render(UIContext context, float alpha) {
        if (alpha <= 0.0f) {
            return;
        }
        BorderRadius radius = BorderRadius.all(CORNER_RADIUS);
        if (dev.bkclient.systems.modules.modules.visuals.Interface.blurGui())
            context.drawBlurredRect(this.x, this.y, this.width, this.height, 12.0f, 2.0f, radius,
                    BACKGROUND_GLASS.withAlpha(BACKGROUND_GLASS.getAlpha() * alpha));
        context.drawRoundedRect(this.x, this.y, this.width, this.height, radius,
                BACKGROUND.withAlpha(BACKGROUND.getAlpha() * alpha));
        context.drawRoundedBorder(this.x, this.y, this.width, this.height, BORDER_THICKNESS, radius,
                BORDER.withAlpha(BORDER.getAlpha() * alpha));
        this.renderContent(context, alpha);
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return GuiUtility.isHovered((double) this.x, (double) this.y, (double) this.width, (double) this.height,
                mouseX, mouseY);
    }

    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT && this.isHovered(mouseX, mouseY)) {
            this.dragging = true;
            this.dragX = (float) (mouseX - this.x);
            this.dragY = (float) (mouseY - this.y);
        }
    }

    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT) {
            this.dragging = false;
        }
    }

    public void onMouseDragged(double mouseX, double mouseY) {
        if (this.dragging) {
            this.x = (float) (mouseX - this.dragX);
            this.y = (float) (mouseY - this.dragY);
        }
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }
}
