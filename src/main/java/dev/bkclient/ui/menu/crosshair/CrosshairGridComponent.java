package dev.bkclient.ui.menu.crosshair;

import dev.bkclient.framework.base.CustomComponent;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.visuals.Crosshair;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;

public class CrosshairGridComponent extends CustomComponent {
    private static final int GRID = 15;
    private static final float CELL = 8.5f;
    private static final float PITCH = 9.5f;

    private final CrosshairEditorScreen editor;
    private final Crosshair module;
    private final boolean[] grid;
    private int paintButton = -1;

    public CrosshairGridComponent(CrosshairEditorScreen editor, Crosshair module, boolean[] grid) {
        this.editor = editor;
        this.module = module;
        this.grid = grid;
        this.width = 176.0f;
        this.height = 150.0f;
    }

    @Override
    protected void renderComponent(UIContext context) {
        float mouseX = context.getMouseX();
        float mouseY = context.getMouseY();

        float startX = this.x + (this.width - GRID * PITCH) / 2.0f;
        float startY = this.y + 4.0f;

        for (int r = 0; r < GRID; r++) {
            for (int c = 0; c < GRID; c++) {
                float cellX = startX + c * PITCH;
                float cellY = startY + r * PITCH;

                boolean active = this.grid[r * GRID + c];
                boolean hovered = mouseX >= cellX && mouseX <= cellX + CELL && mouseY >= cellY && mouseY <= cellY + CELL;
                boolean isCenterGuide = (r == 7 || c == 7);

                ColorRGBA cellColor;
                if (active) {
                    cellColor = Colors.getAccentColor();
                } else if (hovered) {
                    cellColor = new ColorRGBA(255, 255, 255, 60);
                } else if (isCenterGuide) {
                    cellColor = new ColorRGBA(45, 45, 52, 160);
                } else {
                    cellColor = new ColorRGBA(28, 28, 34, 160);
                }

                context.drawRoundedRect(cellX, cellY, CELL, CELL, BorderRadius.all(2.0f), cellColor);
            }
        }
    }

    private void handlePaint(double mouseX, double mouseY, boolean val) {
        float startX = this.x + (this.width - GRID * PITCH) / 2.0f;
        float startY = this.y + 4.0f;

        int c = (int) ((mouseX - startX) / PITCH);
        int r = (int) ((mouseY - startY) / PITCH);

        if (c >= 0 && c < GRID && r >= 0 && r < GRID) {
            float cellRelX = (float) (mouseX - startX - c * PITCH);
            float cellRelY = (float) (mouseY - startY - r * PITCH);
            if (cellRelX <= CELL && cellRelY <= CELL) {
                setPixel(r, c, val);
                if (this.editor.isMirrorX()) setPixel(r, GRID - 1 - c, val);
                if (this.editor.isMirrorY()) setPixel(GRID - 1 - r, c, val);
                if (this.editor.isMirrorX() && this.editor.isMirrorY()) setPixel(GRID - 1 - r, GRID - 1 - c, val);
                this.module.setPixels(this.grid);
            }
        }
    }

    private void setPixel(int r, int c, boolean val) {
        if (r >= 0 && r < GRID && c >= 0 && c < GRID) {
            this.grid[r * GRID + c] = val;
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (this.isHovered(mouseX, mouseY)) {
            if (button == MouseButton.LEFT) {
                this.paintButton = 0;
                handlePaint(mouseX, mouseY, true);
            } else if (button == MouseButton.RIGHT) {
                this.paintButton = 1;
                handlePaint(mouseX, mouseY, false);
            }
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        this.paintButton = -1;
    }

    public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
        if (this.paintButton == 0) {
            handlePaint(mouseX, mouseY, true);
        } else if (this.paintButton == 1) {
            handlePaint(mouseX, mouseY, false);
        }
    }

    @Override
    public float getHeight() {
        return this.height = 152.0f;
    }
}
