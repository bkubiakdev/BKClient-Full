package dev.bkclient.mixin.minecraft.client.gui.overlay;

import dev.bkclient.BKClientInit;
import dev.bkclient.systems.modules.modules.visuals.ScoreboardTweaks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameHud.class)
public class ScoreboardRendererMixin {

    @Redirect(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Lnet/minecraft/text/StringVisitable;)I"
        )
    )
    private int redirectScoreboardGetWidth(TextRenderer textRenderer, StringVisitable text, DrawContext context, ScoreboardObjective objective) {
        ScoreboardTweaks mod = BKClientInit.getInstance().getModuleManager().getModule(ScoreboardTweaks.class);
        if (!mod.isEnabled() || !(text instanceof Text)) {
            return textRenderer.getWidth(text);
        }
        Text t = (Text) text;
        boolean isTitle = t.equals(objective.getDisplayName());
        boolean isScore = isScoreText(t);
        if ((mod.hideTitle.isEnabled() && isTitle) || (mod.hideNumbers.isEnabled() && !isTitle && isScore) || mod.shouldHideLine(t)) {
            return 0;
        }
        return textRenderer.getWidth(mod.applyRules(t));
    }

    @Redirect(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"
        )
    )
    private int redirectScoreboardDrawText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow, DrawContext enclosingContext, ScoreboardObjective objective) {
        ScoreboardTweaks mod = BKClientInit.getInstance().getModuleManager().getModule(ScoreboardTweaks.class);
        if (!mod.isEnabled()) {
            return context.drawText(textRenderer, text, x, y, color, shadow);
        }
        boolean isTitle = text.equals(objective.getDisplayName());
        boolean isScore = isScoreText(text);

        if (mod.hideTitle.isEnabled() && isTitle) {
            return 0;
        }
        if (mod.hideNumbers.isEnabled() && !isTitle && isScore) {
            return 0;
        }
        if (mod.shouldHideLine(text)) {
            return 0;
        }
        return context.drawText(textRenderer, mod.applyRules(text), x, y, color, shadow);
    }

    private static boolean isScoreText(Text text) {
        String s = dev.bkclient.systems.modules.modules.visuals.ScoreboardTweaks.stripFormatting(text.getString()).trim();
        return !s.isEmpty() && s.matches("-?\\d+");
    }
}
