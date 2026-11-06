package dev.bkclient.ui.clicksys;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.ui.clicksys.port.data.ChoiceOption;
import dev.bkclient.ui.clicksys.port.data.Tween;
import dev.bkclient.ui.clicksys.port.gui.widget.ChoiceRow;
import dev.bkclient.ui.clicksys.port.gui.widget.IconButton;
import dev.bkclient.ui.clicksys.port.gui.widget.SmallButton;
import dev.bkclient.ui.clicksys.port.gui.widget.Widget;
import dev.bkclient.ui.clicksys.port.module.CategoryType;
import dev.bkclient.ui.clicksys.port.render.SvgShader;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.ChoiceSetting;
import dev.bkclient.ui.clicksys.port.util.EasingPresets;
import dev.bkclient.ui.components.textfield.TextField;
import java.util.Map;
import java.util.WeakHashMap;
import org.joml.Matrix4f;

/** Reuses ClickSys widgets while the market keeps its existing registry bindings. */
public final class AutoMarketControls {
    private final Map<Object, SmallButton> checks = new WeakHashMap<>();
    private final Map<Object, IconButton[]> actions = new WeakHashMap<>();
    private final Map<TextField, Tween> focus = new WeakHashMap<>();
    private final ChoiceSetting priceMode = new ChoiceSetting("Price Mode", "",
        new ChoiceOption("Unit"), new ChoiceOption("Sum"), false);
    private final ChoiceRow priceRow = new ChoiceRow(priceMode);

    public static Matrix4f matrix(UIContext context) {
        return new Matrix4f(context.getMatrices().peek().getPositionMatrix()).scale(.5f, .5f, 1);
    }

    private static void render(UIContext context, Widget widget, float x, float y, float alpha) {
        context.draw();
        widget.onFloatFloat2(y * 2, x * 2);
        widget.onFloatFloatFloatMatrix4f(alpha, context.getMouseY() * 2, context.getMouseX() * 2, matrix(context));
        TextShader.update2();
    }

    public void checkbox(UIContext context, Object key, float x, float centerY, boolean enabled, float alpha) {
        SmallButton check = checks.computeIfAbsent(key, ignored -> new SmallButton(enabled));
        check.setBoolean(enabled);
        render(context, check, x + 2, centerY - 4, alpha);
    }

    public void actions(UIContext context, Object key, float x, float centerY, float alpha) {
        IconButton[] buttons = actions.computeIfAbsent(key, ignored -> {
            IconButton remove = new IconButton(CategoryType.TRASH, 16);
            remove.setText("Remove item");
            IconButton edit = new IconButton(CategoryType.EDIT, 16);
            edit.setText("Edit item");
            return new IconButton[]{remove, edit};
        });
        render(context, buttons[0], x, centerY - 4, alpha);
        render(context, buttons[1], x + 13, centerY - 4, alpha);
    }

    public void field(UIContext context, TextField field, float x, float y, float w, float h, float alpha) {
        Tween tween = focus.computeIfAbsent(field, ignored -> EasingPresets.getTweenByFloatFloat2(0, .15f));
        tween.setFloat2(field.isFocused() ? 1 : 0);
        context.draw();
        ClickSysMaterial.inputField(matrix(context), x * 2, y * 2, w * 2, h * 2, tween.getFloat(), alpha);
    }

    /** Returns the top of the native segmented selector, in the market's coordinates. */
    public float priceMode(UIContext context, float x, float y, float width, boolean unit, float alpha) {
        priceMode.setBoolean(!unit);
        priceRow.setValue237(width * 2);
        render(context, priceRow, x, y, alpha);
        return y + priceRow.getValue238() / 2 - 16;
    }

    public static void icon(UIContext context, CategoryType icon, float x, float y, float size, int color, float alpha) {
        context.draw();
        SvgShader.onFloatIntMatrix4fFloatCategoryTypeFloatFloatFloat(alpha, color, matrix(context),
            y * 2, icon, size * 2, x * 2, size * 2);
    }
}
