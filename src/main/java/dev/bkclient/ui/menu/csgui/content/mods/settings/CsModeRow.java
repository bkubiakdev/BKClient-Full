/*
 * CsModeRow — ModeSetting (single-select): nazwa + zawijane pigułki.
 * Wybrana pigułka = czerwone tło/biały tekst; pozostałe = ciemne tło/przygaszony tekst.
 */
package dev.bkclient.ui.menu.csgui.content.mods.settings;

import java.util.ArrayList;
import java.util.List;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.Setting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.ui.menu.csgui.CsTheme;
import dev.bkclient.utility.colors.ColorRGBA;

public class CsModeRow implements CsSetting {

    private static final float V_PAD = 4.5f;       // wewnętrzny odstęp góra/dół (jak Select)
    private static final float NAME_GAP = 4.0f;    // odstęp nazwa → pierwszy rząd pigułek
    private static final float PILL_H = 9.0f;      // Figma: 18
    private static final float GAP = 3.0f;
    private static final float PILL_PAD = 5.0f;
    private static final float PILL_FONT = 6.0f;

    private final ModeSetting setting;
    private final List<float[]> pillBounds = new ArrayList<>(); // [x,y,w,h]
    private final List<ModeSetting.Value> pillValues = new ArrayList<>();
    private final CsNameLabel nameLabel = new CsNameLabel();
    private float slideX, slideY, slideW;
    private boolean slideInit;

    public CsModeRow(ModeSetting setting) {
        this.setting = setting;
    }

    @Override
    public Setting getSetting() {
        return this.setting;
    }

    private List<ModeSetting.Value> visibleValues() {
        List<ModeSetting.Value> out = new ArrayList<>();
        for (ModeSetting.Value v : this.setting.getValues()) {
            if (!v.isHidden()) {
                out.add(v);
            }
        }
        return out;
    }

    private float nameH() {
        return Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f).height();
    }

    @Override
    public float getHeight() {
        int rows = lines(availWidth());
        return V_PAD + nameH() + NAME_GAP + rows * PILL_H + (rows - 1) * GAP + V_PAD;
    }

    // szerokość liczona w render(); cache by getHeight() dało spójny wynik
    private float lastW = 100.0f;

    private float availWidth() {
        return this.lastW;
    }

    private int lines(float w) {
        Font pf = Fonts.SEMIBOLD.getFont(PILL_FONT);
        int lineCount = 1;
        float cx = 0.0f;
        for (ModeSetting.Value v : visibleValues()) {
            float pw = pf.width(Localizator.translate(v.getName())) + PILL_PAD * 2.0f;
            if (cx > 0.0f && cx + pw > w) {
                lineCount++;
                cx = 0.0f;
            }
            cx += pw + GAP;
        }
        return lineCount;
    }

    @Override
    public void render(UIContext context, float x, float y, float w, float alpha) {
        this.lastW = w;

        float ny = y + V_PAD;
        Font label = Fonts.SEMIBOLD.getFont(CsTheme.Slider.NAME_FONT + 1.0f);
        context.drawText(label, Localizator.translate(this.setting.getName()), x, ny,
                CsTheme.Color.TEXT_75.withAlpha(255.0f * 0.75f * alpha));

        ModeSetting.Value cur = this.setting.getValue();
        if (cur != null) {
            String curName = Localizator.translate(cur.getName());
            Font cf = Fonts.SEMIBOLD.getFont(6.0f);
            float chipH = 7.0f;
            float chipW = cf.width(curName) + 8.0f;
            float chipX = x + w - chipW;
            float chipY = ny + label.height() / 2.0f - chipH / 2.0f;
            context.drawRoundedRect(chipX, chipY, chipW - 2, chipH, BorderRadius.all(2f),
                    CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));
            context.drawCenteredText(cf, curName, chipX + chipW / 2.0f, chipY + (chipH - cf.height()) / 2.0f,
                    CsTheme.Color.BLACK_75.withAlpha(255.0f * 0.75f * alpha));
        }

        Font pf = Fonts.SEMIBOLD.getFont(PILL_FONT);
        this.pillBounds.clear();
        this.pillValues.clear();
        float startY = ny + label.height() + NAME_GAP;
        float cx = x;
        float cy = startY;

        // 1) policz pozycje wszystkich pigułek + tło (białe 5%) + znajdź cel highlightu
        //    cel zapisujemy WZGLĘDNIE do (x, startY), żeby scroll nie powodował "odlatywania"
        float targetX = 0.0f, targetY = 0.0f, targetW = 0.0f;
        boolean hasTarget = false;
        ColorRGBA offBg = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f);
        for (ModeSetting.Value v : visibleValues()) {
            String name = Localizator.translate(v.getName());
            float pw = pf.width(name) + PILL_PAD * 2.0f;
            if (cx > x && cx + pw > x + w) {
                cx = x;
                cy += PILL_H + GAP;
            }
            context.drawRoundedRect(cx, cy, pw, PILL_H, BorderRadius.all(2.0f),
                    offBg.withAlpha(offBg.getAlpha() * alpha));
            if (this.setting.getValue() == v) {
                targetX = cx - x;          // względne
                targetY = cy - startY;     // względne
                targetW = pw;
                hasTarget = true;
            }
            this.pillBounds.add(new float[] { cx, cy, pw, PILL_H });
            this.pillValues.add(v);
            cx += pw + GAP;
        }

        // 2) płynący akcent — lerp x/y/szerokości do zaznaczonej pigułki (jak Ghost, speed ~0.28)
        //    snap gdy bardzo blisko, by przy szybkim przewijaniu nie "zsuwało się" z pigułki
        if (hasTarget) {
            if (!this.slideInit) {
                this.slideX = targetX;
                this.slideY = targetY;
                this.slideW = targetW;
                this.slideInit = true;
            } else {
                float speed = 0.30f;
                this.slideX += (targetX - this.slideX) * speed;
                this.slideY += (targetY - this.slideY) * speed;
                this.slideW += (targetW - this.slideW) * speed;
                if (Math.abs(targetX - this.slideX) < 0.5f) this.slideX = targetX;
                if (Math.abs(targetY - this.slideY) < 0.5f) this.slideY = targetY;
                if (Math.abs(targetW - this.slideW) < 0.5f) this.slideW = targetW;
            }
            context.drawRoundedRect(x + this.slideX, startY + this.slideY, this.slideW, PILL_H,
                    BorderRadius.all(2.0f), CsTheme.Color.accent().withAlpha(CsTheme.Color.accent().getAlpha() * alpha));
        }

        float absSlideX = x + this.slideX;
        float absSlideY = startY + this.slideY;
        for (int i = 0; i < this.pillBounds.size(); ++i) {
            float[] b = this.pillBounds.get(i);
            String name = Localizator.translate(this.pillValues.get(i).getName());
            float overlap = 0.0f;
            if (hasTarget) {
                float ix = Math.max(b[0], absSlideX);
                float ax = Math.min(b[0] + b[2], absSlideX + this.slideW);
                overlap = Math.clamp((ax - ix) / b[2], 0.0f, 1.0f);
                if (Math.abs(b[1] - absSlideY) > 1.0f) {
                    overlap = 0.0f;  // highlight w innym rzędzie
                }
            }
            ColorRGBA tc = CsTheme.Color.TEXT_25.mix(CsTheme.Color.BLACK_75, overlap);
            float tA = 0.25f + 0.50f * overlap;
            context.drawCenteredText(pf, name, b[0] + b[2] / 2.0f, b[1] + (PILL_H - pf.height()) / 2.0f,
                    tc.withAlpha(255.0f * tA * alpha));
        }
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        for (int i = 0; i < this.pillBounds.size(); ++i) {
            float[] b = this.pillBounds.get(i);
            if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                this.setting.setValue(this.pillValues.get(i));
                return true;
            }
        }
        return false;
    }
}
