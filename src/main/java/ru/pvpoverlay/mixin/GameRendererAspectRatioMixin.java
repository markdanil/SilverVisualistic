package ru.pvpoverlay.mixin;

import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.pvpoverlay.AspectRatioController;

/** Overrides the world projection aspect ratio without resizing the actual window or HUD. */
@Mixin(GameRenderer.class)
public abstract class GameRendererAspectRatioMixin {
    @Inject(method = "getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("RETURN"), cancellable = true)
    private void silvervisualistic$applyAspectRatio(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        float targetAspect = AspectRatioController.getTargetAspectRatio();
        if (targetAspect <= 0.0f || !Float.isFinite(targetAspect)) return;

        Matrix4f vanilla = cir.getReturnValue();
        if (vanilla == null || vanilla.m11() == 0.0f) return;

        // For a perspective matrix, m11 is the vertical focal scale and m00 is
        // the horizontal focal scale. Setting m00 = m11 / targetAspect forces
        // the requested virtual ratio even when the physical window is ultrawide.
        Matrix4f adjusted = new Matrix4f(vanilla);
        adjusted.m00(adjusted.m11() / targetAspect);
        cir.setReturnValue(adjusted);
    }
}
