package dev.bkclient.larp.gui;

import com.google.gson.JsonElement;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public abstract class Setting {
    public final String id;
    public final String name;
    private Runnable listener;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Setting onChange(Runnable listener) {
        this.listener = listener;
        return this;
    }

    protected void changed() {
        if (this.listener != null) {
            this.listener.run();
        }
    }

    public abstract int getHeight();

    public abstract void render(DrawContext var1, TextRenderer var2, int var3, int var4, int var5, boolean var6);

    public abstract void click(double var1, int var3);

    public abstract JsonElement save();

    public abstract void load(JsonElement var1);

    protected void drawFrame(DrawContext ctx, int x, int y, int width, boolean hovered) {
        ctx.fill(x, y, x + width, y + this.getHeight(), hovered ? -535553758 : -536080619);
        ctx.fill(x + 7, y, x + 8, y + this.getHeight(), -13934664);
    }
}
