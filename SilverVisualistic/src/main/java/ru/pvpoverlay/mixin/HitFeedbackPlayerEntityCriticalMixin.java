package ru.pvpoverlay.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.pvpoverlay.PvPOverlayConfig;
import ru.pvpoverlay.hitfeedback.HitTintTracker;

/** Uses vanilla's confirmed critical-hit callback rather than predicting critical hits. */
@Mixin(PlayerEntity.class)
public abstract class HitFeedbackPlayerEntityCriticalMixin {
    @Inject(method = "addCritParticles", at = @At("HEAD"))
    private void silvervisualistic$onConfirmedCriticalHit(Entity target, CallbackInfo ci) {
        PlayerEntity attacker = (PlayerEntity) (Object) this;
        if (!PvPOverlayConfig.hitFeedbackEnabled || attacker != MinecraftClient.getInstance().player
                || !(target instanceof PlayerEntity targetPlayer) || targetPlayer == attacker || !targetPlayer.isAlive()) {
            return;
        }
        HitTintTracker.mark(targetPlayer.getId(), PvPOverlayConfig.hitFeedbackCriticalRgb());
    }
}
