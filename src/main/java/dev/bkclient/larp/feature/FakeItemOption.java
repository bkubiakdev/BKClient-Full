package dev.bkclient.larp.feature;

import java.util.function.Supplier;
import net.minecraft.item.ItemStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.bkclient.larp.feature.Disguise;
import dev.bkclient.larp.gui.Status;

public final class FakeItemOption {
    private final Supplier<Disguise.Spec> spec;
    private String fingerprint;

    public FakeItemOption(Supplier<Disguise.Spec> spec) {
        this.spec = spec;
    }

    public String fingerprint() { return fingerprint; }
    public boolean assigned() { return fingerprint != null && Disguise.matches(fingerprint, spec.get()); }
    public Disguise.Spec spec() {
        return this.spec.get();
    }

    public boolean enable() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            Status.error("Join a world first.");
            return false;
        }
        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            Status.error("Hold an item first.");
            return false;
        }
        if (this.fingerprint != null) Disguise.remove(this.fingerprint);
        this.fingerprint = Disguise.fingerprint(held);
        Disguise.put(this.fingerprint, this.spec.get());
        Status.info("Assigned. Only visible to you.");
        return true;
    }

    public boolean disable() {
        if (this.fingerprint != null) {
            Disguise.remove(this.fingerprint);
            this.fingerprint = null;
        }
        Status.info("Appearance removed.");
        return true;
    }

    public void refresh() {
        if (this.fingerprint != null) {
            Disguise.put(this.fingerprint, this.spec.get());
        }
    }
}
