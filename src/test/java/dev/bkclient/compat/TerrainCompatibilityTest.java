package dev.bkclient.compat;

public final class TerrainCompatibilityTest {
    public static void main(String[] args) {
        for (String hook : new String[]{
                "dev.bkclient.mixin.minecraft.client.render.TerrainWindMixin",
                "dev.bkclient.mixin.minecraft.client.render.VegetationTagMixin",
                "dev.bkclient.mixin.compat.IndigoVegetationMixin"}) {
            if (RenderCompatibilityPlugin.supportsTerrainMixin(hook, true))
                throw new AssertionError("Vanilla terrain hook applied with Sodium: " + hook);
            if (!RenderCompatibilityPlugin.supportsTerrainMixin(hook, false))
                throw new AssertionError("Vanilla/Indigo terrain hook lost: " + hook);
        }
        for (boolean sodium : new boolean[]{false, true}) {
            if (!RenderCompatibilityPlugin.supportsTerrainMixin("dev.bkclient.mixin.minecraft.client.render.WorldRendererMixin", sodium))
                throw new AssertionError("Unrelated render hook disabled");
        }
        System.out.println("PASS: Sodium terrain exclusions; vanilla/Indigo and unrelated hooks preserved");
    }
}
