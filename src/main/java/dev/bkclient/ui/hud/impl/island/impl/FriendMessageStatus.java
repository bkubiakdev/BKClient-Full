package dev.bkclient.ui.hud.impl.island.impl;

import dev.bkclient.BKClientInit;
import dev.bkclient.framework.base.CustomDrawContext;
import dev.bkclient.framework.msdf.Fonts;
import dev.bkclient.framework.objects.BorderRadius;
import dev.bkclient.systems.friends.PrivateMessageDetector;
import dev.bkclient.systems.localization.Localizator;
import dev.bkclient.systems.setting.settings.SelectSetting;
import dev.bkclient.ui.hud.impl.island.IslandStatus;
import dev.bkclient.utility.colors.ColorRGBA;
import dev.bkclient.utility.colors.Colors;
import dev.bkclient.utility.interfaces.IMinecraft;
import dev.bkclient.utility.render.DrawUtility;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public final class FriendMessageStatus extends IslandStatus implements IMinecraft {
    private static FriendMessageStatus instance;
    private String sender = "";
    private long expiresAt;
    private boolean preview;

    public FriendMessageStatus(SelectSetting parent) {
        super(parent, "friend_message");
        instance = this;
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || mc.player == null) return;
            String self = mc.player.getGameProfile().getName();
            var friends = BKClientInit.getInstance().getFriendManager().listFriends();
            String found = null;
            if (message.getContent() instanceof TranslatableTextContent translated
                && translated.getKey().equals("commands.message.display.incoming") && translated.getArgs().length > 0) {
                Object arg = translated.getArgs()[0];
                found = PrivateMessageDetector.matchFriend(arg instanceof Text text ? text.getString() : String.valueOf(arg), self, friends);
            } else {
                found = PrivateMessageDetector.findFriend(message.getString(), self, friends);
            }
            if (found != null) {
                this.show(found);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> this.expiresAt = 0L);
    }

    private void show(String name) {
        this.preview = false;
        this.sender = name;
        this.expiresAt = System.nanoTime() + 5_000_000_000L;
    }

    public static void showPreview() {
        if (instance == null || mc.player == null) return;
        // Bundled skin keeps the local test available without a player-list entry or network request.
        instance.show("bkubiakdev");
        instance.preview = true;
    }

    public boolean isPreviewActive() {
        return this.preview && this.canShow();
    }

    @Override
    public boolean canShow() {
        return mc.player != null && this.expiresAt != 0 && System.nanoTime() < this.expiresAt;
    }

    @Override
    public void draw(CustomDrawContext context) {
        var font = Fonts.MEDIUM.getFont(6.5F);
        String label = this.sender + " " + Localizator.translate("hud.dynamic_dock.friend_message.sent");
        this.size.width = 26.0F + font.width(label);
        this.size.height = 18.0F;
        var dock = BKClientInit.getInstance().getHud().getIsland();
        float x = sr.getScaledWidth() / 2.0F - dock.getSize().width / 2.0F;
        var skin = this.preview ? BKClientInit.id("icons/hud/bkubiakdev-skin.png")
            : dev.bkclient.utility.render.PlayerHeadSkins.get(this.sender);
        DrawUtility.drawPlayerHeadWithRoundedShader(context.getMatrices(), skin, x + 6, 5, 8, BorderRadius.all(2), ColorRGBA.WHITE);
        DrawUtility.drawPlayerHatLayerWithRoundedShader(context.getMatrices(), skin, x + 6, 5, 8, BorderRadius.all(2), ColorRGBA.WHITE);
        context.drawText(font, label, x + 19, (18 - font.height()) / 2.0F, Colors.getTextColor());
    }
}
