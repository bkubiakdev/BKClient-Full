package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.game.AttackEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.entity.LivingEntity;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(name = "Hit Sound", category = ModuleCategory.OTHER, desc = "modules.descriptions.hit_sound")
public class HitSound extends BaseModule {
    public final ModeSetting sound = new ModeSetting(this, "modules.settings.hit_sound.sound");
    public final ModeSetting.Value moans = new ModeSetting.Value(sound, "modules.settings.hit_sound.sound.moans").select();
    public final ModeSetting.Value metallic = new ModeSetting.Value(sound, "modules.settings.hit_sound.sound.metallic");
    public final ModeSetting.Value crime = new ModeSetting.Value(sound, "modules.settings.hit_sound.sound.crime");
    public final SliderSetting volume = new SliderSetting(this, "modules.settings.hit_sound.volume")
        .min(0.1F).max(2.0F).currentValue(1.0F).step(0.1F);

    private final EventListener<AttackEvent> onAttack = event -> {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        float value = volume.getCurrentValue();
        if (sound.is(metallic)) ClientSounds.METALLIC.play(value);
        else if (sound.is(crime)) ClientSounds.CRIME.play(value);
        else switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> ClientSounds.MOAN1.play(value);
            case 1 -> ClientSounds.MOAN2.play(value);
            case 2 -> ClientSounds.MOAN3.play(value);
            default -> ClientSounds.MOAN4.play(value);
        }
    };
}
