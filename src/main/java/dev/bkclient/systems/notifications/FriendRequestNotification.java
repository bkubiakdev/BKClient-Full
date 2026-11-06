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
import dev.bkclient.utility.gui.GuiUtility;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import dev.bkclient.utility.render.PlayerHeadSkins;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** AirDrop-style incoming-request card. Preview actions never send server commands. */
public final class FriendRequestNotification implements IMinecraft {
    private static FriendRequestNotification instance;
    private String sender = "";
    private boolean preview, visible;
    private long expiresAt;
    private final Animation animation = new Animation(300L, 0.0F, Easing.BAKEK);
    private final EventListener<HudRenderEvent> render = e -> {
        if (mc.player == null || System.nanoTime() >= expiresAt) visible = false;
        animation.update(visible ? 1 : 0);
        if (animation.getValue() > .001F) draw(e.getContext());
    };
    private final EventListener<ChatClickEvent> click = e -> {
        if (!visible || e.getButton() != 0) return;
        float w = width(), left = x(w), buttonWidth = (w - 20) / 2;
        if (GuiUtility.isHovered(left + 8, 66, buttonWidth, 18, e.getX(), e.getY())) visible = false;
        if (GuiUtility.isHovered(left + 12 + buttonWidth, 66, buttonWidth, 18, e.getX(), e.getY())) {
            // Server-specific acceptance commands differ; only update the local friend list.
            if (!preview) BKClientInit.getInstance().getFriendManager().add(sender);
            visible = false;
        }
    };
    private FriendRequestNotification() {
        BKClientInit.getInstance().getEventManager().subscribe(this);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { visible = false; expiresAt = 0; });
    }
    public static FriendRequestNotification getInstance() {
        if (instance == null) instance = new FriendRequestNotification();
        return instance;
    }
    public void show(String sender, boolean preview) {
        if (!dev.bkclient.systems.DeferredFeatures.FRIEND_POPUPS) return;
        if (preview && visible && !this.preview) return;
        this.sender = sender; this.preview = preview; visible = true;
        expiresAt = System.nanoTime() + 15_000_000_000L;
        animation.setValue(0);
        PlayerHeadSkins.get(sender);
    }
    private float width() { return Math.min(202, mc.getWindow().getScaledWidth() - 20); }
    private float x(float width) { return mc.getWindow().getScaledWidth() - (width + 10) * animation.getValue(); }
    private void draw(CustomDrawContext c) {
        float w = width(), left = x(w), a = Math.clamp(animation.getValue(), 0, 1);
        c.drawClientRect(left, 10, w, 82, a, 0, 10);
        var skin = PlayerHeadSkins.get(sender);
        DrawUtility.drawCrispPlayerHead(c.getMatrices(), skin, left + 10, 21, 23, BorderRadius.all(5), ColorRGBA.WHITE.mulAlpha(a));
        dev.bkclient.ui.components.FriendRequestIcon.draw(c, left + 27, 37, 11, new ColorRGBA(90, 181, 255).mulAlpha(a));
        c.drawText(Fonts.SEMIBOLD.getFont(8), "Friend request", left + 40, 20, Colors.getTextColor().mulAlpha(a));
        c.drawText(Fonts.MEDIUM.getFont(7), sender, left + 40, 33, Colors.getTextColor().mulAlpha(.8F * a));
        c.drawText(Fonts.MEDIUM.getFont(6), "Wants to be your friend", left + 10, 52, Colors.getTextColor().mulAlpha(.6F * a));
        float buttonWidth = (w - 20) / 2;
        button(c, left + 8, buttonWidth, "Dismiss", a);
        button(c, left + 12 + buttonWidth, buttonWidth, "Add friend", a);
    }
    private void button(CustomDrawContext c, float left, float width, String label, float alpha) {
        boolean hover = visible && GuiUtility.isHovered(left, 66, width, 18, GuiUtility.getMouse().getX(), GuiUtility.getMouse().getY());
        c.drawRoundedRect(left, 66, width, 18, BorderRadius.all(5), Colors.getTextColor().mulAlpha((hover ? .15F : .07F) * alpha));
        c.drawCenteredText(Fonts.MEDIUM.getFont(7), label, left + width / 2, 71, Colors.getTextColor().mulAlpha(alpha));
    }
}
