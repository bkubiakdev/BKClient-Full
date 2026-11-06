package dev.bkclient.systems.modules.modules.other;

import lombok.Generated;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.SliderSetting;
import dev.bkclient.systems.setting.settings.ModeSetting;
import dev.bkclient.utility.sounds.ClientSounds;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Sounds", category = ModuleCategory.OTHER, enabledByDefault = true, desc = "modules.descriptions.sounds")
public class Sounds extends BaseModule {
   private final SliderSetting volume = new SliderSetting(this, "modules.settings.sounds.volume").step(0.1F).min(0.1F).max(1.0F).currentValue(1.0F);
   private final SliderSetting guiVolume = new SliderSetting(this, "modules.settings.sounds.gui_volume").min(0).max(100).step(1).currentValue(20);
   private final ModeSetting guiPitch = new ModeSetting(this, "modules.settings.sounds.gui_pitch");
   private final ModeSetting.Value lowPitch = new ModeSetting.Value(guiPitch, "modules.settings.sounds.pitch_low");
   private final ModeSetting.Value mediumPitch = new ModeSetting.Value(guiPitch, "modules.settings.sounds.pitch_medium").select();
   private final ModeSetting.Value highPitch = new ModeSetting.Value(guiPitch, "modules.settings.sounds.pitch_high");

   public float getGuiVolume() {
      return volume.getCurrentValue() * guiVolume.getCurrentValue() / 100.0F;
   }

   public float getGuiPitch() {
      // SystemSRC exposes three modes; its native playback ratios are unavailable.
      return lowPitch.isSelected() ? 0.8F : highPitch.isSelected() ? 1.2F : 1.0F;
   }
   private final EventListener<ReceivePacketEvent> receivePacket = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String msg = packet.content().getString();
         if (msg.contains("Вы успешно купили") || msg.contains("отправлено игроку")) {
            ClientSounds.APPLEPAY.play(this.volume.getCurrentValue(), 1.0F);
         }
      }
   };

   @Generated
   public SliderSetting getVolume() {
      return this.volume;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getReceivePacket() {
      return this.receivePacket;
   }
}
