package ru.pvpoverlay.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.pvpoverlay.PvPOverlayConfig;
import ru.pvpoverlay.hitfeedback.HitTintTracker;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class HitFeedbackClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void silvervisualistic$onLocalAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (!PvPOverlayConfig.hitFeedbackEnabled || player != MinecraftClient.getInstance().player
                || !(target instanceof PlayerEntity targetPlayer) || targetPlayer == player || !targetPlayer.isAlive()) {
            return;
        }
        // A normal tint is immediate. Minecraft's confirmed critical-hit callback
        // replaces it with the critical color when the hit is actually critical.
        HitTintTracker.mark(targetPlayer.getId(), PvPOverlayConfig.hitFeedbackNormalRgb());
    }
}
