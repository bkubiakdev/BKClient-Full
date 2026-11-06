package dev.bkclient.systems.friends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import dev.bkclient.BKClientInit;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.game.ClientTelemetryUtility;
import dev.bkclient.utility.game.EntityUtility;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;

public class FriendManager implements IMinecraft {
   private final List<String> friends = new ArrayList<>();
   /** Immutable lookup index, replaced whenever {@link #friends} changes. */
   private volatile FriendIndex index = FriendIndex.EMPTY;

   /** Lowercased ASCII names; non-ASCII entries force the exact equalsIgnoreCase scan. */
   private record FriendIndex(int version, Set<String> lowerAscii, boolean anyNonAscii, List<Pattern> ignoreCasePatterns) {
      static final FriendIndex EMPTY = new FriendIndex(0, Set.of(), false, List.of());
   }

   private void reindex() {
      Set<String> lower = new HashSet<>();
      List<Pattern> patterns = new ArrayList<>();
      boolean nonAscii = false;
      for (String friend : this.friends) {
         if (isAscii(friend)) {
            lower.add(friend.toLowerCase(Locale.ROOT));
         } else {
            nonAscii = true;
         }
         if (!friend.isBlank()) {
            patterns.add(Pattern.compile("(?i)" + Pattern.quote(friend)));
         }
      }
      this.index = new FriendIndex(this.index.version() + 1, lower, nonAscii, Collections.unmodifiableList(patterns));
   }

   private static boolean isAscii(String s) {
      for (int i = 0; i < s.length(); i++) {
         if (s.charAt(i) >= 128) return false;
      }
      return true;
   }

   /** Incremented on every change of the friend list. */
   public int getVersion() {
      return this.index.version();
   }

   /**
    * Precompiled {@code "(?i)" + Pattern.quote(friend)} for every non-blank friend, in list order;
    * equivalent to the per-call {@code String.replaceAll} the text filters used before.
    */
   public List<Pattern> getIgnoreCasePatterns() {
      return this.index.ignoreCasePatterns();
   }

   public void add(String name) {
      if (name == null || name.isBlank()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.usage")));
      } else if (BKClientInit.getInstance().getTargetManager().getTarget().stream().anyMatch(target -> target.equalsIgnoreCase(name))) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.target")));
      } else if (this.isFriend(name)) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.exists", name)));
      } else if (name.equalsIgnoreCase(mc.getSession().getUsername())) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.self")));
      } else {
         this.friends.add(name);
         this.reindex();
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.added", name)));
         if (EntityUtility.isInGame()) {
            BKClientInit.getInstance().getFileManager().writeFile("client");
         }
         ClientTelemetryUtility.refreshNow();
      }
   }

   public void remove(String name) {
      if (name != null && this.friends.removeIf(friend -> friend.equalsIgnoreCase(name))) {
         this.reindex();
         ClientTelemetryUtility.forgetFriendPresence(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.removed", name)));
         ClientTelemetryUtility.refreshNow();
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.not_exists", name)));
      }

      BKClientInit.getInstance().getFileManager().writeFile("client");
   }

   public void clear() {
      if (this.friends.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         this.friends.clear();
         this.reindex();
         MessageUtility.info(Text.of("Friend list cleared successfully!"));
         BKClientInit.getInstance().getFileManager().writeFile("client");
         ClientTelemetryUtility.refreshNow();
      }
   }

   public List<String> listFriends() {
      return Collections.unmodifiableList(this.friends);
   }

   public boolean isFriend(String name) {
      if (name == null) return false;
      FriendIndex idx = this.index;
      // For two ASCII strings equalsIgnoreCase is exactly lowercase equality. Non-ASCII chars can
      // case-fold onto ASCII ones (e.g. U+0131, U+212A), so then keep the original scan.
      if (!idx.anyNonAscii() && isAscii(name)) {
         return idx.lowerAscii().contains(name.toLowerCase(Locale.ROOT));
      }
      return this.friends.stream().anyMatch(friend -> friend.equalsIgnoreCase(name));
   }
}
