package dev.bkclient.ui.clicksys.port.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.ui.clicksys.ClickSysMaterial;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme;
import dev.bkclient.ui.menu.csgui.content.AutoMarketContent;
import dev.bkclient.utility.render.ScissorUtility;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** OLD's registry/editor workflow embedded in the ClickSys content area. */
public final class AutoMarketPage extends Widget {
    private final AutoMarketContent content = new AutoMarketContent(true);

    @Override
    public void onFloatFloatFloatMatrix4f(float alpha, float mouseY, float mouseX, Matrix4f matrix) {
        UIContext parent = ClickSysMaterial.context();
        if (parent == null || alpha <= 0.001f) return;
        // ClickSys uses double-sized logical coordinates; both rendering and input
        // must use this conversion, independently of Minecraft's GUI scale.
        UIContext context = parent.withMouse((int) (mouseX / 2), (int) (mouseY / 2));
        TextShader.update2();
        context.draw();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        context.getMatrices().push();
        context.getMatrices().peek().getPositionMatrix().set(matrix).scale(2, 2, 1);
        ScissorUtility.push(context.getMatrices(), value235 / 2, value236 / 2, value237 / 2, value238 / 2);
        try (var theme = ClickGuiV2Theme.openClickSys()) {
            content.render(context, value235 / 2, value236 / 2, value237 / 2, value238 / 2, alpha);
            context.draw();
        } finally {
            ScissorUtility.pop();
            context.getMatrices().pop();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        }
    }

    private boolean contains(double x, double y) {
        return x >= value235 && x < value235 + value237 && y >= value236 && y < value236 + value238;
    }

    @Override
    public boolean isIntDoubleDouble(int button, double x, double y) {
        if (!contains(x, y)) { deactivate(); return false; }
        return button == 0 && content.handleClick(x / 2, y / 2);
    }

    @Override
    public boolean isDoubleDoubleInt(double x, double y, int button) {
        if (button != 0) return false;
        content.handleRelease();
        return contains(x, y);
    }

    @Override
    public boolean isDoubleDoubleIntDoubleDouble(double y, double x, int button, double dx, double dy) {
        return button == 0 && content.handleDrag(x / 2, y / 2);
    }

    @Override
    public boolean isDoubleDoubleDouble(double amount, double x, double y) {
        return contains(x, y) && content.onScroll(x / 2, y / 2, amount);
    }

    @Override
    public boolean isIntIntInt2(int scanCode, int modifiers, int keyCode) {
        return content.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isIntChar(int modifiers, char character) {
        return content.charTyped(character, modifiers);
    }

    public void deactivate() { content.deactivate(); }
}
