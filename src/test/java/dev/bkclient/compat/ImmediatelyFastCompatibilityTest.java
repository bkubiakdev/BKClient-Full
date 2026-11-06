package dev.bkclient.compat;

public final class ImmediatelyFastCompatibilityTest {
    public static final class Legacy {
        public boolean hud_batching = true;
        public boolean experimental_screen_batching = true;
        public boolean fast_text_lookup = true;
        public boolean font_atlas_resizing = true;
    }
    public static final class Modern {
        public boolean enhanced_batching = true;
        public boolean map_atlas_generation = true;
    }
    public static void main(String[] args) throws Exception {
        Legacy legacy = new Legacy();
        ImmediatelyFastCompatibility.disableConflictingBatching(legacy);
        if (legacy.hud_batching || legacy.experimental_screen_batching || !legacy.fast_text_lookup || !legacy.font_atlas_resizing)
            throw new AssertionError("Legacy batching must be disabled without changing independent optimizations");
        ImmediatelyFastCompatibility.disableConflictingBatching(legacy);
        if (legacy.hud_batching) throw new AssertionError("Repeated initialization must remain disabled");
        Modern modern = new Modern();
        ImmediatelyFastCompatibility.disableConflictingBatching(modern);
        if (modern.enhanced_batching || !modern.map_atlas_generation) throw new AssertionError("Modern config handling");
        if (!ImmediatelyFastCompatibility.disableConflictingBatching(new Object()).isEmpty())
            throw new AssertionError("Unsupported schema must be reported");
        System.out.println("ImmediatelyFast: both config schemas, idempotence, preserved optimizations and unsupported schema passed");
    }
}
