package dev.bkclient.ui.mainmenu;

import dev.bkclient.BKClientInit;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.ui.components.textfield.TextField;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.gui.ScrollHandler;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.interfaces.IScaledResolution;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.RenderUtility;
import dev.bkclient.utility.render.ScissorUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;

public class AccountManager implements IMinecraft, IScaledResolution {
    private static final float PANEL_W = 320.0F;
    private static final float PANEL_H = 240.0F;
    private static final float ROW_HEIGHT = 30.0F;
    private static final float MAX_LIST_HEIGHT = 140.0F;

    private final Animation openAnim = new Animation(400L, 0.0F, Easing.BAKEK);
    private final Animation blurAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private boolean open;
    private final TextField nameField;
    private final ScrollHandler scrollHandler = new ScrollHandler();
    private final List<Account> accounts = new ArrayList<>();
    private float panelX;
    private float panelY;

    public AccountManager() {
        this.nameField = new TextField(Fonts.MEDIUM.getFont(8.0F));
        this.nameField.setPreview("Enter nickname");
        loadAccounts();
    }

    public void render(UIContext context) {
        this.openAnim.setDuration(this.open ? 400L : 300L);
        this.openAnim.setEasing(this.open ? Easing.BAKEK : Easing.BAKEK_BACK);
        this.openAnim.update(this.open);
        float progress = this.openAnim.getValue();
        if (progress < 0.01F) return;

        this.blurAnim.update(progress > 0.5F);
        float blurProgress = this.blurAnim.getValue();

        this.panelX = (sr.getScaledWidth() - PANEL_W) / 2.0F;
        this.panelY = (sr.getScaledHeight() - PANEL_H) / 2.0F;

        float alpha = Math.min(1.0F, progress);

        DrawUtility.drawRect(
            context.getMatrices(),
            0.0F, 0.0F, sr.getScaledWidth(), sr.getScaledHeight(),
            new ColorRGBA(0, 0, 0, (int)(60 * alpha * blurProgress))
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        RenderUtility.scale(
            context.getMatrices(),
            this.panelX + PANEL_W / 2.0F,
            this.panelY + PANEL_H / 2.0F,
            0.9F + 0.1F * progress
        );

        context.drawClientRect(this.panelX, this.panelY, PANEL_W, PANEL_H, alpha, 0.0F, 7.0F);

        boolean dark = BKClientInit.getInstance().getThemeManager().getCurrentTheme() != null
            && BKClientInit.getInstance().getThemeManager().getCurrentTheme().isDark();

        float titleY = this.panelY + 12.0F;
        ColorRGBA titleColor = dark ? ColorRGBA.WHITE : new ColorRGBA(10, 10, 10);
        context.drawText(
            Fonts.ROUND_BOLD.getFont(10.0F),
            "Account Manager",
            this.panelX + 14.0F,
            titleY,
            titleColor.withAlpha((int)(255 * alpha))
        );

        float closeX = this.panelX + PANEL_W - 24.0F;
        float closeY = this.panelY + 10.0F;
        boolean closeHovered = GuiUtility.isHovered(closeX, closeY, 14.0F, 14.0F, context.getMouseX(), context.getMouseY());
        if (closeHovered) CursorUtility.set(CursorType.HAND);

        ColorRGBA closeColor = closeHovered
            ? new ColorRGBA(255, 80, 80, (int)(200 * alpha))
            : Colors.getTextColor().mulAlpha(0.3F * alpha);
        context.drawText(
            Fonts.MEDIUM.getFont(7.0F),
            "X",
            closeX + 7.0F - Fonts.MEDIUM.getFont(7.0F).width("X") / 2.0F,
            closeY + 3.0F,
            closeColor
        );

        float separatorY = this.panelY + 28.0F;
        context.drawRect(
            this.panelX + 10.0F, separatorY, PANEL_W - 20.0F, 0.5F,
            Colors.getTextColor().withAlpha((int)(5.1F * alpha))
        );

        float listY = separatorY + 4.0F;
        float listH = Math.min(this.accounts.size() * ROW_HEIGHT, MAX_LIST_HEIGHT);
        ScissorUtility.push(context.getMatrices(), this.panelX + 6.0F, listY, PANEL_W - 12.0F, listH);

        float scroll = (float)(-this.scrollHandler.getValue());
        float yOffset = scroll;

        for (int i = 0; i < this.accounts.size(); i++) {
            Account account = this.accounts.get(i);
            float itemY = listY + yOffset;
            boolean hovered = GuiUtility.isHovered(this.panelX + 6.0F, itemY, PANEL_W - 12.0F, ROW_HEIGHT, context.getMouseX(), context.getMouseY());

            if (hovered) CursorUtility.set(CursorType.HAND);

            ColorRGBA bg = dark
                ? new ColorRGBA(255, 255, 255, (int)(12 * (hovered ? 1 : 0) * alpha))
                : new ColorRGBA(0, 0, 0, (int)(5 * (hovered ? 1 : 0) * alpha));
            context.drawSquircle(this.panelX + 6.0F, itemY, PANEL_W - 12.0F, ROW_HEIGHT, 4.0F, BorderRadius.all(10.0F), bg);

            boolean isCurrent = mc.getSession() != null && mc.getSession().getUsername().equals(account.username);
            ColorRGBA nameColor = isCurrent ? Colors.getAccent().withAlpha((int)(255 * alpha)) : Colors.getTextColor().mulAlpha(0.85F * alpha);
            context.drawText(
                Fonts.MEDIUM.getFont(8.0F),
                account.username,
                this.panelX + 16.0F,
                itemY + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(8.0F).height(), ROW_HEIGHT) - 0.5F,
                nameColor
            );

            String typeLabel = account.type == AccountType.MOJANG ? "Mojang" : "Legacy";
            ColorRGBA typeColor = account.type == AccountType.MOJANG
                ? Colors.getAccent().withAlpha((int)(150 * alpha))
                : Colors.getTextColor().mulAlpha(0.3F * alpha);
            context.drawText(
                Fonts.REGULAR.getFont(6.0F),
                typeLabel,
                this.panelX + PANEL_W - 50.0F,
                itemY + GuiUtility.getMiddleOfBox(Fonts.REGULAR.getFont(6.0F).height(), ROW_HEIGHT) - 0.5F,
                typeColor
            );

            float delX = this.panelX + PANEL_W - 24.0F;
            boolean delHovered = GuiUtility.isHovered(delX - 7.0F, itemY, 14.0F, ROW_HEIGHT, context.getMouseX(), context.getMouseY());
            ColorRGBA delColor = delHovered
                ? new ColorRGBA(255, 80, 80, (int)(200 * alpha))
                : Colors.getTextColor().mulAlpha(0.3F * alpha);
            context.drawText(
                Fonts.MEDIUM.getFont(7.0F),
                "X",
                delX + 7.0F - Fonts.MEDIUM.getFont(7.0F).width("X") / 2.0F,
                itemY + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), ROW_HEIGHT) - 0.5F,
                delColor
            );

            yOffset += ROW_HEIGHT;
        }

        float maxScroll = -Math.max(0.0F, yOffset - scroll - listH);
        this.scrollHandler.setMax(maxScroll);
        ScissorUtility.pop();

        float inputY = this.panelY + PANEL_H - 50.0F;
        context.drawRect(
            this.panelX + 10.0F, inputY - 4.0F, PANEL_W - 20.0F, 0.5F,
            Colors.getTextColor().withAlpha((int)(5.1F * alpha))
        );

        this.nameField.set(this.panelX + 14.0F, inputY + 4.0F, PANEL_W - 80.0F, 22.0F);
        this.nameField.setAlpha(alpha);
        this.nameField.render(context);

        float btnX = this.panelX + PANEL_W - 58.0F;
        boolean btnHovered = GuiUtility.isHovered(btnX, inputY + 4.0F, 44.0F, 22.0F, context.getMouseX(), context.getMouseY());
        boolean hasText = this.nameField != null && !this.nameField.getBuiltText().isBlank();

        if (btnHovered && hasText) CursorUtility.set(CursorType.HAND);

        ColorRGBA btnBg = btnHovered && hasText
            ? Colors.getAccent().withAlpha((int)(180 * alpha))
            : new ColorRGBA(255, 255, 255, (int)(15 * alpha));
        context.drawRoundedRect(btnX, inputY + 4.0F, 44.0F, 22.0F, BorderRadius.all(5.0F), btnBg);

        ColorRGBA btnTextColor = hasText
            ? ColorRGBA.WHITE.withAlpha((int)(255 * alpha))
            : Colors.getTextColor().mulAlpha(0.3F * alpha);
        context.drawText(
            Fonts.MEDIUM.getFont(7.0F),
            "Add",
            btnX + 22.0F - Fonts.MEDIUM.getFont(7.0F).width("Add") / 2.0F,
            inputY + 4.0F + GuiUtility.getMiddleOfBox(Fonts.MEDIUM.getFont(7.0F).height(), 22.0F) - 0.5F,
            btnTextColor
        );

        RenderUtility.end(context.getMatrices());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button != MouseButton.LEFT) return;
        if (!this.open || this.openAnim.getValue() < 0.5F) return;

        float closeX = this.panelX + PANEL_W - 24.0F;
        float closeY = this.panelY + 10.0F;
        if (GuiUtility.isHovered(closeX, closeY, 14.0F, 14.0F, mouseX, mouseY)) {
            close();
            return;
        }

        float listY = this.panelY + 32.0F;
        float scroll = (float)(-this.scrollHandler.getValue());
        float yOffset = scroll;

        for (int i = 0; i < this.accounts.size(); i++) {
            float itemY = listY + yOffset;
            if (GuiUtility.isHovered(this.panelX + 6.0F, itemY, PANEL_W - 12.0F, ROW_HEIGHT, mouseX, mouseY)) {
                float delX = this.panelX + PANEL_W - 24.0F;
                if (GuiUtility.isHovered(delX - 7.0F, itemY, 14.0F, ROW_HEIGHT, mouseX, mouseY)) {
                    this.accounts.remove(i);
                    saveAccounts();
                    return;
                }
                switchTo(this.accounts.get(i));
                return;
            }
            yOffset += ROW_HEIGHT;
        }

        float inputY = this.panelY + PANEL_H - 50.0F;
        float btnX = this.panelX + PANEL_W - 58.0F;
        boolean btnHovered = GuiUtility.isHovered(btnX, inputY + 4.0F, 44.0F, 22.0F, mouseX, mouseY);
        String text = this.nameField != null ? this.nameField.getBuiltText().trim() : "";

        if (btnHovered && !text.isEmpty()) {
            Account newAccount = new Account(text, UUID.randomUUID().toString(), AccountType.LEGACY);
            this.accounts.add(newAccount);
            saveAccounts();
            this.nameField.clear();
            switchTo(newAccount);
            return;
        }

        if (!GuiUtility.isHovered(this.panelX, this.panelY, PANEL_W, PANEL_H, mouseX, mouseY)) {
            close();
            return;
        }

        this.nameField.onMouseClicked(mouseX, mouseY, button);
    }

    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (this.nameField != null) {
            this.nameField.onMouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.open) return false;
        if (keyCode == 256) {
            close();
            return true;
        }
        if (keyCode == 257) {
            String text = this.nameField != null ? this.nameField.getBuiltText().trim() : "";
            if (!text.isEmpty()) {
                Account newAccount = new Account(text, UUID.randomUUID().toString(), AccountType.LEGACY);
                this.accounts.add(newAccount);
                saveAccounts();
                this.nameField.clear();
                switchTo(newAccount);
                return true;
            }
        }
        if (this.nameField != null) {
            this.nameField.onKeyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!this.open) return false;
        if (this.nameField != null) {
            return this.nameField.charTyped(chr, modifiers);
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.open) return false;
        float listY = this.panelY + 32.0F;
        float listH = Math.min(this.accounts.size() * ROW_HEIGHT, MAX_LIST_HEIGHT);
        if (GuiUtility.isHovered(this.panelX + 6.0F, listY, PANEL_W - 12.0F, listH, mouseX, mouseY)) {
            this.scrollHandler.scroll(verticalAmount);
            return true;
        }
        return false;
    }

    public void open() {
        this.open = true;
        this.openAnim.reset(0.0F);
        loadAccounts();
        if (this.nameField != null) {
            this.nameField.setFocused(true);
        }
    }

    public void close() {
        this.open = false;
        if (this.nameField != null) {
            this.nameField.setFocused(false);
        }
    }

    public boolean isOpen() {
        return this.openAnim.getValue() > 0.01F;
    }

    public boolean isClosingAnimating() {
        return !this.open && this.openAnim.getValue() > 0.01F;
    }

    private void switchTo(Account account) {
        AccountType type = account.type != null ? account.type : AccountType.LEGACY;
        UUID uuid = UUID.randomUUID();
        try {
            uuid = UUID.fromString(account.uuid);
        } catch (Exception ignored) {}

        Session newSession = new Session(account.username, uuid, "", Optional.empty(), Optional.empty(), type);
        try {
            Field sessionField = MinecraftClient.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(mc, newSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Persist the selected username immediately. This also covers a clean
        // menu close without waiting for the global shutdown save.
        BKClientInit.getInstance().getFileManager().writeFile("client");
        close();
    }

    private void loadAccounts() {
        this.accounts.clear();
        List<Account> loaded = BKClientInit.getInstance().getAccounts();
        if (loaded != null) {
            this.accounts.addAll(loaded);
        }
    }

    private void saveAccounts() {
        BKClientInit.getInstance().setAccounts(new ArrayList<>(this.accounts));
        BKClientInit.getInstance().getFileManager().writeFile("client");
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts.clear();
        this.accounts.addAll(accounts);
    }

    public List<Account> getAccounts() {
        return this.accounts;
    }

    public static class Account {
        public final String username;
        public final String uuid;
        public final AccountType type;

        public Account(String username, String uuid, AccountType type) {
            this.username = username;
            this.uuid = uuid;
            this.type = type;
        }
    }
}
