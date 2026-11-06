package dev.bkclient.larp.feature;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.sound.SoundEvent;
import dev.bkclient.larp.feature.LegacyText;

final class Titles {
    private Titles() {
    }

    static void show(Text title, Text subtitle) {
        InGameHud hud = MinecraftClient.getInstance().inGameHud;
        hud.setTitleTicks(2, 30, 8);
        hud.setSubtitle(subtitle);
        hud.setTitle(title);
    }

    static void cooldown(int secondsLeft) {
        Titles.show(LegacyText.parse("&cUmiej\u0119tno\u015b\u0107 w odnowieniu"), LegacyText.parse("&7Do u\u017cycia za: &e" + secondsLeft + "s"));
    }

    static void actionBar(Text message) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(message, false);
    }

    static void playSound(SoundEvent sound, float volume, float pitch) {
        if (!dev.bkclient.larp.LarpRuntime.effects()) return;
        MinecraftClient.getInstance().getSoundManager().play((SoundInstance)PositionedSoundInstance.master((SoundEvent)sound, (float)pitch, (float)volume));
    }
}
