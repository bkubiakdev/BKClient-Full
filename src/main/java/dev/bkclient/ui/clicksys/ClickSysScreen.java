package dev.bkclient.ui.clicksys;

import dev.bkclient.framework.base.UIContext;
import dev.bkclient.framework.objects.MouseButton;
import dev.bkclient.ui.menu.MenuScreen;
import dev.bkclient.ui.clicksys.port.concurrent.SystemClient;
import dev.bkclient.ui.clicksys.port.gui.screen.ClickGuiScreen;
import dev.bkclient.ui.clicksys.port.gui.widget.WidgetState;
import dev.bkclient.systems.setting.impl.AbstractSetting;

/** Hosts the original BKClient screen inside BKClient's menu lifecycle. */
public final class ClickSysScreen extends MenuScreen {
    private final ClickGuiScreen delegate = new ClickGuiScreen();
    private boolean initialized;
    private ConfigImportScreen importModal;
    private SlotPickerModal slotModal;
    public void openSlots(dev.bkclient.ui.clicksys.port.setting.ListSetting slots) {
        WidgetState.setWidget2(null);
        slotModal = new SlotPickerModal(slots, () -> slotModal = null);
    }
    public void setImportModal(ConfigImportScreen modal) {
        if (importModal != null) importModal.cancelPending();
        importModal = modal;
        WidgetState.setWidget2(null);
        if (modal != null) modal.init(client, width, height);
    }
    public dev.bkclient.ui.clicksys.port.gui.widget.ConfigsPage getConfigsPage() { return delegate.getConfigsPage(); }

    @Override protected void init() {
        closing = false;
        SystemClient.getInstance().getModuleRegistry().getList32();
        SystemClient.getInstance().getModuleRegistry().sync();
        delegate.attach(client, width, height);
        initialized = true; if (importModal != null) importModal.init(client, width, height);
    }
    @Override protected void refreshWidgetPositions() { init(); }
    @Override public void render(UIContext context) {
        if (!initialized || client.currentScreen != this) return;
        try {
            ClickSysMaterial.begin(context);
            delegate.render(context, importModal == null && slotModal == null ? context.getMouseX() : -10000, importModal == null && slotModal == null ? context.getMouseY() : -10000, context.getDelta());
            if (importModal != null) importModal.render(context);
            if (slotModal != null) slotModal.render(context, width, height);
        } finally { ClickSysMaterial.end(); }
    }
    @Override public void tick() {
        if (!initialized || client.currentScreen != this) return;
        // Settings change on ticks or through their live adapters, not at the frame rate.
        // Keep external config/keybind changes fresh without rebuilding every module's
        // option lists hundreds of times per second while rendering the same values.
        SystemClient.getInstance().getModuleRegistry().sync();
        delegate.tick();
    }
    @Override public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // Match the normal ClickGUI: liquid blurs panels, not the entire world/HUD.
        if (client.world == null) super.renderBackground(context, mouseX, mouseY, delta);
    }
    public void openAutoMarket() { if (initialized) delegate.openAutoMarket(); }

    public void openThemes() { if (initialized) delegate.openThemes(); }
    public void openCosmetics() { if (initialized) delegate.openCosmetics(); }
    @Override public void close() { if(slotModal != null) { slotModal = null; return; } if(importModal != null) importModal.close(); else if(initialized) delegate.close(); else if(client!=null) client.setScreen(null); }
    @Override public boolean shouldPause() { return false; }
    @Override public void removed() {
        slotModal = null;
        if (importModal != null) { importModal.cancelPending(); importModal = null; }
        if (initialized) delegate.close();
        delegate.removed();
        WidgetState.setWidget2(null);
        dev.bkclient.ui.clicksys.port.render.TextShader.clearBatches();
        dev.bkclient.ui.clicksys.port.render.ItemIconCache.update();
        dev.bkclient.ui.clicksys.port.render.MipmapCapture.release();
        dev.bkclient.ui.clicksys.port.render.BlurEffect.update2();
        dev.bkclient.ui.clicksys.port.render.GuiQuad.close();
        dev.bkclient.systems.cosmetics.render.CosmeticRenderer.releasePreviews();
        closing = true;
        AbstractSetting.saveConfig();
    }
    @Override public void onMouseClicked(double x, double y, MouseButton button) { if(slotModal != null) { slotModal.click(x,y,button.getButtonIndex()); return; } if(importModal != null) { importModal.onMouseClicked(x,y,button); return; } if(initialized) delegate.mouseClicked(x, y, button.getButtonIndex()); }
    @Override public void onMouseReleased(double x, double y, MouseButton button) { if(slotModal != null) return; if(importModal != null) { importModal.onMouseReleased(x,y,button); return; } if(initialized) delegate.mouseReleased(x, y, button.getButtonIndex()); }
    @Override public void onMouseDragged(double x, double y, MouseButton button, double dx, double dy) { if(slotModal != null) return; if(importModal != null) { importModal.onMouseDragged(x,y,button,dx,dy); return; } if(initialized) delegate.mouseDragged(x, y, button.getButtonIndex(), dx, dy); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) { if(slotModal != null) return true; if(importModal != null) return true; return initialized && delegate.mouseScrolled(x, y, horizontal, vertical); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) { if(slotModal != null) { if(key==256) slotModal=null; return true; } if(importModal != null) { importModal.keyPressed(key,scan,modifiers); return true; } return initialized && delegate.keyPressed(key, scan, modifiers); }
    @Override public boolean keyReleased(int key, int scan, int modifiers) { if(slotModal != null) return true; if(importModal != null) return true; return initialized && delegate.keyReleased(key, scan, modifiers); }
    @Override public boolean charTyped(char character, int modifiers) { if(slotModal != null) return true; if(importModal != null) { importModal.charTyped(character,modifiers); return true; } return initialized && delegate.charTyped(character, modifiers); }
}
