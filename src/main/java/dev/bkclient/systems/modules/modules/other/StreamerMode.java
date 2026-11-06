package dev.bkclient.systems.modules.modules.other;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomScreen;
import dev.bkclient.systems.modules.api.ModuleCategory;
import dev.bkclient.systems.modules.api.ModuleInfo;
import dev.bkclient.systems.modules.impl.BaseModule;
import dev.bkclient.systems.setting.settings.BooleanSetting;
import dev.bkclient.systems.setting.settings.StringSetting;
import dev.bkclient.utility.game.EntityUtility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(name = "Streamer Mode", category = ModuleCategory.OTHER, desc = "Masks nickname, friends, server IP and all coordinates in F3, chat and HUD")
public class StreamerMode extends BaseModule {
    private static StreamerMode INSTANCE;

    public final BooleanSetting hideSelf = new BooleanSetting(this, "Hide My Nickname").enabled(true);
    public final StringSetting fakeName = new StringSetting(this, "Fake Name").text("Protected");
    public final BooleanSetting hideFriends = new BooleanSetting(this, "Hide Friends").enabled(true);
    public final BooleanSetting hideCoords = new BooleanSetting(this, "Hide Coordinates (F3 & HUD)").enabled(true);
    public final BooleanSetting hideServer = new BooleanSetting(this, "Hide Server IP").enabled(true);

    public StreamerMode() {
        INSTANCE = this;
    }

    public static StreamerMode getInstance() {
        return INSTANCE;
    }

    public static boolean isHideCoords() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.hideCoords.isEnabled();
    }

    public static boolean isHideServer() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.hideServer.isEnabled();
    }

    public String filterText(String text) {
        if (!isEnabled() || text == null || text.isEmpty() || mc.currentScreen instanceof CustomScreen) {
            return text;
        }

        String replacement = fakeName.getText();
        if (replacement.isEmpty()) {
            replacement = "Protected";
        }

        if (hideSelf.isEnabled()) {
            String username = mc.getSession().getUsername();
            if (EntityUtility.isInGame() && mc.player != null) {
                text = text.replace(mc.player.getDisplayName().getString(), replacement);
                text = text.replace(mc.player.getName().getString(), replacement);
            }
            text = text.replace(username, replacement);
        }

        if (hideFriends.isEnabled()) {
            String quotedReplacement = Matcher.quoteReplacement(replacement);
            // Precompiled "(?i)" + Pattern.quote(friend) per non-blank friend, same order as listFriends().
            for (Pattern pattern : BKClientInit.getInstance().getFriendManager().getIgnoreCasePatterns()) {
                text = pattern.matcher(text).replaceAll(quotedReplacement);
            }
        }

        if (hideServer.isEnabled()) {
            if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
                text = text.replace(mc.getCurrentServerEntry().address, "mc.protected-server.net");
            }
        }

        // Every coordinate pattern needs a literal ':' and the replacements only run on matches,
        // so text without ':' can never change here.
        if (hideCoords.isEnabled() && text.indexOf(':') >= 0) {
            text = COORDS_XYZ.matcher(text).replaceAll("$1: # / # / #");
            text = COORDS_BLOCK.matcher(text).replaceAll("Block: # # # [# # #]");
            text = COORDS_CHUNK.matcher(text).replaceAll("Chunk: # # # [# # in r.#.#.mca]");
            text = COORDS_CHUNK_RELATIVE.matcher(text).replaceAll("Chunk-relative: # # #");
            text = COORDS_TARGETED_BLOCK.matcher(text).replaceAll("Targeted Block: #, #, #");
            text = COORDS_TARGETED_FLUID.matcher(text).replaceAll("Targeted Fluid: #, #, #");
            text = COORDS_XYZ_LABELLED.matcher(text).replaceAll("X: #, Y: #, Z: #");
        }

        return text;
    }

    private static final Pattern COORDS_XYZ = Pattern.compile("(?i)(XYZ|Pos|Position|Coords?):\\s*[-0-9.]+\\s*[/, ]\\s*[-0-9.]+\\s*[/, ]\\s*[-0-9.]+");
    private static final Pattern COORDS_BLOCK = Pattern.compile("(?i)Block:\\s*[-0-9.]+\\s+[-0-9.]+\\s+[-0-9.]+(\\s*\\[[^\\]]*\\])?");
    private static final Pattern COORDS_CHUNK = Pattern.compile("(?i)Chunk:\\s*[-0-9.]+\\s+[-0-9.]+\\s+[-0-9.]+(\\s*\\[[^\\]]*\\])?");
    private static final Pattern COORDS_CHUNK_RELATIVE = Pattern.compile("(?i)Chunk-relative:\\s*[-0-9.]+\\s+[-0-9.]+\\s+[-0-9.]+");
    private static final Pattern COORDS_TARGETED_BLOCK = Pattern.compile("(?i)Targeted Block:\\s*[-0-9.]+\\s*[, ]\\s*[-0-9.]+\\s*[, ]\\s*[-0-9.]+");
    private static final Pattern COORDS_TARGETED_FLUID = Pattern.compile("(?i)Targeted Fluid:\\s*[-0-9.]+\\s*[, ]\\s*[-0-9.]+\\s*[, ]\\s*[-0-9.]+");
    private static final Pattern COORDS_XYZ_LABELLED = Pattern.compile("(?i)X:\\s*[-0-9.]+\\s*,?\\s*Y:\\s*[-0-9.]+\\s*,?\\s*Z:\\s*[-0-9.]+");
}
