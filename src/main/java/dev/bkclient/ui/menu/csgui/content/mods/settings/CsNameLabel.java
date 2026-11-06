/*
 * CsNameLabel — pomocnik do rysowania nazwy ustawienia w ograniczonej szerokości.
 * Gdy tekst się nie mieści: domyślnie przycięty z "..", a po najechaniu myszą — płynnie
 * przewija się (marquee), żeby pokazać całość. Stan przewijania trzymany per-instancja.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.render.ScissorUtility;

public class CsNameLabel {

    private static final float SPEED = 18.0f;   // px/s przewijania
    private static final float PAUSE = 0.8f;    // pauza na końcach (s)
    private static final float GAP = 10.0f;     // przerwa przy zapętleniu

    private float scroll;
    private long lastMs = System.currentTimeMillis();
    private float holdTimer;
    private boolean atEnd;

    /**
     * Rysuje nazwę w obszarze [x .. x+maxW], wyśrodkowaną pionowo na centerY.
     * @param hovered czy wiersz jest pod kursorem (uruchamia przewijanie)
     */
    public void render(UIContext context, Font font, String text, float x, float centerY,
            float maxW, boolean hovered, ColorRGBA color) {
        float textW = font.width(text);
        float ty = centerY - font.height() / 2.0f;

        if (textW <= maxW) {
            this.scroll = 0.0f;
            context.drawText(font, text, x, ty, color);
            return;
        }

        long now = System.currentTimeMillis();
        float dt = (now - this.lastMs) / 1000.0f;
        this.lastMs = now;

        ScissorUtility.push(context.getMatrices(), x, centerY - font.height(), maxW, font.height() * 2.0f);
        if (hovered) {
            float maxScroll = textW - maxW;
            if (this.holdTimer > 0.0f) {
                this.holdTimer -= dt;
            } else if (!this.atEnd) {
                this.scroll += SPEED * dt;
                if (this.scroll >= maxScroll) {
                    this.scroll = maxScroll;
                    this.atEnd = true;
                    this.holdTimer = PAUSE;
                }
            } else {
                this.scroll -= SPEED * dt;
                if (this.scroll <= 0.0f) {
                    this.scroll = 0.0f;
                    this.atEnd = false;
                    this.holdTimer = PAUSE;
                }
            }
            context.drawText(font, text, x - this.scroll, ty, color);
        } else {
            this.scroll = 0.0f;
            this.atEnd = false;
            this.holdTimer = 0.0f;
            context.drawText(font, ellipsize(font, text, maxW), x, ty, color);
        }
        ScissorUtility.pop();
    }

    private static String ellipsize(Font font, String text, float maxW) {
        String dots = "...";
        if (font.width(dots) > maxW) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            String candidate = text.substring(0, i + 1) + dots;
            if (font.width(candidate) > maxW) {
                break;
            }
            sb.setLength(0);
            sb.append(text, 0, i + 1);
        }
        return sb.length() == 0 ? dots : sb + dots;
    }
}
