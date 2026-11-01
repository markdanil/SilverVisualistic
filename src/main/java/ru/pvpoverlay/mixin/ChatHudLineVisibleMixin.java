package ru.pvpoverlay.mixin;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.pvpoverlay.BetterMinecraftController;

@Mixin(ChatHudLine.Visible.class)
public abstract class ChatHudLineVisibleMixin {
    @Inject(method = "<init>(ILnet/minecraft/text/OrderedText;Lnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At("RETURN"))
    private void silvervisualistic$registerNewChatLine(int addedTime, OrderedText content,
                                                        MessageIndicator indicator, boolean endOfEntry, CallbackInfo ci) {
        BetterMinecraftController.onChatLineCreated((ChatHudLine.Visible) (Object) this);
    }
}
