package ru.pvpoverlay.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.pvpoverlay.PvPOverlayConfig;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void silvervisualistic$replaceVanillaCrosshair(DrawContext context, RenderTickCounter counter, CallbackInfo ci) {
        if (PvPOverlayConfig.customCrosshairEnabled) ci.cancel();
    }
}
