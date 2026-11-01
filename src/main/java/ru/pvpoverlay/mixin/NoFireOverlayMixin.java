package ru.pvpoverlay.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.pvpoverlay.PvPOverlayConfig;

/** Cancels only the first-person fire overlay rendered for the local player. */
@Mixin(InGameOverlayRenderer.class)
public abstract class NoFireOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void silvervisualistic$hideOwnFire(MatrixStack matrices,
                                                       VertexConsumerProvider vertexConsumers,
                                                       CallbackInfo ci) {
        if (PvPOverlayConfig.hideOwnFireOverlay) {
            ci.cancel();
        }
    }
}
