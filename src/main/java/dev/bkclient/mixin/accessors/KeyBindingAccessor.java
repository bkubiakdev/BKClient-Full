package dev.bkclient.mixin.accessors;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("timesPressed")
    int bkclient$getTimesPressed();

    @Accessor("timesPressed")
    void bkclient$setTimesPressed(int count);

    @Accessor("KEYS_BY_ID")
    static Map<String, KeyBinding> bkclient$getKeysById() {
        throw new AssertionError();
    }
}
