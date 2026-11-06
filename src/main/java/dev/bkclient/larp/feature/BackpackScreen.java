package dev.bkclient.larp.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.gui.screen.Screen;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.LegacyText;
import dev.bkclient.larp.feature.Titles;

public class BackpackScreen
extends Screen {
    private static final Identifier TEXTURE = Identifier.ofVanilla((String)"textures/gui/container/hopper.png");
    private static final int BACKGROUND_W = 176;
    private static final int BACKGROUND_H = 133;
    private static final int SLOTS = 5;
    private static final int SLOT = 18;
    private static final int ICON = 16;
    private static final int BACKPACK_X = 44;
    private static final int BACKPACK_Y = 20;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 51;
    private static final int HOTBAR_Y = 109;
    private static final Map<String, List<ItemStack>> CONTENTS = new HashMap<String, List<ItemStack>>();
    private final List<ItemStack> slots;
    private ItemStack cursor = ItemStack.EMPTY;
    private int left;
    private int top;

    private BackpackScreen(List<ItemStack> slots) {
        super(LegacyText.parse("&8Plecak Drakuli"));
        this.slots = slots;
    }

    public static void open(ItemStack stack) {
        List slots = CONTENTS.computeIfAbsent(Disguise.fingerprint(stack), key -> {
            ArrayList<ItemStack> empty = new ArrayList<ItemStack>(5);
            for (int i = 0; i < 5; ++i) {
                empty.add(ItemStack.EMPTY);
            }
            return empty;
        });
        Titles.playSound(SoundEvents.BLOCK_BARREL_OPEN, 0.45f, 1.15f);
        MinecraftClient.getInstance().setScreen((Screen)new BackpackScreen(slots));
    }

    protected void init() {
        this.left = (this.width - 176) / 2;
        this.top = (this.height - 133) / 2;
    }

    private boolean isOver(int x, int y, double mouseX, double mouseY) {
        return mouseX >= (double)x && mouseX < (double)(x + 16) && mouseY >= (double)y && mouseY < (double)(y + 16);
    }

    private int backpackX(int index) {
        return this.left + 44 + index * 18;
    }

    private int backpackY() {
        return this.top + 20;
    }

    private int inventoryX(int index) {
        return this.left + 8 + index % 9 * 18;
    }

    private int inventoryY(int index) {
        return index < 27 ? this.top + 51 + index / 9 * 18 : this.top + 109;
    }

    private static int inventorySlot(int index) {
        return index < 27 ? index + 9 : index - 27;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, this.left, this.top, 0.0f, 0.0f, 176, 133, 256, 256);
        context.drawText(this.textRenderer, this.title, this.left + 8, this.top + 6, 0x404040, false);
        context.drawText(this.textRenderer, (Text)Text.translatable((String)"container.inventory"), this.left + 8, this.top + 133 - 94, 0x404040, false);
        this.drawBackpack(context, mouseX, mouseY);
        this.drawPlayerInventory(context, mouseX, mouseY);
        this.drawHoverTooltip(context, mouseX, mouseY);
        if (!this.cursor.isEmpty()) {
            context.drawItem(this.cursor, mouseX - 8, mouseY - 8);
            context.drawStackOverlay(this.textRenderer, this.cursor, mouseX - 8, mouseY - 8);
        }
    }

    private void drawBackpack(DrawContext context, int mouseX, int mouseY) {
        for (int i = 0; i < 5; ++i) {
            ItemStack stack;
            int y;
            int x = this.backpackX(i);
            if (this.isOver(x, y = this.backpackY(), mouseX, mouseY)) {
                context.fill(x, y, x + 16, y + 16, -2130706433);
            }
            if ((stack = this.slots.get(i)).isEmpty()) continue;
            context.drawItem(stack, x, y);
            context.drawStackOverlay(this.textRenderer, stack, x, y);
        }
    }

    private void drawPlayerInventory(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        PlayerInventory inventory = client.player.getInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack;
            int y;
            int x = this.inventoryX(i);
            if (this.isOver(x, y = this.inventoryY(i), mouseX, mouseY)) {
                context.fill(x, y, x + 16, y + 16, -2130706433);
            }
            if ((stack = inventory.getStack(BackpackScreen.inventorySlot(i))).isEmpty()) continue;
            context.drawItem(stack, x, y);
            context.drawStackOverlay(this.textRenderer, stack, x, y);
        }
    }

    private void drawHoverTooltip(DrawContext context, int mouseX, int mouseY) {
        if (!this.cursor.isEmpty()) {
            return;
        }
        ItemStack hovered = this.stackUnder(mouseX, mouseY);
        if (hovered != null && !hovered.isEmpty()) {
            context.drawItemTooltip(this.textRenderer, hovered, mouseX, mouseY);
        }
    }

    private ItemStack stackUnder(double mouseX, double mouseY) {
        for (int i = 0; i < 5; ++i) {
            if (!this.isOver(this.backpackX(i), this.backpackY(), mouseX, mouseY)) continue;
            return this.slots.get(i);
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }
        for (int i = 0; i < 36; ++i) {
            if (!this.isOver(this.inventoryX(i), this.inventoryY(i), mouseX, mouseY)) continue;
            return client.player.getInventory().getStack(BackpackScreen.inventorySlot(i));
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 5; ++i) {
            if (!this.isOver(this.backpackX(i), this.backpackY(), mouseX, mouseY)) continue;
            ItemStack inSlot = this.slots.get(i);
            this.slots.set(i, this.cursor);
            this.cursor = inSlot;
            Titles.playSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.4f);
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (int i = 0; i < 36; ++i) {
            if (!this.isOver(this.inventoryX(i), this.inventoryY(i), mouseX, mouseY)) continue;
            this.cursor = this.cursor.isEmpty() ? client.player.getInventory().getStack(BackpackScreen.inventorySlot(i)).copy() : ItemStack.EMPTY;
            Titles.playSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.4f);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void close() {
        this.cursor = ItemStack.EMPTY;
        Titles.playSound(SoundEvents.BLOCK_BARREL_CLOSE, 0.45f, 1.0f);
        super.close();
    }

    public boolean shouldPause() {
        return false;
    }
    public static void resetSession(net.minecraft.client.MinecraftClient client) { CONTENTS.clear(); }
}
