/*
 * ThemePanel — "ThemeClient" (prawa), przeciągalny.
 */
package dev.bkclient.ui.menu.csgui.panel;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.visuals.Interface;
import dev.bkclient.systems.modules.modules.visuals.PlayerCardSettings;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.EntityUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;

public class ThemePanel extends CsPanel implements IMinecraft {

    private static final float W = 125.0f;
    private static final float H = 265.0f;

    private static final float INNER_RADIUS = 7.5f;
    private static final float INNER_MARGIN = 5.0f;
    private static final float INNER1_H = 17.5f;
    private static final float INNER3_H = 38.0f;
    private static final float GAP_23 = 16.0f;

    private static final ColorRGBA INNER1_FILL   = new ColorRGBA(32.0f, 32.0f, 36.0f, 255.0f * 0.5f);
    private static final ColorRGBA INNER1_BORDER = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f);
    private static final ColorRGBA INNER_FILL    = new ColorRGBA(21.0f, 21.0f, 21.0f, 255.0f * 0.5f);
    private static final ColorRGBA INNER_BORDER  = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.15f);

    private static final float LIST_FONT_SIZE  = 6.5f;
    private static final ColorRGBA LIST_TEXT   = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.5f);
    private static final ColorRGBA LIST_SEP    = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.06f);
    private static final float DOT_SIZE        = 6.5f;
    private static final float LIST_PAD_X      = 8.0f;

    private static final String[] ENTRY_NAMES = {
            "Accent", "Modules", "Inactive Text", "Headers",
            "Slider", "Slider Knob", "Slider Window", "Text", "Visual"
    };
    private static final ColorRGBA[] ENTRY_FIXED = {
            null,
            new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f),
            new ColorRGBA(130.0f, 130.0f, 130.0f, 255.0f),
            new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f),
            null,
            new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f),
            new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f),
            new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f),
            new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f),
    };

    private static final float BTN_W        = 29.0f;
    private static final float BTN_H        = 12.5f;
    private static final float BTN_RADIUS   = 5.0f;
    private static final float BTN_ICON_SIZE = 7.5f;
    private static final char  BTN_ICON     = '3';

    private static final float SAVED_FONT_SIZE = 7.0f;
    private static final ColorRGBA SAVED_COLOR = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f);
    private static final float SWATCH_SIZE = 8.5f;
    private static final float SWATCH_PAD  = 5.0f;
    private static final int   SWATCH_PER_ROW = 8;

    private static final ColorRGBA[] SAVED_COLORS = {
            new ColorRGBA(255.0f,  65.0f,  65.0f, 255.0f),
            new ColorRGBA(255.0f, 200.0f,  60.0f, 255.0f),
            new ColorRGBA( 70.0f,  90.0f, 245.0f, 255.0f),
            new ColorRGBA(150.0f, 240.0f,  60.0f, 255.0f),
            new ColorRGBA(150.0f,  70.0f, 165.0f, 255.0f),
            new ColorRGBA(140.0f, 135.0f,  70.0f, 255.0f),
            new ColorRGBA(225.0f, 235.0f, 255.0f, 255.0f),
            new ColorRGBA( 20.0f,  55.0f,  15.0f, 255.0f),
            new ColorRGBA(240.0f, 245.0f, 180.0f, 255.0f),
            new ColorRGBA( 80.0f, 235.0f, 170.0f, 255.0f),
            new ColorRGBA( 20.0f, 110.0f,  75.0f, 255.0f),
            new ColorRGBA(165.0f, 225.0f, 245.0f, 255.0f),
            new ColorRGBA(140.0f, 195.0f, 140.0f, 255.0f),
    };

    private final float[] swatchX = new float[SAVED_COLORS.length];
    private final float[] swatchY = new float[SAVED_COLORS.length];

    private boolean cornersMode = false;
    private float boxBtnX, boxBtnY, boxBtnW, segBtnH;
    private float cornBtnX;

    private int gearSide = 1;
    private float gearAlong = 0.5f;
    private boolean gearDragging;
    private float gearDragDX, gearDragDY;
    private float gearRectX, gearRectY, gearRectW, gearRectH;
    private float modelCenterX, modelCenterY;
    private float boxX, boxY, boxW, boxH;
    private final dev.bkclient.utility.animation.base.Animation gearAnimX =
            new dev.bkclient.utility.animation.base.Animation(260L,
                    dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT);
    private final dev.bkclient.utility.animation.base.Animation gearAnimY =
            new dev.bkclient.utility.animation.base.Animation(260L,
                    dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT);
    private boolean gearAnimInit;

    private int nameSide = 0;
    private float nameAlong = 0.5f;
    private boolean nameDragging;
    private float nameDragDX, nameDragDY;
    private float nameRectX, nameRectY, nameRectW, nameRectH;
    private final dev.bkclient.utility.animation.base.Animation nameAnimX =
            new dev.bkclient.utility.animation.base.Animation(260L,
                    dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT);
    private final dev.bkclient.utility.animation.base.Animation nameAnimY =
            new dev.bkclient.utility.animation.base.Animation(260L,
                    dev.bkclient.utility.animation.base.Easing.FIGMA_EASE_IN_OUT);
    private boolean nameAnimInit;
    private boolean playerCardPlacementLoaded;

    private Interface iface() {
        return BKClientInit.getInstance().getModuleManager().getModule(Interface.class);
    }



    private boolean showCardName() { return PlayerCardSettings.showName; }

    private boolean showCardPing() { return PlayerCardSettings.showPing; }

    private boolean showCardHealth() { return PlayerCardSettings.showHealth; }

    public ThemePanel(float x, float y) {
        super(x, y, W, H);
    }

    @Override
    protected void renderContent(UIContext context, float alpha) {
        float ix = this.x + INNER_MARGIN;
        float iw = this.width - INNER_MARGIN * 2.0f;
        this.loadPlayerCardPlacement();

        float i1y = this.y + INNER_MARGIN;
        this.drawInner(context, ix, i1y, iw, INNER1_H, INNER1_FILL, INNER1_BORDER, alpha);

        Font titleFont = Fonts.MEDIUM.getFont(5.5f + 1);
        context.drawText(titleFont, "Theme", ix + 8.0f,
                i1y + (INNER1_H - titleFont.height()) / 2.0f,
                new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.40f).withAlpha(255.0f * 0.40f * alpha));

        ColorRGBA accent = Colors.getAccentColor();
        float btnX = ix + iw - 5.0f - BTN_W;
        float btnY = i1y + (INNER1_H - BTN_H) / 2.0f;
        context.drawRoundedRect(btnX, btnY, BTN_W, BTN_H, BorderRadius.all(BTN_RADIUS),
                accent.withAlpha(accent.getAlpha() * 0.95f * alpha));
        context.drawIcon(Fonts.ICONS, BTN_ICON,
                btnX + (BTN_W - BTN_ICON_SIZE) / 2.0f - 1,
                btnY + (BTN_H  - BTN_ICON_SIZE) / 2.0f - 1f,
                BTN_ICON_SIZE + 2, ColorRGBA.BLACK.withAlpha(255.0f * alpha));

        float i3y = this.y + this.height - INNER_MARGIN - INNER3_H;
        float i2y = i1y + INNER1_H + INNER_MARGIN;
        float i2h = i3y - GAP_23 - i2y;
        this.drawInner(context, ix, i2y, iw, i2h, INNER_FILL, INNER_BORDER, alpha);

        if (mc.player != null) {
            float unit = (i2h * 0.6f) / 32.0f;
            float modelH = 32.0f * unit;
            float modelW = 16.0f * unit;
            float sx = ix + (iw - modelW) / 2.0f;
            float sy = i2y + (i2h - modelH) / 2.0f - 6.0f;

            float pad = 4.0f;
            float bx = sx - pad;
            float by = sy - pad;
            float bw = modelW + pad * 2.0f;
            float bh = modelH + pad * 2.0f;
            ColorRGBA boxAccent = Colors.getAccentColor().withAlpha(255.0f * alpha);
            if (this.cornersMode) {
                this.drawCorners(context, bx, by, bw, bh, boxAccent);
            } else {
                context.drawRoundedBorder(bx, by, bw, bh, 0.5f, BorderRadius.all(0.0f), boxAccent);
            }

            float hpFrac = mc.player.getHealth() / mc.player.getMaxHealth();
            hpFrac = Math.max(0.0f, Math.min(1.0f, hpFrac));
            float hbW = 1.5f;
            float hbGap = 2.5f;
            float hbX = bx - hbGap - hbW;
            float hbY = by;
            float hbH = bh;
            context.drawRect(hbX - 0.5f, hbY - 0.5f, hbW + 1.0f, hbH + 1.0f,
                    new ColorRGBA(0.0f, 0.0f, 0.0f, 255.0f * 0.5f * alpha));
            float fillH = hbH * hpFrac;
            ColorRGBA hpColor = new ColorRGBA(
                    Math.min(255.0f, 255.0f * (1.0f - hpFrac) * 2.0f),
                    Math.min(255.0f, 255.0f * hpFrac * 2.0f), 60.0f, 255.0f).withAlpha(255.0f * alpha);
            context.drawRect(hbX, hbY + hbH - fillH, hbW, fillH, hpColor);

            context.drawSkin2D(mc.player.getSkinTextures().texture(), sx, sy, unit,
                    ColorRGBA.WHITE.withAlpha(255.0f * alpha));

            String playerName = mc.player.getName().getString();
            int ping = 0;
            if (mc.getNetworkHandler() != null
                    && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
                ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
            }

            Font nameFont = Fonts.MEDIUM.getFont(6.0f);
            String pingNum = String.valueOf(ping);
            String pingMs = "ms";
            String hpText = Math.round(EntityUtility.getHealth(mc.player)) + " hp";
            boolean showCardName = this.showCardName();
            boolean showCardPing = this.showCardPing();
            boolean showCardHealth = this.showCardHealth();
            boolean drawNamePlate = showCardName || showCardPing || showCardHealth;

            float nameW = nameFont.width(playerName);
            float pingNumW = nameFont.width(pingNum);
            float pingMsW = nameFont.width(pingMs);
            float hpW = nameFont.width(hpText);
            float sepW2 = 1.0f;
            float sepGap2 = 4.0f;
            float namePadX = 5.0f;
            float namePadY = 2.5f;

            float nameContentLen = namePadX * 2.0f;
            boolean hasNameSegment = false;
            if (showCardName) {
                nameContentLen += nameW;
                hasNameSegment = true;
            }
            if (showCardPing) {
                if (hasNameSegment) {
                    nameContentLen += sepGap2 * 2.0f + sepW2;
                }
                nameContentLen += pingNumW + 1.0f + pingMsW;
                hasNameSegment = true;
            }
            if (showCardHealth) {
                if (hasNameSegment) {
                    nameContentLen += sepGap2 * 2.0f + sepW2;
                }
                nameContentLen += hpW;
            }
            float nameContentThick = nameFont.height() + namePadY * 2.0f;

            net.minecraft.item.ItemStack[] hands = {
                    mc.player.getMainHandStack(),
                    mc.player.getOffHandStack()
            };
            net.minecraft.item.ItemStack[] armorPieces = {
                    mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD),
                    mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST),
                    mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS),
                    mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET)
            };

            float gIcon = 8.0f;
            float gGap = 1.5f;
            float gPadX = 3.0f;
            float gPadY = 2.0f;
            float sepW = 1.0f;
            float sepGap = 2.0f;
            int slotCount = hands.length + armorPieces.length;
            float rowLen = slotCount * gIcon + (slotCount - 1) * gGap + sepGap * 2.0f + sepW;

            this.modelCenterX = bx + bw / 2.0f;
            this.modelCenterY = by + bh / 2.0f;
            this.boxX = bx; this.boxY = by; this.boxW = bw; this.boxH = bh;
            float margin = 3.0f;
            float hpClear = 5.0f;

            if (this.gearDragging) {
                float mx = (float) GuiUtility.getMouse().getX();
                float my = (float) GuiUtility.getMouse().getY();
                float dx = mx - this.modelCenterX;
                float dy = my - this.modelCenterY;
                if (Math.abs(dx) > Math.abs(dy)) {
                    this.gearSide = dx < 0 ? 2 : 3;
                    this.gearAlong = (my - by) / Math.max(1.0f, bh);
                } else {
                    this.gearSide = dy < 0 ? 0 : 1;
                    this.gearAlong = (mx - bx) / Math.max(1.0f, bw);
                }
                this.gearAlong = Math.max(0.0f, Math.min(1.0f, this.gearAlong));
                this.saveGearPlacement(false);
            }

            if (this.nameDragging) {
                float my = (float) GuiUtility.getMouse().getY();
                float mx = (float) GuiUtility.getMouse().getX();
                this.nameSide = my < this.modelCenterY ? 0 : 1;
                this.nameAlong = (mx - bx) / Math.max(1.0f, bw);
                this.nameAlong = Math.max(0.0f, Math.min(1.0f, this.nameAlong));
                this.saveNamePlacement(false);
            }

            PlayerCardSettings.cornersMode = this.cornersMode;
            PlayerCardSettings.gearSide = this.gearSide;
            PlayerCardSettings.gearAlong = this.gearAlong;
            PlayerCardSettings.nameSide = this.nameSide;
            PlayerCardSettings.nameAlong = this.nameAlong;
            PlayerCardSettings.showName = showCardName;
            PlayerCardSettings.showPing = showCardPing;
            PlayerCardSettings.showHealth = showCardHealth;

            boolean vertical = (this.gearSide == 2 || this.gearSide == 3);
            float rectW = vertical ? (gIcon + gPadY * 2.0f) : (rowLen + gPadX * 2.0f);
            float rectH = vertical ? (rowLen + gPadX * 2.0f) : (gIcon + gPadY * 2.0f);
            this.gearRectW = rectW;
            this.gearRectH = rectH;

            this.nameRectW = drawNamePlate ? nameContentLen : 0.0f;
            this.nameRectH = drawNamePlate ? nameContentThick : 0.0f;
            if (!drawNamePlate) {
                this.nameDragging = false;
            }
            float stackGap = 6.0f;

            float along = Math.max(0.0f, Math.min(1.0f, this.gearAlong));
            float offTX, offTY;
            switch (this.gearSide) {
                case 1 -> { offTX = along * (bw - rectW); offTY = bh + margin; }
                case 2 -> { offTX = -margin - rectW - hpClear; offTY = along * (bh - rectH); }
                case 3 -> { offTX = bw + margin; offTY = along * (bh - rectH); }
                default -> { offTX = along * (bw - rectW); offTY = -margin - rectH; }
            }

            float nameAlongClamped = Math.max(0.0f, Math.min(1.0f, this.nameAlong));
            float nameOffTX, nameOffTY;
            switch (this.nameSide) {
                case 1 -> { nameOffTX = nameAlongClamped * (bw - this.nameRectW); nameOffTY = bh + margin; }
                case 2 -> { nameOffTX = -margin - this.nameRectW - hpClear; nameOffTY = nameAlongClamped * (bh - this.nameRectH); }
                case 3 -> { nameOffTX = bw + margin; nameOffTY = nameAlongClamped * (bh - this.nameRectH); }
                default -> { nameOffTX = nameAlongClamped * (bw - this.nameRectW); nameOffTY = -margin - this.nameRectH; }
            }

            if (drawNamePlate && this.gearSide == this.nameSide && !this.gearDragging && !this.nameDragging) {
                if (this.gearSide == 0) {
                    nameOffTY -= (this.nameRectH + stackGap);
                } else if (this.gearSide == 1) {
                    nameOffTY += (rectH + stackGap);
                } else if (this.gearSide == 2) {
                    nameOffTX -= (this.nameRectW + stackGap);
                } else {
                    nameOffTX += (rectW + stackGap);
                }
            }

            if (!this.gearAnimInit) {
                this.gearAnimX.setValue(offTX);
                this.gearAnimY.setValue(offTY);
                this.gearAnimInit = true;
            }
            float rectX, rectY;
            if (this.gearDragging) {
                rectX = (float) GuiUtility.getMouse().getX() - this.gearDragDX;
                rectY = (float) GuiUtility.getMouse().getY() - this.gearDragDY;
                this.gearAnimX.setValue(rectX - bx);
                this.gearAnimY.setValue(rectY - by);
            } else {
                rectX = bx + this.gearAnimX.update(offTX);
                rectY = by + this.gearAnimY.update(offTY);
            }
            this.gearRectX = rectX;
            this.gearRectY = rectY;

            if (!this.nameAnimInit) {
                this.nameAnimX.setValue(nameOffTX);
                this.nameAnimY.setValue(nameOffTY);
                this.nameAnimInit = true;
            }
            float nameRX, nameRY;
            if (this.nameDragging) {
                nameRX = (float) GuiUtility.getMouse().getX() - this.nameDragDX;
                nameRY = (float) GuiUtility.getMouse().getY() - this.nameDragDY;
                this.nameAnimX.setValue(nameRX - bx);
                this.nameAnimY.setValue(nameRY - by);
            } else {
                nameRX = bx + this.nameAnimX.update(nameOffTX);
                nameRY = by + this.nameAnimY.update(nameOffTY);
            }
            this.nameRectX = nameRX;
            this.nameRectY = nameRY;

            if (drawNamePlate) {
                context.drawRoundedRect(nameRX, nameRY, this.nameRectW, this.nameRectH, BorderRadius.all(2.0f),
                        new ColorRGBA(21.0f, 21.0f, 21.0f, 255.0f * 0.9f).withAlpha(255.0f * 0.9f * alpha));

                ColorRGBA nameColor = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.8f).withAlpha(255.0f * 0.8f * alpha);
                ColorRGBA sepColor = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f).withAlpha(255.0f * 0.25f * alpha);
                ColorRGBA pingColor = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f).withAlpha(255.0f * alpha);
                ColorRGBA accentPing = Colors.getAccentColor().withAlpha(255.0f * alpha);
                ColorRGBA hpTextColor = this.healthColor(alpha);

                float textY = nameRY + namePadY;
                float cursorX = nameRX + namePadX;
                boolean hasSegment = false;
                if (showCardName) {
                    context.drawText(nameFont, playerName, cursorX, textY, nameColor);
                    cursorX += nameW;
                    hasSegment = true;
                }
                if (showCardPing) {
                    if (hasSegment) {
                        cursorX += sepGap2;
                        context.drawRect(cursorX, nameRY + 2.5f, sepW2, this.nameRectH - 5.0f, sepColor);
                        cursorX += sepW2 + sepGap2;
                    }
                    context.drawText(nameFont, pingNum, cursorX, textY, pingColor);
                    cursorX += pingNumW + 1.0f;
                    context.drawText(nameFont, pingMs, cursorX, textY, accentPing);
                    cursorX += pingMsW;
                    hasSegment = true;
                }
                if (showCardHealth) {
                    if (hasSegment) {
                        cursorX += sepGap2;
                        context.drawRect(cursorX, nameRY + 2.5f, sepW2, this.nameRectH - 5.0f, sepColor);
                        cursorX += sepW2 + sepGap2;
                    }
                    context.drawText(nameFont, hpText, cursorX, textY, hpTextColor);
                }
            }

            context.drawRoundedRect(rectX, rectY, rectW, rectH, BorderRadius.all(2.0f),
                    new ColorRGBA(21.0f, 21.0f, 21.0f, 255.0f * 0.9f).withAlpha(255.0f * 0.9f * alpha));

            ColorRGBA emptyColor = new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.25f).withAlpha(255.0f * 0.25f * alpha);
            if (vertical) {
                float gx = rectX + gPadY;
                float gy = rectY + gPadX;
                gy = this.drawGearSlots(context, hands, gx, gy, gIcon, gGap, emptyColor, alpha, true);
                context.drawRect(gx + 1.0f, gy - gGap + sepGap, gIcon - 2.0f, sepW, emptyColor);
                gy += sepGap * 2.0f + sepW - gGap;
                this.drawGearSlots(context, armorPieces, gx, gy, gIcon, gGap, emptyColor, alpha, true);
            } else {
                float gx = rectX + gPadX;
                float gy = rectY + gPadY;
                gx = this.drawGearSlots(context, hands, gx, gy, gIcon, gGap, emptyColor, alpha, false);
                context.drawRect(gx - gGap + sepGap, gy + 1.1f, sepW, gIcon - 2.0f, emptyColor);
                gx += sepGap * 2.0f + sepW - gGap;
                this.drawGearSlots(context, armorPieces, gx, gy, gIcon, gGap, emptyColor, alpha, false);
            }
            Font segFont = Fonts.MEDIUM.getFont(5.0f);
            this.segBtnH = 8.5f;
            float segGap = 3.0f;
            this.boxBtnW = (iw - segGap) / 2.0f - 6.0f;
            float segY = i2y + i2h - this.segBtnH - 5.0f;
            this.boxBtnX = ix + 6.0f;
            this.cornBtnX = this.boxBtnX + this.boxBtnW + segGap;
            this.boxBtnY = segY;

            this.drawSegButton(context, this.boxBtnX, segY, this.boxBtnW, this.segBtnH, "Box",
                    !this.cornersMode, segFont, alpha);
            this.drawSegButton(context, this.cornBtnX, segY, this.boxBtnW, this.segBtnH, "Corners",
                    this.cornersMode, segFont, alpha);
        }

        Font savedFont = Fonts.MEDIUM.getFont(SAVED_FONT_SIZE);
        context.drawText(savedFont, "Saved", ix + 5.0f,
                i2y + i2h + (GAP_23 - savedFont.height()) / 2.0f,
                SAVED_COLOR.withAlpha(255.0f * 0.25f * alpha));

        this.drawInner(context, ix, i3y, iw, INNER3_H, INNER_FILL, INNER_BORDER, alpha);

        float gap  = (iw - SWATCH_PER_ROW * SWATCH_SIZE) / (SWATCH_PER_ROW + 1);
        float gridY = i3y + SWATCH_PAD;
        for (int i = 0; i < SAVED_COLORS.length; ++i) {
            int col = i % SWATCH_PER_ROW;
            int row = i / SWATCH_PER_ROW;
            float sx = ix + gap + col * (SWATCH_SIZE + gap);
            float sy = gridY + row * (SWATCH_SIZE + gap);
            this.swatchX[i] = sx;
            this.swatchY[i] = sy;

            ColorRGBA cur = iface().getAccentColor().getColor();
            ColorRGBA sw  = SAVED_COLORS[i];
            boolean active = Math.abs(sw.getRed() - cur.getRed()) < 2 &&
                             Math.abs(sw.getGreen() - cur.getGreen()) < 2 &&
                             Math.abs(sw.getBlue() - cur.getBlue()) < 2;
            float swAlpha = active ? 1.0f : 0.75f;

            context.drawRoundedRect(sx, sy, SWATCH_SIZE, SWATCH_SIZE,
                    BorderRadius.all(SWATCH_SIZE / 2.0f),
                    SAVED_COLORS[i].withAlpha(SAVED_COLORS[i].getAlpha() * swAlpha * alpha));

            if (active) {
                context.drawRoundedBorder(sx - 1.5f, sy - 1.5f, SWATCH_SIZE + 3.0f, SWATCH_SIZE + 3.0f,
                        0.08f, BorderRadius.all(SWATCH_SIZE / 2.0f + 1.5f),
                        SAVED_COLORS[i].withAlpha(200.0f * alpha));
            }
        }
    }

    private ColorRGBA entryColor(int i) {
        if (i == 0) return Colors.getAccentColor();
        if (i == 4) {
            ColorRGBA a = Colors.getAccentColor();
            return new ColorRGBA(
                    Math.min(255, a.getRed() + 50),
                    Math.min(255, a.getGreen() + 50),
                    Math.min(255, a.getBlue() + 50), 255.0f);
        }
        return ENTRY_FIXED[i];
    }

    private ColorRGBA healthColor(float alpha) {
        if (mc.player == null) {
            return new ColorRGBA(255.0f, 85.0f, 85.0f, 255.0f * alpha);
        }
        float maxHp = Math.max(1.0f, EntityUtility.getSmartMaxHealth(mc.player));
        float pct = Math.max(0.0f, Math.min(1.0f, EntityUtility.getHealth(mc.player) / maxHp));
        if (pct > 0.6f) {
            return new ColorRGBA(160.0f, 220.0f, 45.0f, 255.0f * alpha);
        }
        if (pct > 0.3f) {
            return new ColorRGBA(255.0f, 180.0f, 60.0f, 255.0f * alpha);
        }
        return new ColorRGBA(255.0f, 85.0f, 85.0f, 255.0f * alpha);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) {
            super.onMouseClicked(mouseX, mouseY, button);
            return;
        }
        if (GuiUtility.isHovered(this.gearRectX, this.gearRectY, this.gearRectW, this.gearRectH, mouseX, mouseY)) {
            this.gearDragging = true;
            this.gearDragDX = (float) (mouseX - this.gearRectX);
            this.gearDragDY = (float) (mouseY - this.gearRectY);
            return;
        }
        if (GuiUtility.isHovered(this.nameRectX, this.nameRectY, this.nameRectW, this.nameRectH, mouseX, mouseY)) {
            this.nameDragging = true;
            this.nameDragDX = (float) (mouseX - this.nameRectX);
            this.nameDragDY = (float) (mouseY - this.nameRectY);
            return;
        }
        for (int i = 0; i < SAVED_COLORS.length; i++) {
            if (GuiUtility.isHovered(this.swatchX[i], this.swatchY[i], SWATCH_SIZE, SWATCH_SIZE, mouseX, mouseY)) {
                iface().getAccentColor().setColor(SAVED_COLORS[i]);
                return;
            }
        }
        if (GuiUtility.isHovered(this.boxBtnX, this.boxBtnY, this.boxBtnW, this.segBtnH, mouseX, mouseY)) {
            this.cornersMode = false;
            this.saveCornersMode(true);
            return;
        }
        if (GuiUtility.isHovered(this.cornBtnX, this.boxBtnY, this.boxBtnW, this.segBtnH, mouseX, mouseY)) {
            this.cornersMode = true;
            this.saveCornersMode(true);
            return;
        }
        super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT) {
            if (this.gearDragging) {
                this.gearDragging = false;
                this.saveGearPlacement(true);
                return;
            }
            if (this.nameDragging) {
                this.nameDragging = false;
                this.saveNamePlacement(true);
                return;
            }
        }
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY) {
        if (this.gearDragging || this.nameDragging) return;
        super.onMouseDragged(mouseX, mouseY);
    }

    private float drawGearSlots(UIContext context, net.minecraft.item.ItemStack[] slots, float gx, float gy,
            float gIcon, float gGap, ColorRGBA emptyColor, float alpha, boolean vertical) {
        float prevA = com.mojang.blaze3d.systems.RenderSystem.getShaderColor()[3];
        float cursor = vertical ? gy : gx;
        for (net.minecraft.item.ItemStack g : slots) {
            float px = vertical ? gx : cursor;
            float py = vertical ? cursor : gy;
            if (g == null || g.isEmpty()) {
                float cr = gIcon * 0.7f;
                context.drawIcon(Fonts.ICONS, 'f', px + (gIcon - cr) / 2.0f, py + (gIcon - cr) / 2.0f, cr, emptyColor);
            } else {
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                context.drawItem(g, px, py, gIcon / 16.0f);
            }
            cursor += gIcon + gGap;
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, prevA);
        return cursor;
    }

    private void loadPlayerCardPlacement() {
        if (this.playerCardPlacementLoaded) return;
        this.cornersMode = PlayerCardSettings.cornersMode;
        this.gearSide = PlayerCardSettings.gearSide;
        this.gearAlong = PlayerCardSettings.gearAlong;
        this.nameSide = PlayerCardSettings.nameSide;
        this.nameAlong = PlayerCardSettings.nameAlong;
        this.playerCardPlacementLoaded = true;
    }

    private void saveCornersMode(boolean save) { PlayerCardSettings.cornersMode = this.cornersMode; }

    private void saveGearPlacement(boolean save) {
        PlayerCardSettings.gearSide = this.gearSide;
        PlayerCardSettings.gearAlong = this.gearAlong;
    }

    private void saveNamePlacement(boolean save) {
        PlayerCardSettings.nameSide = this.nameSide;
        PlayerCardSettings.nameAlong = this.nameAlong;
    }

    private void drawCorners(UIContext context, float px, float py, float pw, float ph, ColorRGBA c) {
        float len = Math.min(pw, ph) * 0.25f;
        float th = 0.6f;
        context.drawRect(px, py, len, th, c);
        context.drawRect(px, py, th, len, c);
        context.drawRect(px + pw - len, py, len, th, c);
        context.drawRect(px + pw - th, py, th, len, c);
        context.drawRect(px, py + ph - th, len, th, c);
        context.drawRect(px, py + ph - len, th, len, c);
        context.drawRect(px + pw - len, py + ph - th, len, th, c);
        context.drawRect(px + pw - th, py + ph - len, th, len, c);
    }

    private void drawSegButton(UIContext context, float px, float py, float pw, float ph, String label,
            boolean active, Font font, float alpha) {
        ColorRGBA accent = Colors.getAccentColor();
        ColorRGBA fill = active ? accent.withAlpha(accent.getAlpha() * 0.95f * alpha)
                : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.05f).withAlpha(255.0f * 0.05f * alpha);
        context.drawRoundedRect(px, py, pw, ph, BorderRadius.all(2.0f), fill);
        ColorRGBA textColor = active ? ColorRGBA.BLACK.withAlpha(255.0f * alpha)
                : new ColorRGBA(255.0f, 255.0f, 255.0f, 255.0f * 0.5f).withAlpha(255.0f * 0.5f * alpha);
        context.drawCenteredText(font, label, px + pw / 2.0f, py + (ph - font.height()) / 2.0f, textColor);
    }

    private void drawInner(UIContext context, float px, float py, float pw, float ph,
            ColorRGBA fill, ColorRGBA border, float alpha) {
        BorderRadius r = BorderRadius.all(INNER_RADIUS);
        context.drawRoundedRect(px, py, pw, ph, r, fill.withAlpha(fill.getAlpha() * alpha));
        context.drawRoundedBorder(px, py, pw, ph, BORDER_THICKNESS, r, border.withAlpha(border.getAlpha() * alpha));
    }
}
