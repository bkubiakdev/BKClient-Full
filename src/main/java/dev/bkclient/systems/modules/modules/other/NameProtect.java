package dev.bkclient.systems.modules.modules.other;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Generated;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.utility.game.EntityUtility;
import net.minecraft.client.network.PlayerListEntry;

@ModuleInfo(name = "Name Protect", category = ModuleCategory.OTHER, desc = "modules.descriptions.name_protect")
public class NameProtect extends BaseModule {
   private final SelectSetting protect = new SelectSetting(this, "modules.settings.name_protect.protect");
   private final SelectSetting.Value ownName = new SelectSetting.Value(
      this.protect, "modules.settings.name_protect.protect.own_name"
   ).select();
   private final SelectSetting.Value friends = new SelectSetting.Value(
      this.protect, "modules.settings.name_protect.protect.friends"
   );
   private final SelectSetting.Value server = new SelectSetting.Value(
      this.protect, "modules.settings.name_protect.protect.server"
   );
   private final StringSetting fakeName = new StringSetting(this, "modules.settings.name_protect.fake_name").text("Player");
   public final BooleanSetting hideSkin = new BooleanSetting(this, "modules.settings.name_protect.hide_skin");

   /** Protect complete display text before GUI code truncates it or extracts initials. */
   public static String protectDisplayText(String text) {
      var manager = BKClientInit.getInstance().getModuleManager();
      if (manager == null) return text;
      NameProtect module = manager.getModule(NameProtect.class);
      return module != null && module.isEnabled() ? module.patchName(text) : text;
   }

   private static final long SNAPSHOT_REFRESH_NANOS = 50_000_000L;
   private static final int MAX_PATCH_CACHE_ENTRIES = 1024;
   private static final int MAX_CACHED_TEXT_LENGTH = 512;
   private volatile Snapshot snapshot;

   /**
    * Every input of {@link #patchName} except the text itself. Cheap fields are compared on every call;
    * the expensive ones (display name, tab list names) are re-read at most once per client tick (50 ms),
    * or immediately when a cheap field (settings, session, player/world/handler identity, tab size,
    * friend list version, server address) changes. A rebuilt snapshot equal to the previous one keeps
    * the previous text cache.
    */
   private static final class Snapshot {
      final long createdAt;
      final boolean own;
      final boolean friendsSel;
      final boolean serverSel;
      final String replacement;
      final Object session;
      final String username;
      final Object player;
      final Object world;
      final boolean inGame;
      final String displayName;
      final String playerName;
      final int friendVersion;
      final List<Pattern> friendPatterns;
      final String quotedReplacement;
      final String serverAddress;
      final Object networkHandler;
      final int tabSize;
      final String[] tabNames;
      ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

      Snapshot(NameProtect module, long now) {
         var friendManager = BKClientInit.getInstance().getFriendManager();
         this.createdAt = now;
         this.own = module.ownName.isSelected();
         this.friendsSel = module.friends.isSelected();
         this.serverSel = module.server.isSelected();
         this.replacement = module.fakeName.getText();
         this.session = mc.getSession();
         this.username = mc.getSession().getUsername();
         this.player = mc.player;
         this.world = mc.world;
         this.inGame = EntityUtility.isInGame() && mc.player != null;
         this.displayName = this.own && this.inGame ? mc.player.getDisplayName().getString() : null;
         this.playerName = this.own && this.inGame ? mc.player.getName().getString() : null;
         this.friendVersion = friendManager.getVersion();
         this.friendPatterns = this.friendsSel ? friendManager.getIgnoreCasePatterns() : List.of();
         this.quotedReplacement = this.friendsSel ? Matcher.quoteReplacement(this.replacement) : null;
         this.serverAddress = currentServerAddress();
         var handler = mc.getNetworkHandler();
         this.networkHandler = handler;
         if (this.serverSel && handler != null) {
            var list = handler.getPlayerList();
            List<String> names = new ArrayList<>(list.size());
            for (PlayerListEntry entry : list) {
               String otherName = entry.getProfile().getName();
               if (otherName.equals(this.username)) continue;
               boolean friend = friendManager.isFriend(otherName);
               if (!friend || this.friendsSel) {
                  names.add(otherName);
               }
            }
            this.tabSize = list.size();
            this.tabNames = names.toArray(new String[0]);
         } else {
            this.tabSize = handler != null ? handler.getPlayerList().size() : -1;
            this.tabNames = null;
         }
      }

      boolean cheapStillValid(NameProtect module, long now) {
         if (now - this.createdAt >= SNAPSHOT_REFRESH_NANOS) return false;
         if (this.own != module.ownName.isSelected()
            || this.friendsSel != module.friends.isSelected()
            || this.serverSel != module.server.isSelected()) return false;
         if (!Objects.equals(this.replacement, module.fakeName.getText())) return false;
         if (this.session != mc.getSession() || this.player != mc.player || this.world != mc.world) return false;
         if (this.friendVersion != BKClientInit.getInstance().getFriendManager().getVersion()) return false;
         if (!Objects.equals(this.serverAddress, currentServerAddress())) return false;
         var handler = mc.getNetworkHandler();
         if (this.networkHandler != handler) return false;
         return handler == null || this.tabSize == handler.getPlayerList().size();
      }

      boolean sameContent(Snapshot o) {
         return this.own == o.own && this.friendsSel == o.friendsSel && this.serverSel == o.serverSel
            && this.inGame == o.inGame
            && Objects.equals(this.replacement, o.replacement)
            && Objects.equals(this.username, o.username)
            && Objects.equals(this.displayName, o.displayName)
            && Objects.equals(this.playerName, o.playerName)
            && this.friendPatterns == o.friendPatterns
            && Objects.equals(this.serverAddress, o.serverAddress)
            && Arrays.equals(this.tabNames, o.tabNames);
      }

      private static String currentServerAddress() {
         var entry = mc.getCurrentServerEntry();
         return entry != null ? entry.address : null;
      }

      /** Same steps, order and String/regex calls as the original per-call implementation. */
      String apply(String text) {
         String replacement = this.replacement;
         if (this.own) {
            if (this.inGame) {
               text = text.replace(this.displayName, replacement);
               text = text.replace(this.playerName, replacement);
            }
            text = text.replace(this.username, replacement);
         }

         if (this.friendsSel) {
            // Sequential per friend (not one alternation): keeps overlapping-name semantics.
            for (Pattern pattern : this.friendPatterns) {
               text = pattern.matcher(text).replaceAll(this.quotedReplacement);
            }
         }

         if (this.serverSel) {
            if (this.serverAddress != null) {
               text = text.replace(this.serverAddress, "Server");
            }
            if (this.tabNames != null) {
               for (String otherName : this.tabNames) {
                  text = text.replace(otherName, replacement);
               }
            }
         }
         return text;
      }
   }

   private Snapshot currentSnapshot() {
      long now = System.nanoTime();
      Snapshot snap = this.snapshot;
      if (snap != null && snap.cheapStillValid(this, now)) {
         return snap;
      }
      Snapshot fresh = new Snapshot(this, now);
      if (snap != null && fresh.sameContent(snap)) {
         fresh.cache = snap.cache;
      }
      this.snapshot = fresh;
      return fresh;
   }

   public String patchName(String text) {
      if (text == null || text.isEmpty()) {
         return text;
      }

      Snapshot snap = this.currentSnapshot();
      if (text.length() > MAX_CACHED_TEXT_LENGTH) {
         return snap.apply(text);
      }
      ConcurrentHashMap<String, String> cache = snap.cache;
      String cached = cache.get(text);
      if (cached != null) {
         return cached;
      }
      String patched = snap.apply(text);
      if (cache.size() >= MAX_PATCH_CACHE_ENTRIES) {
         cache.clear();
      }
      cache.put(text, patched);
      return patched;
   }

   @Generated
   public StringSetting getFakeName() {
      return this.fakeName;
   }
}
