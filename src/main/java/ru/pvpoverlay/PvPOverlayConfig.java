package ru.pvpoverlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PvPOverlayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvpoverlay.json");
    // Optional portable profile: it may be placed beside the mod in /mods or in /config.
    // A profile in /mods has priority, then /config, then the regular pvpoverlay.json.
    private static final Path CONFIG_PROFILE = FabricLoader.getInstance().getConfigDir().resolve("SilverVisualistic.cfg");
    private static final Path MODS_PROFILE = FabricLoader.getInstance().getGameDir().resolve("mods").resolve("SilverVisualistic.cfg");

    // 0 = Russian, 1 = English. Russian is the default for existing installations.
    public static int language = 0;

    public static boolean showEffects = true;
    public static boolean showEffectIcons = true;
    public static boolean showTarget = true;
    public static boolean showTargetHealth = true;
    public static boolean showTargetPing = true;
    public static boolean showTargetArmor = true;
    public static boolean showTimeOfDay = true;

    // HitFeedback is built into SilverVisualistic; it is not a separate Fabric mod.
    public static boolean hitFeedbackEnabled = true;
    public static String hitFeedbackNormalColor = "#39E65C";
    public static String hitFeedbackCriticalColor = "#FF3838";
    public static int hitFeedbackDurationMs = 500;
    public static int hitFeedbackStrength = 72;

    // SilverVisible presence is automatic after account login; this legacy flag is retained for config compatibility.
    public static boolean silverVisibleEnabled = false;
    public static boolean silverVisibleShowHud = true;
    // Visual PvP utilities. Disabled by default so existing installations keep their look.
    public static boolean fullBrightEnabled = false;
    public static boolean gpsEnabled = false;
    public static boolean hideOwnFireOverlay = false;

    // Independent enemy target render. All values are local client presentation options.
    public static boolean enemyHighlightEnabled = true;
    public static int enemyHighlightStyle = 0; // 0 fixed square cube face, 1 fog rings, 2 orbiting glow orbs
    public static int enemyHighlightTargetMode = 0; // 0 crosshair target, 1 last attacked target
    public static int enemyHighlightColorMode = 0; // 0 custom gradient, 1 rainbow
    public static int enemyHighlightColorCount = 2;
    public static String enemyHighlightColor1 = "#FF4D5A";
    public static String enemyHighlightColor2 = "#FFC36B";
    public static String enemyHighlightColor3 = "#7CE7D2";
    public static String enemyHighlightColor4 = "#8C75FF";
    public static int enemyHighlightGradientSpeed = 35;
    public static int enemyHighlightMarkerThickness = 3; // square face edge thickness, 1..8
    public static int enemyHighlightCylinderOpacity = 28; // translucent connecting fog, 5..70
    public static int enemyHighlightOrbGlow = 85; // glow strength for orbiting spheres, 10..100
    public static int enemyHighlightOrbSize = 35; // sphere size, 0..100
    public static int enemyHighlightOrbOpacity = 62; // translucent fill opacity, 10..100
    public static boolean enemyHighlightOrbTrailsEnabled = true;
    public static int enemyHighlightOrbTrailLength = 55; // comet tail length, 10..100
    public static int enemyHighlightOrbTrailOpacity = 58; // comet tail opacity, 10..100
    public static int enemyHighlightOpacity = 90;
    public static int enemyHighlightScale = 100;
    public static int enemyHighlightMaxDistance = 48;
    public static int enemyHighlightHoldMs = 4000; // how long to keep the last selected target highlighted
    public static int gpsX = 8;
    public static int gpsY = 82;
    public static int gpsOpacity = 82;
    public static boolean fogEnabled = false;
    public static boolean freeLookEnabled = true;
    public static boolean freeLookHoldMode = true;
    public static int freeLookHudOffsetX = 0;
    public static int freeLookHudOffsetY = 0;
    public static boolean autoSprintEnabled = false;
    // Streamer HUD: displays the player's nickname and the current Minecraft server locally.
    public static boolean streamerModeEnabled = false;
    public static boolean streamerShowServer = true;
    public static boolean streamerShowNickname = true;
    public static boolean optimizationEnabled = false;
    public static boolean optimizationParticlesEnabled = true;
    public static boolean optimizationCloudsEnabled = false;
    public static boolean optimizationEntityShadowsEnabled = false;
    public static boolean optimizationEntityDistanceEnabled = false;
    public static int optimizationEntityDistancePercent = 75;
    public static boolean optimizationRenderScaleEnabled = false;
    public static int optimizationRenderScalePercent = 70;
    public static boolean optimizationVerticalRenderEnabled = false;
    public static int optimizationVerticalRenderPercent = 50;

    public static boolean customCrosshairEnabled = false;
    public static int customCrosshairType = 0; // 0 cross, 1 dot, 2 circle, 3 circle+dot, 4 four dots
    public static int customCrosshairSize = 8;
    public static int customCrosshairThickness = 2;
    public static int customCrosshairGap = 3;
    public static int customCrosshairColor = 0xFFFFFF;
    public static boolean customCrosshairOutline = true;

    // 0 = native, 1 = 16:10, 2 = 16:9, 3 = 4:3, 4 = custom.
    public static int aspectRatioMode = 0;
    public static int aspectRatioCustomWidth = 16;
    public static int aspectRatioCustomHeight = 9;
    // Original window geometry is kept so selecting "Standard" can fully restore it.
    public static int aspectOriginalWidth = 0;
    public static int aspectOriginalHeight = 0;
    public static boolean aspectOriginalFullscreen = false;

    // Better Minecraft: lightweight HUD motion and atmospheric presentation.
    public static boolean betterMinecraftEnabled = false;
    public static boolean betterChatAnimation = true;
    public static int betterChatDurationMs = 850;
    public static boolean betterHotbarAnimation = true;
    public static int betterHotbarDurationMs = 220;
    public static boolean betterHotbarPanelEnabled = true;
    public static boolean betterHotbarSeparatorsEnabled = true;
    public static int betterHotbarCornerRadius = 8;
    public static int betterHotbarOpacity = 82;
    public static int betterHotbarColor = 0x111820;
    public static int betterHotbarHighlightColor = 0xD9C39B;
    public static boolean betterSoftVignette = true;
    public static int betterVignetteStrength = 32;
    public static boolean betterCelestialMarker = true;
    public static boolean betterCelestialGlow = true;
    public static boolean betterSoftMist = false;
    public static int betterMistStrength = 35;
    public static boolean betterShaderAtmosphere = true;
    public static int betterShaderStrength = 62;

    // Temporary snapshots let the optimizer restore vanilla settings after being disabled
    // or after a restart that happened while optimization was active.
    public static boolean hasSavedParticlesMode = false;
    public static String savedParticlesMode = "ALL";
    public static boolean hasSavedCloudMode = false;
    public static String savedCloudMode = "FANCY";
    public static boolean hasSavedEntityShadows = false;
    public static boolean savedEntityShadows = true;
    public static boolean hasSavedEntityDistance = false;
    public static double savedEntityDistance = 1.0;
    public static String fogColorHex = "#C8D8E8";
    public static int fogStartDistance = 32;
    public static int freeLookDistance = 25; // tenths of a block; capped at 2.5


    // Effects panel position is its top-left corner. Scale is percentage of the original size.
    public static int effectsX = 8, effectsY = 8;
    public static int effectsBg = 0x202020, effectsTextColor = 0xFFFFFF, effectsOpacity = 75;
    public static int effectsIconSize = 18;
    public static int effectsCornerRadius = 8;
    public static int effectsRowGap = 4;
    public static int effectsScale = 100;

    // Target HUD panel offsets from the top-centre anchor.
    public static int targetOffsetX = 0, targetOffsetY = 0, targetHudY = 8;
    public static int targetColor = 0xFFFFFF, targetBg = 0x101010, targetOpacity = 75;
    public static int targetHoldMs = 5000, armorColor = 0xFFD65A;
    public static int targetScale = 100;

    // Time-of-day widget is anchored to the top-right corner.
    public static int timeX = 8, timeY = 8;
    public static int timeBg = 0x101820, timeTextColor = 0xFFFFFF;
    public static int timeOpacity = 80, timeCornerRadius = 8;

    public static int warningPercent = 15, warningMode = 0, durabilityMode = 1;

    private PvPOverlayConfig() { }

    public static void load() {
        try {
            // Merge settings in order: normal config -> config profile -> profile beside the mod.
            // Only keys present in a portable profile override the previous source.
            JsonObject object = new JsonObject();
            mergeJsonFile(object, FILE);
            mergeJsonFile(object, CONFIG_PROFILE);
            mergeJsonFile(object, MODS_PROFILE);
            if (object.isEmpty()) return;

            Snapshot saved = GSON.fromJson(object, Snapshot.class);
            if (saved == null) return;

            language = object.has("language") ? clamp(saved.language, 0, 1) : 0;
            showEffects = saved.showEffects;
            showEffectIcons = object.has("showEffectIcons") ? saved.showEffectIcons : true;
            showTarget = saved.showTarget;
            showTargetHealth = object.has("showTargetHealth") ? saved.showTargetHealth : true;
            showTargetPing = object.has("showTargetPing") ? saved.showTargetPing : true;
            showTargetArmor = object.has("showTargetArmor") ? saved.showTargetArmor : true;
            showTimeOfDay = object.has("showTimeOfDay") ? saved.showTimeOfDay : true;
            hitFeedbackEnabled = !object.has("hitFeedbackEnabled") || saved.hitFeedbackEnabled;
            hitFeedbackNormalColor = normalizeColorHex(
                    object.has("hitFeedbackNormalColor") ? saved.hitFeedbackNormalColor : "#39E65C", "#39E65C");
            hitFeedbackCriticalColor = normalizeColorHex(
                    object.has("hitFeedbackCriticalColor") ? saved.hitFeedbackCriticalColor : "#FF3838", "#FF3838");
            hitFeedbackDurationMs = object.has("hitFeedbackDurationMs") ? clamp(saved.hitFeedbackDurationMs, 150, 2000) : 500;
            hitFeedbackStrength = object.has("hitFeedbackStrength") ? clamp(saved.hitFeedbackStrength, 10, 100) : 72;
            silverVisibleEnabled = object.has("silverVisibleEnabled") && saved.silverVisibleEnabled;
            silverVisibleShowHud = !object.has("silverVisibleShowHud") || saved.silverVisibleShowHud;
            fullBrightEnabled = object.has("fullBrightEnabled") && saved.fullBrightEnabled;
            gpsEnabled = object.has("gpsEnabled") && saved.gpsEnabled;
            hideOwnFireOverlay = object.has("hideOwnFireOverlay") && saved.hideOwnFireOverlay;
            enemyHighlightEnabled = object.has("enemyHighlightEnabled") ? saved.enemyHighlightEnabled : true;
            enemyHighlightStyle = object.has("enemyHighlightStyle") ? clamp(saved.enemyHighlightStyle, 0, 2) : 0;
            enemyHighlightTargetMode = object.has("enemyHighlightTargetMode") ? clamp(saved.enemyHighlightTargetMode, 0, 1) : 0;
            enemyHighlightColorMode = object.has("enemyHighlightColorMode") ? clamp(saved.enemyHighlightColorMode, 0, 1) : 0;
            enemyHighlightColorCount = object.has("enemyHighlightColorCount") ? clamp(saved.enemyHighlightColorCount, 1, 4) : 2;
            enemyHighlightColor1 = normalizeColorHex(object.has("enemyHighlightColor1") ? saved.enemyHighlightColor1 : "#FF4D5A", "#FF4D5A");
            enemyHighlightColor2 = normalizeColorHex(object.has("enemyHighlightColor2") ? saved.enemyHighlightColor2 : "#FFC36B", "#FFC36B");
            enemyHighlightColor3 = normalizeColorHex(object.has("enemyHighlightColor3") ? saved.enemyHighlightColor3 : "#7CE7D2", "#7CE7D2");
            enemyHighlightColor4 = normalizeColorHex(object.has("enemyHighlightColor4") ? saved.enemyHighlightColor4 : "#8C75FF", "#8C75FF");
            enemyHighlightGradientSpeed = object.has("enemyHighlightGradientSpeed") ? clamp(saved.enemyHighlightGradientSpeed, 1, 100) : 35;
            enemyHighlightMarkerThickness = object.has("enemyHighlightMarkerThickness") ? clamp(saved.enemyHighlightMarkerThickness, 1, 8) : 3;
            enemyHighlightCylinderOpacity = object.has("enemyHighlightCylinderOpacity") ? clamp(saved.enemyHighlightCylinderOpacity, 5, 70) : 28;
            enemyHighlightOrbGlow = object.has("enemyHighlightOrbGlow") ? clamp(saved.enemyHighlightOrbGlow, 10, 100) : 85;
            enemyHighlightOrbSize = object.has("enemyHighlightOrbSize") ? clamp(saved.enemyHighlightOrbSize, 0, 100) : 35;
            enemyHighlightOrbOpacity = object.has("enemyHighlightOrbOpacity") ? clamp(saved.enemyHighlightOrbOpacity, 10, 100) : 62;
            enemyHighlightOrbTrailsEnabled = !object.has("enemyHighlightOrbTrailsEnabled") || saved.enemyHighlightOrbTrailsEnabled;
            enemyHighlightOrbTrailLength = object.has("enemyHighlightOrbTrailLength") ? clamp(saved.enemyHighlightOrbTrailLength, 10, 100) : 55;
            enemyHighlightOrbTrailOpacity = object.has("enemyHighlightOrbTrailOpacity") ? clamp(saved.enemyHighlightOrbTrailOpacity, 10, 100) : 58;
            enemyHighlightOpacity = object.has("enemyHighlightOpacity") ? clamp(saved.enemyHighlightOpacity, 10, 100) : 90;
            enemyHighlightScale = object.has("enemyHighlightScale") ? clamp(saved.enemyHighlightScale, 50, 180) : 100;
            enemyHighlightMaxDistance = object.has("enemyHighlightMaxDistance") ? clamp(saved.enemyHighlightMaxDistance, 8, 96) : 48;
            enemyHighlightHoldMs = object.has("enemyHighlightHoldMs") ? clamp(saved.enemyHighlightHoldMs, 500, 15000) : 4000;
            gpsX = object.has("gpsX") ? clamp(saved.gpsX, 0, 2000) : 8;
            gpsY = object.has("gpsY") ? clamp(saved.gpsY, 0, 2000) : 82;
            gpsOpacity = object.has("gpsOpacity") ? clamp(saved.gpsOpacity, 0, 100) : 82;
            fogEnabled = object.has("fogEnabled") && saved.fogEnabled;
            freeLookEnabled = !object.has("freeLookEnabled") || saved.freeLookEnabled;
            freeLookHoldMode = !object.has("freeLookHoldMode") || saved.freeLookHoldMode;
            freeLookHudOffsetX = object.has("freeLookHudOffsetX") ? clamp(saved.freeLookHudOffsetX, -2000, 2000) : 0;
            freeLookHudOffsetY = object.has("freeLookHudOffsetY") ? clamp(saved.freeLookHudOffsetY, -2000, 2000) : 0;
            autoSprintEnabled = object.has("autoSprintEnabled") && saved.autoSprintEnabled;
            streamerModeEnabled = object.has("streamerModeEnabled") && saved.streamerModeEnabled;
            streamerShowServer = !object.has("streamerShowServer") || saved.streamerShowServer;
            streamerShowNickname = !object.has("streamerShowNickname") || saved.streamerShowNickname;
            optimizationEnabled = object.has("optimizationEnabled") && saved.optimizationEnabled;
            optimizationParticlesEnabled = !object.has("optimizationParticlesEnabled") || saved.optimizationParticlesEnabled;
            optimizationCloudsEnabled = object.has("optimizationCloudsEnabled") && saved.optimizationCloudsEnabled;
            optimizationEntityShadowsEnabled = object.has("optimizationEntityShadowsEnabled") && saved.optimizationEntityShadowsEnabled;
            optimizationEntityDistanceEnabled = object.has("optimizationEntityDistanceEnabled") && saved.optimizationEntityDistanceEnabled;
            optimizationEntityDistancePercent = object.has("optimizationEntityDistancePercent")
                    ? clamp(saved.optimizationEntityDistancePercent, 50, 100) : 75;
            optimizationRenderScaleEnabled = object.has("optimizationRenderScaleEnabled") && saved.optimizationRenderScaleEnabled;
            optimizationRenderScalePercent = object.has("optimizationRenderScalePercent")
                    ? clamp(saved.optimizationRenderScalePercent, 50, 100) : 70;
            optimizationVerticalRenderEnabled = object.has("optimizationVerticalRenderEnabled") && saved.optimizationVerticalRenderEnabled;
            optimizationVerticalRenderPercent = object.has("optimizationVerticalRenderPercent")
                    ? clamp(saved.optimizationVerticalRenderPercent, 25, 100) : 50;

            customCrosshairEnabled = object.has("customCrosshairEnabled") && saved.customCrosshairEnabled;
            customCrosshairType = object.has("customCrosshairType") ? clamp(saved.customCrosshairType, 0, 4) : 0;
            customCrosshairSize = object.has("customCrosshairSize") ? clamp(saved.customCrosshairSize, 3, 16) : 8;
            customCrosshairThickness = object.has("customCrosshairThickness") ? clamp(saved.customCrosshairThickness, 1, 4) : 2;
            customCrosshairGap = object.has("customCrosshairGap") ? clamp(saved.customCrosshairGap, 1, 10) : 3;
            customCrosshairColor = object.has("customCrosshairColor") ? saved.customCrosshairColor & 0xFFFFFF : 0xFFFFFF;
            customCrosshairOutline = !object.has("customCrosshairOutline") || saved.customCrosshairOutline;
            aspectRatioMode = object.has("aspectRatioMode") ? clamp(saved.aspectRatioMode, 0, 4) : 0;
            aspectRatioCustomWidth = object.has("aspectRatioCustomWidth") ? clamp(saved.aspectRatioCustomWidth, 1, 64) : 16;
            aspectRatioCustomHeight = object.has("aspectRatioCustomHeight") ? clamp(saved.aspectRatioCustomHeight, 1, 64) : 9;
            aspectOriginalWidth = object.has("aspectOriginalWidth") ? clamp(saved.aspectOriginalWidth, 0, 7680) : 0;
            aspectOriginalHeight = object.has("aspectOriginalHeight") ? clamp(saved.aspectOriginalHeight, 0, 4320) : 0;
            aspectOriginalFullscreen = object.has("aspectOriginalFullscreen") && saved.aspectOriginalFullscreen;

            betterMinecraftEnabled = object.has("betterMinecraftEnabled") && saved.betterMinecraftEnabled;
            betterChatAnimation = !object.has("betterChatAnimation") || saved.betterChatAnimation;
            betterChatDurationMs = object.has("betterChatDurationMs") ? clamp(saved.betterChatDurationMs, 120, 3000) : 850;
            betterHotbarAnimation = !object.has("betterHotbarAnimation") || saved.betterHotbarAnimation;
            betterHotbarDurationMs = object.has("betterHotbarDurationMs") ? clamp(saved.betterHotbarDurationMs, 90, 500) : 220;
            betterHotbarPanelEnabled = !object.has("betterHotbarPanelEnabled") || saved.betterHotbarPanelEnabled;
            betterHotbarSeparatorsEnabled = !object.has("betterHotbarSeparatorsEnabled") || saved.betterHotbarSeparatorsEnabled;
            betterHotbarCornerRadius = object.has("betterHotbarCornerRadius") ? clamp(saved.betterHotbarCornerRadius, 0, 11) : 8;
            betterHotbarOpacity = object.has("betterHotbarOpacity") ? clamp(saved.betterHotbarOpacity, 0, 100) : 82;
            betterHotbarColor = object.has("betterHotbarColor") ? saved.betterHotbarColor & 0xFFFFFF : 0x111820;
            betterHotbarHighlightColor = object.has("betterHotbarHighlightColor") ? saved.betterHotbarHighlightColor & 0xFFFFFF : 0xD9C39B;
            betterSoftVignette = !object.has("betterSoftVignette") || saved.betterSoftVignette;
            betterVignetteStrength = object.has("betterVignetteStrength") ? clamp(saved.betterVignetteStrength, 0, 100) : 32;
            betterCelestialMarker = !object.has("betterCelestialMarker") || saved.betterCelestialMarker;
            betterCelestialGlow = !object.has("betterCelestialGlow") || saved.betterCelestialGlow;
            betterSoftMist = object.has("betterSoftMist") && saved.betterSoftMist;
            betterMistStrength = object.has("betterMistStrength") ? clamp(saved.betterMistStrength, 0, 100) : 35;
            betterShaderAtmosphere = !object.has("betterShaderAtmosphere") || saved.betterShaderAtmosphere;
            betterShaderStrength = object.has("betterShaderStrength") ? clamp(saved.betterShaderStrength, 0, 100) : 62;

            hasSavedParticlesMode = object.has("hasSavedParticlesMode") && saved.hasSavedParticlesMode;
            savedParticlesMode = object.has("savedParticlesMode") ? saved.savedParticlesMode : "ALL";
            hasSavedCloudMode = object.has("hasSavedCloudMode") && saved.hasSavedCloudMode;
            savedCloudMode = object.has("savedCloudMode") ? saved.savedCloudMode : "FANCY";
            hasSavedEntityShadows = object.has("hasSavedEntityShadows") && saved.hasSavedEntityShadows;
            savedEntityShadows = !object.has("savedEntityShadows") || saved.savedEntityShadows;
            hasSavedEntityDistance = object.has("hasSavedEntityDistance") && saved.hasSavedEntityDistance;
            savedEntityDistance = object.has("savedEntityDistance") ? Math.max(0.0, Math.min(1.0, saved.savedEntityDistance)) : 1.0;
            fogColorHex = normalizeFogHex(object.has("fogColorHex") ? saved.fogColorHex : "#C8D8E8");
            fogStartDistance = object.has("fogStartDistance") ? clamp(saved.fogStartDistance, 0, 256) : 32;
            freeLookDistance = object.has("freeLookDistance") ? clamp(saved.freeLookDistance, 5, 25) : 25;

            effectsX = clamp(saved.effectsX, 0, 2000);
            effectsY = clamp(saved.effectsY, 0, 2000);
            effectsBg = saved.effectsBg & 0xFFFFFF;
            effectsTextColor = saved.effectsTextColor & 0xFFFFFF;
            effectsOpacity = clamp(saved.effectsOpacity, 0, 100);
            effectsIconSize = object.has("effectsIconSize") ? clamp(saved.effectsIconSize, 12, 36) : 18;
            effectsCornerRadius = object.has("effectsCornerRadius") ? clamp(saved.effectsCornerRadius, 0, 20) : 8;
            effectsRowGap = object.has("effectsRowGap") ? clamp(saved.effectsRowGap, 2, 12) : 4;
            effectsScale = object.has("effectsScale") ? clamp(saved.effectsScale, 25, 200) : 100;

            targetOffsetX = object.has("targetOffsetX") ? clamp(saved.targetOffsetX, -1000, 1000) : 0;
            // targetOffsetY is retained only for old config compatibility; targetHudY controls the new HUD card.
            targetOffsetY = object.has("targetOffsetY") ? clamp(saved.targetOffsetY, -300, 300) : 0;
            targetHudY = object.has("targetHudY") ? clamp(saved.targetHudY, 0, 1000) : 8;
            targetColor = saved.targetColor & 0xFFFFFF;
            targetBg = saved.targetBg & 0xFFFFFF;
            targetOpacity = clamp(saved.targetOpacity, 0, 100);
            targetHoldMs = object.has("targetHoldMs") ? clamp(saved.targetHoldMs, 500, 15000) : 5000;
            armorColor = saved.armorColor & 0xFFFFFF;
            targetScale = object.has("targetScale") ? clamp(saved.targetScale, 25, 200) : 100;

            timeX = object.has("timeX") ? clamp(saved.timeX, 0, 2000) : 8;
            timeY = object.has("timeY") ? clamp(saved.timeY, 0, 1000) : 8;
            timeBg = object.has("timeBg") ? saved.timeBg & 0xFFFFFF : 0x101820;
            timeTextColor = object.has("timeTextColor") ? saved.timeTextColor & 0xFFFFFF : 0xFFFFFF;
            timeOpacity = object.has("timeOpacity") ? clamp(saved.timeOpacity, 0, 100) : 80;
            timeCornerRadius = object.has("timeCornerRadius") ? clamp(saved.timeCornerRadius, 0, 20) : 8;

            warningPercent = clamp(saved.warningPercent, 1, 50);
            warningMode = clamp(saved.warningMode, 0, 2);
            durabilityMode = clamp(saved.durabilityMode, 0, 2);
        } catch (Exception e) {
            System.err.println("[SilverVisualistic] Не удалось загрузить настройки: " + e.getMessage());
        }
    }

    private static void mergeJsonFile(JsonObject destination, Path source) {
        if (!Files.exists(source)) return;
        try {
            JsonObject loaded = JsonParser.parseString(Files.readString(source)).getAsJsonObject();
            for (var entry : loaded.entrySet()) destination.add(entry.getKey(), entry.getValue());
        } catch (Exception invalidFile) {
            System.err.println("[SilverVisualistic] Не удалось прочитать " + source.getFileName()
                    + ": " + invalidFile.getMessage());
        }
    }

    public static Path getPortableProfilePath() {
        return Files.exists(MODS_PROFILE) ? MODS_PROFILE : CONFIG_PROFILE;
    }

    public static void save() {
        try {
            fogColorHex = normalizeFogHex(fogColorHex);
            hitFeedbackNormalColor = normalizeColorHex(hitFeedbackNormalColor, "#39E65C");
            hitFeedbackCriticalColor = normalizeColorHex(hitFeedbackCriticalColor, "#FF3838");
            enemyHighlightColor1 = normalizeColorHex(enemyHighlightColor1, "#FF4D5A");
            enemyHighlightColor2 = normalizeColorHex(enemyHighlightColor2, "#FFC36B");
            enemyHighlightColor3 = normalizeColorHex(enemyHighlightColor3, "#7CE7D2");
            enemyHighlightColor4 = normalizeColorHex(enemyHighlightColor4, "#8C75FF");
            String snapshot = GSON.toJson(new Snapshot());
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, snapshot);

            // A portable CFG is always available after the first save. When the user
            // dropped one next to the mod, keep that same file updated from the menu.
            Path profile = getPortableProfilePath();
            Files.createDirectories(profile.getParent());
            Files.writeString(profile, snapshot);
        } catch (Exception e) {
            System.err.println("[SilverVisualistic] Не удалось сохранить настройки: " + e.getMessage());
        }
    }

    public static String normalizeFogHex(String value) {
        if (value == null) return "#C8D8E8";
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("(?i)[0-9a-f]{6}")) return "#C8D8E8";
        return "#" + hex.toUpperCase(java.util.Locale.ROOT);
    }

    public static String normalizeColorHex(String value, String fallback) {
        if (value == null) return fallback;
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("(?i)[0-9a-f]{6}")) return fallback;
        return "#" + hex.toUpperCase(java.util.Locale.ROOT);
    }

    public static int[] getEnemyHighlightColors() {
        if (enemyHighlightColorMode == 1) {
            return new int[]{0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x0000FF, 0x4B0082, 0x8B00FF};
        }
        int count = clamp(enemyHighlightColorCount, 1, 4);
        String[] hex = {enemyHighlightColor1, enemyHighlightColor2, enemyHighlightColor3, enemyHighlightColor4};
        int[] colors = new int[count];
        for (int i = 0; i < count; i++) {
            String normalized = normalizeColorHex(hex[i], "#FFFFFF");
            colors[i] = Integer.parseInt(normalized.substring(1), 16);
        }
        return colors;
    }

    public static int hitFeedbackNormalRgb() {
        return Integer.parseInt(normalizeColorHex(hitFeedbackNormalColor, "#39E65C").substring(1), 16);
    }

    public static int hitFeedbackCriticalRgb() {
        return Integer.parseInt(normalizeColorHex(hitFeedbackCriticalColor, "#FF3838").substring(1), 16);
    }

    public static int getFogColorRgb() {
        return Integer.parseInt(normalizeFogHex(fogColorHex).substring(1), 16);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Snapshot {
        int language = PvPOverlayConfig.language;
        boolean showEffects = PvPOverlayConfig.showEffects;
        boolean showEffectIcons = PvPOverlayConfig.showEffectIcons;
        boolean showTarget = PvPOverlayConfig.showTarget;
        boolean showTargetHealth = PvPOverlayConfig.showTargetHealth;
        boolean showTargetPing = PvPOverlayConfig.showTargetPing;
        boolean showTargetArmor = PvPOverlayConfig.showTargetArmor;
        boolean showTimeOfDay = PvPOverlayConfig.showTimeOfDay;
        boolean hitFeedbackEnabled = PvPOverlayConfig.hitFeedbackEnabled;
        String hitFeedbackNormalColor = PvPOverlayConfig.hitFeedbackNormalColor;
        String hitFeedbackCriticalColor = PvPOverlayConfig.hitFeedbackCriticalColor;
        int hitFeedbackDurationMs = PvPOverlayConfig.hitFeedbackDurationMs;
        int hitFeedbackStrength = PvPOverlayConfig.hitFeedbackStrength;
        boolean silverVisibleEnabled = PvPOverlayConfig.silverVisibleEnabled;
        boolean silverVisibleShowHud = PvPOverlayConfig.silverVisibleShowHud;
        boolean fullBrightEnabled = PvPOverlayConfig.fullBrightEnabled;
        boolean gpsEnabled = PvPOverlayConfig.gpsEnabled;
        boolean hideOwnFireOverlay = PvPOverlayConfig.hideOwnFireOverlay;
        boolean enemyHighlightEnabled = PvPOverlayConfig.enemyHighlightEnabled;
        int enemyHighlightStyle = PvPOverlayConfig.enemyHighlightStyle;
        int enemyHighlightTargetMode = PvPOverlayConfig.enemyHighlightTargetMode;
        int enemyHighlightColorMode = PvPOverlayConfig.enemyHighlightColorMode;
        int enemyHighlightColorCount = PvPOverlayConfig.enemyHighlightColorCount;
        String enemyHighlightColor1 = PvPOverlayConfig.enemyHighlightColor1;
        String enemyHighlightColor2 = PvPOverlayConfig.enemyHighlightColor2;
        String enemyHighlightColor3 = PvPOverlayConfig.enemyHighlightColor3;
        String enemyHighlightColor4 = PvPOverlayConfig.enemyHighlightColor4;
        int enemyHighlightGradientSpeed = PvPOverlayConfig.enemyHighlightGradientSpeed;
        int enemyHighlightMarkerThickness = PvPOverlayConfig.enemyHighlightMarkerThickness;
        int enemyHighlightCylinderOpacity = PvPOverlayConfig.enemyHighlightCylinderOpacity;
        int enemyHighlightOrbGlow = PvPOverlayConfig.enemyHighlightOrbGlow;
        int enemyHighlightOrbSize = PvPOverlayConfig.enemyHighlightOrbSize;
        int enemyHighlightOrbOpacity = PvPOverlayConfig.enemyHighlightOrbOpacity;
        boolean enemyHighlightOrbTrailsEnabled = PvPOverlayConfig.enemyHighlightOrbTrailsEnabled;
        int enemyHighlightOrbTrailLength = PvPOverlayConfig.enemyHighlightOrbTrailLength;
        int enemyHighlightOrbTrailOpacity = PvPOverlayConfig.enemyHighlightOrbTrailOpacity;
        int enemyHighlightOpacity = PvPOverlayConfig.enemyHighlightOpacity;
        int enemyHighlightScale = PvPOverlayConfig.enemyHighlightScale;
        int enemyHighlightMaxDistance = PvPOverlayConfig.enemyHighlightMaxDistance;
        int enemyHighlightHoldMs = PvPOverlayConfig.enemyHighlightHoldMs;
        int gpsX = PvPOverlayConfig.gpsX, gpsY = PvPOverlayConfig.gpsY;
        int gpsOpacity = PvPOverlayConfig.gpsOpacity;
        boolean fogEnabled = PvPOverlayConfig.fogEnabled;
        boolean freeLookEnabled = PvPOverlayConfig.freeLookEnabled;
        boolean freeLookHoldMode = PvPOverlayConfig.freeLookHoldMode;
        int freeLookHudOffsetX = PvPOverlayConfig.freeLookHudOffsetX;
        int freeLookHudOffsetY = PvPOverlayConfig.freeLookHudOffsetY;
        boolean autoSprintEnabled = PvPOverlayConfig.autoSprintEnabled;
        boolean streamerModeEnabled = PvPOverlayConfig.streamerModeEnabled;
        boolean streamerShowServer = PvPOverlayConfig.streamerShowServer;
        boolean streamerShowNickname = PvPOverlayConfig.streamerShowNickname;
        boolean optimizationEnabled = PvPOverlayConfig.optimizationEnabled;
        boolean optimizationParticlesEnabled = PvPOverlayConfig.optimizationParticlesEnabled;
        boolean optimizationCloudsEnabled = PvPOverlayConfig.optimizationCloudsEnabled;
        boolean optimizationEntityShadowsEnabled = PvPOverlayConfig.optimizationEntityShadowsEnabled;
        boolean optimizationEntityDistanceEnabled = PvPOverlayConfig.optimizationEntityDistanceEnabled;
        int optimizationEntityDistancePercent = PvPOverlayConfig.optimizationEntityDistancePercent;
        boolean optimizationRenderScaleEnabled = PvPOverlayConfig.optimizationRenderScaleEnabled;
        int optimizationRenderScalePercent = PvPOverlayConfig.optimizationRenderScalePercent;
        boolean optimizationVerticalRenderEnabled = PvPOverlayConfig.optimizationVerticalRenderEnabled;
        int optimizationVerticalRenderPercent = PvPOverlayConfig.optimizationVerticalRenderPercent;
        boolean customCrosshairEnabled = PvPOverlayConfig.customCrosshairEnabled;
        int customCrosshairType = PvPOverlayConfig.customCrosshairType;
        int customCrosshairSize = PvPOverlayConfig.customCrosshairSize;
        int customCrosshairThickness = PvPOverlayConfig.customCrosshairThickness;
        int customCrosshairGap = PvPOverlayConfig.customCrosshairGap;
        int customCrosshairColor = PvPOverlayConfig.customCrosshairColor;
        boolean customCrosshairOutline = PvPOverlayConfig.customCrosshairOutline;
        int aspectRatioMode = PvPOverlayConfig.aspectRatioMode;
        int aspectRatioCustomWidth = PvPOverlayConfig.aspectRatioCustomWidth;
        int aspectRatioCustomHeight = PvPOverlayConfig.aspectRatioCustomHeight;
        int aspectOriginalWidth = PvPOverlayConfig.aspectOriginalWidth;
        int aspectOriginalHeight = PvPOverlayConfig.aspectOriginalHeight;
        boolean aspectOriginalFullscreen = PvPOverlayConfig.aspectOriginalFullscreen;
        boolean betterMinecraftEnabled = PvPOverlayConfig.betterMinecraftEnabled;
        boolean betterChatAnimation = PvPOverlayConfig.betterChatAnimation;
        int betterChatDurationMs = PvPOverlayConfig.betterChatDurationMs;
        boolean betterHotbarAnimation = PvPOverlayConfig.betterHotbarAnimation;
        int betterHotbarDurationMs = PvPOverlayConfig.betterHotbarDurationMs;
        boolean betterHotbarPanelEnabled = PvPOverlayConfig.betterHotbarPanelEnabled;
        boolean betterHotbarSeparatorsEnabled = PvPOverlayConfig.betterHotbarSeparatorsEnabled;
        int betterHotbarCornerRadius = PvPOverlayConfig.betterHotbarCornerRadius;
        int betterHotbarOpacity = PvPOverlayConfig.betterHotbarOpacity;
        int betterHotbarColor = PvPOverlayConfig.betterHotbarColor;
        int betterHotbarHighlightColor = PvPOverlayConfig.betterHotbarHighlightColor;
        boolean betterSoftVignette = PvPOverlayConfig.betterSoftVignette;
        int betterVignetteStrength = PvPOverlayConfig.betterVignetteStrength;
        boolean betterCelestialMarker = PvPOverlayConfig.betterCelestialMarker;
        boolean betterCelestialGlow = PvPOverlayConfig.betterCelestialGlow;
        boolean betterSoftMist = PvPOverlayConfig.betterSoftMist;
        int betterMistStrength = PvPOverlayConfig.betterMistStrength;
        boolean betterShaderAtmosphere = PvPOverlayConfig.betterShaderAtmosphere;
        int betterShaderStrength = PvPOverlayConfig.betterShaderStrength;
        boolean hasSavedParticlesMode = PvPOverlayConfig.hasSavedParticlesMode;
        String savedParticlesMode = PvPOverlayConfig.savedParticlesMode;
        boolean hasSavedCloudMode = PvPOverlayConfig.hasSavedCloudMode;
        String savedCloudMode = PvPOverlayConfig.savedCloudMode;
        boolean hasSavedEntityShadows = PvPOverlayConfig.hasSavedEntityShadows;
        boolean savedEntityShadows = PvPOverlayConfig.savedEntityShadows;
        boolean hasSavedEntityDistance = PvPOverlayConfig.hasSavedEntityDistance;
        double savedEntityDistance = PvPOverlayConfig.savedEntityDistance;
        String fogColorHex = PvPOverlayConfig.fogColorHex;
        int fogStartDistance = PvPOverlayConfig.fogStartDistance;
        int freeLookDistance = PvPOverlayConfig.freeLookDistance;
        int effectsX = PvPOverlayConfig.effectsX, effectsY = PvPOverlayConfig.effectsY;
        int effectsBg = PvPOverlayConfig.effectsBg, effectsTextColor = PvPOverlayConfig.effectsTextColor;
        int effectsOpacity = PvPOverlayConfig.effectsOpacity, effectsIconSize = PvPOverlayConfig.effectsIconSize;
        int effectsCornerRadius = PvPOverlayConfig.effectsCornerRadius, effectsRowGap = PvPOverlayConfig.effectsRowGap;
        int effectsScale = PvPOverlayConfig.effectsScale;
        int targetOffsetX = PvPOverlayConfig.targetOffsetX, targetOffsetY = PvPOverlayConfig.targetOffsetY;
        int targetHudY = PvPOverlayConfig.targetHudY;
        int targetColor = PvPOverlayConfig.targetColor, targetBg = PvPOverlayConfig.targetBg;
        int targetOpacity = PvPOverlayConfig.targetOpacity, targetHoldMs = PvPOverlayConfig.targetHoldMs;
        int armorColor = PvPOverlayConfig.armorColor;
        int targetScale = PvPOverlayConfig.targetScale;
        int timeX = PvPOverlayConfig.timeX, timeY = PvPOverlayConfig.timeY;
        int timeBg = PvPOverlayConfig.timeBg, timeTextColor = PvPOverlayConfig.timeTextColor;
        int timeOpacity = PvPOverlayConfig.timeOpacity, timeCornerRadius = PvPOverlayConfig.timeCornerRadius;
        int warningPercent = PvPOverlayConfig.warningPercent, warningMode = PvPOverlayConfig.warningMode;
        int durabilityMode = PvPOverlayConfig.durabilityMode;
    }
}
