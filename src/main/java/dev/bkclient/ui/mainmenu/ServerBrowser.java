package dev.bkclient.ui.mainmenu;

import static dev.bkclient.ui.mainmenu.ModernMenuLayout.GOLD;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.MUTED;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.ONLINE;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.PAPER;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.copy;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.ellipsize;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.glass;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.initials;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.joined;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.primaryGlass;
import static dev.bkclient.ui.mainmenu.ModernMenuLayout.width;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.ui.components.LucideIcons;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import java.io.IOException;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelSummary;

/**
 * Server and world picker for the new menu layout: a large card for the selected entry (icon, players online,
 * ping, MOTD) above a strip of all entries ending in an "add" tile, plus edit/delete and direct connect.
 * The clock, account chip and dock of the menu stay around it. Servers are read from and written to the
 * vanilla server list, so the regular Multiplayer screen stays in sync.
 */
final class ServerBrowser {
    private enum Modal { NONE, ADD, EDIT, DIRECT }

    private static final ColorRGBA OFFLINE = new ColorRGBA(248.0F, 113.0F, 113.0F);
    private static final ColorRGBA WARNING = new ColorRGBA(251.0F, 191.0F, 36.0F);
    // Background / letter colours for servers without a favicon, picked by name.
    private static final ColorRGBA[][] TINTS = {
        {new ColorRGBA(36.0F, 33.0F, 24.0F), GOLD},
        {new ColorRGBA(20.0F, 44.0F, 44.0F), new ColorRGBA(96.0F, 176.0F, 170.0F)},
        {new ColorRGBA(40.0F, 31.0F, 60.0F), new ColorRGBA(156.0F, 128.0F, 214.0F)},
        {new ColorRGBA(56.0F, 36.0F, 24.0F), new ColorRGBA(208.0F, 134.0F, 84.0F)},
        {new ColorRGBA(42.0F, 42.0F, 30.0F), new ColorRGBA(172.0F, 162.0F, 112.0F)},
        {new ColorRGBA(24.0F, 34.0F, 56.0F), new ColorRGBA(112.0F, 152.0F, 222.0F)}
    };
    private static final float TILE = 28.0F;
    private static final float TILE_GAP = 8.0F;
    private static final float PILL_H = 20.0F;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final ServerIconLoader pinger = new ServerIconLoader();
    private final Animation visibility = new Animation(260L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation heroFade = new Animation(220L, 1.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation modalFade = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Map<String, Animation> hovers = new HashMap<>();
    private final Map<String, Identifier> textures = new HashMap<>();
    private final Map<String, byte[]> textureBytes = new HashMap<>();

    private boolean open;
    private boolean worlds;
    private ServerList serverList;
    private final List<ServerInfo> servers = new ArrayList<>();
    private final List<LevelSummary> levels = new ArrayList<>();
    private boolean loadingWorlds;
    private String worldError;
    private int generation;
    private int selected;
    private float stripOffset;
    private boolean menuOpen;
    private boolean confirmDelete;

    private Modal modal = Modal.NONE;
    private final TextField nameField = new TextField(Fonts.MEDIUM.getFont(8.0F));
    private final TextField addressField = new TextField(Fonts.MEDIUM.getFont(8.0F));

    // Hit boxes of the last frame: x, y, width, height (tiles also carry their index).
    private final float[] backBox = new float[4];
    private final float[] directBox = new float[4];
    private final float[] joinBox = new float[4];
    private final float[] moreBox = new float[4];
    private final float[] prevBox = new float[4];
    private final float[] nextBox = new float[4];
    private final float[] addTile = new float[4];
    private final float[][] menuBoxes = new float[4][4];
    private final List<float[]> tileBoxes = new ArrayList<>();
    private final float[] modalPanel = new float[4];
    private final float[] cancelBox = new float[4];
    private final float[] confirmBox = new float[4];
    private int visibleTiles = 1;

    boolean isOpen() {
        return this.open || this.visibility.getValue() > 0.01F;
    }

    float visibility() {
        return this.visibility.getValue();
    }

    void open(boolean worlds) {
        this.open = true;
        this.closeModal();
        this.switchTo(worlds);
    }

    void close() {
        this.open = false;
        this.menuOpen = false;
        this.closeModal();
        this.pinger.stop();
    }

    void dispose() {
        this.close();
        this.generation++;
        for (Identifier id : this.textures.values()) if (id != null) this.mc.getTextureManager().destroyTexture(id);
        this.textures.clear();
        this.textureBytes.clear();
    }

    void tick() {
        if (this.open && !this.worlds) this.pinger.tick();
    }

    private void switchTo(boolean worlds) {
        this.worlds = worlds;
        this.selected = 0;
        this.stripOffset = 0.0F;
        this.menuOpen = false;
        this.confirmDelete = false;
        this.heroFade.reset(0.0F);
        if (worlds) {
            this.pinger.stop();
            this.loadWorlds();
        } else {
            this.loadServers(null);
        }
    }

    private void loadServers(String selectAddress) {
        this.serverList = new ServerList(this.mc);
        this.serverList.loadFile();
        this.servers.clear();
        for (int i = 0; i < this.serverList.size(); i++) this.servers.add(this.serverList.get(i));
        if (selectAddress != null) {
            for (int i = 0; i < this.servers.size(); i++) if (this.servers.get(i).address.equals(selectAddress)) this.selected = i;
        }
        this.selected = this.servers.isEmpty() ? 0 : Math.clamp(this.selected, 0, this.servers.size() - 1);
        this.pinger.start(this.servers);
    }

    private void loadWorlds() {
        this.levels.clear();
        this.worldError = null;
        this.loadingWorlds = true;
        int request = ++this.generation;
        // Vanilla's summary reader handles versions, locks and corrupt saves; no world is opened here.
        CompletableFuture.supplyAsync(() -> {
            try {
                return this.mc.getLevelStorage().getLevelList();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenCompose(list -> this.mc.getLevelStorage().loadSummaries(list)).whenComplete((list, error) -> this.mc.execute(() -> {
            if (request != this.generation || !this.worlds) return;
            this.loadingWorlds = false;
            if (error != null) {
                this.worldError = copy("Nie udało się wczytać światów", "Unable to read worlds");
                return;
            }
            list.stream().sorted().forEach(this.levels::add);
            this.selected = this.levels.isEmpty() ? 0 : Math.clamp(this.selected, 0, this.levels.size() - 1);
        }));
    }

    private int count() {
        return this.worlds ? this.levels.size() : this.servers.size();
    }

    private void select(int index) {
        if (this.count() == 0) return;
        int next = Math.floorMod(index, this.count());
        if (next == this.selected) return;
        this.selected = next;
        this.menuOpen = false;
        this.confirmDelete = false;
        this.heroFade.reset(0.0F);
    }


    /**
     * @param top    first free line below the menu clock
     * @param bottom last free line above the menu dock
     */
    void draw(UIContext ctx, int width, int height, float top, float bottom) {
        this.visibility.update(this.open);
        float alpha = this.visibility.getValue();
        if (alpha < 0.01F) return;
        this.heroFade.update(true);
        this.modalFade.update(this.modal != Modal.NONE);
        boolean interactive = this.open && this.modal == Modal.NONE;
        int mouseX = interactive ? ctx.getMouseX() : -1;
        int mouseY = interactive ? ctx.getMouseY() : -1;

        float barY = top + 8.0F;
        this.drawTopRow(ctx, barY, alpha, mouseX, mouseY);
        float areaTop = barY + PILL_H + 10.0F;
        float areaBottom = bottom - 8.0F;
        if (this.count() == 0) {
            this.drawEmpty(ctx, width, areaTop, areaBottom, alpha, mouseX, mouseY);
        } else {
            // Card and strip form one group, centered in the free area like in the menu.
            // The first card size whose group (card + strip) fits the free area; the smallest otherwise.
            int size = 0;
            while (size < HERO_ICON.length - 1 && heroHeight(size) + 24.0F + TILE + 18.0F > areaBottom - areaTop) size++;
            float heroH = heroHeight(size);
            float group = heroH + 24.0F + TILE + 18.0F;
            float groupY = areaTop + Math.max(0.0F, (areaBottom - areaTop - group) * 0.45F);
            this.drawHero(ctx, width, groupY, size, alpha, mouseX, mouseY);
            this.drawStrip(ctx, width, groupY + heroH + 24.0F, alpha, mouseX, mouseY);
            if (this.menuOpen && this.moreBox[2] > 0.0F) {
                this.drawEntryMenu(ctx, this.moreBox[0] + this.moreBox[2] / 2.0F, this.moreBox[1] + this.moreBox[3] + 6.0F, alpha, mouseX, mouseY);
            }
        }
        if (this.modalFade.getValue() > 0.01F) this.drawModal(ctx, width, height, alpha * this.modalFade.getValue());
    }

    /** "Menu" back pill below the clock and, for servers, direct connect next to it. */
    private void drawTopRow(UIContext ctx, float y, float alpha, int mouseX, int mouseY) {
        Font font = Fonts.MEDIUM.getFont(7.5F);
        String back = "Menu";
        float backW = 9.0F + 9.0F + 5.0F + width(font, back) + 10.0F;
        box(this.backBox, 16.0F, y, backW, PILL_H);
        glass(ctx, 16.0F, y, backW, PILL_H, PILL_H / 2.0F, alpha, this.hover("back", this.backBox, mouseX, mouseY));
        ctx.draw();
        LucideIcons.MENU_BACK.draw(ctx, 25.0F, y + 5.5F, 9.0F, ColorRGBA.WHITE.withAlpha(225.0F * alpha));
        ctx.drawText(font, back, 39.0F, y + GuiUtility.getMiddleOfBox(font.height(), PILL_H), ColorRGBA.WHITE.withAlpha(230.0F * alpha));
        if (this.worlds) {
            this.directBox[2] = 0.0F;
            return;
        }
        String direct = copy("Szybkie połączenie", "Direct connect");
        float directW = 9.0F + 9.0F + 5.0F + width(font, direct) + 10.0F;
        box(this.directBox, 16.0F + backW + 6.0F, y, directW, PILL_H);
        glass(ctx, this.directBox[0], y, directW, PILL_H, PILL_H / 2.0F, alpha, this.hover("direct", this.directBox, mouseX, mouseY));
        ctx.draw();
        LucideIcons.SEND.draw(ctx, this.directBox[0] + 9.0F, y + 5.5F, 9.0F, ColorRGBA.WHITE.withAlpha(215.0F * alpha));
        ctx.drawText(font, direct, this.directBox[0] + 23.0F, y + GuiUtility.getMiddleOfBox(font.height(), PILL_H), ColorRGBA.WHITE.withAlpha(230.0F * alpha));
    }

    private void drawEmpty(UIContext ctx, int width, float top, float bottom, float alpha, int mouseX, int mouseY) {
        float cy = (top + bottom) / 2.0F;
        String title = this.worlds
            ? (this.loadingWorlds ? copy("Wczytywanie światów...", "Loading worlds...")
                : this.worldError != null ? this.worldError : copy("Nie masz jeszcze światów", "No worlds yet"))
            : copy("Nie masz jeszcze serwerów", "No servers yet");
        ctx.drawCenteredText(Fonts.BOLD.getFont(14.0F), title, width / 2.0F, cy - 24.0F, ColorRGBA.WHITE.withAlpha(235.0F * alpha));
        this.moreBox[2] = this.addTile[2] = this.prevBox[2] = this.nextBox[2] = 0.0F;
        this.tileBoxes.clear();
        if (this.worlds && this.loadingWorlds) {
            this.joinBox[2] = 0.0F;
            return;
        }
        String action = this.worlds ? copy("Stwórz świat", "Create world") : copy("Dodaj serwer", "Add server");
        Font font = Fonts.SEMIBOLD.getFont(8.0F);
        float w = width(font, action) + 40.0F;
        box(this.joinBox, width / 2.0F - w / 2.0F, cy, w, 22.0F);
        float hover = this.hover("empty", this.joinBox, mouseX, mouseY);
        primaryGlass(ctx, this.joinBox[0], cy, w, 22.0F, 11.0F, alpha, hover);
        ctx.drawCenteredText(font, "+  " + action, width / 2.0F, cy + GuiUtility.getMiddleOfBox(font.height(), 22.0F), ColorRGBA.WHITE.withAlpha(250.0F * alpha));
    }

    private static final float TITLE_GAP = 7.0F;
    // Three sizes of the selected-entry card, the smaller ones for short windows: icon, title font, MOTD line.
    private static final float[] HERO_ICON = {52.0F, 38.0F, 28.0F};
    private static final float[] HERO_TITLE = {24.0F, 18.0F, 14.0F};
    private static final boolean[] HERO_DETAIL = {true, true, false};

    private static float heroHeight(int size) {
        return HERO_ICON[size] + 10.0F + Fonts.BOLD.getFont(HERO_TITLE[size]).height() + TITLE_GAP + Fonts.REGULAR.getFont(7.0F).height()
            * (HERO_DETAIL[size] ? 2.0F : 1.0F) + 8.0F + Fonts.MEDIUM.getFont(7.5F).height() + 6.0F + (HERO_DETAIL[size] ? 0.0F : -3.0F) + 11.0F + 22.0F;
    }

    private void drawHero(UIContext ctx, int width, float top, int size, float alpha, int mouseX, int mouseY) {
        float fade = alpha * this.heroFade.getValue();
        float rise = (1.0F - this.heroFade.getValue()) * 6.0F;
        float centerX = width / 2.0F;
        float iconSize = HERO_ICON[size];
        Font title = Fonts.BOLD.getFont(HERO_TITLE[size]);
        Font mono = Fonts.REGULAR.getFont(7.0F);
        Font status = Fonts.MEDIUM.getFont(7.5F);
        float y = top + rise;

        for (int layer = 3; layer > 0; layer--) {
            float spread = layer * 3.5F;
            ctx.drawRoundedRect(centerX - iconSize / 2.0F - spread, y - spread + 4.0F, iconSize + spread * 2.0F, iconSize + spread * 2.0F,
                BorderRadius.all(13.0F + spread), ColorRGBA.BLACK.withAlpha(15.0F * fade));
        }
        float iconX = centerX - iconSize / 2.0F;
        this.drawTileArt(ctx, this.selected, iconX, y, iconSize, 12.0F, fade, 1.0F, 18.0F);
        y += iconSize + 10.0F;

        String name = this.entryName(this.selected);
        ctx.drawCenteredText(title, ellipsize(title, name, width - 80.0F), centerX, y, PAPER.withAlpha(252.0F * fade));
        // The bold face draws below its nominal height, so the address keeps a little extra room.
        y += title.height() + TITLE_GAP;
        String subtitle = this.worlds ? this.levels.get(this.selected).getName() : this.servers.get(this.selected).address;
        ctx.drawCenteredText(mono, subtitle, centerX, y, MUTED.withAlpha(225.0F * fade));
        y += mono.height() + 8.0F;

        if (this.worlds) this.drawWorldInfo(ctx, status, centerX, y, fade);
        else this.drawServerStatus(ctx, status, centerX, y, fade);
        y += status.height() + 6.0F;

        if (HERO_DETAIL[size]) {
            String detail = this.worlds ? this.worldDetails(this.levels.get(this.selected)) : this.motd(this.servers.get(this.selected));
            if (!detail.isBlank()) ctx.drawCenteredText(mono, ellipsize(mono, detail, Math.min(190.0F, width - 60.0F)), centerX, y, MUTED.withAlpha(190.0F * fade));
            y += mono.height() + 11.0F;
        } else {
            y += 8.0F;
        }

        this.drawHeroButtons(ctx, centerX, y, fade, mouseX, mouseY);
    }

    private void drawServerStatus(UIContext ctx, Font font, float centerX, float y, float alpha) {
        ServerInfo server = this.servers.get(this.selected);
        ColorRGBA dot;
        String players;
        String ping = null;
        if (server.players != null) {
            dot = ONLINE;
            players = formatCount(server.players.online()) + " online";
            if (server.ping >= 0L) ping = server.ping + " ms";
        } else if (server.getStatus() == ServerInfo.Status.INCOMPATIBLE) {
            dot = WARNING;
            players = copy("Inna wersja", "Incompatible version");
        } else if (server.getStatus() == ServerInfo.Status.UNREACHABLE) {
            dot = OFFLINE;
            players = "Offline";
        } else {
            float pulse = 0.5F + 0.5F * (float)Math.sin(System.nanoTime() / 1.6E8D);
            dot = MUTED.withAlpha(110.0F + 145.0F * pulse);
            players = copy("Łączenie...", "Pinging...");
        }
        float textW = width(font, players);
        float pingW = ping == null ? 0.0F : 14.0F + 9.0F + width(font, ping);
        float startX = centerX - (8.0F + textW + pingW) / 2.0F;
        float mid = y + font.height() * 0.5F;
        ctx.drawRoundedRect(startX, mid - 2.0F, 4.0F, 4.0F, BorderRadius.all(2.0F), dot.withAlpha(dot.getAlpha() * alpha));
        ctx.drawText(font, players, startX + 8.0F, y, ColorRGBA.WHITE.withAlpha(215.0F * alpha));
        if (ping != null) {
            float barsX = startX + 8.0F + textW + 14.0F;
            int lit = server.ping < 80L ? 4 : server.ping < 150L ? 3 : server.ping < 300L ? 2 : 1;
            ColorRGBA barColor = lit >= 3 ? ONLINE : lit == 2 ? WARNING : OFFLINE;
            for (int i = 0; i < 4; i++) {
                float barH = 2.0F + i * 1.5F;
                ctx.drawRoundedRect(barsX + i * 2.0F, mid + 3.0F - barH, 1.2F, barH, BorderRadius.all(0.6F),
                    (i < lit ? barColor : MUTED).withAlpha((i < lit ? 255.0F : 80.0F) * alpha));
            }
            ctx.drawText(font, ping, barsX + 10.0F, y, ColorRGBA.WHITE.withAlpha(215.0F * alpha));
        }
    }

    private void drawWorldInfo(UIContext ctx, Font font, float centerX, float y, float alpha) {
        LevelSummary level = this.levels.get(this.selected);
        String mode = level.isHardcore() ? "Hardcore" : switch (level.getGameMode()) {
            case CREATIVE -> copy("Kreatywny", "Creative");
            case ADVENTURE -> copy("Przygoda", "Adventure");
            case SPECTATOR -> copy("Obserwator", "Spectator");
            default -> copy("Przetrwanie", "Survival");
        };
        String played = level.getLastPlayed() > 0L ? new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(level.getLastPlayed())) : "-";
        String[] parts = {mode, copy("Ostatnio ", "Last played ") + played};
        float w = joined(ctx, font, 0.0F, 0.0F, ColorRGBA.WHITE, true, parts);
        joined(ctx, font, centerX - w / 2.0F, y, ColorRGBA.WHITE.withAlpha(215.0F * alpha), false, parts);
    }

    private void drawHeroButtons(UIContext ctx, float centerX, float y, float alpha, int mouseX, int mouseY) {
        Font font = Fonts.SEMIBOLD.getFont(8.0F);
        String label = this.worlds ? copy("Graj", "Play") : copy("Dołącz", "Join");
        float h = 22.0F;
        float joinW = width(font, label) + 44.0F;
        float x = centerX - (joinW + 6.0F + h) / 2.0F;
        box(this.joinBox, x, y, joinW, h);
        box(this.moreBox, x + joinW + 6.0F, y, h, h);
        float joinHover = this.hover("join", this.joinBox, mouseX, mouseY);
        float moreHover = this.hover("more", this.moreBox, mouseX, mouseY);
        primaryGlass(ctx, x, y, joinW, h, h / 2.0F, alpha, joinHover);
        float contentW = 8.0F + 6.0F + width(font, label);
        float contentX = x + (joinW - contentW) / 2.0F;
        ctx.draw();
        ctx.drawIcon(Fonts.ICONS, ModernMenuLayout.ICON_PLAY, contentX, y + 7.0F, 8.0F, ColorRGBA.WHITE.withAlpha(250.0F * alpha));
        ctx.drawText(font, label, contentX + 14.0F, y + GuiUtility.getMiddleOfBox(font.height(), h), ColorRGBA.WHITE.withAlpha(250.0F * alpha));
        glass(ctx, this.moreBox[0], y, h, h, h / 2.0F, alpha, Math.max(moreHover, this.menuOpen ? 1.0F : 0.0F));
        ctx.draw();
        ctx.drawIcon(Fonts.ICONS, ModernMenuLayout.ICON_MORE, this.moreBox[0] + 6.0F, y + 6.0F, 10.0F, ColorRGBA.WHITE.withAlpha(230.0F * alpha));
        // The entry menu is drawn after the strip (see draw) so the tiles never cover it.
        if (!this.menuOpen) for (float[] box : this.menuBoxes) box[2] = 0.0F;
    }

    private String[] menuLabels() {
        String delete = this.confirmDelete ? copy("Na pewno usunąć?", "Really delete?") : copy("Usuń", "Delete");
        return new String[]{copy("Edytuj", "Edit"), copy("Odśwież", "Refresh"), delete, null};
    }

    private void drawEntryMenu(UIContext ctx, float centerX, float y, float alpha, int mouseX, int mouseY) {
        Font font = Fonts.MEDIUM.getFont(7.5F);
        String[] labels = this.menuLabels();
        float w = 0.0F;
        for (String label : labels) if (label != null) w = Math.max(w, width(font, label));
        w += 24.0F;
        float rowH = 17.0F;
        int rows = 0;
        for (String label : labels) if (label != null) rows++;
        float x = centerX - w / 2.0F;
        float h = rows * rowH + 6.0F;
        glass(ctx, x, y, w, h, 8.0F, alpha, 0.0F);
        // A dark veil keeps the labels readable over the server tiles and bright backgrounds.
        ctx.drawRoundedRect(x, y, w, h, BorderRadius.all(8.0F), ColorRGBA.BLACK.withAlpha(150.0F * alpha));
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == null) {
                this.menuBoxes[i][2] = 0.0F;
                continue;
            }
            float rowY = y + 3.0F + i * rowH;
            box(this.menuBoxes[i], x + 3.0F, rowY, w - 6.0F, rowH);
            float hover = this.hover("menu" + i, this.menuBoxes[i], mouseX, mouseY);
            if (hover > 0.01F) ctx.drawRoundedRect(x + 3.0F, rowY, w - 6.0F, rowH, BorderRadius.all(5.0F), ColorRGBA.WHITE.withAlpha(18.0F * hover * alpha));
            ColorRGBA color = i == 2 ? OFFLINE : ColorRGBA.WHITE;
            ctx.drawText(font, labels[i], x + 11.0F, rowY + GuiUtility.getMiddleOfBox(font.height(), rowH), color.withAlpha(235.0F * alpha));
        }
    }

    /** Entries in pages, the last slot of every page view ends with an "add" tile. */
    private void drawStrip(UIContext ctx, int width, float y, float alpha, int mouseX, int mouseY) {
        int count = this.count();
        int slots = Math.max(3, (int)((width - 120.0F) / (TILE + TILE_GAP)));
        // One slot is reserved for the add tile.
        int visible = Math.max(1, Math.min(count, Math.min(slots - 1, 7)));
        this.visibleTiles = visible;
        int page = this.selected / visible;
        int pages = (count + visible - 1) / visible;
        float target = page * visible;
        this.stripOffset += (target - this.stripOffset) * 0.25F;
        if (Math.abs(target - this.stripOffset) < 0.01F) this.stripOffset = target;

        float stripW = (visible + 1) * TILE + visible * TILE_GAP;
        float startX = width / 2.0F - stripW / 2.0F;
        this.tileBoxes.clear();
        for (int i = 0; i < count; i++) {
            float slot = i - this.stripOffset;
            if (slot < -0.5F || slot > visible - 0.5F) continue;
            float x = startX + slot * (TILE + TILE_GAP);
            float edge = Math.min(1.0F, Math.min(slot + 0.5F, visible - 0.5F - slot) * 2.0F);
            float tileAlpha = alpha * Math.max(0.0F, edge);
            boolean isSelected = i == this.selected;
            float[] tile = {x, y, TILE, TILE, i};
            this.tileBoxes.add(tile);
            float hover = this.hover("tile" + i, tile, mouseX, mouseY);
            float dim = isSelected ? 1.0F : 0.55F + 0.45F * hover;
            // The selected entry sits on a lit glass plate instead of an outline.
            if (isSelected) primaryGlass(ctx, x - 4.0F, y - 4.0F, TILE + 8.0F, TILE + 8.0F, 11.0F, tileAlpha, 0.4F);
            this.drawTileArt(ctx, i, x, y, TILE, 8.0F, tileAlpha, dim, 9.0F);
        }

        float addX = startX + visible * (TILE + TILE_GAP);
        box(this.addTile, addX, y, TILE, TILE);
        float addHover = this.hover("addtile", this.addTile, mouseX, mouseY);
        glass(ctx, addX, y, TILE, TILE, 8.0F, alpha, addHover);
        Font plus = Fonts.REGULAR.getFont(14.0F);
        ctx.drawCenteredText(plus, "+", addX + TILE / 2.0F, y + GuiUtility.getMiddleOfBox(plus.height(), TILE) - 0.5F,
            ColorRGBA.WHITE.withAlpha((150.0F + 90.0F * addHover) * alpha));

        float arrowY = y + TILE / 2.0F - 9.0F;
        box(this.prevBox, startX - 26.0F, arrowY, 18.0F, 18.0F);
        box(this.nextBox, startX + stripW + 8.0F, arrowY, 18.0F, 18.0F);
        Font counterFont = Fonts.REGULAR.getFont(6.5F);
        if (pages > 1) {
            this.drawArrow(ctx, this.prevBox, "<", page > 0, alpha, mouseX, mouseY);
            this.drawArrow(ctx, this.nextBox, ">", page < pages - 1, alpha, mouseX, mouseY);
            String[] counter = {String.valueOf(page + 1), "/", String.valueOf(pages)};
            String text = counter[0] + " " + counter[1] + " " + counter[2];
            ctx.drawCenteredText(counterFont, text, width / 2.0F, y + TILE + 9.0F, MUTED.withAlpha(200.0F * alpha));
        } else {
            this.prevBox[2] = 0.0F;
            this.nextBox[2] = 0.0F;
        }
    }

    /** Favicon / world icon, or the entry's initials on a tinted tile. */
    private void drawTileArt(UIContext ctx, int index, float x, float y, float size, float radius, float alpha, float dim, float letterSize) {
        Identifier texture = this.texture(index);
        if (texture != null) {
            ctx.drawRoundedTexture(texture, x, y, size, size, BorderRadius.all(radius), ColorRGBA.WHITE.withAlpha(255.0F * alpha * dim));
            return;
        }
        String name = this.entryName(index);
        ColorRGBA[] tint = TINTS[Math.floorMod(name.hashCode(), TINTS.length)];
        glass(ctx, x, y, size, size, radius, alpha * (0.6F + 0.4F * dim), 0.0F);
        Font letters = Fonts.BOLD.getFont(letterSize);
        ctx.drawCenteredText(letters, initials(name), x + size / 2.0F, y + GuiUtility.getMiddleOfBox(letters.height(), size),
            tint[1].withAlpha(245.0F * alpha * dim));
    }

    private void drawArrow(UIContext ctx, float[] box, String glyph, boolean enabled, float alpha, int mouseX, int mouseY) {
        float hover = enabled ? this.hover("arrow" + glyph, box, mouseX, mouseY) : 0.0F;
        if (!enabled) box[2] = 0.0F;
        Font font = Fonts.REGULAR.getFont(11.0F);
        ctx.drawCenteredText(font, glyph, box[0] + 9.0F, box[1] + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
            ColorRGBA.WHITE.withAlpha((enabled ? 150.0F + 105.0F * hover : 45.0F) * alpha));
    }

    private void drawModal(UIContext ctx, int width, int height, float alpha) {
        ctx.drawRoundedRect(0.0F, 0.0F, width, height, BorderRadius.ZERO, ColorRGBA.BLACK.withAlpha(110.0F * alpha));
        boolean direct = this.modal == Modal.DIRECT;
        float w = 240.0F;
        float h = direct ? 112.0F : 150.0F;
        float x = width / 2.0F - w / 2.0F;
        float y = height / 2.0F - h / 2.0F + (1.0F - alpha) * 8.0F;
        box(this.modalPanel, x, y, w, h);
        glass(ctx, x, y, w, h, 12.0F, alpha, 0.0F);
        ctx.drawRoundedRect(x, y, w, h, BorderRadius.all(12.0F), ColorRGBA.BLACK.withAlpha(70.0F * alpha));

        Font titleFont = Fonts.BOLD.getFont(10.5F);
        String title = switch (this.modal) {
            case ADD -> copy("Dodaj serwer", "Add server");
            case EDIT -> copy("Edytuj serwer", "Edit server");
            default -> copy("Szybkie połączenie", "Direct connect");
        };
        ctx.drawText(titleFont, title, x + 14.0F, y + 13.0F, PAPER.withAlpha(250.0F * alpha));
        float fieldY = y + 34.0F;
        if (!direct) {
            this.drawField(ctx, this.nameField, copy("Nazwa", "Name"), x + 14.0F, fieldY, w - 28.0F, alpha);
            fieldY += 38.0F;
        }
        this.drawField(ctx, this.addressField, copy("Adres serwera", "Server address"), x + 14.0F, fieldY, w - 28.0F, alpha);

        Font font = Fonts.SEMIBOLD.getFont(7.5F);
        float buttonH = 20.0F;
        float buttonY = y + h - 14.0F - buttonH;
        String confirm = direct ? copy("Połącz", "Connect") : copy("Zapisz", "Save");
        String cancel = copy("Anuluj", "Cancel");
        float confirmW = width(font, confirm) + 28.0F;
        float cancelW = width(font, cancel) + 28.0F;
        box(this.confirmBox, x + w - 14.0F - confirmW, buttonY, confirmW, buttonH);
        box(this.cancelBox, this.confirmBox[0] - 6.0F - cancelW, buttonY, cancelW, buttonH);
        int mouseX = ctx.getMouseX();
        int mouseY = ctx.getMouseY();
        boolean ready = !this.addressField.getBuiltText().isBlank();
        float confirmHover = ready ? this.hover("confirm", this.confirmBox, mouseX, mouseY) : 0.0F;
        float cancelHover = this.hover("cancel", this.cancelBox, mouseX, mouseY);
        glass(ctx, this.cancelBox[0], buttonY, cancelW, buttonH, 6.0F, alpha, cancelHover);
        ctx.drawCenteredText(font, cancel, this.cancelBox[0] + cancelW / 2.0F, buttonY + GuiUtility.getMiddleOfBox(font.height(), buttonH), ColorRGBA.WHITE.withAlpha(225.0F * alpha));
        if (ready) primaryGlass(ctx, this.confirmBox[0], buttonY, confirmW, buttonH, 6.0F, alpha, confirmHover);
        else glass(ctx, this.confirmBox[0], buttonY, confirmW, buttonH, 6.0F, alpha * 0.6F, 0.0F);
        ctx.drawCenteredText(font, confirm, this.confirmBox[0] + confirmW / 2.0F, buttonY + GuiUtility.getMiddleOfBox(font.height(), buttonH),
            ColorRGBA.WHITE.withAlpha((ready ? 250.0F : 110.0F) * alpha));
    }

    private void drawField(UIContext ctx, TextField field, String label, float x, float y, float w, float alpha) {
        ctx.drawText(Fonts.REGULAR.getFont(6.5F), label, x + 1.0F, y, MUTED.withAlpha(230.0F * alpha));
        float fieldY = y + 10.0F;
        float fieldH = 20.0F;
        glass(ctx, x, fieldY, w, fieldH, 6.0F, alpha, field.isFocused() ? 0.7F : 0.0F);
        field.set(x + 4.0F, fieldY, w - 8.0F, fieldH);
        field.setAlpha(alpha);
        field.render(ctx);
    }


    void click(double x, double y, MouseButton button, Screen parent) {
        if (!this.open) return;
        if (this.modal != Modal.NONE) {
            this.clickModal(x, y, button, parent);
            return;
        }
        if (button == MouseButton.RIGHT) {
            this.close();
            return;
        }
        if (button != MouseButton.LEFT) return;
        if (this.menuOpen) {
            String[] labels = this.menuLabels();
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] != null && inside(this.menuBoxes[i], x, y)) {
                    this.runMenu(i, parent);
                    return;
                }
            }
            this.menuOpen = false;
            this.confirmDelete = false;
            if (!inside(this.moreBox, x, y)) return;
        }
        if (inside(this.backBox, x, y)) this.close();
        else if (inside(this.directBox, x, y)) this.openModal(Modal.DIRECT);
        else if (inside(this.addTile, x, y)) this.add(parent);
        else if (inside(this.joinBox, x, y)) { if (this.count() == 0) this.add(parent); else this.join(parent); }
        else if (inside(this.moreBox, x, y)) this.menuOpen = !this.menuOpen;
        else if (inside(this.prevBox, x, y)) this.select(Math.max(0, this.selected - this.visibleTiles));
        else if (inside(this.nextBox, x, y)) this.select(Math.min(this.count() - 1, this.selected + this.visibleTiles));
        else {
            for (float[] tile : this.tileBoxes) {
                if (inside(tile, x, y)) {
                    int index = (int)tile[4];
                    if (index == this.selected) this.join(parent); else this.select(index);
                    return;
                }
            }
        }
    }

    private void runMenu(int index, Screen parent) {
        if (this.worlds) {
            LevelSummary level = this.levels.get(this.selected);
            if (index == 2 && !this.confirmDelete) {
                // Deleting a world cannot be undone, so the first click only asks again.
                this.confirmDelete = true;
                return;
            }
            this.menuOpen = false;
            this.confirmDelete = false;
            switch (index) {
                case 0 -> this.editWorld(level, parent);
                case 1 -> this.loadWorlds();
                default -> this.deleteWorld(level);
            }
            return;
        }
        ServerInfo server = this.servers.get(this.selected);
        if (index == 0) {
            this.menuOpen = false;
            this.nameField.setText(server.name);
            this.addressField.setText(server.address);
            this.openModal(Modal.EDIT);
        } else if (index == 1) {
            this.menuOpen = false;
            this.loadServers(server.address);
        } else if (!this.confirmDelete) {
            this.confirmDelete = true;
        } else {
            this.menuOpen = false;
            this.confirmDelete = false;
            this.serverList.remove(server);
            this.serverList.saveFile();
            this.selected = Math.max(0, this.selected - 1);
            this.loadServers(null);
            this.heroFade.reset(0.0F);
        }
    }

    /** Vanilla's world editor (rename, backup, open folder, optimize); the list is read again when it closes. */
    private void editWorld(LevelSummary level, Screen parent) {
        LevelStorage.Session session;
        try {
            session = this.mc.getLevelStorage().createSession(level.getName());
        } catch (IOException | SymlinkValidationException e) {
            SystemToast.addWorldAccessFailureToast(this.mc, level.getName());
            return;
        }
        try {
            this.mc.setScreen(EditWorldScreen.create(this.mc, session, edited -> {
                session.tryClose();
                this.loadWorlds();
                this.mc.setScreen(parent);
            }));
        } catch (IOException e) {
            session.tryClose();
            SystemToast.addWorldAccessFailureToast(this.mc, level.getName());
        }
    }

    /** Removes the world folder the same way the vanilla world list does, then reads the list again. */
    private void deleteWorld(LevelSummary level) {
        try (LevelStorage.Session session = this.mc.getLevelStorage().createSessionWithoutSymlinkCheck(level.getName())) {
            session.deleteSessionLock();
        } catch (IOException e) {
            SystemToast.addWorldDeleteFailureToast(this.mc, level.getName());
        }
        this.selected = Math.max(0, this.selected - 1);
        Identifier icon = this.textures.remove("w:" + level.getName());
        if (icon != null) this.mc.getTextureManager().destroyTexture(icon);
        this.loadWorlds();
        this.heroFade.reset(0.0F);
    }

    private void clickModal(double x, double y, MouseButton button, Screen parent) {
        if (inside(this.cancelBox, x, y)) {
            this.closeModal();
            return;
        }
        if (inside(this.confirmBox, x, y)) {
            this.confirmModal(parent);
            return;
        }
        if (!inside(this.modalPanel, x, y)) {
            this.closeModal();
            return;
        }
        if (this.modal != Modal.DIRECT) {
            this.nameField.onMouseClicked(x, y, button);
            if (!this.nameField.isFocused()) this.nameField.setFocused(false);
        }
        this.addressField.onMouseClicked(x, y, button);
        if (!this.addressField.isFocused()) this.addressField.setFocused(false);
    }

    void mouseReleased(double x, double y, MouseButton button) {
        this.nameField.onMouseReleased(x, y, button);
        this.addressField.onMouseReleased(x, y, button);
    }

    boolean key(int keyCode, int scanCode, int modifiers, Screen parent) {
        if (this.modal != Modal.NONE) {
            if (keyCode == 256) this.closeModal();
            else if (keyCode == 257 || keyCode == 335) this.confirmModal(parent);
            else if (keyCode == 258 && this.modal != Modal.DIRECT) {
                boolean toAddress = this.nameField.isFocused();
                this.nameField.setFocused(!toAddress);
                this.addressField.setFocused(toAddress);
            } else {
                this.nameField.onKeyPressed(keyCode, scanCode, modifiers);
                this.addressField.onKeyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (keyCode == 256) {
            if (this.menuOpen) this.menuOpen = false; else this.close();
        } else if (keyCode == 263) {
            this.select(this.selected - 1);
        } else if (keyCode == 262) {
            this.select(this.selected + 1);
        } else if (keyCode == 257 || keyCode == 335) {
            this.join(parent);
        }
        return true;
    }

    boolean charTyped(char chr, int modifiers) {
        if (this.modal == Modal.NONE) return true;
        if (this.nameField.isFocused()) return this.nameField.charTyped(chr, modifiers);
        if (this.addressField.isFocused()) return this.addressField.charTyped(chr, modifiers);
        return true;
    }

    void scroll(double amount) {
        if (this.open && this.modal == Modal.NONE && amount != 0.0D) this.select(this.selected + (amount > 0.0D ? -1 : 1));
    }


    private void add(Screen parent) {
        if (this.worlds) {
            this.close();
            CreateWorldScreen.show(this.mc, parent);
            return;
        }
        this.nameField.setText("");
        this.addressField.setText("");
        this.openModal(Modal.ADD);
    }

    private void openModal(Modal modal) {
        if (modal == Modal.DIRECT) this.addressField.setText("");
        this.modal = modal;
        this.menuOpen = false;
        boolean nameFirst = modal == Modal.ADD;
        this.nameField.setFocused(nameFirst);
        this.addressField.setFocused(!nameFirst);
    }

    private void closeModal() {
        this.modal = Modal.NONE;
        this.nameField.setFocused(false);
        this.addressField.setFocused(false);
    }

    private void confirmModal(Screen parent) {
        String address = this.addressField.getBuiltText().trim();
        if (address.isEmpty()) return;
        if (this.modal == Modal.DIRECT) {
            this.closeModal();
            this.close();
            ServerInfo info = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
            ConnectScreen.connect(parent, this.mc, ServerAddress.parse(address), info, false, null);
            return;
        }
        String name = this.nameField.getBuiltText().trim();
        if (name.isEmpty()) name = address;
        if (this.modal == Modal.ADD) {
            this.serverList.add(new ServerInfo(name, address, ServerInfo.ServerType.OTHER), false);
        } else {
            ServerInfo server = this.servers.get(this.selected);
            server.name = name;
            server.address = address;
        }
        this.serverList.saveFile();
        this.closeModal();
        this.loadServers(address);
        this.heroFade.reset(0.0F);
    }

    private void join(Screen parent) {
        if (this.count() == 0) return;
        if (this.worlds) {
            LevelSummary level = this.levels.get(this.selected);
            this.close();
            if (!level.isSelectable()) {
                this.mc.setScreen(new SelectWorldScreen(parent));
                return;
            }
            this.mc.createIntegratedServerLoader().start(level.getName(), () -> this.mc.setScreen(parent));
            return;
        }
        ServerInfo server = this.servers.get(this.selected);
        this.close();
        ConnectScreen.connect(parent, this.mc, ServerAddress.parse(server.address), server, false, null);
    }


    private String entryName(int index) {
        if (this.worlds) return this.levels.get(index).getDisplayName();
        ServerInfo server = this.servers.get(index);
        String name = server.name == null ? "" : printable(server.name);
        return name.isBlank() ? server.address : name;
    }

    private String motd(ServerInfo server) {
        if (server.label == null || server.players == null) return "";
        return printable(server.label.getString());
    }

    /**
     * MOTDs often use glyphs from the server's resource-pack font (private-use or symbol code points) that our font
     * cannot draw. Only Latin text with its punctuation stays; the rest is dropped and gaps are collapsed.
     */
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡʏᴢ";
    private static final String SMALL_CAPS_PLAIN = "ABCDEFGHIJKLMNOPQRSTUVWYZ";

    static String printable(String text) {
        text = text.replaceAll("§.", "");
        // Fancy MOTDs write in small capitals and full-width/maths letters: NFKC and the table below turn them back into plain letters.
        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int small = SMALL_CAPS.indexOf(c);
            if (small >= 0) c = SMALL_CAPS_PLAIN.charAt(small);
            boolean latin = c >= 0x20 && c < 0x7F || c >= 0xA1 && c <= 0x17F || c == 0x2013 || c == 0x2014 || c == 0x2022
                || c == 0x2018 || c == 0x2019 || c == 0x201C || c == 0x201D || c == 0x2026;
            out.append(latin ? c : ' ');
        }
        // Leftover separators from removed glyphs ("| |", dangling quotes) are trimmed as well.
        return out.toString().replaceAll("\\s+", " ").replaceAll("^[\\s|\"'.\\-:]+|[\\s|\"'.\\-:]+$", "").trim();
    }

    private String worldDetails(LevelSummary level) {
        return copy("Wersja ", "Version ") + level.getVersion().getString() + (level.hasCheats() ? copy("   komendy", "   cheats") : "");
    }

    /** Server favicons (64x64) or world icons, uploaded once and refreshed when a new favicon arrives. */
    private Identifier texture(int index) {
        String key = this.worlds ? "w:" + this.levels.get(index).getName() : "s:" + this.servers.get(index).address;
        byte[] png = null;
        try {
            if (this.worlds) {
                if (this.textures.containsKey(key)) return this.textures.get(key);
                var original = this.levels.get(index).getIconPath();
                var preview = original.resolveSibling(WorldCarouselPreview.FILE_NAME);
                var path = java.nio.file.Files.isRegularFile(preview) && java.nio.file.Files.size(preview) <= 4 * 1024 * 1024 ? preview : original;
                if (java.nio.file.Files.isRegularFile(path) && !java.nio.file.Files.isSymbolicLink(path) && java.nio.file.Files.size(path) <= 4 * 1024 * 1024) {
                    png = java.nio.file.Files.readAllBytes(path);
                }
            } else {
                png = this.pinger.favicon(this.servers.get(index));
                if (this.textures.containsKey(key) && Arrays.equals(this.textureBytes.get(key), png)) return this.textures.get(key);
            }
        } catch (Exception ignored) {
            png = null;
        }
        Identifier previous = this.textures.remove(key);
        if (previous != null) this.mc.getTextureManager().destroyTexture(previous);
        this.textureBytes.put(key, png);
        Identifier id = null;
        if (png != null) {
            try {
                NativeImage image = NativeImage.read(new java.io.ByteArrayInputStream(png));
                if (image.getWidth() < 1 || image.getWidth() > 2048 || image.getHeight() < 1 || image.getHeight() > 2048) {
                    image.close();
                } else {
                    id = Identifier.of("bkclient", "server-browser/" + Integer.toHexString(key.hashCode()));
                    this.mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
                }
            } catch (Exception ignored) {
                id = null;
            }
        }
        this.textures.put(key, id);
        return id;
    }

    private float hover(String key, float[] box, int mouseX, int mouseY) {
        boolean hovered = inside(box, mouseX, mouseY);
        if (hovered) CursorUtility.set(CursorType.HAND);
        Animation animation = this.hovers.computeIfAbsent(key, k -> new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT));
        animation.update(hovered);
        return animation.getValue();
    }

    private static boolean inside(float[] box, double x, double y) {
        return box[2] > 0.0F && GuiUtility.isHovered(box[0], box[1], box[2], box[3], x, y);
    }

    private static void box(float[] box, float x, float y, float w, float h) {
        box[0] = x;
        box[1] = y;
        box[2] = w;
        box[3] = h;
    }

    private static String formatCount(int value) {
        // 18259 -> "18 259", independent of the system locale's grouping character.
        String digits = Integer.toString(Math.abs(value));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(' ');
            out.append(digits.charAt(i));
        }
        return (value < 0 ? "-" : "") + out;
    }
}
