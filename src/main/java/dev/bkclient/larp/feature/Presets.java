package dev.bkclient.larp.feature;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.component.DataComponentTypes;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.feature.FakeItemOption;
import dev.bkclient.larp.feature.LegacyText;

public final class Presets {
    private static final Disguise.Consumable VAMPIRE_APPLE = new Disguise.Consumable(300, List.of(new Disguise.Effect((RegistryEntry<StatusEffect>)StatusEffects.REGENERATION, 600, 1), new Disguise.Effect((RegistryEntry<StatusEffect>)StatusEffects.FIRE_RESISTANCE, 6000, 0), new Disguise.Effect((RegistryEntry<StatusEffect>)StatusEffects.RESISTANCE, 6000, 0), new Disguise.Effect((RegistryEntry<StatusEffect>)StatusEffects.ABSORPTION, 2400, 3), new Disguise.Effect((RegistryEntry<StatusEffect>)StatusEffects.STRENGTH, 60, 1)));
    private static final String RUBIK_SKIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODUxM2YwZjFkYzA2MzJkYjhmYzFjMjc5MmE2NjI5ZTA5ZDQ3YzdiMTJjNjM4MjRhNzVjOTRiZjRjZDdlODFkYyJ9fX0=";
    public static final FakeItemOption GRINCH_AXE = Presets.option(Items.GOLDEN_AXE, 1.0f, "&2Siekiera Grincha", List.of("&8 \u00bb &7Jest to przedmiot z:", "&8 \u00bb &feventu \u015bwi\u0105tecznego (2023)", "&r", "&8 \u00bb &7Po &auderzeniu &7strzela &fpiorun", "&8 \u00bb &7w przeciwnika zadaj\u0105c mu &c30%", "&8 \u00bb &7jego \u017cycia! &fUmiej\u0119tno\u015b\u0107&7 dzia\u0142a", "&8 \u00bb &7raz na 1 minute."), false, false, Disguise.Ability.GRINCH_LIGHTNING, null);
    public static final FakeItemOption DROP_POUCH = Presets.option(Items.RABBIT_FOOT, 1.0f, "&a&lSakiewka dropu", List.of("&7", "&8 \u00bb &7Posiadaj\u0105c ten przedmiot w", "&8 \u00bb &7ekwipunku po &czabiciu &7dowolnego", "&8 \u00bb &fgracza &7jego przedmioty trafiaj\u0105", "&8 \u00bb &7do twojej &asakiewki&7!", "&7", "&8 \u00bb &eKliknij PRAWYM, aby otworzy\u0107 sakiewk\u0119!", "&7"), false, true, Disguise.Ability.DROP_POUCH, null);
    public static final FakeItemOption VAMPIRE_APPLE_ITEM = Presets.option(Items.ENCHANTED_GOLDEN_APPLE, 5324.0f, "&c&lWampirze jab\u0142ko", List.of("&8 \u00bb &7Jest to przedmiot z:", "&8 \u00bb &feventu halloween (2024)", "", "&8 \u00bb &7Po zjedzeniu otrzymujesz na", "&8 \u00bb &7kr\u00f3tki czas efekt &csi\u0142y II&7!"), true, true, Disguise.Ability.NONE, VAMPIRE_APPLE);
    public static final FakeItemOption GUARDIAN_ROD = Presets.option(Items.FISHING_ROD, 20266.0f, "&3&lW\u0119dka Guardiania", List.of("&8 \u00bb &7Jest to przedmiot zdobyty podczas", "&8 \u00bb &fWydarzenia Wakacyjnego 2026&7!", "&7", "&8 \u00bb &7Zarzu\u0107 w\u0119dk\u0119, aby wystawi\u0107 &3Stra\u017cnika G\u0142\u0119bin&7,", "&8 \u00bb &7kt\u00f3ry w promieniu &b6 kratek &7nak\u0142ada na wrog\u00f3w", "&8 \u00bb &fzm\u0119czenie kopania I&7.", "&7", "&8 \u00bb &7Stra\u017cnik trzyma si\u0119 \u017cy\u0142ki, wi\u0119c musisz ca\u0142y czas", "&8 \u00bb &7trzyma\u0107 w\u0119dk\u0119 w r\u0119ce, inaczej efekt znika", "&8 \u00bb &7Da si\u0119 go zabi\u0107, ale efekt trwa jeszcze &f3 sekundy&7.", "&7", "&8 \u00bb &7Na ciebie efekt nie dzia\u0142a!"), true, true, Disguise.Ability.GUARDIAN_ROD, null);
    public static final FakeItemOption ILLUSIONIST_WAND = Presets.option(Items.GOLDEN_HOE, 1.0f, "&5&lR\u00f3\u017cd\u017cka iluzjonisty", List.of("&8 \u00bb &7Jest to przedmiot zdobyty podczas", "&8 \u00bb &fwydarzenia cyrkowego 2025&7!", "&7", "&8 \u00bb &7LPM: przywo\u0142uje &d3 fale Szcz\u0119k Evokera", "&8 \u00bb &7atakuj\u0105ce przeciwnik\u00f3w na wprost.", "&7", "&8 \u00bb &7PPM: &dznikasz na &e4s &7i tworzysz", "&8 \u00bb &dklona &7biegn\u0105cego prosto przed siebie!"), false, true, Disguise.Ability.ILLUSIONIST_WAND, null);
    public static final FakeItemOption PHANTOM_BLOCK = Presets.option(Items.STRUCTURE_BLOCK, 0.0f, "&c&lBlok widmo", List.of("&8 \u00bb &7Jest to przedmiot zdobyty podczas", "&8 \u00bb &fDnia Dziecka 2025&7!", "&7", "&8 \u00bb &7Po postawieniu na ziemi,", "&8 \u00bb &7uwalnia &eenergi\u0119&7, kt\u00f3ra obni\u017ca", "&8 \u00bb &fmaksymalny limit &7serc pobliskim", "&8 \u00bb &7graczom na okre\u015blony czas."), true, true, Disguise.Ability.PHANTOM_BLOCK, null);
    public static final FakeItemOption MAGIC_LANTERN = Presets.option(Items.BEACON, 0.0f, "&d&lCudowna Latarnia", List.of("&8 \u00bb &7Przedmiot z &f\u015bwi\u0105tecznego wydarzenia 2025&7!", "&7", "&8 \u00bb &7Po postawieniu otrzymujesz:", "&8 \u00bb &dRegeneracje V &8(20s)", "&8 \u00bb &eAbsorbcje VI &8(10s)", "&8 \u00bb &cSi\u0142\u0119 II &8(10s)", "&7", "&8 \u00bb &7Fontanna pozostanie przez &f30 sekund&7.", "&8 \u00bb &7Efekty dzia\u0142aj\u0105 w zasi\u0119gu &f30 kratek&7.", "&8 \u00bb &7Zniszczenie fontanny usuwa efekty."), true, true, Disguise.Ability.MAGIC_LANTERN, null);
    public static final FakeItemOption LOLLIPOP = Presets.option(Items.ALLIUM, 1.0f, "&d&lLizak", List.of("&8 \u00bb &7Jest to przedmiot zdobyty podczas", "&8 \u00bb &fDnia dziecka 2026&7!", "&7", "&8 \u00bb &7Trzymaj\u0105c go w r\u0119ce zyskujesz &fsi\u0142e I&7,", "&8 \u00bb &7a Twoja posta\u0107 &dzmniejsza si\u0119 &7do", "&8 \u00bb &d0.65x &7swojego normalnego rozmiaru!"), false, true, Disguise.Ability.LOLLIPOP, null);
    public static final FakeItemOption FLIGHTLESS_ROD = Presets.option(Items.FISHING_ROD, 2.0f, "&5&lW\u0119dka nielota", List.of("&8 \u00bb &7W\u0119dka nielota by\u0142a do zdobycia", "&8 \u00bb &7w &f2024 roku &7podczas &adnia dziecka&7.", "&r", "&8 \u00bb &7Po z\u0142apaniu gracza na haczyk", "&8 \u00bb &7nie mo\u017ce on odlecie\u0107 &delytr\u0105&7!"), false, true, Disguise.Ability.FLIGHTLESS_ROD, null);
    public static final FakeItemOption POISON_PENCIL = Presets.option(Items.LIME_CANDLE, 14.0f, "&a&lZatruty o\u0142\u00f3wek", List.of("&8 \u00bb &7Jest to przedmiot zdobyty z", "&8 \u00bb &fwydarzenia szkolnego 2024&7!", "&r", "&8 \u00bb &7Po uderzeniu przeciwnika", "&8 \u00bb &7otrzymuje on &ctruj\u0105cy efekt&7!"), false, true, Disguise.Ability.POISON_PENCIL, null);
    public static final FakeItemOption CUPID_ROSE = Presets.option(Items.POPPY, 1.0f, "&c&lR\u00f3\u017ca kupidyna", List.of("&8 \u00bb &7Przedmiot zdobyty w &fwalentynki w 2026 roku&7!", "&8 \u00bb &7Trzymaj go w r\u0119ce aby otrzyma\u0107:", "&8  - &fEfekt odporno\u015bci 1", "&8  - &fEfekt regeneracji 1", "&8  - &f+5 dodatkowych serc"), false, true, Disguise.Ability.CUPID_ROSE, null);
    public static final FakeItemOption DRACULA_BACKPACK = Presets.option(Items.RED_BUNDLE, 1.0f, "&c&lPlecak Drakuli", List.of("&8 \u00bb &7Przedmiot z &fwydarzenia halloween 2025&7!", "&7", "&8 \u00bb &7Otwiera przeno\u015bny &dplecak &7na &e5 slot\u00f3w&7.", "&8 \u00bb &7Zawarto\u015b\u0107 zapisywana jest w &dtym egzemplarzu&7.", "&8 \u00bb &7Edytowanie zawarto\u015bci podczas &cwalki &7jest zablokowane.", "&8 \u00bb &7Przedmiot &anie traci si\u0119 po \u015bmierci&7."), false, true, Disguise.Ability.DRACULA_BACKPACK, null);
    public static final FakeItemOption SCYTHE = Presets.option(Items.NETHERITE_HOE, 1.0f, "&8&lKosa", List.of("&8 \u00bb &7Jest to przedmiot z:", "&8 \u00bb &feventu halloween (2023)", "&r", "&8 \u00bb &7Twoje uderzenie mo\u017ce&f co minut\u0119", "&8 \u00bb &7przestraszy\u0107 przeciwnika nadaj\u0105c", "&8 \u00bb &7mu efekty utrudniaj\u0105ce widoczno\u015b\u0107."), true, true, Disguise.Ability.SCYTHE, null);
    public static final FakeItemOption GRINCH_SHOVEL = Presets.option(Items.DIAMOND_SHOVEL, 4572321.0f, "&a&l\u0141opata grincha", List.of("&8 \u00bb &7Jest to przedmiot z:", "&8 \u00bb &feventu \u015bwi\u0105tecznego (2024)", "&r", "&8 \u00bb &7Po &auderzeniu &7przeciwnika", "&8 \u00bb &7zostaje on &cog\u0142uszony&7 przez co", "&8 \u00bb &7rotacja zostaje wylosowana!"), true, true, Disguise.Ability.GRINCH_SHOVEL, null);
    public static final FakeItemOption DIVINE_AXE = Presets.option(Items.IRON_AXE, 3462232.0f, "&b&lBoski top\u00f3r", List.of("&8 \u00bb &7Jest to przedmiot z:", "&8 \u00bb &fspecjalnego wydarzenia (2025)", "&r", "&8 \u00bb &7Po klikni\u0119ciu prawym przyciskiem", "&8 \u00bb &7aktywujesz &epot\u0119\u017cn\u0105 fale uderzeniow\u0105", "&8 \u00bb &7kt\u00f3ra zapewnia Tobie &cnie\u015bmiertelno\u015b\u0107&7!"), true, true, Disguise.Ability.DIVINE_AXE, null);
    public static final FakeItemOption ARCUS_MAGNUS = Presets.option(Items.BOW, 5324.0f, "&a&lArcus Magnus", List.of("&8 \u00bb &7Jest to przedmiot z:", "&8 \u00bb &feventu halloween (2024)", "&r", "&8 \u00bb &7\u0141uk, kt\u00f3ry pozwala na", "&8 \u00bb &eniszczycielskie &7combo!", "&8 \u00bb &7Po trafieniu przeciwnika", "&8 \u00bb &7zadajesz mu &cogromne obra\u017cenia&7, kt\u00f3re", "&8 \u00bb &7zwi\u0119kszaj\u0105 si\u0119 z ka\u017cdym trafieniem!"), false, true, Disguise.Ability.ARCUS_MAGNUS, null);
    public static final FakeItemOption RUBIKS_CUBE = Presets.option(Items.PLAYER_HEAD, 0.0f, "&e&lKostka rubika", List.of("&8 \u00bb &7Kostka rubika by\u0142a do zdobycia", "&8 \u00bb &7w &f2024 roku&7 podczas &adnia dziecka&7.", "&r", "&8 \u00bb &7Uderz gracza, aby jego ekwipunek", "&8 \u00bb &7zosta\u0142 &fprzemieszany&7!"), false, true, Disguise.Ability.RUBIKS_CUBE, null);
    public static final FakeItemOption ROD = Presets.option(Items.STICK, 0.0f, "&8R\u00f3zga", List.of("&8 \u00bb &7Jest to przetmiot z:", "&8 \u00bb &feventu \u015bwi\u0105teczego (2023)", "&r", "&8 \u00bb &cOdrzuca &7z pot\u0119zn\u0105 si\u0142\u0105", "&8 \u00bb &7niegrzecznych graczy!"), true, true, Disguise.Ability.NONE, null);

    private Presets() {
    }

    public static void init() {
        Disguise.modelOverride(RUBIKS_CUBE.spec(), Presets.rubikHead());
    }

    private static ItemStack rubikHead() {
        ItemStack head = new ItemStack((ItemConvertible)Items.PLAYER_HEAD);
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("easyeventowka:rubik".getBytes()), "rubik");
        profile.getProperties().put("textures", new Property("textures", RUBIK_SKIN));
        head.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
        return head;
    }

    private static FakeItemOption option(Item modelItem, float customModelData, String name, List<String> lore, boolean glint, boolean hideAttributes, Disguise.Ability ability, Disguise.Consumable consumable) {
        List<Text> lines = lore.stream().map(LegacyText::parse).toList();
        Disguise.Spec spec = new Disguise.Spec(modelItem, LegacyText.parse(name), lines, Float.valueOf(customModelData), glint, hideAttributes, true, ability, consumable, 0);
        return new FakeItemOption(() -> spec);
    }
}
