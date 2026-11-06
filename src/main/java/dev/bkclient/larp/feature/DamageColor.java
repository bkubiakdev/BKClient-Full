package dev.bkclient.larp.feature;

public final class DamageColor {
    private DamageColor() {
    }

    public static String of(int damage) {
        if (damage <= 0) {
            return "&c";
        }
        if (damage <= 10) {
            return "&7";
        }
        if (damage <= 29) {
            return "&f";
        }
        if (damage <= 44) {
            return "&e";
        }
        return "&6";
    }

    public static String line(int damage) {
        return DamageColor.of(damage) + "Dodatkowe obra\u017cenia " + damage + "%";
    }
}
