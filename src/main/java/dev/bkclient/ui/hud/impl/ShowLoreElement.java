package dev.bkclient.ui.hud.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.modules.modules.visuals.ShowLore;
import dev.bkclient.ui.hud.HudList;
import dev.bkclient.ui.hud.HudTheme;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.gui.GuiUtility;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public class ShowLoreElement extends HudList {
    private static final float MIN_WIDTH = 128.0F;
    private static final float MAX_WIDTH = 190.0F;
    private static final float LINE_HEIGHT = 9.0F;
    private static final float HEADER_HEIGHT = 23.0F;
    private static final float BODY_BOTTOM_PADDING = 6.0F;
    private static final float BODY_MIN_HEIGHT = 28.0F;
    private static final float ICON_SIZE = 16.0F;
    private static final float TEXT_INSET = 29.0F;

    private record DisplayLine(String text, ColorRGBA color) {
    }

    public ShowLoreElement() {
        super("hud.showlore", "icons/hud/enchant.png");
    }

    @Override
    public void update(UIContext context) {
        ItemStack stack = getTargetStack();
        List<DisplayLine> lines = getTargetLoreLines(stack);
        if (stack != null && lines != null && !lines.isEmpty()) {
            Font nameFont = Fonts.MEDIUM.getFont(7.0F);
            Font loreFont = Fonts.REGULAR.getFont(6.0F);
            float textWidth = nameFont.width(readableText(stack.getName()));
            for (DisplayLine line : lines) textWidth = Math.max(textWidth, loreFont.width(line.text()));
            // Keep one stable icon column and leave enough room for the 16x16 item
            // renderer. The old 16px body clipped the icon and pushed the text out
            // of the dark surface whenever a lore line was present.
            this.width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, 44.0F + textWidth));
            float bodyHeight = Math.max(BODY_MIN_HEIGHT, 12.0F + (lines.size() + 1.0F) * LINE_HEIGHT);
            this.height = HEADER_HEIGHT + bodyHeight + BODY_BOTTOM_PADDING;
        } else {
            this.width = MIN_WIDTH;
            this.height = 50.0F;
        }
        super.update(context);
    }

    @Override
    protected void renderContent(UIContext context) {
        super.renderContent(context);
        ItemStack stack = getTargetStack();
        List<DisplayLine> lines = getTargetLoreLines(stack);
        float rowX = this.x + 6.0F;
        float rowY = this.y + HEADER_HEIGHT;
        float rowW = this.width - 12.0F;
        float rowH = this.height - HEADER_HEIGHT - BODY_BOTTOM_PADDING;
        context.drawRoundedRect(rowX, rowY, rowW, rowH, BorderRadius.all(4.0F), HudTheme.surfaceColor().withAlpha(110));

        if (stack == null || lines == null || lines.isEmpty()) {
            Font font = Fonts.REGULAR.getFont(6.0F);
            context.drawText(font, "Target an item with matching lore", rowX + 7.0F,
                rowY + GuiUtility.getMiddleOfBox(font.height(), rowH), HudTheme.textColor().withAlpha(150));
            return;
        }

        // The icon has its own fixed 20px slot, so it stays completely inside the
        // inner surface while the two text lines share the same left edge.
        context.drawItem(stack, rowX + 6.0F, rowY + (rowH - ICON_SIZE) / 2.0F, 1.0F);
        Font nameFont = Fonts.MEDIUM.getFont(7.0F);
        Font loreFont = Fonts.REGULAR.getFont(6.0F);
        float textX = rowX + TEXT_INSET;
        context.drawFadeoutText(nameFont, readableText(stack.getName()), textX, rowY + 5.0F,
            colorOf(stack.getName(), HudTheme.textColor()), 0.82F, 1.0F, rowW - 28.0F);
        float lineY = rowY + 15.0F;
        for (DisplayLine line : lines) {
            context.drawFadeoutText(loreFont, line.text(), textX, lineY, line.color(), 0.82F, 1.0F, rowW - 28.0F);
            lineY += LINE_HEIGHT;
        }
    }

    private ItemStack getTargetStack() {
        Entity selected = BKClientInit.getInstance().getTargetManager().getCurrentTarget();
        LivingEntity target = selected instanceof LivingEntity living ? living
            : mc.targetedEntity instanceof LivingEntity living ? living
            : mc.currentScreen instanceof ChatScreen ? mc.player : null;
        if (target == null) return null;
        ItemStack stack = target.getMainHandStack();
        return stack.isEmpty() ? target.getOffHandStack() : stack;
    }

    private List<DisplayLine> getTargetLoreLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return null;
        ShowLore module = BKClientInit.getInstance().getModuleManager().getModule(ShowLore.class);
        if (module.fullLore.isEnabled()) {
            return lore.lines().stream().map(this::displayLine).filter(line -> !line.text().isBlank()).toList();
        }
        String keyword = normalize(module.getKeyword().getText());
        if (keyword.isEmpty()) return null;
        for (Text line : lore.lines()) {
            if (normalize(line.getString()).contains(keyword)) return List.of(displayLine(line));
        }
        return normalize(stack.getName().getString()).contains(keyword) ? List.of(displayLine(stack.getName())) : null;
    }

    private DisplayLine displayLine(Text text) {
        return new DisplayLine(readableText(text), colorOf(text, HudTheme.textColor()));
    }

    private String readableText(Text text) {
        return text == null ? "" : text.getString()
            .replace('ą', 'a').replace('Ą', 'A').replace('ć', 'c').replace('Ć', 'C')
            .replace('ę', 'e').replace('Ę', 'E').replace('ł', 'l').replace('Ł', 'L')
            .replace('ń', 'n').replace('Ń', 'N').replace('ó', 'o').replace('Ó', 'O')
            .replace('ś', 's').replace('Ś', 'S').replace('ź', 'z').replace('Ź', 'Z')
            .replace('ż', 'z').replace('Ż', 'Z');
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private ColorRGBA colorOf(Text text, ColorRGBA fallback) {
        if (text == null) return fallback;
        final int[] rgb = {-1};
        text.visit((style, value) -> {
            if (rgb[0] < 0 && value != null && !value.isBlank() && style.getColor() != null) rgb[0] = style.getColor().getRgb();
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return rgb[0] < 0 ? fallback : ColorRGBA.fromInt(0xFF000000 | rgb[0]);
    }

    @Override
    public boolean show() {
        ShowLore module = BKClientInit.getInstance().getModuleManager().getModule(ShowLore.class);
        List<DisplayLine> lines = getTargetLoreLines(getTargetStack());
        return module.isEnabled() && ((lines != null && !lines.isEmpty()) || mc.currentScreen instanceof ChatScreen);
    }
}
