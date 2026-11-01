package ru.pvpoverlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class FreeLookController {
    private static KeyBinding freeLookKey;
    private static boolean active;

    private FreeLookController() {}

    public static void register() {
        freeLookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpoverlay.freelook",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.pvpoverlay"
        ));

        MinecraftClient client = MinecraftClient.getInstance();
        OptimizationController.initialize(client);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!PvPOverlayConfig.freeLookEnabled || mc.player == null || mc.world == null || mc.currentScreen != null) {
                active = false;
            } else if (PvPOverlayConfig.freeLookHoldMode) {
                active = freeLookKey.isPressed();
            } else {
                while (freeLookKey.wasPressed()) active = !active;
            }

            OptimizationController.tick(mc);
        });
    }

    public static boolean isActive() {
        return active && PvPOverlayConfig.freeLookEnabled;
    }

    public static double getDistance() {
        return Math.max(0.5, Math.min(2.5, PvPOverlayConfig.freeLookDistance / 10.0));
    }
}
