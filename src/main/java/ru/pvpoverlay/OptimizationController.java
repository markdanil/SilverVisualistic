package ru.pvpoverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.particle.ParticlesMode;

/** Applies optional client-side rendering reductions and restores the player's original options. */
public final class OptimizationController {
    private static boolean initialized;

    private OptimizationController() { }

    public static void initialize(MinecraftClient client) {
        if (client == null || client.options == null) return;

        // If Minecraft saved optimized values during the previous session, put the user's
        // original values back first. The normal tick handler will then re-apply enabled options.
        restorePersistedSnapshot(client);
        initialized = true;
        apply(client);
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.options == null) return;
        if (!initialized) {
            initialize(client);
            return;
        }
        apply(client);
    }

    private static void apply(MinecraftClient client) {
        boolean master = PvPOverlayConfig.optimizationEnabled;
        applyParticles(client, master && PvPOverlayConfig.optimizationParticlesEnabled);
        applyClouds(client, master && PvPOverlayConfig.optimizationCloudsEnabled);
        applyEntityShadows(client, master && PvPOverlayConfig.optimizationEntityShadowsEnabled);
        applyEntityDistance(client, master && PvPOverlayConfig.optimizationEntityDistanceEnabled);
    }

    private static void applyParticles(MinecraftClient client, boolean enabled) {
        if (enabled) {
            if (!PvPOverlayConfig.hasSavedParticlesMode) {
                PvPOverlayConfig.savedParticlesMode = client.options.getParticles().getValue().name();
                PvPOverlayConfig.hasSavedParticlesMode = true;
                PvPOverlayConfig.save();
            }
            ParticlesMode original = parseParticles(PvPOverlayConfig.savedParticlesMode);
            client.options.getParticles().setValue(original == ParticlesMode.MINIMAL
                    ? ParticlesMode.MINIMAL : ParticlesMode.DECREASED);
        } else if (PvPOverlayConfig.hasSavedParticlesMode) {
            client.options.getParticles().setValue(parseParticles(PvPOverlayConfig.savedParticlesMode));
            PvPOverlayConfig.hasSavedParticlesMode = false;
            PvPOverlayConfig.savedParticlesMode = "ALL";
            PvPOverlayConfig.save();
        }
    }

    private static void applyClouds(MinecraftClient client, boolean enabled) {
        if (enabled) {
            if (!PvPOverlayConfig.hasSavedCloudMode) {
                PvPOverlayConfig.savedCloudMode = client.options.getCloudRenderMode().getValue().name();
                PvPOverlayConfig.hasSavedCloudMode = true;
                PvPOverlayConfig.save();
            }
            client.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        } else if (PvPOverlayConfig.hasSavedCloudMode) {
            client.options.getCloudRenderMode().setValue(parseClouds(PvPOverlayConfig.savedCloudMode));
            PvPOverlayConfig.hasSavedCloudMode = false;
            PvPOverlayConfig.savedCloudMode = "FANCY";
            PvPOverlayConfig.save();
        }
    }

    private static void applyEntityShadows(MinecraftClient client, boolean enabled) {
        if (enabled) {
            if (!PvPOverlayConfig.hasSavedEntityShadows) {
                PvPOverlayConfig.savedEntityShadows = client.options.getEntityShadows().getValue();
                PvPOverlayConfig.hasSavedEntityShadows = true;
                PvPOverlayConfig.save();
            }
            client.options.getEntityShadows().setValue(false);
        } else if (PvPOverlayConfig.hasSavedEntityShadows) {
            client.options.getEntityShadows().setValue(PvPOverlayConfig.savedEntityShadows);
            PvPOverlayConfig.hasSavedEntityShadows = false;
            PvPOverlayConfig.savedEntityShadows = true;
            PvPOverlayConfig.save();
        }
    }

    private static void applyEntityDistance(MinecraftClient client, boolean enabled) {
        if (enabled) {
            if (!PvPOverlayConfig.hasSavedEntityDistance) {
                PvPOverlayConfig.savedEntityDistance = client.options.getEntityDistanceScaling().getValue();
                PvPOverlayConfig.hasSavedEntityDistance = true;
                PvPOverlayConfig.save();
            }
            double configured = PvPOverlayConfig.optimizationEntityDistancePercent / 100.0;
            double original = PvPOverlayConfig.savedEntityDistance;
            client.options.getEntityDistanceScaling().setValue(Math.min(original, configured));
        } else if (PvPOverlayConfig.hasSavedEntityDistance) {
            client.options.getEntityDistanceScaling().setValue(PvPOverlayConfig.savedEntityDistance);
            PvPOverlayConfig.hasSavedEntityDistance = false;
            PvPOverlayConfig.savedEntityDistance = 1.0;
            PvPOverlayConfig.save();
        }
    }

    private static void restorePersistedSnapshot(MinecraftClient client) {
        boolean changed = false;
        if (PvPOverlayConfig.hasSavedParticlesMode) {
            client.options.getParticles().setValue(parseParticles(PvPOverlayConfig.savedParticlesMode));
            PvPOverlayConfig.hasSavedParticlesMode = false;
            PvPOverlayConfig.savedParticlesMode = "ALL";
            changed = true;
        }
        if (PvPOverlayConfig.hasSavedCloudMode) {
            client.options.getCloudRenderMode().setValue(parseClouds(PvPOverlayConfig.savedCloudMode));
            PvPOverlayConfig.hasSavedCloudMode = false;
            PvPOverlayConfig.savedCloudMode = "FANCY";
            changed = true;
        }
        if (PvPOverlayConfig.hasSavedEntityShadows) {
            client.options.getEntityShadows().setValue(PvPOverlayConfig.savedEntityShadows);
            PvPOverlayConfig.hasSavedEntityShadows = false;
            PvPOverlayConfig.savedEntityShadows = true;
            changed = true;
        }
        if (PvPOverlayConfig.hasSavedEntityDistance) {
            client.options.getEntityDistanceScaling().setValue(PvPOverlayConfig.savedEntityDistance);
            PvPOverlayConfig.hasSavedEntityDistance = false;
            PvPOverlayConfig.savedEntityDistance = 1.0;
            changed = true;
        }
        if (changed) PvPOverlayConfig.save();
    }

    private static ParticlesMode parseParticles(String value) {
        try {
            return ParticlesMode.valueOf(value);
        } catch (Exception ignored) {
            return ParticlesMode.ALL;
        }
    }

    private static CloudRenderMode parseClouds(String value) {
        try {
            return CloudRenderMode.valueOf(value);
        } catch (Exception ignored) {
            return CloudRenderMode.FANCY;
        }
    }
}
