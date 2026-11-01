package ru.pvpoverlay.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.pvpoverlay.BetterMinecraftController;

import java.util.List;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;
    @Unique private ChatHudLine.Visible silvervisualistic$activeLine;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", at = @At("HEAD"))
    private void silvervisualistic$beginChatRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        silvervisualistic$activeLine = null;
    }

    @Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"), require = 0)
    private Object silvervisualistic$captureOnlyVisibleChatRow(List<?> list, int index) {
        Object value = list.get(index);
        // ChatHud.render has several list accesses. Track only the actual visible chat rows.
        if (list == visibleMessages && value instanceof ChatHudLine.Visible visible) {
            silvervisualistic$activeLine = visible;
        } else if (list == visibleMessages) {
            silvervisualistic$activeLine = null;
        }
        return value;
    }

    @ModifyArgs(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0), require = 0)
    private void silvervisualistic$slideOnlyActiveRowBackground(Args args) {
        int offset = silvervisualistic$currentOffset();
        if (offset == 0) return;
        args.set(0, (int) args.get(0) + offset);
        args.set(2, (int) args.get(2) + offset);
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"),
            index = 2, require = 0)
    private int silvervisualistic$slideOnlyActiveRowText(int x) {
        return x + silvervisualistic$currentOffset();
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;drawIndicatorIcon(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/gui/hud/MessageIndicator$Icon;)V"),
            index = 1, require = 0)
    private int silvervisualistic$slideOnlyActiveRowIndicator(int x) {
        return x + silvervisualistic$currentOffset();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V", at = @At("RETURN"))
    private void silvervisualistic$endChatRender(DrawContext context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        silvervisualistic$activeLine = null;
    }

    @Unique
    private int silvervisualistic$currentOffset() {
        if (silvervisualistic$activeLine == null) return 0;
        return BetterMinecraftController.chatLineOffset(
                silvervisualistic$activeLine,
                MinecraftClient.getInstance().getWindow().getScaledWidth());
    }
}
