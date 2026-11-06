package dev.bkclient.ui.clicksys.port.gui.widget;

import dev.bkclient.ui.clicksys.port.api.Theme;
import dev.bkclient.ui.clicksys.port.render.TextShader;
import dev.bkclient.ui.clicksys.port.setting.ActionSetting;
import org.joml.Matrix4f;
import java.util.function.Consumer;

/** Separate creation/import actions; invalid input remains available for correction. */
public final class ConfigImportForm extends FormWidget {
    private final TextField nameField = new TextField(180, 32, "Config name...", 14, 10, 0);
    private final TextField textField = new TextField(180, 32, "Cloud Code...", 14, 10, 0);
    private final ActionRow createButton;
    private final ActionRow importButton;
    private Consumer<String> consumer;
    private Consumer<String> createConsumer;
    private boolean busy;
    private boolean importing;
    private boolean error;
    private String status = "Share and import Cloud Codes from ClickGUI.";

    public ConfigImportForm() {
        value237 = 300; value238 = 192;
        ActionSetting create = new ActionSetting("Create", ""); create.setRunnable(this::create);
        ActionSetting load = new ActionSetting("Import", ""); load.setRunnable(this::submit);
        createButton = new ActionRow(create); importButton = new ActionRow(load);
        nameField.onRunnable(this::create); textField.onRunnable(this::submit);
    }
    public void setConsumer(Consumer<String> consumer) { this.consumer = consumer; }
    public void setCreateConsumer(Consumer<String> consumer) { this.createConsumer = consumer; }
    public TextField getTextField() { return textField; }
    public boolean hasFocus() { return nameField.isFlag4() || textField.isFlag4(); }
    public void setFloat(float width) { value237 = width; }
    public void cancelled() { busy=false;error=false;status="Import cancelled."; }
    public void update4() { complete(false); }
    public void complete(boolean success) {
        busy = false; error = !success;
        status = success ? (importing ? "Config imported." : "Config created.") : "Failed. Check the input and try again.";
        if (success) {
            TextField field = importing ? textField : nameField;
            field.update3(); field.setBoolean(false);
        }
    }
    private void create() {
        if (busy || createConsumer == null) return;
        String name = nameField.getString().trim();
        if (name.isEmpty()) { error = true; status = "Enter a config name."; return; }
        busy = true; importing = false; error = false; status = "Creating config...";
        createConsumer.accept(name);
    }
    private void submit() {
        if (busy || consumer == null) return;
        String input = textField.getString().trim();
        if (input.isEmpty()) { error = true; status = "Paste a Cloud Code."; return; }
        busy = true; importing = true; error = false; status = "Importing config...";
        consumer.accept(input);
    }
    @Override public boolean isIntDoubleDouble(int button, double x, double y) {
        if (busy) return isDoubleDouble(x, y);
        boolean name = nameField.isIntDoubleDouble(button, x, y);
        boolean code = textField.isIntDoubleDouble(button, x, y);
        return name || code || createButton.isIntDoubleDouble(button, x, y) || importButton.isIntDoubleDouble(button, x, y);
    }
    @Override public boolean isIntIntInt2(int scan, int modifiers, int key) {
        if (busy) return hasFocus();
        return nameField.isIntIntInt2(scan, modifiers, key) || textField.isIntIntInt2(scan, modifiers, key);
    }
    @Override public boolean isIntChar(int modifiers, char c) {
        return !busy && (nameField.isIntChar(modifiers, c) || textField.isIntChar(modifiers, c));
    }
    @Override protected float getFloat() { return 14; }
    @Override protected void onFloatMatrix4fFloatFloat(float mouseX, Matrix4f matrix, float mouseY, float alpha) {
        float x = value235 + 16, y = value236 + 17, width = value237 - 32;
        TextShader.onFloatStringFloatFloatIntFloatMatrix4f(alpha, dev.bkclient.systems.localization.Localizator.translate("Create or import"), 16, x, Theme.foreground(), y, matrix);
        float buttonWidth = Math.max(68, TextShader.getFloatByStringFloat(dev.bkclient.systems.localization.Localizator.translate("Import"), 14) + 18);
        float fieldWidth = width - buttonWidth - 8;
        nameField.setValue237(fieldWidth); textField.setValue237(fieldWidth);
        nameField.onFloatFloat2(y + 30, x); textField.onFloatFloat2(y + 76, x);
        createButton.onFloatFloat2(y + 30, x + fieldWidth + 8); importButton.onFloatFloat2(y + 76, x + fieldWidth + 8);
        createButton.setValue237(buttonWidth); importButton.setValue237(buttonWidth);
        float controlAlpha = alpha * (busy ? .55f : 1);
        nameField.onFloatFloatFloatMatrix4f(controlAlpha, mouseY, mouseX, matrix);
        textField.onFloatFloatFloatMatrix4f(controlAlpha, mouseY, mouseX, matrix);
        createButton.onFloatFloatFloatMatrix4f(controlAlpha, mouseY, mouseX, matrix);
        importButton.onFloatFloatFloatMatrix4f(controlAlpha, mouseY, mouseX, matrix);
        String label = dev.bkclient.ui.clicksys.port.data.TextTrimmer.getStringByFloatStringFloat2(width, dev.bkclient.systems.localization.Localizator.translate(status), 12);
        TextShader.onFloatStringFloatFloatIntFloatMatrix4f(alpha, label, 12, x, error ? Theme.danger() : Theme.mutedFg(), y + 129, matrix);
    }
}
