package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import net.minecraft.util.Identifier;

@ModuleInfo(name = "Capes", category = ModuleCategory.VISUALS, desc = "Choose your custom cape.")
public class Capes extends BaseModule {
    public final BooleanSetting waveyAnimation = new BooleanSetting(this, "Animation")
            .enabled(true);
    public final SliderSetting waveyGravity = new SliderSetting(this, "Gravity", () -> !waveyAnimation.isEnabled())
            .min(2f).max(10f).step(0.5f).currentValue(2f);

    public final ModeSetting mode = new ModeSetting(this, "Cape");

    public final ModeSetting.Value anniversary15 = new ModeSetting.Value(mode, "15th Anniversary");
    public final ModeSetting.Value bacon = new ModeSetting.Value(mode, "Bacon");
    public final ModeSetting.Value birthday = new ModeSetting.Value(mode, "Birthday");
    public final ModeSetting.Value cherryBlossom = new ModeSetting.Value(mode, "Cherry Blossom");
    public final ModeSetting.Value cobalt = new ModeSetting.Value(mode, "Cobalt");
    public final ModeSetting.Value common = new ModeSetting.Value(mode, "Common");
    public final ModeSetting.Value dannyBstyle = new ModeSetting.Value(mode, "Danny Bstyle");
    public final ModeSetting.Value followers = new ModeSetting.Value(mode, "Followers");
    public final ModeSetting.Value home = new ModeSetting.Value(mode, "Home");
    public final ModeSetting.Value mcc15th = new ModeSetting.Value(mode, "MCC 15th");
    public final ModeSetting.Value menace = new ModeSetting.Value(mode, "Menace");
    public final ModeSetting.Value migrator = new ModeSetting.Value(mode, "Migrator");
    public final ModeSetting.Value millionth = new ModeSetting.Value(mode, "Millionth Customer");
    public final ModeSetting.Value minecon2011 = new ModeSetting.Value(mode, "Minecon 2011");
    public final ModeSetting.Value minecon2012 = new ModeSetting.Value(mode, "Minecon 2012");
    public final ModeSetting.Value minecon2013 = new ModeSetting.Value(mode, "Minecon 2013");
    public final ModeSetting.Value minecon2014 = new ModeSetting.Value(mode, "Minecon 2014");
    public final ModeSetting.Value minecon2015 = new ModeSetting.Value(mode, "Minecon 2015");
    public final ModeSetting.Value minecon2016 = new ModeSetting.Value(mode, "Minecon 2016");
    public final ModeSetting.Value minecon2017 = new ModeSetting.Value(mode, "Minecon 2017");
    public final ModeSetting.Value minecon2018 = new ModeSetting.Value(mode, "Minecon 2018");
    public final ModeSetting.Value minecon2019 = new ModeSetting.Value(mode, "Minecon 2019");
    public final ModeSetting.Value experience = new ModeSetting.Value(mode, "Experience");
    public final ModeSetting.Value mojang = new ModeSetting.Value(mode, "Mojang");
    public final ModeSetting.Value mojangOld = new ModeSetting.Value(mode, "Mojang Old");
    public final ModeSetting.Value mojangStudios = new ModeSetting.Value(mode, "Mojang Studios");
    public final ModeSetting.Value mojira = new ModeSetting.Value(mode, "Mojira Moderator");
    public final ModeSetting.Value newYear2010 = new ModeSetting.Value(mode, "New Year 2010");
    public final ModeSetting.Value newYear2011 = new ModeSetting.Value(mode, "New Year 2011");
    public final ModeSetting.Value office = new ModeSetting.Value(mode, "Office");
    public final ModeSetting.Value pancake = new ModeSetting.Value(mode, "Pancake");
    public final ModeSetting.Value prismarine = new ModeSetting.Value(mode, "Prismarine");
    public final ModeSetting.Value purpleHeart = new ModeSetting.Value(mode, "Purple Heart");
    public final ModeSetting.Value realmsNew = new ModeSetting.Value(mode, "Realms New");
    public final ModeSetting.Value realmsOld = new ModeSetting.Value(mode, "Realms Old");
    public final ModeSetting.Value scrolls = new ModeSetting.Value(mode, "Scrolls");
    public final ModeSetting.Value snowman = new ModeSetting.Value(mode, "Snowman");
    public final ModeSetting.Value spade = new ModeSetting.Value(mode, "Spade");
    public final ModeSetting.Value test = new ModeSetting.Value(mode, "Test");
    public final ModeSetting.Value translator = new ModeSetting.Value(mode, "Translator");
    public final ModeSetting.Value turtle = new ModeSetting.Value(mode, "Turtle");
    public final ModeSetting.Value valentine = new ModeSetting.Value(mode, "Valentine");
    public final ModeSetting.Value vanilla = new ModeSetting.Value(mode, "Vanilla");
    public final ModeSetting.Value yearn = new ModeSetting.Value(mode, "Yearn");

    public final ModeSetting.Value bkAnimated = new ModeSetting.Value(mode, "BK Animated");

    public final ModeSetting.Value customCape = new ModeSetting.Value(mode, "Custom Cape");
    public final dev.bkclient.systems.setting.settings.StringSetting customSource =
            new dev.bkclient.systems.setting.settings.StringSetting(this, "File / URL", () -> !customCape.isSelected()).text("");
    private final dev.bkclient.systems.setting.settings.StringSetting appliedCustomSource =
            new dev.bkclient.systems.setting.settings.StringSetting(this, "Custom Cape Source", () -> true).text("");
    private final dev.bkclient.systems.setting.settings.ButtonSetting loadCustomCape =
            new dev.bkclient.systems.setting.settings.ButtonSetting(this, "Load / Reload", () -> !customCape.isSelected())
                    .action(() -> {
                        appliedCustomSource.text(customSource.getText());
                        dev.bkclient.utility.render.CustomCape.load(appliedCustomSource.getText(), true);
                    });
    private final dev.bkclient.systems.setting.settings.ButtonSetting openCapeFolder =
            new dev.bkclient.systems.setting.settings.ButtonSetting(this, "Open Folder", () -> !customCape.isSelected())
                    .action(dev.bkclient.utility.render.CustomCape::openFolder);
    private boolean customWasActive;

    @Override public void onDisable() {
        dev.bkclient.utility.render.AnimatedCape.shutdown();
        dev.bkclient.utility.render.CustomCape.shutdown();
        customWasActive = false;
        super.onDisable();
    }

    public Identifier getCapeIdentifier() {
        if (customCape.isSelected()) {
            customWasActive = true;
            dev.bkclient.utility.render.AnimatedCape.releaseTexture();
            return dev.bkclient.utility.render.CustomCape.currentTexture(appliedCustomSource.getText());
        }
        if (customWasActive) { dev.bkclient.utility.render.CustomCape.shutdown(); customWasActive = false; }
        if (bkAnimated.isSelected()) return dev.bkclient.utility.render.AnimatedCape.currentTexture();
        dev.bkclient.utility.render.AnimatedCape.releaseTexture();
        String base = "textures/capes/default/";
        String name = mode.getValue().getName();

        String fileName = switch (name) {
            case "15th Anniversary" -> "15th_anniversary.png";
            case "Bacon" -> "bacon.png";
            case "Birthday" -> "birthday.png";
            case "Cherry Blossom" -> "cherry_blossom.png";
            case "Cobalt" -> "cobalt.png";
            case "Common" -> "common.png";
            case "Danny Bstyle" -> "danny_bstyle.png";
            case "Followers" -> "followers.png";
            case "Home" -> "home.png";
            case "MCC 15th" -> "mcc_15th_years.png";
            case "Menace" -> "menace.png";
            case "Migrator" -> "migrator.png";
            case "Millionth Customer" -> "millionth_customer.png";
            case "Minecon 2011" -> "minecon_2011.png";
            case "Minecon 2012" -> "minecon_2012.png";
            case "Minecon 2013" -> "minecon_2013.png";
            case "Minecon 2014" -> "minecon_2014.png";
            case "Minecon 2015" -> "minecon_2015.png";
            case "Minecon 2016" -> "minecon_2016.png";
            case "Minecon 2017" -> "minecon_2017.png";
            case "Minecon 2018" -> "minecon_2018.png";
            case "Minecon 2019" -> "minecon_2019.png";
            case "Experience" -> "minecraft_experience.png";
            case "Mojang" -> "mojang.png";
            case "Mojang Old" -> "mojang_old.png";
            case "Mojang Studios" -> "mojang_studios.png";
            case "Mojira Moderator" -> "mojira_moderator.png";
            case "New Year 2010" -> "new_years_2010.png";
            case "New Year 2011" -> "new_years_2011.png";
            case "Office" -> "office.png";
            case "Pancake" -> "pancake.png";
            case "Prismarine" -> "prismarine.png";
            case "Purple Heart" -> "purple_heart.png";
            case "Realms New" -> "realms_new.png";
            case "Realms Old" -> "realms_old.png";
            case "Scrolls" -> "scrolls.png";
            case "Snowman" -> "snowman.png";
            case "Spade" -> "spade.png";
            case "Test" -> "test.png";
            case "Translator" -> "translator.png";
            case "Turtle" -> "turtle.png";
            case "Valentine" -> "valentine.png";
            case "Vanilla" -> "vanilla.png";
            case "Yearn" -> "yearn.png";
            default -> "minecon_2011.png";
        };

        return BKClientInit.id(base + fileName);
    }
}
