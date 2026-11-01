package ru.pvpoverlay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.pvpoverlay.PvPOverlayConfig;

/** Optional hook for Sodium's section occlusion culler. Absent Sodium versions simply skip it. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller", remap = false)
public abstract class SodiumVerticalRenderMixin {
    private static final ThreadLocal<float[]> SILVER_SIGNED_DY = ThreadLocal.withInitial(() -> new float[1]);

    @Redirect(
            method = "visitNode",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(F)F", remap = false),
            require = 0,
            remap = false
    )
    private float silvervisualistic$captureVerticalDirection(float dy) {
        SILVER_SIGNED_DY.get()[0] = dy;
        return Math.abs(dy);
    }

    @Redirect(
            method = "visitNode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/occlusion/OcclusionCuller;testDistance(FFF)Z",
                    ordinal = 0,
                    remap = false
            ),
            require = 0,
            remap = false
    )
    private boolean silvervisualistic$limitChunksAboveCamera(float xzSquared, float verticalDistance, float maxDistance) {
        float signedDy = SILVER_SIGNED_DY.get()[0];
        SILVER_SIGNED_DY.get()[0] = 0.0F;
        if (PvPOverlayConfig.optimizationEnabled && PvPOverlayConfig.optimizationVerticalRenderEnabled
                && signedDy > 0.0F) {
            float maxAbove = maxDistance * (Math.max(25, Math.min(100, PvPOverlayConfig.optimizationVerticalRenderPercent)) / 100.0F);
            if (verticalDistance >= maxAbove) return false;
        }
        return xzSquared < maxDistance * maxDistance && verticalDistance < maxDistance;
    }
}
