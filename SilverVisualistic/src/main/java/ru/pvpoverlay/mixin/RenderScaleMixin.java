package ru.pvpoverlay.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.pvpoverlay.RenderScaleController;

@Mixin(GameRenderer.class)
public abstract class RenderScaleMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void silvervisualistic$beginScaledWorld(RenderTickCounter counter, CallbackInfo ci) {
        RenderScaleController.begin(client);
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void silvervisualistic$endScaledWorld(RenderTickCounter counter, CallbackInfo ci) {
        RenderScaleController.end(client);
    }
}
