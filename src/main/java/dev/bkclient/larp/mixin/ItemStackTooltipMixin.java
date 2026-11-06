package dev.bkclient.larp.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.text.TextContent;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.bkclient.larp.feature.Disguise;

@Mixin(value={ItemStack.class})
public class ItemStackTooltipMixin {
    private static final String COMPONENT_COUNT_KEY = "item.components";

    @Inject(method={"appendTooltip(Lnet/minecraft/component/ComponentType;Lnet/minecraft/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/item/tooltip/TooltipType;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$dropRealEnchantsAndLore(ComponentType<?> type, Item.TooltipContext context, Consumer<Text> consumer, TooltipType tooltipType, CallbackInfo ci) {
        if (!Disguise.isDisguised((ItemStack)(Object)this)) {
            return;
        }
        if (type == DataComponentTypes.ENCHANTMENTS || type == DataComponentTypes.STORED_ENCHANTMENTS || type == DataComponentTypes.LORE) {
            ci.cancel();
        }
    }

    @Inject(method={"appendAttributeModifiersTooltip"}, at={@At(value="HEAD")}, cancellable=true)
    private void easyeventowka$dropAttributes(Consumer<Text> consumer, PlayerEntity player, CallbackInfo ci) {
        if (Disguise.hidesAttributes((ItemStack)(Object)this)) {
            ci.cancel();
        }
    }

    @Inject(method={"getTooltip"}, at={@At(value="RETURN")}, cancellable=true)
    private void easyeventowka$disguiseTooltip(CallbackInfoReturnable<List<Text>> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (!Disguise.isDisguised(self)) {
            return;
        }
        List lines = (List)cir.getReturnValue();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        String realId = Disguise.realItemId(self);
        ArrayList<Text> result = new ArrayList<Text>();
        for (int i = 0; i < lines.size(); ++i) {
            Text line = (Text)lines.get(i);
            if (realId.equals(line.getString()) || ItemStackTooltipMixin.isTranslatable(line, COMPONENT_COUNT_KEY)) continue;
            result.add(i == 0 ? Disguise.name(self) : line);
            if (i != 0) continue;
            result.addAll(Disguise.lore(self));
        }
        cir.setReturnValue(result);
    }

    private static boolean isTranslatable(Text line, String key) {
        TranslatableTextContent translatable;
        TextContent class_74172 = line.getContent();
        return class_74172 instanceof TranslatableTextContent && key.equals((translatable = (TranslatableTextContent)class_74172).getKey());
    }
}
