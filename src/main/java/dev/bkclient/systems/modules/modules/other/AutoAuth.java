package dev.bkclient.systems.modules.modules.other;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.network.ReceivePacketEvent;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.utility.game.TextUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Auto Auth", category = ModuleCategory.OTHER, desc = "Automatically registers and logs into the server")
public class AutoAuth extends BaseModule {
   private final BooleanSetting random = new BooleanSetting(this, "Random");
   private final StringSetting password = new StringSetting(this, "Password", this.random::isEnabled).text("123123");
   private final Map<String, String> nickAndPassword = new HashMap<>();
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet && mc.player != null) {
         String message = packet.content().getString().toLowerCase();
         String randomPass = TextUtility.getRandomNick();
         String password = this.random.isEnabled() ? randomPass : this.password.getText();
         this.nickAndPassword.put(mc.player.getDisplayName().getString(), " " + randomPass);
         if (!message.contains("зарегистрируйтесь") && !message.contains("/reg")) {
            if (message.contains("авторизуйтесь") || message.contains("/login") || message.contains("/l") && message.matches("/l(\\s|$)")) {
               mc.player.networkHandler.sendChatCommand(String.format("l %s", password));
            }
         } else {
            mc.player.networkHandler.sendChatCommand(String.format("reg %s %s", password, password));
         }
      }
   };

   public Map<String, String> listPassword() {
      return Collections.unmodifiableMap(this.nickAndPassword);
   }

   public void put(String key, String value) {
      this.nickAndPassword.put(key, value);
   }
}
