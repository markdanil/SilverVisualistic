package ru.pvpoverlay.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.pvpoverlay.hitfeedback.HitTintTracker;
import ru.pvpoverlay.hitfeedback.render.TintedVertexConsumerProvider;

/** Wraps the model's vertex provider only for a player marked by the local attack. */
@Mixin(LivingEntityRenderer.class)
public abstract class HitFeedbackLivingEntityRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private VertexConsumerProvider silvervisualistic$tintOnlyLocallyHitPlayer(
            VertexConsumerProvider original, LivingEntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return original;
        }
        Integer rgb = HitTintTracker.getActiveColor(playerState.id);
        if (rgb == null) {
            return original;
        }
        return new TintedVertexConsumerProvider(original, rgb, HitTintTracker.strength());
    }
}
