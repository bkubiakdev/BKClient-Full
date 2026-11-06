package dev.bkclient.compat;

public final class PerspektiveCompatibilityTest {
    public static final class Api {
        public static final Api INSTANCE = new Api();
        boolean enabled;
        public boolean getFreeLookEnabled() { return enabled; }
    }
    public static void main(String[] args) throws Exception {
        var active = PerspektiveCompatibility.bind(Api.class);
        check(!active.getAsBoolean(), "installed but inactive mod must not suppress aura view");
        Api.INSTANCE.enabled = true;
        check(active.getAsBoolean(), "active perspective must own camera and mouse");
        Api.INSTANCE.enabled = false;
        check(!active.getAsBoolean(), "releasing perspective must restore aura view immediately");
        try {
            PerspektiveCompatibility.bind(Object.class);
            throw new AssertionError("unsupported API must fail discovery");
        } catch (ReflectiveOperationException expected) { }
        System.out.println("Perspektive compatibility: activation, release and unsupported API passed");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
