package dev.bkclient.ui.menu.clickgui;

/** V2 navigation and MSDF icons with the original ClickGUI liquid-glass material and controls. */
public final class ClickGuiV3Screen extends ClickGuiV2Screen {
    public ClickGuiV3Screen() { super(); }
    public ClickGuiV3Screen(boolean openAutoMarketInitially) { super(openAutoMarketInitially); }
    @Override protected boolean liquidStyle() { return true; }
}
