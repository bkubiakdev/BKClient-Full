package dev.bkclient.systems.modules.modules.bkmods.automarket;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.function.Consumer;
import java.lang.ref.WeakReference;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;

/** Called on the client thread only. Packet identity never conflates equal consecutive sales. */
public final class MinestarPacketRouter {
    public record Message(Text text, boolean chat, MinestarMessages.Event event) {}

    private WeakReference<Object> connection = new WeakReference<>(null);
    private final IdentityHashMap<Packet<?>, Boolean> seen = new IdentityHashMap<>();
    private final ArrayDeque<Packet<?>> order = new ArrayDeque<>();

    public void accept(Packet<?> packet, Object connection, Consumer<Message> consumer) {
        if (connection == null) return;
        if (this.connection.get() != connection) {
            this.connection = new WeakReference<>(connection);
            seen.clear();
            order.clear();
        }
        Text text;
        boolean chat = false;
        if (packet instanceof GameMessageS2CPacket game) {
            text = game.content();
            chat = !game.overlay();
        } else if (packet instanceof TitleS2CPacket title) {
            text = title.text();
        } else if (packet instanceof SubtitleS2CPacket subtitle) {
            text = subtitle.text();
        } else {
            return;
        }
        if (seen.put(packet, Boolean.TRUE) != null) return;
        order.addLast(packet);
        if (order.size() > 256) seen.remove(order.removeFirst());

        MinestarMessages.Event event = MinestarMessages.parse(text.getString());
        // Only the limit is meaningful on the screen; transactions require server chat.
        if (!chat && (event == null || event.kind() != MinestarMessages.Kind.LIMIT)) return;
        consumer.accept(new Message(text, chat, event));
    }
}
