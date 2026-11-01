package ru.pvpoverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Client-side hold-forward sprint helper. It asks vanilla to sprint rather than
 * changing movement speed, attributes, packets, or server-side state directly.
 */
public final class AutoSprintController {
    private static boolean forcedSprint;

    private AutoSprintController() { }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (!PvPOverlayConfig.autoSprintEnabled || client.currentScreen != null) {
            if (forcedSprint && !client.options.sprintKey.isPressed()) player.setSprinting(false);
            forcedSprint = false;
            return;
        }

        boolean forwardHeld = client.options.forwardKey.isPressed();
        boolean sneaking = client.options.sneakKey.isPressed() || player.isSneaking();
        boolean canSprintNormally = !sneaking
                && !player.hasVehicle()
                && !player.isUsingItem()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && player.getHungerManager().getFoodLevel() > 6;

        if (forwardHeld && canSprintNormally) {
            player.setSprinting(true);
            forcedSprint = true;
        } else {
            // Do not leave the sprint forced on after W is released. A manually
            // held Ctrl key still keeps vanilla's normal sprint control intact.
            if (forcedSprint && !client.options.sprintKey.isPressed()) player.setSprinting(false);
            forcedSprint = false;
        }
    }
}
