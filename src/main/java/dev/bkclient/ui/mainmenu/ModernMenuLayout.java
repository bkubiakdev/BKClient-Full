package dev.bkclient.ui.mainmenu;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Font;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.localization.Language;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.game.TextUtility;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.PlayerHeadSkins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.session.Session;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Centered launcher-style menu: wordmark, two slim main buttons, a "continue" card for the last server,
 * account chip, small clock and a bottom dock. The clock, chip and dock stay visible while the server
 * browser covers the center. Every action is supplied by the title screen.
 */
final class ModernMenuLayout {
    record Actions(Runnable singleplayer, Runnable multiplayer, Runnable continueServer, Runnable settings,
                   Runnable customize, Runnable accounts, Runnable quit) {
    }

    static final ColorRGBA PAPER = new ColorRGBA(238.0F, 235.0F, 230.0F);
    static final ColorRGBA MUTED = new ColorRGBA(150.0F, 150.0F, 156.0F);
    static final ColorRGBA ONLINE = new ColorRGBA(74.0F, 222.0F, 128.0F);
    static final ColorRGBA GOLD = new ColorRGBA(236.0F, 196.0F, 72.0F);
    // Glyphs of the MSDF icon font (Fonts.ICONS), sharp at any GUI scale unlike the old PNG icons.
    static final char ICON_SETTINGS = '5';
    static final char ICON_CUSTOMIZE = '2';
    static final char ICON_PLAYER = 'F';
    static final char ICON_PLAYERS = 's';
    static final char ICON_QUIT = 'f';
    static final char ICON_PLAY = 'V';
    static final char ICON_MORE = 'w';
    private static final float BUTTON_H = 24.0F;
    private static final float BUTTON_RADIUS = 7.0F;

    private final Actions actions;
    private final Animation intro = new Animation(700L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation singleHover = new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation multiHover = new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation continueHover = new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation playHover = new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation chipHover = new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation[] dockHover = {
        new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT), new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT),
        new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT), new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT)
    };

    // Hit boxes of the last frame: x, y, width, height.
    private final float[] singleBox = new float[4];
    private final float[] multiBox = new float[4];
    private final float[] continueBox = new float[4];
    private final float[] chipBox = new float[4];
    private final float[][] dockBoxes = new float[4][4];
    private float musicCardY = -1.0F;
    private float centerAlpha;
    private float clockBottom = 50.0F;
    private String iconAddress;
    private Identifier iconId;

    ModernMenuLayout(Actions actions) {
        this.actions = actions;
    }

    void reset() {
        this.intro.reset(0.0F);
        // Re-read the favicon on every menu visit; the server may have sent a new one.
        this.releaseLastServerIcon();
    }

    /** Favicon of the last server, taken from the saved server list; null when it has none. */
    private Identifier lastServerIcon() {
        String address = LastPlayedServer.address();
        if (address.equals(this.iconAddress)) return this.iconId;
        this.releaseLastServerIcon();
        this.iconAddress = address;
        try {
            ServerList list = new ServerList(MinecraftClient.getInstance());
            list.loadFile();
            for (int i = 0; i < list.size(); i++) {
                ServerInfo entry = list.get(i);
                if (!entry.address.equalsIgnoreCase(address) || entry.getFavicon() == null) continue;
                NativeImage image = NativeImage.read(new java.io.ByteArrayInputStream(entry.getFavicon()));
                this.iconId = Identifier.of("bkclient", "menu-continue-icon");
                MinecraftClient.getInstance().getTextureManager().registerTexture(this.iconId, new NativeImageBackedTexture(image));
                break;
            }
        } catch (Exception ignored) {
            this.iconId = null;
        }
        return this.iconId;
    }

    private void releaseLastServerIcon() {
        if (this.iconId != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(this.iconId);
        this.iconId = null;
        this.iconAddress = null;
    }

    /** Vertical position for the music card, or -1 when there is no room for it. */
    float musicCardY() {
        return this.musicCardY;
    }

    /** Bottom edge of the clock block; the server browser starts its own controls below it. */
    float clockBottom() {
        return this.clockBottom;
    }

    /**
     * @param center visibility of the center stack (wordmark, buttons, cards); the clock, chip and dock
     *               stay while a browser covers the center.
     */
    void render(UIContext ctx, int width, int height, double time, float center) {
        this.intro.update(true);
        float alpha = this.intro.getValue();
        if (alpha < 0.01F) return;
        this.centerAlpha = alpha * center;
        float rise = (1.0F - this.intro.getValue()) * 10.0F;
        int mouseX = ctx.getMouseX();
        int mouseY = ctx.getMouseY();

        // The corner clock only shows when the center (which carries the time) is covered.
        this.drawClock(ctx, alpha * (1.0F - center));
        this.drawAccountChip(ctx, width, alpha, mouseX, mouseY);
        float dockY = height - 38.0F;
        float dockW = this.drawDock(ctx, width / 2.0F, dockY, alpha, mouseX, mouseY);
        this.drawFooter(ctx, width, height, dockW, alpha);

        if (this.centerAlpha < 0.01F) {
            this.singleBox[2] = this.multiBox[2] = this.continueBox[2] = 0.0F;
            this.musicCardY = -1.0F;
            return;
        }
        boolean centerHover = center > 0.99F;
        int cx = centerHover ? mouseX : -1;
        int cy = centerHover ? mouseY : -1;
        float a = this.centerAlpha;
        float centerX = width / 2.0F;
        // The center stack is sized first, then placed between the top bar and the dock.
        float top = 44.0F;
        float bottom = dockY - 12.0F;
        boolean card = LastPlayedServer.known();
        float cardBlock = card ? 10.0F + 34.0F : 0.0F;
        float timeSize = 56.0F;
        float stack = clockBlockHeight(timeSize) + 18.0F + BUTTON_H + cardBlock;
        if (stack > bottom - top) {
            timeSize = 38.0F;
            stack = clockBlockHeight(timeSize) + 12.0F + BUTTON_H + cardBlock;
        }
        float buttonGap = timeSize > 45.0F ? 18.0F : 12.0F;
        float stackY = top + Math.max(0.0F, (bottom - top - stack) * 0.42F) + rise;
        float clockBottom = this.drawClockBlock(ctx, centerX, stackY, timeSize, a);
        float buttonsY = clockBottom + buttonGap;
        float rowW = this.drawMainButtons(ctx, centerX, buttonsY, a, cx, cy);
        float nextY = buttonsY + BUTTON_H + 10.0F;
        if (card) {
            this.drawContinueCard(ctx, centerX, nextY, rowW, a, cx, cy);
            nextY += 34.0F + 10.0F;
        } else {
            this.continueBox[2] = 0.0F;
        }
        this.musicCardY = nextY + 60.0F < bottom ? nextY : -1.0F;
    }

    /** Clicks on the always-visible chrome (chip, dock) work even while the browser is open. */
    boolean clickChrome(double x, double y, int button) {
        if (button != 0 || this.intro.getValue() < 0.6F) return false;
        if (inside(this.chipBox, x, y)) return run(this.actions.accounts());
        Runnable[] dock = {this.actions.settings(), this.actions.customize(), this.actions.accounts(), this.actions.quit()};
        for (int i = 0; i < dock.length; i++) {
            if (inside(this.dockBoxes[i], x, y)) return run(dock[i]);
        }
        return false;
    }

    boolean click(double x, double y, int button) {
        if (this.clickChrome(x, y, button)) return true;
        if (button != 0 || this.centerAlpha < 0.6F) return false;
        if (inside(this.singleBox, x, y)) return run(this.actions.singleplayer());
        if (inside(this.multiBox, x, y)) return run(this.actions.multiplayer());
        if (inside(this.continueBox, x, y)) return run(this.actions.continueServer());
        return false;
    }

    private static boolean run(Runnable action) {
        action.run();
        return true;
    }

    private static boolean inside(float[] box, double x, double y) {
        return box[2] > 0.0F && GuiUtility.isHovered(box[0], box[1], box[2], box[3], x, y);
    }

    private static boolean hover(float[] box, int mouseX, int mouseY) {
        boolean hovered = inside(box, mouseX, mouseY);
        if (hovered) CursorUtility.set(CursorType.HAND);
        return hovered;
    }

    private static void box(float[] box, float x, float y, float w, float h) {
        box[0] = x;
        box[1] = y;
        box[2] = w;
        box[3] = h;
    }

    /** Liquid-glass surface used throughout the menu; the glass draws its own light edge, so no border is added. */
    static void glass(UIContext ctx, float x, float y, float w, float h, float radius, float alpha, float hover) {
        CustomButton.drawMenuSurface(ctx, x, y, w, h, BorderRadius.all(radius), alpha, hover * alpha);
    }

    /** Glass for the main action of a group: same material with a touch more light in it. */
    static void primaryGlass(UIContext ctx, float x, float y, float w, float h, float radius, float alpha, float hover) {
        CustomButton.drawMenuSurface(ctx, x, y, w, h, BorderRadius.all(radius), alpha, (0.35F + 0.65F * hover) * alpha);
        ctx.drawRoundedRect(x, y, w, h, BorderRadius.all(radius), ColorRGBA.WHITE.withAlpha((18.0F + 14.0F * hover) * alpha));
    }

    /** Text parts joined by small round dots (the fonts have no middle-dot glyph). Returns the total width. */
    static float joined(UIContext ctx, Font font, float x, float y, ColorRGBA color, boolean measureOnly, String... parts) {
        float gap = 5.0F;
        float cursor = x;
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                if (!measureOnly) {
                    ctx.drawRoundedRect(cursor + gap - 0.9F, y + font.height() * 0.42F, 1.8F, 1.8F, BorderRadius.all(0.9F), color.mulAlpha(0.8F));
                }
                cursor += gap * 2.0F;
            }
            if (!measureOnly) ctx.drawText(font, parts[i], cursor, y, color);
            cursor += width(font, parts[i]);
        }
        return cursor - x;
    }

    private void drawClock(UIContext ctx, float alpha) {
        Font timeFont = Fonts.BOLD.getFont(22.0F);
        float y = 14.0F;
        if (MenuPreferences.Option.CLOCK.enabled) {
            ctx.drawText(timeFont, TextUtility.getCurrentTime(), 18.0F, y, ColorRGBA.WHITE.withAlpha(240.0F * alpha));
            y += timeFont.height() + 3.0F;
        }
        if (MenuPreferences.Option.DATE.enabled) {
            Font dateFont = Fonts.REGULAR.getFont(7.5F);
            ctx.drawText(dateFont, TextUtility.getFormattedDate(), 18.5F, y, MUTED.withAlpha(225.0F * alpha));
            y += dateFont.height();
        }
        this.clockBottom = y;
    }

    private void drawAccountChip(UIContext ctx, int width, float alpha, int mouseX, int mouseY) {
        if (!MenuPreferences.Option.USERNAME.enabled) {
            this.chipBox[2] = 0.0F;
            return;
        }
        Session session = MinecraftClient.getInstance().getSession();
        String nick = session == null ? "BK-Client" : session.getUsername();
        boolean microsoft = session != null && session.getAccountType() == Session.AccountType.MSA;
        String type = microsoft ? "Microsoft" : "Offline";
        Font nickFont = Fonts.SEMIBOLD.getFont(8.0F);
        Font typeFont = Fonts.MEDIUM.getFont(6.0F);
        float dotGap = 7.5F;
        float textW = Math.max(width(nickFont, nick), dotGap + width(typeFont, type));
        float h = 23.0F;
        // The skin face is 8x8 pixels; a head whose physical size is a multiple of 8 keeps every skin pixel the same size.
        float scale = (float)MinecraftClient.getInstance().getWindow().getScaleFactor();
        float head = Math.max(8.0F, Math.round(15.0F * scale / 8.0F) * 8.0F) / scale;
        float pad = (h - head) / 2.0F;
        float w = pad + head + 6.0F + textW + 11.0F;
        // The centre of the pill lines up with the menu preferences button in the corner.
        float x = width - 34.0F - w;
        float y = 18.0F - h / 2.0F;
        box(this.chipBox, x, y, w, h);
        this.chipHover.update(hover(this.chipBox, mouseX, mouseY));
        glass(ctx, x, y, w, h, h / 2.0F, alpha, this.chipHover.getValue());
        // Snapped to the physical pixel grid, otherwise the head looks slightly crooked.
        this.drawHead(ctx, nick, Math.round((x + pad) * scale) / scale, Math.round((y + pad) * scale) / scale, head, alpha);

        // Both lines are centred as one block, so the pill never looks lopsided.
        float gap = 1.0F;
        float block = nickFont.height() + gap + typeFont.height();
        float textX = x + pad + head + 6.0F;
        float nickY = y + (h - block) / 2.0F;
        float typeY = nickY + nickFont.height() + gap;
        ctx.drawText(nickFont, nick, textX, nickY, ColorRGBA.WHITE.withAlpha(245.0F * alpha));
        float dot = 2.8F;
        ctx.drawRoundedRect(textX, typeY + (typeFont.height() - dot) / 2.0F, dot, dot, BorderRadius.all(dot / 2.0F),
            (microsoft ? ONLINE : MUTED).withAlpha(235.0F * alpha));
        ctx.drawText(typeFont, type, textX + dotGap, typeY, MUTED.withAlpha(240.0F * alpha));
    }

    private void drawHead(UIContext ctx, String nick, float x, float y, float size, float alpha) {
        Identifier skin = PlayerHeadSkins.get(nick);
        if (skin == null) return;
        ColorRGBA tint = ColorRGBA.WHITE.withAlpha(255.0F * alpha);
        // A circle; the crisp variant samples the skin without smoothing, so the face stays sharp instead of blurry.
        DrawUtility.drawCrispPlayerHead(ctx.getMatrices(), skin, x, y, size, BorderRadius.all(size / 2.0F), tint);
    }

    private static float clockBlockHeight(float timeSize) {
        float height = 0.0F;
        if (MenuPreferences.Option.DATE.enabled) height += Fonts.MEDIUM.getFont(9.0F).height() + 6.0F;
        if (MenuPreferences.Option.CLOCK.enabled) height += Fonts.BOLD.getFont(timeSize).height();
        return height;
    }

    /** Date above a big time, centered; the corner clock hides while this is shown. Returns the bottom edge. */
    private float drawClockBlock(UIContext ctx, float centerX, float y, float timeSize, float alpha) {
        if (MenuPreferences.Option.DATE.enabled) {
            Font dateFont = Fonts.MEDIUM.getFont(9.0F);
            String date = TextUtility.getFormattedDate();
            // A soft shadow keeps the date readable over bright parts of the background.
            ctx.drawCenteredText(dateFont, date, centerX, y + 1.2F, ColorRGBA.BLACK.withAlpha(110.0F * alpha));
            ctx.drawCenteredText(dateFont, date, centerX, y, ColorRGBA.WHITE.withAlpha(235.0F * alpha));
            y += dateFont.height() + 6.0F;
        }
        if (MenuPreferences.Option.CLOCK.enabled) {
            Font timeFont = Fonts.BOLD.getFont(timeSize);
            String time = TextUtility.getCurrentTime();
            ctx.drawCenteredText(timeFont, time, centerX, y + 1.8F, ColorRGBA.BLACK.withAlpha(80.0F * alpha));
            ctx.drawCenteredText(timeFont, time, centerX, y, ColorRGBA.WHITE.withAlpha(252.0F * alpha));
            y += timeFont.height();
        }
        return y;
    }

    /** Two equal, slim buttons; returns the width of the row so the cards below can match it. */
    private float drawMainButtons(UIContext ctx, float centerX, float y, float alpha, int mouseX, int mouseY) {
        Font font = Fonts.SEMIBOLD.getFont(8.5F);
        String single = copy("Jeden gracz", "Singleplayer");
        String multi = copy("Wielu graczy", "Multiplayer");
        float gap = 7.0F;
        float w = Math.max(96.0F, Math.max(width(font, single), width(font, multi)) + 44.0F);
        float startX = centerX - (w * 2.0F + gap) / 2.0F;
        box(this.singleBox, startX, y, w, BUTTON_H);
        box(this.multiBox, startX + w + gap, y, w, BUTTON_H);
        this.singleHover.update(hover(this.singleBox, mouseX, mouseY));
        this.multiHover.update(hover(this.multiBox, mouseX, mouseY));

        // Both buttons are liquid glass; singleplayer is the slightly brighter primary one.
        primaryGlass(ctx, startX, y, w, BUTTON_H, BUTTON_RADIUS, alpha, this.singleHover.getValue());
        this.drawButtonContent(ctx, font, ICON_PLAYER, single, this.singleBox, ColorRGBA.WHITE.withAlpha(250.0F * alpha));
        glass(ctx, this.multiBox[0], y, w, BUTTON_H, BUTTON_RADIUS, alpha, this.multiHover.getValue());
        this.drawButtonContent(ctx, font, ICON_PLAYERS, multi, this.multiBox, ColorRGBA.WHITE.withAlpha(240.0F * alpha));
        return w * 2.0F + gap;
    }

    private void drawButtonContent(UIContext ctx, Font font, char icon, String label, float[] box, ColorRGBA color) {
        float iconSize = 11.0F;
        float contentW = iconSize + 6.0F + width(font, label);
        float x = box[0] + (box[2] - contentW) / 2.0F;
        ctx.drawIcon(Fonts.ICONS, icon, x, box[1] + (box[3] - iconSize) / 2.0F, iconSize, color);
        ctx.drawText(font, label, x + iconSize + 6.0F, box[1] + GuiUtility.getMiddleOfBox(font.height(), box[3]), color);
    }

    private void drawContinueCard(UIContext ctx, float centerX, float y, float w, float alpha, int mouseX, int mouseY) {
        float h = 34.0F;
        float x = centerX - w / 2.0F;
        box(this.continueBox, x, y, w, h);
        this.continueHover.update(hover(this.continueBox, mouseX, mouseY));
        glass(ctx, x, y, w, h, 9.0F, alpha, this.continueHover.getValue());

        // Server names may carry colour codes or fancy glyphs the menu font cannot draw; same cleaning as in the server picker.
        String name = ServerBrowser.printable(LastPlayedServer.name());
        if (name.isBlank()) name = LastPlayedServer.address();
        float iconSize = 22.0F;
        float iconX = x + 6.0F;
        float iconY = y + (h - iconSize) / 2.0F;
        Identifier logo = this.lastServerIcon();
        if (logo != null) {
            ctx.drawRoundedTexture(logo, iconX, iconY, iconSize, iconSize, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
        } else {
            glass(ctx, iconX, iconY, iconSize, iconSize, 6.0F, alpha, 0.0F);
            Font letters = Fonts.BOLD.getFont(9.0F);
            ctx.drawCenteredText(letters, initials(name), iconX + iconSize / 2.0F,
                iconY + GuiUtility.getMiddleOfBox(letters.height(), iconSize), GOLD.withAlpha(240.0F * alpha));
        }

        Font small = Fonts.REGULAR.getFont(6.0F);
        Font title = Fonts.BOLD.getFont(9.0F);
        float textX = iconX + iconSize + 7.0F;
        joined(ctx, small, textX, y + 7.5F, MUTED.withAlpha(230.0F * alpha), false,
            copy("Kontynuuj", "Continue"), timeAgo(LastPlayedServer.playedAt()));
        ctx.drawText(title, ellipsize(title, name, w - (textX - x) - 34.0F), textX, y + 16.5F, ColorRGBA.WHITE.withAlpha(245.0F * alpha));

        float play = 20.0F;
        float playX = x + w - play - 7.0F;
        float playY = y + (h - play) / 2.0F;
        this.playHover.update(this.continueHover.getValue() > 0.5F);
        primaryGlass(ctx, playX, playY, play, play, play / 2.0F, alpha, this.playHover.getValue());
        ctx.draw();
        ctx.drawIcon(Fonts.ICONS, ICON_PLAY, playX + 6.0F, playY + 5.5F, 9.0F, ColorRGBA.WHITE.withAlpha(250.0F * alpha));
    }

    /** Returns the dock width so the footer can keep clear of it. */
    private float drawDock(UIContext ctx, float centerX, float y, float alpha, int mouseX, int mouseY) {
        float slot = 22.0F;
        float pad = 4.0F;
        float divider = 9.0F;
        float w = slot * 4.0F + pad * 2.0F + divider;
        float h = slot + pad * 2.0F;
        float x = centerX - w / 2.0F;
        glass(ctx, x, y, w, h, 9.0F, alpha, 0.0F);
        for (int i = 0; i < 4; i++) {
            // The power button sits behind a thin divider, apart from the others.
            float sx = x + pad + i * slot + (i == 3 ? divider : 0.0F);
            box(this.dockBoxes[i], sx, y + pad, slot, slot);
            this.dockHover[i].update(hover(this.dockBoxes[i], mouseX, mouseY));
            float hover = this.dockHover[i].getValue();
            if (hover > 0.01F) {
                ctx.drawRoundedRect(sx, y + pad, slot, slot, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(24.0F * hover * alpha));
            }
        }
        ctx.drawRoundedRect(x + pad + slot * 3.0F + divider / 2.0F - 0.3F, y + pad + 4.0F, 0.6F, slot - 8.0F, BorderRadius.ZERO,
            ColorRGBA.WHITE.withAlpha(40.0F * alpha));
        ColorRGBA icon = ColorRGBA.WHITE.withAlpha(225.0F * alpha);
        float iconSize = 12.0F;
        float iconY = y + pad + (slot - iconSize) / 2.0F;
        char[] glyphs = {ICON_SETTINGS, ICON_CUSTOMIZE, ICON_PLAYER, ICON_QUIT};
        for (int i = 0; i < glyphs.length; i++) ctx.drawIcon(Fonts.ICONS, glyphs[i], this.dockBoxes[i][0] + (slot - iconSize) / 2.0F, iconY, iconSize, icon);
        return w;
    }

    private void drawFooter(UIContext ctx, int width, int height, float dockW, float alpha) {
        Font font = Fonts.REGULAR.getFont(6.0F);
        ColorRGBA color = ColorRGBA.WHITE.withAlpha(70.0F * alpha);
        String notice = copy("BK-Client nie jest powiązany z Mojang Studios ani Microsoft.",
            "BK-Client is not affiliated with Mojang Studios or Microsoft.");
        String[] version = {BKClientInit.VERSION, "Minecraft 1.21.4"};
        float versionW = joined(ctx, font, 0.0F, 0.0F, color, true, version);
        float side = (width - dockW) / 2.0F - 20.0F;
        // Narrow windows: the notice goes first so nothing runs into the dock.
        if (width(font, notice) + 12.0F <= side) ctx.drawText(font, notice, 12.0F, height - 12.0F, color);
        if (versionW + 12.0F <= side) joined(ctx, font, width - 12.0F - versionW, height - 12.0F, color, false, version);
    }

    private static String timeAgo(long millis) {
        long minutes = millis <= 0 ? -1 : (System.currentTimeMillis() - millis) / 60000L;
        if (minutes < 0) return copy("niedawno", "recently");
        if (minutes < 1) return copy("przed chwilą", "just now");
        if (minutes < 60) return minutes + copy(" min temu", " min ago");
        long hours = minutes / 60;
        if (hours < 24) return hours + copy(" godz. temu", "h ago");
        long days = hours / 24;
        return days + copy(days == 1 ? " dzień temu" : " dni temu", days == 1 ? " day ago" : " days ago");
    }

    /** "Anarchia" -> "AN", "Mine Star" -> "MS": two letters like the server tiles. */
    static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.trim().split("[\\s._-]+");
        if (words.length >= 2 && !words[1].isEmpty()) return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
        return name.trim().substring(0, Math.min(2, name.trim().length())).toUpperCase();
    }

    static String ellipsize(Font font, String text, float maxWidth) {
        if (width(font, text) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && width(font, cut + "...") > maxWidth) cut = cut.substring(0, cut.length() - 1);
        return cut + "...";
    }

    static float width(Font font, String text) {
        return font.getFont().getWidth(text, font.getSize());
    }

    static String copy(String polish, String english) {
        return Localizator.getCurrentLanguage() == Language.PL_PL ? polish : english;
    }
}
