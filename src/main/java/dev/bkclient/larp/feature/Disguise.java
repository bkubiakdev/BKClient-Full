package dev.bkclient.larp.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registries;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.DataComponentTypes;
import dev.bkclient.larp.feature.RubiksCube;

public final class Disguise {
    private static final java.util.Set<ItemStack> PREVIEWS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final Map<String, Spec> SPECS = new LinkedHashMap<String, Spec>();
    private static final Map<Look, ItemStack> LOOKS = new HashMap<Look, ItemStack>();
    private static final Map<Spec, ItemStack> MODEL_OVERRIDE = new HashMap<Spec, ItemStack>();

    private Disguise() {
    }

    private static String key(ItemStack stack) {
        ArrayList<String> parts = new ArrayList<>();
        for (Map.Entry entry : stack.getComponentChanges().entrySet()) {
            if (entry.getKey() == DataComponentTypes.DAMAGE) continue;
            parts.add(String.valueOf(entry.getKey()) + "=" + String.valueOf(((Optional)entry.getValue()).orElse(null)));
        }
        Collections.sort(parts);
        StringBuilder id = new StringBuilder(Registries.ITEM.getId(stack.getItem()).toString());
        for (String string : parts) {
            id.append('|').append(string);
        }
        return id.toString();
    }

    public static String fingerprint(ItemStack stack) {
        return Disguise.key(stack);
    }

    public static void modelOverride(Spec spec, ItemStack look) {
        MODEL_OVERRIDE.put(spec, look);
    }

    public static void put(String fingerprint, Spec spec) {
        SPECS.put(fingerprint, spec);
    }

    public static void remove(String fingerprint) {
        SPECS.remove(fingerprint);
    }

    public static boolean isVisualStack(ItemStack stack) { return PREVIEWS.contains(stack) || LOOKS.containsValue(stack) || MODEL_OVERRIDE.containsValue(stack); }
    public static boolean matches(String key, Spec spec) { return spec.equals(SPECS.get(key)); }
    public static void clear() {
        SPECS.clear();
    }

    public static Spec specFor(ItemStack stack) {
        if (!dev.bkclient.larp.LarpRuntime.active() || SPECS.isEmpty() || stack == null || stack.isEmpty()) {
            return null;
        }
        return SPECS.get(Disguise.key(stack));
    }

    public static boolean isDisguised(ItemStack stack) {
        return Disguise.specFor(stack) != null;
    }

    public static ItemStack model(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        if (spec == null) {
            return stack;
        }
        ItemStack ready = MODEL_OVERRIDE.get(spec);
        if (ready != null) {
            return ready;
        }
        Look look = new Look(spec.modelItem(), spec.customModelData(), spec.glint());
        return LOOKS.computeIfAbsent(look, Disguise::buildLook);
    }

    public static ItemStack preview(Spec spec) {
        ItemStack item = MODEL_OVERRIDE.containsKey(spec) ? MODEL_OVERRIDE.get(spec).copy() : buildLook(new Look(spec.modelItem(), spec.customModelData(), spec.glint()));
        item.set(DataComponentTypes.CUSTOM_NAME, spec.name());
        PREVIEWS.add(item);
        return item;
    }

    private static ItemStack buildLook(Look look) {
        ItemStack stack = new ItemStack((ItemConvertible)look.modelItem());
        if (look.customModelData() != null) {
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(look.customModelData()), List.of(), List.of(), List.of()));
        }
        if (look.glint()) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    public static Text name(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        return spec == null ? null : spec.name();
    }

    public static List<Text> lore(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        return spec == null || spec.lore() == null ? List.of() : spec.lore();
    }

    public static boolean hidesAttributes(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        return spec != null && spec.hideAttributes();
    }

    public static boolean hidesDurability(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        return spec != null && spec.hideDurability();
    }

    public static Ability abilityOf(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        return spec == null ? Ability.NONE : spec.ability();
    }

    public static boolean hasAbility(ItemStack stack, Ability ability) {
        return dev.bkclient.larp.LarpRuntime.abilities() && Disguise.abilityOf(stack) == ability;
    }

    public static Consumable consumableOf(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        return !dev.bkclient.larp.LarpRuntime.abilities() || spec == null ? null : spec.consumable();
    }

    public static int displayCount(ItemStack stack) {
        Spec spec = Disguise.specFor(stack);
        if (spec == null) {
            return 0;
        }
        if (spec.ability() == Ability.RUBIKS_CUBE) {
            return RubiksCube.shownCount(stack);
        }
        return spec.count();
    }

    public static boolean blocksPlacing(ItemStack stack, World world, BlockPos clicked) {
        if (!(stack.getItem() instanceof BlockItem) || !Disguise.isDisguised(stack)) {
            return false;
        }
        BlockState state = world.getBlockState(clicked);
        if (state.hasBlockEntity()) {
            return false;
        }
        return !state.isIn(BlockTags.DOORS) && !state.isIn(BlockTags.TRAPDOORS) && !state.isIn(BlockTags.FENCE_GATES) && !state.isIn(BlockTags.BUTTONS);
    }

    public static String realItemId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    public record Spec(Item modelItem, Text name, List<Text> lore, Float customModelData, boolean glint, boolean hideAttributes, boolean hideDurability, Ability ability, Consumable consumable, int count) {
    }

    private record Look(Item modelItem, Float customModelData, boolean glint) {
    }

    public static enum Ability {
        NONE,
        HYDRO_CAGE,
        ILLUSIONIST_WAND,
        PHANTOM_BLOCK,
        MAGIC_LANTERN,
        GRINCH_LIGHTNING,
        DROP_POUCH,
        GUARDIAN_ROD,
        LOLLIPOP,
        FLIGHTLESS_ROD,
        POISON_PENCIL,
        CUPID_ROSE,
        DRACULA_BACKPACK,
        SCYTHE,
        GRINCH_SHOVEL,
        DIVINE_AXE,
        ARCUS_MAGNUS,
        RUBIKS_CUBE,
        RABBIT_SWORD;

    }

    public record Consumable(int cooldownTicks, List<Effect> effects) {
    }

    public record Effect(RegistryEntry<StatusEffect> effect, int duration, int amplifier) {
    }
}
