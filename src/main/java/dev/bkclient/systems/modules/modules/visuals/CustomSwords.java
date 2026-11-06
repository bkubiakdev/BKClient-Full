package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;

/**
 * Selects custom sword models through vanilla custom_model_data item definitions.
 * ItemModelManagerMixin modifies a render-only copy of the stack.
 */
@ModuleInfo(name = "Custom Swords", desc = "modules.descriptions.custom_swords", category = ModuleCategory.VISUALS)
public class CustomSwords extends BaseModule {
    private static CustomSwords INSTANCE;

    private static final String[] WEAPONS = {
        "Abominable Blade", "Abominable Great Saber", "Abominable Scythe", "Acidic Cleaver",
        "Amethyst Shuriken", "Ancient Royal Great Sword", "Aquatic Sacred Blade", "Arcanethyst",
        "Ashura's Blade", "Awakened Lichblade", "Blood Edge", "Bloody Death",
        "Bramblethorn", "Brimstone Claymore", "Carian Knight's Sword", "Chrono Blade",
        "Corrupted Mythic Blade", "Creation Splitter", "Crescent Rose", "Cyber Katana",
        "Cyber Mantis Blade", "Cyber Sword", "Cybernetic Chainsaw Blade", "Cybernetic Katana",
        "Cybernetic Knife", "Dainsleif", "Dark Blade", "Dark Cleaver",
        "Death Knight's Dagger", "Death Knight's Sword", "Demigod's Unholy Blade", "Demigod's Unholy Halberd",
        "Demon Lord's Great Axe", "Demon Lord's Sword", "Demonic Blade", "Demonic Cleaver",
        "Divine Axe Rhitta", "Divine Justice", "Divine Punisher", "Divine Reaper",
        "Dragon Slaying blade", "Edge Of The Astral Plane", "Emberblade", "Enigma",
        "Epic Sword", "Estoc", "Fallen God's Spear", "Fallen God's Sword",
        "Floral Longsword", "Floral Sabre", "Forest Guardian's Glaive", "Frost Axe",
        "Frost Blade", "Frost Scythe", "Hearthflame", "Hero Sword",
        "Holy Moonlight Sword", "Hornet's Needle", "Icewhisper", "Jade Halberd",
        "Katana", "Legendary Sword", "Longsword", "Magi Scythe",
        "Masamune", "Mjolnir", "Molten Blade", "Molten Sword",
        "Muramasa", "Mystical Spellblade", "Mythic Blade", "Ocean's Rage",
        "Partisan", "Pharaoh's Treasure", "Pheonix Grace", "Plague Longsword",
        "Power Fuse Hammer", "Power Fuse Sword", "Requiem of the Ninth Abyss", "Ribbon Cleaver",
        "Righteous Relic", "Rivers Of Blood", "Royal Chakram", "Royal Rapier",
        "Sabre", "Scissor Blade", "Sculk Cleaver", "Sculk Scythe",
        "Sculk Sword", "Sentinel's Will", "Silverine Blade", "Soul Claws",
        "Soul Collector", "Soul Devourer", "Soul Edge", "Soul Harvester",
        "Soul Stealer", "Soulrender", "Star's Edge", "Steel Sword",
        "Stop Sign", "Storm Bringer", "Storm's Edge", "Sunbreak",
        "Tengen's Blade", "Terra Blade", "Thousand Demon Daggers", "Thunder Bringer",
        "Thunderbrand", "True Excalibur", "Vampiric Needle", "Wakizashi",
        "Watcher Claymore", "Watching Warglaive", "Waxweaver", "Whisperwind",
        "Wickpiercer", "Wraith Scythe", "Yoru"
    };

    public final BooleanSetting selfOnly = new BooleanSetting(this, "Self Only").enable();
    public final ModeSetting weapon = new ModeSetting(this, "Weapon");

    public CustomSwords() {
        INSTANCE = this;
        for (String name : WEAPONS) {
            new ModeSetting.Value(this.weapon, name);
        }
    }

    public static CustomSwords getInstance() {
        return INSTANCE;
    }

    public boolean isSelfOnly() {
        return this.selfOnly.isEnabled();
    }

    /** The {@code custom_model_data} string the item definitions match on. */
    public String getSelectedWeapon() {
        ModeSetting.Value selected = this.weapon.getValue();
        return selected == null ? WEAPONS[0] : selected.getName();
    }
}
