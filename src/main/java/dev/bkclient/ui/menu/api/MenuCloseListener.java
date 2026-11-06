package dev.bkclient.ui.menu.api;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.modules.modules.visuals.MenuModule;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.menu.csgui.CsGuiScreen;
import dev.bkclient.ui.menu.spotlight.SpotlightScreen;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;

public class MenuCloseListener implements IMinecraft {
   private final EventListener<HudRenderEvent> onHudRender = event -> {
      MenuScreen menuScreen = BKClientInit.getInstance().getMenuScreen();
      if (mc.currentScreen == null) {
         MenuModule menuModule = BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class);
         boolean correctType = menuModule.isClickGui()
            ? menuScreen instanceof dev.bkclient.ui.clicksys.ClickSysScreen
            : menuModule.isOld()
            ? menuScreen instanceof CsGuiScreen
            : menuScreen instanceof SpotlightScreen;
         if (!correctType) {
            menuScreen = menuModule.createMenuScreen();
            BKClientInit.getInstance().setMenuScreen(menuScreen);
         }
      }

      if (menuScreen != null) {
         menuScreen.getMenuAnimation().update(menuScreen.isClosing() ? 0.0F : 1.0F);
         if (!(mc.currentScreen instanceof MenuScreen) && BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class).isEnabled()) {
            BKClientInit.getInstance().getModuleManager().getModule(MenuModule.class).setEnabled(false);
         }

         if (menuScreen.getMenuAnimation().getValue() > 0.001F && mc.currentScreen == null && menuScreen.isClosing()) {
            UIContext context = UIContext.of(event.getContext(), -1, -1, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
            menuScreen.render(context);
         }
      }
   };

   public MenuCloseListener() {
      BKClientInit.getInstance().getEventManager().subscribe(this);
   }
}
