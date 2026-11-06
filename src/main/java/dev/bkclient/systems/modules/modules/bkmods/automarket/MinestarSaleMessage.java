package dev.bkclient.systems.modules.modules.bkmods.automarket;

/** Sale-only view retained for callers; all recognition lives in MinestarMessages. */
public final class MinestarSaleMessage {

    public record Sale(String displayPrice, double amount) {}

    private MinestarSaleMessage() {}

    public static Sale parse(String message) {
        MinestarMessages.Event event = MinestarMessages.parse(message);
        return event != null && event.kind() == MinestarMessages.Kind.SALE
                ? new Sale(event.displayPrice(), event.amount()) : null;
    }
}
