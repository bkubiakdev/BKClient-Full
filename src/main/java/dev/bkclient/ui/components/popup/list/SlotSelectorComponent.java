package dev.bkclient.ui.components.popup.list;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.systems.modules.modules.player.MineHelper;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.framework.base.CustomComponent;

public class SlotSelectorComponent extends CustomComponent {
    private final MineHelper module;
    private final float slotSize = 16.0f;
    private final float gap = 2.0f;

    public SlotSelectorComponent(MineHelper module) {
        this.module = module;
        this.height = (slotSize + gap) * 4 + 4.0f; // 4 rows
    }

    @Override
    protected void renderComponent(UIContext context) {
        if (dev.bkclient.utility.interfaces.IMinecraft.mc.player == null)
            return;

        float startX = this.x + (this.width - (9 * (slotSize + gap) - gap)) / 2.0f;
        float startY = this.y + 2.0f;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int index = getSlotIndex(row, col);
                float slotX = startX + col * (slotSize + gap);
                float slotY = startY + row * (slotSize + gap);

                int state = module.slotStates[index];
                boolean hovered = GuiUtility.isHovered(slotX, slotY, slotSize, slotSize, context.getMouseX(),
                        context.getMouseY());

                // Selection Colors - MATCHING THE BEAUTIFUL STYLE
                int fillColor = 0;
                int borderColor = 0;

                if (state == 1) { // Green
                    fillColor = new ColorRGBA(0, 255, 0, 40).getRGB();
                    borderColor = new ColorRGBA(0, 255, 0, 200).getRGB();
                } else if (state == 2) { // Blue
                    fillColor = new ColorRGBA(0, 150, 255, 40).getRGB();
                    borderColor = new ColorRGBA(0, 150, 255, 200).getRGB();
                } else {
                    fillColor = Colors.getBackgroundColor().withAlpha(60.0f).getRGB();
                    borderColor = Colors.getOutlineColor().withAlpha(30.0f).getRGB();
                }

                var theme=dev.bkclient.ui.menu.clickgui.ClickGuiV2Theme.current();
                if(state==0 && theme!=null && !theme.liquid){
                    fillColor=theme.field.getRGB();
                    borderColor=theme.border.getRGB();
                }
                if (hovered) {
                    borderColor = Colors.getTextColor().withAlpha(100.0f).getRGB();
                }

                dev.bkclient.ui.components.SlotHighlight.draw(context, (int) slotX, (int) slotY, fillColor, borderColor);

                net.minecraft.item.ItemStack stack = dev.bkclient.utility.interfaces.IMinecraft.mc.player
                        .getInventory().getStack(index);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, slotX, slotY, 1.0f);
                }
            }
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT)
            return;

        float startX = this.x + (this.width - (9 * (slotSize + gap) - gap)) / 2.0f;
        float startY = this.y + 2.0f;

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int index = getSlotIndex(row, col);
                float slotX = startX + col * (slotSize + gap);
                float slotY = startY + row * (slotSize + gap);

                if (GuiUtility.isHovered(slotX, slotY, slotSize, slotSize, mouseX, mouseY)) {
                    module.slotStates[index] = (module.slotStates[index] + 1) % 3;
                    dev.bkclient.systems.setting.impl.AbstractSetting.saveConfig();
                    return;
                }
            }
        }
    }

    private int getSlotIndex(int row, int col) {
        if (row == 3)
            return col; // Hotbar 0-8
        return (row + 1) * 9 + col; // Rows 0-2 -> 9-35
    }

    @Override
    public float getHeight() {
        return (slotSize + gap) * 4 + 4.0f;
    }
}
