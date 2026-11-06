package dev.bkclient.ui.menu.crosshair;

import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.visuals.Crosshair;
import dev.bkclient.ui.components.popup.Popup;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import net.minecraft.client.gui.screen.Screen;

import java.util.Arrays;

public final class CrosshairEditorScreen extends CustomScreen implements IScaledResolution, IMinecraft {
    private final Crosshair module;
    private final Screen parent;
    private final boolean[] grid;
    private final Popup popup;
    private final CrosshairGridComponent gridComponent;

    private boolean mirrorX = true;
    private boolean mirrorY = false;

    public CrosshairEditorScreen(Crosshair module, Screen parent) {
        this.module = module;
        this.parent = parent;
        this.grid = module.customGrid();

        float pw = 184.0f;
        float px = (IScaledResolution.sr.getScaledWidth() - pw) / 2.0f;
        float py = Math.max(10.0f, (IScaledResolution.sr.getScaledHeight() - 270.0f) / 2.0f);

        this.popup = new Popup(px, py, pw);
        this.popup.title("Crosshair Designer");
        this.popup.separator();

        this.gridComponent = new CrosshairGridComponent(this, this.module, this.grid);
        this.popup.add(this.gridComponent);

        this.popup.separator();
        this.popup.checkbox("Mirror X (Horizontal)", this.mirrorX, val -> this.mirrorX = val);
        this.popup.checkbox("Mirror Y (Vertical)", this.mirrorY, val -> this.mirrorY = val);

        this.popup.separator();
        this.popup.button("Clear Grid", "icons/trash.png", p -> {
            Arrays.fill(this.grid, false);
            this.module.setPixels(this.grid);
        });
        this.popup.button("Default Template", "icons/refresh.png", p -> {
            this.module.loadDefault();
            System.arraycopy(this.module.customGrid(), 0, this.grid, 0, this.grid.length);
        });

        this.popup.onClose(() -> {
            this.module.setPixels(this.grid);
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        });
    }

    @Override
    public void render(UIContext context) {
        float sw = this.width;
        float sh = this.height;
        context.drawRect(0, 0, sw, sh, new ColorRGBA(0, 0, 0, 110));
        this.popup.render(context);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        this.gridComponent.onMouseClicked(mouseX, mouseY, button);
        this.popup.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        this.gridComponent.onMouseReleased(mouseX, mouseY, button);
        this.popup.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
        this.gridComponent.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        this.module.setPixels(this.grid);
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public boolean isMirrorX() {
        return this.mirrorX;
    }

    public boolean isMirrorY() {
        return this.mirrorY;
    }
}
