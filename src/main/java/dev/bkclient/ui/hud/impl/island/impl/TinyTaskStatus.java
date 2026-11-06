package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.other.TinyTask;
import dev.bkclient.systems.modules.modules.other.tinytask.TinyTaskController;
import dev.bkclient.ui.hud.impl.island.IslandTokenStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import java.util.List;

/** Live recording and playback state. */
public class TinyTaskStatus extends IslandTokenStatus {
   private static final char ICON_TINY_TASK = 'R';

   public TinyTaskStatus(SelectSetting setting) {
      super(setting, "tinytask");
   }

   @Override
   protected List<Token> buildTokens() {
      TinyTaskController controller = controller();
      if (controller == null || !canShow()) return List.of();
      return this.tokens(
         Token.icon(ICON_TINY_TASK, Colors.getAccent()),
         Token.divider(),
         Token.text("TinyTask", ColorRGBA.WHITE, false),
         Token.divider(),
         Token.text(controller.statusLabel(), Colors.getAccent(), false),
         Token.divider(),
         Token.text(controller.tickCount() / 20 + "s", WHITE_75, false),
         controller.isPlaying() ? Token.ring(controller.progress()) : Token.text("", WHITE_75, false)
      );
   }

   @Override
   public boolean canShow() {
      TinyTaskController controller = controller();
      return controller != null && (controller.isRecording() || controller.isPlaying());
   }

   private TinyTaskController controller() {
      TinyTask module = BKClientInit.getInstance().getModuleManager().getModule(TinyTask.class);
      return module == null ? null : module.getController();
   }
}
