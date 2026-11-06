package dev.bkclient.ui.clicksys;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.ui.clicksys.port.gui.widget.ActionRow;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.ActionSetting;
import org.joml.Matrix4f;

/** Native ClickSys action at the standard UI coordinate scale. */
public final class ClickSysActionButton {
    public static final float HEIGHT = 16, WIDTH = 34;
    private final ActionRow row;
    public ClickSysActionButton(String label) {
        row = new ActionRow(new ActionSetting(label, ""));
    }
    public void render(UIContext context, float x, float y, float width, float alpha, boolean enabled) {
        context.draw();
        row.onFloatFloat2(y * 2, x * 2);
        row.setValue237(width * 2);
        row.onFloatFloatFloatMatrix4f(alpha * (enabled ? 1 : .45f),
            enabled ? context.getMouseY() * 2 : -10000,
            enabled ? context.getMouseX() * 2 : -10000,
            new Matrix4f(context.getMatrices().peek().getPositionMatrix()).scale(.5f, .5f, 1));
        TextShader.update2();
        context.draw();
    }
}
