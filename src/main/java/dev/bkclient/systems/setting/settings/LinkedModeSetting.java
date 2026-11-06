package dev.bkclient.systems.setting.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.bkclient.systems.setting.SettingsContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** One UI choice backed by existing settings, preserving old configuration keys. */
public final class LinkedModeSetting extends ModeSetting {
    private record Binding(Value value, BooleanSupplier selected, Runnable select) {}
    private final List<Binding> bindings = new ArrayList<>();

    public LinkedModeSetting(SettingsContainer parent, String name, BooleanSupplier hidden, BooleanSetting... legacy) {
        super(parent, name, hidden);
        int index = -1;
        for (BooleanSetting setting : legacy) {
            index = Math.max(index, parent.getSettings().indexOf(setting));
            setting.hideControl();
        }
        // Load the selector after its legacy inputs; show it where those controls were.
        if (index >= 0) {
            parent.getSettings().remove(this);
            parent.getSettings().add(index + 1, this);
        }
    }

    public LinkedModeSetting choice(String name, BooleanSupplier selected, Runnable select) {
        return choice(name, selected, select, () -> false);
    }

    public LinkedModeSetting choice(String name, BooleanSupplier selected, Runnable select, BooleanSupplier hidden) {
        bindings.add(new Binding(new Value(this, name, "", hidden), selected, select));
        return this;
    }

    @Override public Value getValue() {
        for (Binding binding : bindings) if (binding.selected.getAsBoolean()) return binding.value;
        return super.getValue();
    }

    @Override public boolean is(Value value) { return getValue() == value; }

    @Override public void setValue(Value value) {
        for (Binding binding : bindings) {
            if (binding.value == value) { binding.select.run(); return; }
        }
    }

    @Override public JsonElement save() { return new JsonPrimitive(getValue().getName()); }

    @Override public void load(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return;
        // Preserve dormant per-style legacy values when the visible choice already agrees.
        if (getValue().getName().equalsIgnoreCase(element.getAsString())) return;
        for (Binding binding : bindings) {
            if (binding.value.getName().equalsIgnoreCase(element.getAsString())) {
                binding.select.run();
                return;
            }
        }
    }

    public static LinkedModeSetting toggle(SettingsContainer parent, String name, BooleanSetting setting,
                                            String enabled, String disabled) {
        BooleanSupplier hidden = setting.getHideCondition();
        return new LinkedModeSetting(parent, name, hidden, setting)
            .choice(enabled, setting::isEnabled, () -> setting.setEnabled(true))
            .choice(disabled, () -> !setting.isEnabled(), () -> setting.setEnabled(false));
    }
}
