package dev.bkclient.ui.clicksys.port.audio;
import dev.bkclient.ui.clicksys.port.enums.SoundEvent;
public final class SoundEngine {
 private static final SoundEngine INSTANCE=new SoundEngine();public static SoundEngine getInstance(){return INSTANCE;}
 public void onSoundEvent(SoundEvent event){dev.bkclient.ui.clicksys.ClickSysSounds.play(event);}
}