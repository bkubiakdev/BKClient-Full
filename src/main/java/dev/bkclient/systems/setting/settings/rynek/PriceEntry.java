package dev.bkclient.systems.setting.settings.rynek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PriceEntry {

    public static class PotionEffectEntry {
        private String effectId;
        private int amplifier;
        private int duration; // in ticks

        public PotionEffectEntry() {}

        public PotionEffectEntry(String effectId, int amplifier, int duration) {
            this.effectId = effectId;
            this.amplifier = amplifier;
            this.duration = duration;
        }

        public String getEffectId() { return effectId; }
        public void setEffectId(String effectId) { this.effectId = effectId; }
        public int getAmplifier() { return amplifier; }
        public void setAmplifier(int amplifier) { this.amplifier = amplifier; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }

    private String name;
    private String material;
    private String lore;
    private String bannedLore = "";
    private String enchants;
    private String excludedEnchants;

    private Map<String, Integer> enchantments = new HashMap<>();
    private List<String> bannedEnchantsList = new ArrayList<>();

    private double maxPrice = 0;
    private double sellPrice = 0;
    private int requiredCount = 1;
    private int sellCount = 1;
    private int minBuyCount = 1;
    private int minSellCount = 1;
    private boolean buyEnabled = true;
    private boolean sellEnabled = false;
    private boolean unitMode = false;
    private Integer customModelData;
    private boolean ignoreCustomModelData = false; // gdy true: matchowanie pomija CustomModelData (kostiumy/pety z r\u00F3\u017Cnym CMD per egzemplarz)
    private Integer minDaysLeft;
    private Integer dyeColor;
    private String headTextureValue; // base64 texture for player_head icons
    private List<PotionEffectEntry> potionEffects;
    private transient net.minecraft.item.ItemStack cachedIconStack;


    public PriceEntry() {
    }

    public PriceEntry(String name, String material) {
        this.name = name;
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getLore() {
        return lore;
    }

    public void setLore(String lore) {
        this.lore = lore;
    }

    /** Literal phrases separated by semicolons or newlines; empty disables the buy filter. */
    public String getBannedLore() {
        return bannedLore == null ? "" : bannedLore;
    }

    public void setBannedLore(String bannedLore) {
        this.bannedLore = bannedLore == null ? "" : bannedLore;
    }

    public String getEnchants() {
        return enchants;
    }

    public void setEnchants(String enchants) {
        this.enchants = enchants;
    }

    public String getExcludedEnchants() {
        return excludedEnchants;
    }

    public void setExcludedEnchants(String excludedEnchants) {
        this.excludedEnchants = excludedEnchants;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public int getRequiredCount() {
        return minBuyCount;
    }

    public void setRequiredCount(int requiredCount) {
        this.requiredCount = requiredCount;
        this.minBuyCount = requiredCount;
    }

    public int getSellCount() {
        return minSellCount;
    }

    public void setSellCount(int sellCount) {
        this.sellCount = sellCount;
        this.minSellCount = sellCount;
    }

    public boolean isBuyEnabled() {
        return buyEnabled;
    }

    public void setBuyEnabled(boolean buyEnabled) {
        this.buyEnabled = buyEnabled;
    }

    public boolean isSellEnabled() {
        return sellEnabled;
    }

    public boolean isEnabled() {
        return buyEnabled || sellEnabled;
    }

    public void setSellEnabled(boolean sellEnabled) {
        this.sellEnabled = sellEnabled;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public boolean isIgnoreCustomModelData() {
        return ignoreCustomModelData;
    }

    public void setIgnoreCustomModelData(boolean ignoreCustomModelData) {
        this.ignoreCustomModelData = ignoreCustomModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public Integer getDyeColor() {
        return dyeColor;
    }

    public void setDyeColor(Integer dyeColor) {
        this.dyeColor = dyeColor;
    }

    public String getHeadTextureValue() {
        return headTextureValue;
    }

    public void setHeadTextureValue(String headTextureValue) {
        this.headTextureValue = headTextureValue;
    }

    public net.minecraft.item.ItemStack getCachedIconStack() {
        return cachedIconStack;
    }

    public void setCachedIconStack(net.minecraft.item.ItemStack stack) {
        this.cachedIconStack = stack;
    }

    public Integer getMinDaysLeft() {
        return minDaysLeft;
    }

    public void setMinDaysLeft(Integer minDaysLeft) {
        this.minDaysLeft = minDaysLeft;
    }

    public List<PotionEffectEntry> getPotionEffects() {
        return potionEffects;
    }

    public void setPotionEffects(List<PotionEffectEntry> potionEffects) {
        this.potionEffects = potionEffects;
    }

    public boolean hasPotionEffects() {
        return potionEffects != null && !potionEffects.isEmpty();
    }

    public List<String> getLoreLines() {
        List<String> result = new ArrayList<>();
        if (lore != null && !lore.isEmpty()) {
            for (String part : lore.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) result.add(trimmed);
            }
        }
        return result;
    }

    public void setLoreLines(List<String> loreLines) {
        if (loreLines != null && !loreLines.isEmpty()) {
            this.lore = String.join(";", loreLines);
        }
    }

    public Map<String, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(Map<String, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public int getMinBuyCount() {
        return minBuyCount;
    }

    public void setMinBuyCount(int minBuyCount) {
        this.minBuyCount = minBuyCount;
        this.requiredCount = minBuyCount;
    }

    public boolean isUnitMode() {
        return unitMode;
    }

    public void setUnitMode(boolean unitMode) {
        this.unitMode = unitMode;
    }

    public int getMinSellCount() {
        return minSellCount;
    }

    public void setMinSellCount(int minSellCount) {
        this.minSellCount = minSellCount;
        this.sellCount = minSellCount;
    }

    public List<String> getBannedEnchantsList() {
        return bannedEnchantsList;
    }

    public void setBannedEnchantsList(List<String> bannedEnchantsList) {
        this.bannedEnchantsList = bannedEnchantsList;
    }



}


