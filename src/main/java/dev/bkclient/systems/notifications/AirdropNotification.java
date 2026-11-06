package dev.bkclient.systems.notifications;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.event.EventListener;
import dev.bkclient.systems.event.impl.render.HudRenderEvent;
import dev.bkclient.systems.event.impl.window.ChatClickEvent;
import dev.bkclient.utility.animation.base.Animation;
import dev.bkclient.utility.animation.base.Easing;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.game.MessageUtility;
import dev.bkclient.utility.game.cursor.CursorType;
import dev.bkclient.utility.game.cursor.CursorUtility;
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;

public class AirdropNotification implements IMinecraft {
    private static AirdropNotification instance;

    private String pendingId;
    private String pendingSender;
    private String pendingConfigName;
    private String pendingConfigData;
    private boolean visible;
    private boolean preview;
    private final Animation anim = new Animation(300L, 0.0F, Easing.BAKEK);
    private final dev.bkclient.utility.time.Timer lifeTimer = new dev.bkclient.utility.time.Timer();
    private static final float MARGIN = 10.0F;
    private static final float PADDING = 8.0F;
    private static final float HEIGHT = 66.0F;
    private static final float BTN_HEIGHT = 18.0F;
    private static final float BTN_GAP = 4.0F;

    private final EventListener<HudRenderEvent> onHudRender = event -> {
        if (!visible && anim.getValue() == 0.0F) return;
        draw(event.getContext());
    };

    private final EventListener<ChatClickEvent> onClick = event -> {
        if (!visible || event.getButton() != 0) return;
        float width = popupWidth();
        float x = popupX(width);
        float btnY = MARGIN + HEIGHT - PADDING - BTN_HEIGHT;
        float btnW = (width - PADDING * 2 - BTN_GAP) / 2.0F;
        float declineX = x + PADDING;
        float acceptX = declineX + btnW + BTN_GAP;
        if (GuiUtility.isHovered(declineX, btnY, btnW, BTN_HEIGHT, event.getX(), event.getY())) {
            reject();
        } else if (GuiUtility.isHovered(acceptX, btnY, btnW, BTN_HEIGHT, event.getX(), event.getY())) {
            accept();
        }
    };

    private AirdropNotification() {
        BKClientInit.getInstance().getEventManager().subscribe(this);
    }

    public static AirdropNotification getInstance() {
        if (instance == null) {
            instance = new AirdropNotification();
        }
        return instance;
    }

    public static void dismissIfInitialized() {
        if (instance != null) instance.dismiss();
    }

    public void show(String id, String sender, String configName, String configData) {
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) return;
        this.preview = false;
        this.pendingId = id;
        this.pendingSender = sender;
        this.pendingConfigName = configName;
        this.pendingConfigData = configData;
        this.visible = true;
        this.anim.setValue(0.0F);
        this.anim.setEasing(Easing.BAKEK);
        this.anim.setDuration(300L);
        this.lifeTimer.reset();
        this.acceptHover.setValue(0.0F);
        this.declineHover.setValue(0.0F);
    }

    public void showPreview() {
        if (!dev.bkclient.systems.DeferredFeatures.AIRDROP) return;
        // Do not replace a real pending transfer with a visual test.
        if (this.visible && !this.preview && this.pendingId != null) return;
        String sender = mc.player != null ? mc.player.getGameProfile().getName() : "TestUser";
        show("local-preview", sender, "My config", null);
        this.preview = true;
    }

    private void hide() {
        this.visible = false;
        this.anim.setEasing(Easing.BAKEK_BACK);
        this.anim.setDuration(200L);
        this.anim.update(0.0F);
    }

    private void accept() {
        if (pendingId == null) return;
        if (this.preview) {
            dismiss();
            return;
        }
        dev.bkclient.systems.supabase.SupabaseService.getInstance().acceptAirdrop(pendingId, pendingConfigData);
        pendingId = null;
        hide();
    }

    private void reject() {
        if (pendingId == null) return;
        if (this.preview) {
            dismiss();
            return;
        }
        updateStatus(pendingId, "rejected");
        MessageUtility.info(Text.of("Airdrop rejected."));
        pendingId = null;
        hide();
    }

    /**
     * Dismisses the local popup without changing remote status, including test previews.
     */
    public void dismiss() {
        this.pendingId = null;
        hide();
    }

    private void updateStatus(String id, String status) {
        dev.bkclient.systems.supabase.SupabaseService.getInstance().updateStatus(id, status);
    }

    private final Animation acceptHover = new Animation(150L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation declineHover = new Animation(150L, Easing.FIGMA_EASE_IN_OUT);

    private float popupWidth() {
        return Math.min(192.0F, mc.getWindow().getScaledWidth() - MARGIN * 2.0F);
    }

    private float popupX(float width) {
        return mc.getWindow().getScaledWidth() - (width + MARGIN) * this.anim.getValue();
    }

    private String fit(String text, float width) {
        var font = Fonts.MEDIUM.getFont(6.5F);
        String value = text == null ? "" : text;
        if (font.width(value) <= width) return value;
        while (!value.isEmpty() && font.width(value + "...") > width) value = value.substring(0, value.length() - 1);
        return value + "...";
    }

    private void draw(CustomDrawContext context) {
        this.anim.update(visible ? 1.0F : 0.0F);
        if (this.anim.getValue() <= 0.0F) return;
        float alpha = Math.clamp(this.anim.getValue(), 0.0F, 1.0F);
        float width = popupWidth();
        float x = popupX(width);
        float y = MARGIN;
        context.drawClientRect(x, y, width, HEIGHT, alpha, 0.0F, 10.0F);
        ColorRGBA text = Colors.getTextColor();

        float iconX = x + PADDING;
        float iconY = y + 10.0F;
        context.drawTextureSmooth(BKClientInit.id("icons/airdrop-receive.png"), iconX, iconY, 24.0F, 24.0F,
            new ColorRGBA(32, 174, 245).mulAlpha(alpha));
        context.drawSquircle(iconX + 15.0F, iconY + 16.0F, 11.0F, 11.0F, 2.0F, BorderRadius.all(5.5F),
            Colors.getBackgroundColor().mulAlpha(alpha));
        var sender = mc.world == null ? null : mc.world.getPlayers().stream()
            .filter(player -> player.getGameProfile().getName().equalsIgnoreCase(pendingSender)).findFirst().orElse(null);
        if (sender != null) {
            context.drawHead(sender, iconX + 16.0F, iconY + 17.0F, 9.0F, BorderRadius.all(4.5F), ColorRGBA.WHITE.mulAlpha(alpha));
        } else {
            context.drawTextureSmooth(BKClientInit.id("icons/hud/player.png"), iconX + 17.0F, iconY + 18.0F,
                7.0F, 7.0F, text.mulAlpha(0.8F * alpha));
        }

        float textX = x + 39.0F;
        float textWidth = Math.max(10.0F, x + width - PADDING - textX);
        context.drawText(Fonts.SEMIBOLD.getFont(8.0F), "AirDrop", textX, y + 8.0F, text.mulAlpha(alpha));
        context.drawText(Fonts.MEDIUM.getFont(6.5F), fit(pendingSender, textWidth), textX, y + 19.0F, text.mulAlpha(0.7F * alpha));
        context.drawText(Fonts.MEDIUM.getFont(6.5F), fit("Shares " + pendingConfigName, textWidth), textX, y + 28.0F,
            text.mulAlpha(0.5F * alpha));

        float btnY = y + HEIGHT - PADDING - BTN_HEIGHT;
        float btnW = (width - PADDING * 2 - BTN_GAP) / 2.0F;
        drawAction(context, x + PADDING, btnY, btnW, "Decline", this.declineHover, alpha);
        drawAction(context, x + PADDING + btnW + BTN_GAP, btnY, btnW, "Accept", this.acceptHover, alpha);
    }

    private void drawAction(CustomDrawContext context, float x, float y, float width, String label, Animation hover, float alpha) {
        boolean hovered = visible && GuiUtility.isHovered(x, y, width, BTN_HEIGHT, GuiUtility.getMouse().getX(), GuiUtility.getMouse().getY());
        hover.update(hovered);
        if (hovered) CursorUtility.set(CursorType.HAND);
        context.drawRoundedRect(x, y, width, BTN_HEIGHT, BorderRadius.all(5.0F),
            Colors.getTextColor().withAlpha((10.0F + 14.0F * hover.getValue()) * alpha));
        var font = Fonts.MEDIUM.getFont(7.0F);
        context.drawCenteredText(font, label, x + width / 2.0F, y + (BTN_HEIGHT - font.height()) / 2.0F,
            Colors.getTextColor().mulAlpha((0.8F + 0.2F * hover.getValue()) * alpha));
    }
}
