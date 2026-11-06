package dev.bkclient.mixin.minecraft.client.gui.overlay;

import dev.bkclient.ui.hud.impl.CustomChat;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Unique;
import org.joml.Matrix4f;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("TAIL"))
    private void capture(net.minecraft.text.Text message, net.minecraft.network.message.MessageSignatureData signature,
            net.minecraft.client.gui.hud.MessageIndicator indicator, CallbackInfo ci) {
        CustomChat.captureMessage(message);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(net.minecraft.client.gui.DrawContext context, int updateCounter, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (CustomChat.active()) {
            ci.cancel();
        }
    }

    /*
     * Vanilla draws every chat line as background fill (GUI layer) followed by its text (text layer). Each layer switch
     * flushes the shared Immediate buffer, so an open chat costs two draw calls per visible line. Text calls are only
     * recorded here and replayed in the same order when render returns: all fills and indicator icons go out first in
     * one batch, then all text. Lines do not overlap and text sits at z=50 above its fill, so the image is unchanged.
     */
    @Unique private final java.util.ArrayList<Runnable> bk$deferredText = new java.util.ArrayList<>();
    @Unique private final java.util.ArrayList<Matrix4f> bk$deferredMatrices = new java.util.ArrayList<>();
    @Unique private final java.util.ArrayList<Matrix4f> bk$matrixPool = new java.util.ArrayList<>();
    @Unique private boolean bk$deferring;

    @Inject(method = "render", at = @At("HEAD"))
    private void bk$beginDefer(net.minecraft.client.gui.DrawContext context, int updateCounter, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        bk$clearDeferred();
        bk$deferring = true;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"))
    private int bk$deferLine(net.minecraft.client.gui.DrawContext context, net.minecraft.client.font.TextRenderer renderer, net.minecraft.text.OrderedText text, int x, int y, int color, Operation<Integer> original) {
        if (!bk$deferring) return original.call(context, renderer, text, x, y, color);
        bk$record(context, () -> original.call(context, renderer, text, x, y, color));
        return 0; // ChatHud discards the return value
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private int bk$deferCount(net.minecraft.client.gui.DrawContext context, net.minecraft.client.font.TextRenderer renderer, net.minecraft.text.Text text, int x, int y, int color, Operation<Integer> original) {
        if (!bk$deferring) return original.call(context, renderer, text, x, y, color);
        bk$record(context, () -> original.call(context, renderer, text, x, y, color));
        return 0;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void bk$flushDeferred(net.minecraft.client.gui.DrawContext context, int updateCounter, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        bk$deferring = false;
        if (bk$deferredText.isEmpty()) return;
        var matrices = context.getMatrices();
        try {
            for (int i = 0; i < bk$deferredText.size(); i++) {
                matrices.push();
                matrices.peek().getPositionMatrix().set(bk$deferredMatrices.get(i));
                try {
                    bk$deferredText.get(i).run();
                } finally {
                    matrices.pop();
                }
            }
        } finally {
            bk$clearDeferred();
        }
    }

    @Unique
    private void bk$record(net.minecraft.client.gui.DrawContext context, Runnable draw) {
        Matrix4f m = bk$matrixPool.isEmpty() ? new Matrix4f() : bk$matrixPool.remove(bk$matrixPool.size() - 1);
        m.set(context.getMatrices().peek().getPositionMatrix());
        bk$deferredMatrices.add(m);
        bk$deferredText.add(draw);
    }

    @Unique
    private void bk$clearDeferred() {
        bk$matrixPool.addAll(bk$deferredMatrices);
        bk$deferredMatrices.clear();
        bk$deferredText.clear();
    }
}
