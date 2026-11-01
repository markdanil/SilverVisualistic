package ru.pvpoverlay.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.pvpoverlay.PvPOverlayConfig;

/** Forces the vanilla night-vision lightmap blend to full strength while enabled. */
@Mixin(GameRenderer.class)
public abstract class FullBrightMixin {
    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void silvervisualistic$fullBrightness(LivingEntity entity, float tickDelta,
                                                          CallbackInfoReturnable<Float> cir) {
        if (PvPOverlayConfig.fullBrightEnabled) {
            cir.setReturnValue(1.0F);
        }
    }
}
