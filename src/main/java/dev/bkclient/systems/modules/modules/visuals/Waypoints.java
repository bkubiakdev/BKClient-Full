package dev.bkclient.systems.modules.modules.visuals;

import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.PostHudRenderEvent;
import dev.bkclient.systems.file.FileManager;
import dev.bkclient.systems.modules.api.*;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.ButtonSetting;
import dev.bkclient.systems.waypoints.WaypointStore;
import dev.bkclient.ui.hud.impl.WaypointRenderer;
import dev.bkclient.ui.menu.waypoints.WaypointScreen;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import java.io.IOException;
import java.util.*;

@ModuleInfo(name = "Waypoints", category = ModuleCategory.VISUALS, desc = "Saved locations with glass markers and live distance")
public final class Waypoints extends BaseModule implements IMinecraft {
   private final WaypointStore store = new WaypointStore(FileManager.DIRECTORY.toPath().resolve("waypoints.json"));
   private final ButtonSetting addHere = new ButtonSetting(this, "Add Waypoint").action(this::openManager);
   private String activeScope = "", activeDimension = "";
   private List<WaypointStore.Point> visible = List.of();
   private long appearedAt;
   private String error = "";
   private final EventListener<PostHudRenderEvent> render = this::draw;

   public static String scope() {
      if (mc.world == null) return "";
      if (mc.getServer() != null)
         return "local:" + mc.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
      var server = mc.getCurrentServerEntry();
      return server == null ? "" : "server:" + server.address.strip().toLowerCase(Locale.ROOT);
   }
   public static String dimension() { return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString(); }
   public WaypointStore store() { return store; }
   public void refresh() { activeScope = ""; error = ""; }
   public void openManager() {
      if (mc.player == null || scope().isEmpty()) {
         if (mc.player != null) mc.player.sendMessage(Text.literal("Waypoints: join a world first."), false);
         return;
      }
      mc.setScreen(new WaypointScreen(this, mc.currentScreen));
   }
   @Override public void onEnable() { refresh(); }
   @Override public void onDisable() { visible = List.of(); activeScope = ""; }
   private void draw(PostHudRenderEvent event) {
      if (mc.player == null || mc.world == null) { visible = List.of(); activeScope = ""; return; }
      String scope = scope(), dimension = dimension();
      if (scope.isEmpty()) return;
      if (!scope.equals(activeScope) || !dimension.equals(activeDimension)) {
         activeScope = scope; activeDimension = dimension; appearedAt = System.nanoTime();
         try { visible = store.list(scope, dimension); }
         catch (IOException failure) {
            visible = List.of();
            if (error.isEmpty()) mc.player.sendMessage(Text.literal("Waypoints: could not read saved points."), false);
            error = failure.getMessage();
         }
      }
      if (mc.options.hudHidden) return;
      // Draw near markers last so distant labels do not obscure the closest destination.
      visible.stream().filter(WaypointStore.Point::visible)
         .sorted(Comparator.comparingDouble((WaypointStore.Point p) -> mc.player.squaredDistanceTo(p.x(), p.y(), p.z())).reversed())
         .forEach(p -> WaypointRenderer.draw(event, new Vec3d(p.x(), p.y(), p.z()), p.name(), ColorRGBA.fromInt(p.color()), appearedAt));
   }
}
