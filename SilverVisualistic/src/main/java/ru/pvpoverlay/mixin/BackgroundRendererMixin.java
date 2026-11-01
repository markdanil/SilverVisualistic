package ru.pvpoverlay.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.pvpoverlay.PvPOverlayConfig;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private static void pvpoverlay$gradeSkyColor(Camera camera, float tickDelta, ClientWorld world,
                                                  int clampedViewDistance, float skyDarkness,
                                                  CallbackInfoReturnable<Vector4f> cir) {
        if (!PvPOverlayConfig.betterMinecraftEnabled || !PvPOverlayConfig.betterShaderAtmosphere
                || PvPOverlayConfig.fogEnabled || world == null) return;
        Vector4f original = cir.getReturnValue();
        if (original == null) return;

        float strength = clamp01(PvPOverlayConfig.betterShaderStrength / 100.0f);
        if (strength <= 0.0f) return;
        int tint = shaderAtmosphereTint(world.getTimeOfDay());
        float dusk = duskFactor(world.getTimeOfDay());
        float amount = strength * (0.12f + dusk * 0.10f);
        amount = Math.min(0.34f, amount);

        float red = mix(original.x(), channel(tint, 16), amount);
        float green = mix(original.y(), channel(tint, 8), amount);
        float blue = mix(original.z(), channel(tint, 0), amount);
        cir.setReturnValue(new Vector4f(red, green, blue, original.w()));
    }

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void pvpoverlay$adjustFog(CallbackInfoReturnable<Fog> cir) {
        Fog original = cir.getReturnValue();
        if (original == null) return;

        if (PvPOverlayConfig.fogEnabled) {
            // Custom fog takes priority over Better Minecraft's atmospheric grading.
            float end = Math.max(1.0f, original.end());
            float start = Math.max(0.0f, Math.min(PvPOverlayConfig.fogStartDistance, end - 1.0f));
            int rgb = PvPOverlayConfig.getFogColorRgb();
            float red = ((rgb >> 16) & 0xFF) / 255.0f;
            float green = ((rgb >> 8) & 0xFF) / 255.0f;
            float blue = (rgb & 0xFF) / 255.0f;
            cir.setReturnValue(new Fog(start, end, original.shape(), red, green, blue, original.alpha()));
            return;
        }

        boolean shaderAtmosphere = PvPOverlayConfig.betterMinecraftEnabled && PvPOverlayConfig.betterShaderAtmosphere;
        boolean softMist = PvPOverlayConfig.betterMinecraftEnabled && PvPOverlayConfig.betterSoftMist;
        if (!shaderAtmosphere && !softMist) return;

        float end = Math.max(1.0f, original.end());
        float start = original.start();
        float blend = 0.0f;
        int tint = atmosphericTint();

        if (shaderAtmosphere) {
            float strength = clamp01(PvPOverlayConfig.betterShaderStrength / 100.0f);
            if (strength > 0.0f) {
                tint = shaderAtmosphereTint(MinecraftClient.getInstance().world == null
                        ? 6000L : MinecraftClient.getInstance().world.getTimeOfDay());
                blend = Math.max(blend, strength * 0.24f);
                start = Math.min(start, end * (0.94f - 0.08f * strength));
            }
        }

        if (softMist) {
            float mistStrength = clamp01(PvPOverlayConfig.betterMistStrength / 100.0f);
            if (mistStrength > 0.0f) {
                blend = Math.max(blend, mistStrength * 0.24f);
                start = Math.min(start, end * (0.90f - 0.28f * mistStrength));
            }
        }
        if (blend <= 0.0f) return;

        float red = original.red() * (1.0f - blend) + channel(tint, 16) * blend;
        float green = original.green() * (1.0f - blend) + channel(tint, 8) * blend;
        float blue = original.blue() * (1.0f - blend) + channel(tint, 0) * blend;
        cir.setReturnValue(new Fog(start, end, original.shape(), red, green, blue, original.alpha()));
    }

    private static int shaderAtmosphereTint(long worldTime) {
        long time = Math.floorMod(worldTime, 24000L);
        if (time < 1000L) return blend(0x667A98, 0xD98F62, smooth(time / 1000.0));
        if (time < 3000L) return blend(0xD98F62, 0xD6C3A7, smooth((time - 1000L) / 2000.0));
        if (time < 10000L) return 0xD6C3A7;
        if (time < 12000L) return blend(0xD6C3A7, 0xF0A16E, smooth((time - 10000L) / 2000.0));
        if (time < 14000L) return 0xF0A16E;
        if (time < 16000L) return blend(0xF0A16E, 0x697991, smooth((time - 14000L) / 2000.0));
        if (time < 22000L) return 0x697991;
        return blend(0x697991, 0xD98F62, smooth((time - 22000L) / 2000.0));
    }

    private static float duskFactor(long worldTime) {
        long time = Math.floorMod(worldTime, 24000L);
        if (time < 3000L) return 1.0f - smooth(Math.abs(time - 1000L) / 2000.0);
        if (time >= 10000L && time < 15000L) return 1.0f - smooth(Math.abs(time - 12000L) / 3000.0);
        return 0.0f;
    }

    private static float channel(int rgb, int shift) {
        return ((rgb >> shift) & 0xFF) / 255.0f;
    }

    private static float mix(float from, float to, float amount) {
        float t = clamp01(amount);
        return from * (1.0f - t) + to * t;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float smooth(double value) {
        double x = Math.max(0.0, Math.min(1.0, value));
        return (float) (x * x * (3.0 - 2.0 * x));
    }

    private static int blend(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int r = Math.round(((from >> 16) & 0xFF) * (1.0f - t) + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * (1.0f - t) + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1.0f - t) + (to & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int atmosphericTint() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return 0xB9C9D6;
        long time = Math.floorMod(client.world.getTimeOfDay(), 24000L);
        if (time < 2000L) return blend(0xE0AD88, 0xC4D5E2, smooth(time / 2000.0));
        if (time < 10000L) return 0xC4D5E2;
        if (time < 14000L) return blend(0xC4D5E2, 0xE0AD88, smooth((time - 10000L) / 4000.0));
        if (time < 16000L) return blend(0xE0AD88, 0x7185A6, smooth((time - 14000L) / 2000.0));
        if (time < 22000L) return 0x7185A6;
        return blend(0x7185A6, 0xE0AD88, smooth((time - 22000L) / 2000.0));
    }

}
