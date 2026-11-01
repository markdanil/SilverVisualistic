package ru.pvpoverlay.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.pvpoverlay.AspectRatioController;

@Mixin(Camera.class)
public abstract class CameraAspectRatioMixin {
    @Inject(method = "getProjection()Lnet/minecraft/client/render/Camera$Projection;", at = @At("RETURN"))
    private void silvervisualistic$syncCameraProjection(CallbackInfoReturnable<Camera.Projection> cir) {
        AspectRatioController.adjustCameraProjection(cir.getReturnValue());
    }
}
