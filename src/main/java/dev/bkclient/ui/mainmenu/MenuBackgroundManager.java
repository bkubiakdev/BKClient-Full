package dev.bkclient.ui.mainmenu;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.DrawUtility;
import java.util.ArrayList;
import java.util.List;

/** Wallpaper preview; browsing is temporary until the centre card is confirmed. */
public class MenuBackgroundManager implements IScaledResolution {
    private final List<MenuBackground> carousel = new ArrayList<>();
    private boolean open;
    private float progress;
    private float position;
    private int targetIndex;
    private int selectedIndex;
    private float aspect = 16.0F / 9.0F;
    private long lastFrameNanos;
    private boolean pressed;
    private boolean dragging;
    private double pressX;
    private double pressY;
    private float pressPosition;
    private float screenW;
    private float screenH;
    private float cardW;
    private float cardH;
    private float dotStep;
    private float folderX, folderY, folderW;
    private final dev.bkclient.utility.animation.base.Animation folderHover = new dev.bkclient.utility.animation.base.Animation(
        300L,0.0F,dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT);
    private boolean overFolder(double x,double y) {
        return GuiUtility.isHovered(folderX,folderY,folderW,24,x,y);
    }

    public void open() {
        if (this.open) return;
        this.carousel.clear();
        this.carousel.addAll(MenuBackground.builtins());
        this.carousel.addAll(CustomBackgroundLoader.scan());
        String selected = BKClientInit.getInstance().getSelectedMenuBackground();
        this.selectedIndex = 0;
        for (int i = 0; i < this.carousel.size(); i++) {
            if (this.carousel.get(i).getUniqueId().equals(selected)) this.selectedIndex = i;
        }
        this.aspect = Math.max(0.1F, this.carousel.get(this.selectedIndex).getAspect());
        this.position = this.targetIndex = this.selectedIndex;
        this.pressed = this.dragging = false;
        this.open = true;
        this.lastFrameNanos = System.nanoTime();
    }

    public void close() {
        this.open = false;
        this.pressed = this.dragging = false;
        this.targetIndex = this.selectedIndex;
    }

    public boolean isOpen() {
        // Capture input immediately on open, and throughout the closing animation.
        return this.open || this.progress > 0.001F;
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(this.carousel.size() - 1, index));
    }

    private void confirm() {
        this.selectedIndex = this.clampIndex(Math.round(this.position));
        String selected = this.carousel.get(this.selectedIndex).getUniqueId();
        if (!selected.equals(BKClientInit.getInstance().getSelectedMenuBackground())) {
            BKClientInit.getInstance().setSelectedMenuBackground(selected);
            BKClientInit.getInstance().getFileManager().writeFile("client");
        }
        this.close();
    }

    public void render(UIContext context) {
        if (!this.isOpen()) return;
        long now = System.nanoTime();
        float seconds = Math.min(0.1F, Math.max(0.0F, (now - this.lastFrameNanos) / 1_000_000_000.0F));
        this.lastFrameNanos = now;
        float smoothing = 1.0F - (float)Math.pow(0.0009F, seconds);
        float target = this.open ? 1.0F : 0.0F;
        this.progress += (target - this.progress) * smoothing;
        if (Math.abs(target - this.progress) < 0.0008F) this.progress = target;
        if (!this.dragging) {
            this.position += (this.targetIndex - this.position) * smoothing;
            if (Math.abs(this.targetIndex - this.position) < 0.0008F) this.position = this.targetIndex;
        }
        if (this.progress <= 0.001F) return;

        this.screenW = sr.getScaledWidth();
        this.screenH = sr.getScaledHeight();
        this.cardW = Math.min(this.screenW * 0.52F, this.screenH * 0.62F * this.aspect);
        this.cardH = this.cardW / this.aspect;
        this.dotStep = Math.min(10.0F, Math.max(1.0F, (this.screenW - 32.0F) / Math.max(1, this.carousel.size())));
        float p = this.progress;
        context.drawRoundedRect(0, 0, this.screenW, this.screenH, BorderRadius.ZERO, ColorRGBA.BLACK.withAlpha(255 * p));

        float fullWidth = Math.max(this.screenW + 56.0F, (this.screenH + 56.0F) * this.aspect);
        float width = fullWidth + (this.cardW - fullWidth) * p;
        float height = width / this.aspect;
        float spacing = this.cardW * 1.16F * p;
        float centerY = this.screenH / 2.0F - 4.0F * p;
        // At most four visible cards; draw farthest first without sorting/allocating each frame.
        int left = Math.max(0, (int)Math.ceil(this.position - 1.85F));
        int right = Math.min(this.carousel.size() - 1, (int)Math.floor(this.position + 1.85F));
        while (left <= right) {
            int index = Math.abs(left - this.position) > Math.abs(right - this.position) ? left++ : right--;
            float offset = index - this.position;
            float distance = Math.min(1.0F, Math.abs(offset));
            float scale = 1.0F - 0.14F * distance;
            float w = width * scale;
            float h = height * scale;
            float x = this.screenW / 2.0F + offset * spacing - w / 2.0F;
            float y = centerY - h / 2.0F;
            float alpha = (1.0F - 0.4F * distance) * Math.max(p, Math.max(0.0F, 1.0F - Math.abs(offset) * 6.0F));
            if (alpha <= 0.003F) continue;
            BorderRadius radius = BorderRadius.all(18.0F * p);
            context.drawShadow(x, y + 6.0F * p, w, h, 16.0F * p, radius, ColorRGBA.BLACK.withAlpha(150 * p * alpha));
            this.drawCover(context, this.carousel.get(index), x, y, w, h, radius, alpha);
            context.drawRoundedRect(x, y, w, h, radius, ColorRGBA.BLACK.withAlpha(95 * distance * p));
        }

        Font titleFont = Fonts.BOLD.getFont(21.0F);
        float cardTop = this.screenH / 2.0F - 4.0F - this.cardH / 2.0F;
        float titleY = Math.max(6.0F, Math.min(Math.min(42.0F, this.screenH * 0.13F),
            cardTop - titleFont.height() - Math.max(10.0F, Math.min(28.0F, this.screenH * 0.045F)))) - 14 * (1 - p);
        for (int i = Math.max(0, (int)Math.floor(this.position)); i <= Math.min(this.carousel.size() - 1, (int)Math.ceil(this.position)); i++) {
            float offset = i - this.position;
            float fade = 1.0F - Math.abs(offset);
            float x = this.screenW / 2.0F + offset * 140.0F;
            String label = this.carousel.get(i).getDisplayName();
            context.drawCenteredText(titleFont, label, x, titleY + 1, ColorRGBA.BLACK.withAlpha(130 * p * fade * fade));
            context.drawCenteredText(titleFont, label, x, titleY, ColorRGBA.WHITE.withAlpha(255 * p * fade * fade));
        }
        float dotSize = Math.min(6.0F, this.dotStep * 0.6F);
        for (int i = 0; i < this.carousel.size(); i++) {
            float active = 1.0F - Math.min(1.0F, Math.abs(i - this.position));
            context.drawRoundedRect(this.dotX(i) - dotSize / 2, this.screenH - 26 - dotSize / 2,
                dotSize, dotSize, BorderRadius.all(dotSize / 2), ColorRGBA.WHITE.withAlpha(255 * (0.3F + 0.7F * active) * p));
        }
        folderW=24;
        folderX=screenW-folderW-12;folderY=12;
        boolean folderOver=open && overFolder(context.getMouseX(),context.getMouseY());
        folderHover.update(folderOver);
        CustomButton.drawMenuSurface(context,folderX,folderY,24,24,BorderRadius.all(12),p,folderHover.getValue()*p);
        context.draw();
        dev.bkclient.ui.components.LucideIcons.FOLDER.draw(context,folderX+6,folderY+6,12,ColorRGBA.WHITE.withAlpha(230*p));
        if(folderOver)CursorUtility.set(CursorType.HAND);
        if (this.open && this.overCenter(context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
        }
    }

    private void drawCover(UIContext context, MenuBackground background, float x, float y, float w, float h, BorderRadius radius, float alpha) {
        if (background.isVideo()) {
            background.getVideoPlayer().update();
            if (!background.getVideoPlayer().hasFrame()) background = MenuBackground.CINEMATIC;
        }
        float ratio = w / h;
        float sourceRatio = Math.max(0.1F, background.getAspect());
        float uSpan = Math.min(1, ratio / sourceRatio);
        float vSpan = Math.min(1, sourceRatio / ratio);
        DrawUtility.drawRoundedTextureWithUV(context.getMatrices(), background.id(), x, y, w, h, radius,
            ColorRGBA.WHITE.withAlpha(255 * alpha), (1 - uSpan) / 2, (1 - vSpan) / 2, (1 + uSpan) / 2, (1 + vSpan) / 2);
    }

    private float dotX(int index) {
        return this.screenW / 2 - (this.carousel.size() - 1) * this.dotStep / 2 + index * this.dotStep;
    }

    private boolean overCenter(double x, double y) {
        return GuiUtility.isHovered((this.screenW - this.cardW) / 2, (this.screenH - this.cardH) / 2 - 4, this.cardW, this.cardH, x, y);
    }

    public void onMouseClicked(double x, double y, MouseButton button) {
        if (!this.open) return;
        if (button == MouseButton.RIGHT) { this.close(); return; }
        if (button != MouseButton.LEFT || this.progress < 0.5F) return;
        if (overFolder(x,y)) {
            this.pressed=this.dragging=false;
            net.minecraft.util.Util.getOperatingSystem().open(CustomBackgroundLoader.getFolder());
            return;
        }
        this.pressed = true;
        this.dragging = false;
        this.pressX = x;
        this.pressY = y;
        this.pressPosition = this.position;
    }

    public void onMouseDragged(double x, double y, MouseButton button) {
        if (!this.open || !this.pressed || button != MouseButton.LEFT) return;
        if (Math.abs(x - this.pressX) > 4 || Math.abs(y - this.pressY) > 4) this.dragging = true;
        if (this.dragging) {
            this.position = Math.max(0, Math.min(this.carousel.size() - 1, this.pressPosition - (float)(x - this.pressX) / Math.max(1, this.cardW * 1.16F)));
            this.targetIndex = this.clampIndex(Math.round(this.position));
        }
    }

    public void onMouseReleased(double x, double y, MouseButton button) {
        if (button != MouseButton.LEFT || !this.open || !this.pressed) return;
        this.pressed = false;
        if (this.dragging) { this.dragging = false; return; }
        if (Math.abs(y - (this.screenH - 26)) <= 7) {
            int dot = Math.round((float)(x - this.dotX(0)) / Math.max(1, this.dotStep));
            if (dot >= 0 && dot < this.carousel.size() && Math.abs(x - this.dotX(dot)) <= this.dotStep / 2 + 1) {
                this.targetIndex = dot;
                return;
            }
        }
        int nearest = this.clampIndex(Math.round(this.position));
        int clicked = this.clampIndex(Math.round(this.position + (float)(x - this.screenW / 2) / Math.max(1, this.cardW * 1.16F)));
        if (clicked == nearest && this.overCenter(x, y)) this.confirm();
        else if (clicked != nearest) this.targetIndex = clicked;
    }

    public boolean mouseScrolled(double horizontal, double vertical) {
        if (!this.isOpen()) return false;
        double amount = vertical != 0 ? vertical : horizontal;
        if (this.open && amount != 0) this.move(amount > 0 ? -1 : 1);
        return true;
    }

    private void move(int direction) {
        this.targetIndex = this.clampIndex((this.dragging ? Math.round(this.position) : this.targetIndex) + direction);
        this.pressed = this.dragging = false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isOpen()) return false;
        if (this.open) {
            if (keyCode == 256) this.close();
            else if (keyCode == 263) this.move(-1);
            else if (keyCode == 262) this.move(1);
            else if (keyCode == 257 || keyCode == 335) this.confirm();
        }
        return true;
    }
}
