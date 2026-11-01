package ru.pvpoverlay.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.pvpoverlay.PvPOverlayConfig;

/**
 * Forces both vanilla lightmap brightness paths to maximum while FullBright is enabled.
 * Minecraft 1.21.4 uses both overloads: one based on a dimension type and one
 * based on the computed ambient light. Overriding both prevents dimension-specific
 * darkness from bypassing the feature.
 */
@Mixin(LightmapTextureManager.class)
public abstract class LightmapBrightnessMixin {
    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true)
    private static void silvervisualistic$forceAmbientFullLight(float ambientLight, int lightLevel,
                                                                 CallbackInfoReturnable<Float> cir) {
        if (PvPOverlayConfig.fullBrightEnabled) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F",
            at = @At("HEAD"), cancellable = true)
    private static void silvervisualistic$forceDimensionFullLight(DimensionType dimensionType, int lightLevel,
                                                                   CallbackInfoReturnable<Float> cir) {
        if (PvPOverlayConfig.fullBrightEnabled) {
            cir.setReturnValue(1.0F);
        }
    }
}
