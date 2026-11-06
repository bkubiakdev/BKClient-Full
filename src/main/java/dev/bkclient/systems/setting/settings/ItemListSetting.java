package dev.bkclient.systems.setting.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import dev.bkclient.systems.setting.SettingsContainer;
import dev.bkclient.systems.setting.impl.AbstractSetting;
import dev.bkclient.systems.setting.settings.rynek.PriceEntry;

public class ItemListSetting extends AbstractSetting {
    private final List<PriceEntry> value = new ArrayList<>();
    private Runnable onChanged;

    public ItemListSetting(SettingsContainer container, String name) {
        super(container, name);
    }

    public ItemListSetting onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    public List<PriceEntry> getValue() {
        return value;
    }

    @Override
    public JsonElement save() {
        JsonArray array = new JsonArray();
        for (PriceEntry entry : value) {
            JsonObject obj = new JsonObject();
            if (entry.getName() != null)
                obj.addProperty("name", entry.getName());
            if (entry.getMaterial() != null)
                obj.addProperty("material", entry.getMaterial());
            if (entry.getLore() != null)
                obj.addProperty("lore", entry.getLore());
            if (!entry.getBannedLore().isBlank())
                obj.addProperty("bannedLore", entry.getBannedLore());
            if (entry.getEnchants() != null)
                obj.addProperty("enchants", entry.getEnchants());
            if (entry.getExcludedEnchants() != null)
                obj.addProperty("excludedEnchants", entry.getExcludedEnchants());

            obj.addProperty("maxPrice", entry.getMaxPrice());
            obj.addProperty("sellPrice", entry.getSellPrice());
            obj.addProperty("minBuyCount", entry.getMinBuyCount());
            obj.addProperty("minSellCount", entry.getMinSellCount());
            obj.addProperty("buyEnabled", entry.isBuyEnabled());
            obj.addProperty("sellEnabled", entry.isSellEnabled());
            obj.addProperty("unitMode", entry.isUnitMode());

            if (entry.getCustomModelData() != null)
                obj.addProperty("customModelData", entry.getCustomModelData());
            if (entry.isIgnoreCustomModelData())
                obj.addProperty("ignoreCustomModelData", true);
            if (entry.getMinDaysLeft() != null)
                obj.addProperty("minDaysLeft", entry.getMinDaysLeft());
            if (entry.getDyeColor() != null)
                obj.addProperty("dyeColor", entry.getDyeColor());
            if (entry.getHeadTextureValue() != null)
                obj.addProperty("headTextureValue", entry.getHeadTextureValue());

            if (!entry.getEnchantments().isEmpty()) {
                JsonObject enchantsObj = new JsonObject();
                entry.getEnchantments().forEach(enchantsObj::addProperty);
                obj.add("enchantments", enchantsObj);
            }

            if (!entry.getBannedEnchantsList().isEmpty()) {
                JsonArray bannedArray = new JsonArray();
                for (String ench : entry.getBannedEnchantsList())
                    bannedArray.add(ench);
                obj.add("bannedEnchantsList", bannedArray);
            }

            if (entry.hasPotionEffects()) {
                JsonArray potionArray = new JsonArray();
                for (PriceEntry.PotionEffectEntry fx : entry.getPotionEffects()) {
                    JsonObject fxObj = new JsonObject();
                    fxObj.addProperty("effectId", fx.getEffectId());
                    fxObj.addProperty("amplifier", fx.getAmplifier());
                    fxObj.addProperty("duration", fx.getDuration());
                    potionArray.add(fxObj);
                }
                obj.add("potionEffects", potionArray);
            }

            array.add(obj);
        }
        return array;
    }

    @Override
    public void load(JsonElement element) {
        value.clear();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement el : array) {
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    PriceEntry entry = new PriceEntry();

                    if (obj.has("name"))
                        entry.setName(obj.get("name").getAsString());
                    if (obj.has("material"))
                        entry.setMaterial(obj.get("material").getAsString());
                    if (obj.has("lore"))
                        entry.setLore(obj.get("lore").getAsString());
                    if (obj.has("bannedLore") && !obj.get("bannedLore").isJsonNull())
                        entry.setBannedLore(obj.get("bannedLore").getAsString());
                    if (obj.has("enchants"))
                        entry.setEnchants(obj.get("enchants").getAsString());
                    if (obj.has("excludedEnchants"))
                        entry.setExcludedEnchants(obj.get("excludedEnchants").getAsString());

                    if (obj.has("maxPrice"))
                        entry.setMaxPrice(obj.get("maxPrice").getAsDouble());
                    if (obj.has("sellPrice"))
                        entry.setSellPrice(obj.get("sellPrice").getAsDouble());
                    if (obj.has("minBuyCount"))
                        entry.setMinBuyCount(obj.get("minBuyCount").getAsInt());
                    else if (obj.has("requiredCount"))
                        entry.setMinBuyCount(obj.get("requiredCount").getAsInt());
                    if (obj.has("minSellCount"))
                        entry.setMinSellCount(obj.get("minSellCount").getAsInt());
                    else if (obj.has("sellCount"))
                        entry.setMinSellCount(obj.get("sellCount").getAsInt());
                    if (obj.has("buyEnabled"))
                        entry.setBuyEnabled(obj.get("buyEnabled").getAsBoolean());
                    if (obj.has("sellEnabled"))
                        entry.setSellEnabled(obj.get("sellEnabled").getAsBoolean());
                    if (obj.has("unitMode"))
                        entry.setUnitMode(obj.get("unitMode").getAsBoolean());

                    // Backward compatibility (old names from my previous quick implement)
                    if (obj.has("buy") && !obj.has("buyEnabled"))
                        entry.setBuyEnabled(obj.get("buy").getAsBoolean());
                    if (obj.has("sell") && !obj.has("sellEnabled"))
                        entry.setSellEnabled(obj.get("sell").getAsBoolean());

                    if (obj.has("customModelData"))
                        entry.setCustomModelData(obj.get("customModelData").getAsInt());
                    if (obj.has("ignoreCustomModelData"))
                        entry.setIgnoreCustomModelData(obj.get("ignoreCustomModelData").getAsBoolean());
                    if (obj.has("minDaysLeft"))
                        entry.setMinDaysLeft(obj.get("minDaysLeft").getAsInt());
                    if (obj.has("dyeColor"))
                        entry.setDyeColor(obj.get("dyeColor").getAsInt());
                    if (obj.has("headTextureValue"))
                        entry.setHeadTextureValue(obj.get("headTextureValue").getAsString());

                    if (obj.has("enchantments") && !isBlockMaterial(entry.getMaterial())) {
                        JsonObject enchantsObj = obj.getAsJsonObject("enchantments");
                        enchantsObj.entrySet()
                                .forEach(e -> entry.getEnchantments().put(e.getKey(), e.getValue().getAsInt()));
                    }
                    if (obj.has("bannedEnchantsList")) {
                        JsonArray bannedArray = obj.getAsJsonArray("bannedEnchantsList");
                        for (JsonElement ench : bannedArray)
                            entry.getBannedEnchantsList().add(ench.getAsString());
                    }

                    if (obj.has("potionEffects")) {
                        JsonArray potionArray = obj.getAsJsonArray("potionEffects");
                        java.util.List<PriceEntry.PotionEffectEntry> effects = new java.util.ArrayList<>();
                        for (JsonElement fxEl : potionArray) {
                            JsonObject fxObj = fxEl.getAsJsonObject();
                            PriceEntry.PotionEffectEntry fx = new PriceEntry.PotionEffectEntry();
                            if (fxObj.has("effectId")) fx.setEffectId(fxObj.get("effectId").getAsString());
                            if (fxObj.has("amplifier")) fx.setAmplifier(fxObj.get("amplifier").getAsInt());
                            if (fxObj.has("duration")) fx.setDuration(fxObj.get("duration").getAsInt());
                            effects.add(fx);
                        }
                        entry.setPotionEffects(effects);
                    }

                    value.add(entry);
                }
            }
        }
    }

    private boolean isBlockMaterial(String material) {
        if (material == null || material.isBlank()) {
            return false;
        }
        String id = material.toLowerCase();
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        try {
            net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM
                    .get(net.minecraft.util.Identifier.of(id));
            return item instanceof net.minecraft.item.BlockItem;
        } catch (Exception ignored) {
            return false;
        }
    }
}


