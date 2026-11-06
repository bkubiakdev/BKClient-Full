package dev.bkclient.larp.feature;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.screen.Screen;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public class PouchScreen
extends Screen {
    private static final Identifier TEXTURE = Identifier.ofVanilla((String)"textures/gui/container/generic_54.png");
    private static final int ROWS = 6;
    private static final int BACKGROUND_W = 176;
    private static final int BACKGROUND_H = 222;
    private static final int PAYOUT_SLOT = 49;
    private static final int EXIT_SLOT = 53;
    private static final int SLOT = 18;
    private static final int ICON = 16;
    private final Button payout = new Button(49, new ItemStack((ItemConvertible)Items.LIME_DYE), List.of(LegacyText.parse("&aWyp\u0142a\u0107 wszystko"), LegacyText.parse("&8 \u00bb &7Kliknij, aby odebra\u0107 przedmioty!")));
    private final Button exit = new Button(53, new ItemStack((ItemConvertible)Items.BARRIER), List.of(LegacyText.parse("&cWyj\u015bcie"), LegacyText.parse("&8 \u00bb &7Kliknij, aby zamkn\u0105\u0107 sakiewk\u0119!")));
    private int left;
    private int top;
    private boolean playCloseSound = true;

    public PouchScreen() {
        super(LegacyText.parse("&8Sakiewka dropu"));
    }

    public static void open() {
        Titles.playSound(SoundEvents.BLOCK_BARREL_OPEN, 0.5f, 1.2f);
        MinecraftClient.getInstance().setScreen((Screen)new PouchScreen());
    }

    protected void init() {
        this.left = (this.width - 176) / 2;
        this.top = (this.height - 222) / 2;
    }

    private int slotX(int slot) {
        return this.left + 8 + slot % 9 * 18;
    }

    private int slotY(int slot) {
        return this.top + 18 + slot / 9 * 18;
    }

    private boolean isOver(int x, int y, double mouseX, double mouseY) {
        return mouseX >= (double)x && mouseX < (double)(x + 16) && mouseY >= (double)y && mouseY < (double)(y + 16);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int chestPart = 125;
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, this.left, this.top, 0.0f, 0.0f, 176, chestPart, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, this.left, this.top + chestPart, 0.0f, 126.0f, 176, 96, 256, 256);
        context.drawText(this.textRenderer, this.title, this.left + 8, this.top + 6, 0x404040, false);
        context.drawText(this.textRenderer, (Text)Text.translatable((String)"container.inventory"), this.left + 8, this.top + 222 - 94, 0x404040, false);
        this.drawPlayerInventory(context);
        this.drawButton(context, this.payout, mouseX, mouseY);
        this.drawButton(context, this.exit, mouseX, mouseY);
        this.drawTooltipFor(context, this.payout, mouseX, mouseY);
        this.drawTooltipFor(context, this.exit, mouseX, mouseY);
    }

    private void drawPlayerInventory(DrawContext context) {
        ItemStack stack;
        int i;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        PlayerInventory inventory = client.player.getInventory();
        int mainTop = this.top + 139;
        int hotbarTop = this.top + 197;
        for (i = 0; i < 27; ++i) {
            stack = inventory.getStack(i + 9);
            if (stack.isEmpty()) continue;
            context.drawItem(stack, this.left + 8 + i % 9 * 18, mainTop + i / 9 * 18);
        }
        for (i = 0; i < 9; ++i) {
            stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            context.drawItem(stack, this.left + 8 + i * 18, hotbarTop);
        }
    }

    private void drawButton(DrawContext context, Button button, int mouseX, int mouseY) {
        int y;
        int x = this.slotX(button.slot());
        if (this.isOver(x, y = this.slotY(button.slot()), mouseX, mouseY)) {
            context.fill(x, y, x + 16, y + 16, -2130706433);
        }
        context.drawItem(button.icon(), x, y);
    }

    private void drawTooltipFor(DrawContext context, Button button, int mouseX, int mouseY) {
        if (!this.isOver(this.slotX(button.slot()), this.slotY(button.slot()), mouseX, mouseY)) {
            return;
        }
        context.drawTooltip(this.textRenderer, new ArrayList<Text>(button.tooltip()), mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isOver(this.slotX(53), this.slotY(53), mouseX, mouseY)) {
            this.close();
            return true;
        }
        if (this.isOver(this.slotX(49), this.slotY(49), mouseX, mouseY)) {
            this.playCloseSound = false;
            Titles.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
            Titles.show(LegacyText.parse("&a&lSAKIEWKA DROPU"), LegacyText.parse("&7Odebra\u0142e\u015b wszystkie dropy do &fEQ&7!"));
            this.close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void close() {
        if (this.playCloseSound) {
            Titles.playSound(SoundEvents.BLOCK_BARREL_CLOSE, 0.5f, 1.2f);
        }
        super.close();
    }

    public boolean shouldPause() {
        return false;
    }

    private record Button(int slot, ItemStack icon, List<Text> tooltip) {
    }
}
