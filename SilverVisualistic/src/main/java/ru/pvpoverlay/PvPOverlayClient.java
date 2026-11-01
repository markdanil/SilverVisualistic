package ru.pvpoverlay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;
import ru.pvpoverlay.hitfeedback.HitTintTracker;
import net.minecraft.client.world.ClientWorld;

public class PvPOverlayClient implements ClientModInitializer {
    private static UUID targetId;
    private static long targetUntil;
    private static UUID highlightTargetId;
    private static long highlightTargetUntil;
    private static boolean warnedHelmet, warnedChest, warnedLegs, warnedBoots;
    private static KeyBinding openSettingsKey;
    private static ClientWorld hitFeedbackWorld;

    private record EffectRow(Identifier icon, String text, int textWidth) { }
    private record HudLine(String text, int color) { }

    public static void setTarget(PlayerEntity target) {
        if (target == null) return;
        targetId = target.getUuid();
        targetUntil = System.currentTimeMillis() + PvPOverlayConfig.targetHoldMs;
    }


    public static void rememberHighlightTarget(LivingEntity target) {
        if (target == null) return;
        highlightTargetId = target.getUuid();
        highlightTargetUntil = System.currentTimeMillis() + PvPOverlayConfig.enemyHighlightHoldMs;
    }

    static LivingEntity findHighlightTarget(MinecraftClient mc) {
        if (highlightTargetId == null || mc.world == null) return null;
        if (System.currentTimeMillis() > highlightTargetUntil) {
            highlightTargetId = null;
            return null;
        }
        for (var entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && living.getUuid().equals(highlightTargetId)) {
                if (!living.isAlive()) {
                    highlightTargetId = null;
                    return null;
                }
                return living;
            }
        }
        return null;
    }

    @Override
    public void onInitializeClient() {
        PvPOverlayConfig.load();
        FreeLookController.register();
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvpoverlay.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.pvpoverlay"
        ));
        // Use Fabric's interaction event instead of relying on a renderer-specific mixin.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (player == client.player && entity instanceof LivingEntity living && living != player) {
                rememberHighlightTarget(living);
                if (living instanceof PlayerEntity targetPlayer) setTarget(targetPlayer);
            }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            OptimizationController.tick(client);
            AutoSprintController.tick(client);
            SilverVisibleClient.tick(client);
            if (client.world != hitFeedbackWorld) {
                HitTintTracker.clear();
                hitFeedbackWorld = client.world;
            }
            if (PvPOverlayConfig.hitFeedbackEnabled) HitTintTracker.removeExpired();
            else HitTintTracker.clear();
            boolean openSettingsPressed = openSettingsKey != null && openSettingsKey.wasPressed();
            if (openSettingsPressed && client.currentScreen == null) {
                client.setScreen(SilverVisibleAuthScreen.gate(null));
            }
            if (client.player == null || client.world == null) {
                targetId = null;
                highlightTargetId = null;
                warnedHelmet = warnedChest = warnedLegs = warnedBoots = false;
                return;
            }
            checkArmor(client);
            if (targetId != null && System.currentTimeMillis() > targetUntil) targetId = null;
            if (highlightTargetId != null && System.currentTimeMillis() > highlightTargetUntil) highlightTargetId = null;
        });
        HudRenderCallback.EVENT.register((draw, tickCounter) -> renderHud(draw, tickCounter.getTickDelta(true)));
        WorldRenderEvents.AFTER_ENTITIES.register(EnemyHighlightRenderer::render);
    }

    private static void checkArmor(MinecraftClient mc) {
        ItemStack[] stacks = {
                mc.player.getEquippedStack(EquipmentSlot.HEAD),
                mc.player.getEquippedStack(EquipmentSlot.CHEST),
                mc.player.getEquippedStack(EquipmentSlot.LEGS),
                mc.player.getEquippedStack(EquipmentSlot.FEET)
        };
        boolean[] was = {warnedHelmet, warnedChest, warnedLegs, warnedBoots};
        String[] names = {UiLanguage.tr("шлем"), UiLanguage.tr("нагрудник"), UiLanguage.tr("поножи"), UiLanguage.tr("ботинки")};
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            boolean low = !stack.isEmpty() && stack.isDamageable()
                    && durabilityPercent(stack) <= PvPOverlayConfig.warningPercent;
            if (low && !was[i]) warn(mc, names[i], stack);
            was[i] = low;
        }
        warnedHelmet = was[0];
        warnedChest = was[1];
        warnedLegs = was[2];
        warnedBoots = was[3];
    }

    private static int durabilityPercent(ItemStack stack) {
        return Math.max(0, Math.round(100f * (stack.getMaxDamage() - stack.getDamage())
                / Math.max(1, stack.getMaxDamage())));
    }

    private static String durability(ItemStack stack) {
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamage());
        return switch (PvPOverlayConfig.durabilityMode) {
            case 1 -> durabilityPercent(stack) + "%";
            case 2 -> durabilityPercent(stack) + "% / " + remaining;
            default -> remaining + (PvPOverlayConfig.language == 1 ? " durability" : " прочн.");
        };
    }

    private static void warn(MinecraftClient mc, String part, ItemStack stack) {
        String message = UiLanguage.tr("Низкая прочность") + ": " + part + " (" + durability(stack) + ")";
        switch (PvPOverlayConfig.warningMode) {
            case 0 -> {
                if (mc.player != null) mc.player.sendMessage(Text.literal("§c" + message), true);
            }
            case 1 -> {
                if (mc.player != null) mc.player.sendMessage(Text.literal("§c" + message), false);
            }
            case 2 -> { /* подсветка отображается в HUD */ }
            default -> { }
        }
    }

    private static void renderHud(DrawContext draw, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (BetterMinecraftController.isEnabled()) {
            if (PvPOverlayConfig.betterSoftVignette) renderSoftVignette(draw);
        }
        if (PvPOverlayConfig.gpsEnabled) renderGps(draw, mc);
        if (PvPOverlayConfig.showEffects) renderEffectsTable(draw, mc);
        if (PvPOverlayConfig.showTarget) renderTargetPanel(draw, mc);
        if (PvPOverlayConfig.showTimeOfDay) renderTimeOfDay(draw, mc);
        if (PvPOverlayConfig.warningMode == 2) renderHotbarWarnings(draw, mc);
        if (FreeLookController.isActive()) renderFreeLookInfo(draw, mc);
        if (PvPOverlayConfig.customCrosshairEnabled) renderCustomCrosshair(draw);
        if (SilverVisibleClient.isAuthenticated() && PvPOverlayConfig.silverVisibleShowHud) renderSilverVisibleHud(draw, mc);
        if (PvPOverlayConfig.streamerModeEnabled) renderStreamerModeHud(draw, mc);
    }


    /** Compact online counter for the authenticated SilverVisible network. */
    private static void renderSilverVisibleHud(DrawContext draw, MinecraftClient mc) {
        int panelWidth = 174;
        int panelHeight = 31;
        int x = draw.getScaledWindowWidth() - panelWidth - 10;
        int y = PvPOverlayConfig.showTimeOfDay ? 34 : 8;
        drawRoundedPanel(draw, x, y, panelWidth, panelHeight, 9, 0xE8171424);
        draw.fill(x + 7, y + 7, x + 9, y + panelHeight - 7, 0xFF9D83E8);
        int count = SilverVisibleClient.onlineCount();
        String title = "SilverVisible";
        String subtitle = (PvPOverlayConfig.language == 1 ? "Online players: " : "Игроков в сети: ") + count;
        draw.drawText(mc.textRenderer, title, x + 16, y + 5, 0xFFE9E2FF, true);
        draw.drawText(mc.textRenderer, subtitle, x + 16, y + 17, 0xFFC5BBD9, false);
        int dotColor = SilverVisibleClient.isServiceReachable() ? 0xFF72D6A0 : 0xFFE0A65C;
        draw.fill(x + panelWidth - 11, y + 9, x + panelWidth - 7, y + 13, dotColor);
    }


    /** Local streamer information panel; it does not transmit anything by itself. */
    private static void renderStreamerModeHud(DrawContext draw, MinecraftClient mc) {
        if (mc.player == null) return;
        String nickname = mc.player.getName().getString();
        String server = SilverVisibleClient.currentServerAddress(mc);
        java.util.List<String> lines = new java.util.ArrayList<>(2);
        if (PvPOverlayConfig.streamerShowNickname) lines.add("Ник: " + nickname);
        if (PvPOverlayConfig.streamerShowServer) lines.add("Сервер: " + server);
        if (lines.isEmpty()) return;

        int width = 214;
        int height = 9 + lines.size() * 12;
        int x = draw.getScaledWindowWidth() - width - 10;
        int y = PvPOverlayConfig.silverVisibleShowHud && SilverVisibleClient.isAuthenticated() ? 76
                : PvPOverlayConfig.showTimeOfDay ? 56 : 8;
        drawRoundedPanel(draw, x, y, width, height, 8, 0xE8171424);
        draw.fill(x + 7, y + 6, x + 9, y + height - 6, 0xFF9D83E8);
        int lineY = y + 5;
        for (String line : lines) {
            String shown = line;
            int maxWidth = width - 28;
            while (mc.textRenderer.getWidth(shown) > maxWidth && shown.length() > 8) {
                shown = shown.substring(0, shown.length() - 2) + "…";
            }
            draw.drawText(mc.textRenderer, shown, x + 16, lineY, 0xFFE9E2FF, false);
            lineY += 12;
        }
    }

    private static void renderCustomCrosshair(DrawContext draw) {
        int cx = draw.getScaledWindowWidth() / 2;
        int cy = draw.getScaledWindowHeight() / 2;
        int size = Math.max(3, Math.min(16, PvPOverlayConfig.customCrosshairSize));
        int thickness = Math.max(1, Math.min(4, PvPOverlayConfig.customCrosshairThickness));
        int gap = Math.max(1, Math.min(10, PvPOverlayConfig.customCrosshairGap));
        int color = 0xFF000000 | (PvPOverlayConfig.customCrosshairColor & 0xFFFFFF);
        boolean outline = PvPOverlayConfig.customCrosshairOutline;

        switch (PvPOverlayConfig.customCrosshairType) {
            case 1 -> crosshairRect(draw, cx - thickness / 2, cy - thickness / 2, thickness, thickness, color, outline);
            case 2 -> crosshairCircle(draw, cx, cy, size, thickness, color, outline);
            case 3 -> {
                crosshairCircle(draw, cx, cy, size, thickness, color, outline);
                crosshairRect(draw, cx - 1, cy - 1, 2, 2, color, outline);
            }
            case 4 -> {
                int d = Math.max(2, gap + size / 2);
                crosshairRect(draw, cx - d - thickness / 2, cy - d - thickness / 2, thickness, thickness, color, outline);
                crosshairRect(draw, cx + d - thickness / 2, cy - d - thickness / 2, thickness, thickness, color, outline);
                crosshairRect(draw, cx - d - thickness / 2, cy + d - thickness / 2, thickness, thickness, color, outline);
                crosshairRect(draw, cx + d - thickness / 2, cy + d - thickness / 2, thickness, thickness, color, outline);
            }
            default -> {
                crosshairRect(draw, cx - gap - size, cy - thickness / 2, size, thickness, color, outline);
                crosshairRect(draw, cx + gap + 1, cy - thickness / 2, size, thickness, color, outline);
                crosshairRect(draw, cx - thickness / 2, cy - gap - size, thickness, size, color, outline);
                crosshairRect(draw, cx - thickness / 2, cy + gap + 1, thickness, size, color, outline);
            }
        }
    }

    private static void crosshairRect(DrawContext draw, int x, int y, int width, int height, int color, boolean outline) {
        if (outline) draw.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF101010);
        draw.fill(x, y, x + width, y + height, color);
    }

    private static void crosshairCircle(DrawContext draw, int cx, int cy, int radius, int thickness, int color, boolean outline) {
        if (outline) drawCircleRing(draw, cx, cy, radius + 1, thickness + 2, 0xFF101010);
        drawCircleRing(draw, cx, cy, radius, thickness, color);
    }

    private static void drawCircleRing(DrawContext draw, int cx, int cy, int radius, int thickness, int color) {
        int innerRadius = Math.max(0, radius - thickness);
        int outerSq = radius * radius;
        int innerSq = innerRadius * innerRadius;
        for (int dy = -radius; dy <= radius; dy++) {
            int outerX = (int) Math.floor(Math.sqrt(Math.max(0, outerSq - dy * dy)));
            int innerX = Math.abs(dy) < innerRadius
                    ? (int) Math.floor(Math.sqrt(Math.max(0, innerSq - dy * dy))) : -1;
            int y = cy + dy;
            if (innerX < 0) {
                draw.fill(cx - outerX, y, cx + outerX + 1, y + 1, color);
            } else {
                draw.fill(cx - outerX, y, cx - innerX, y + 1, color);
                draw.fill(cx + innerX + 1, y, cx + outerX + 1, y + 1, color);
            }
        }
    }

    /** Draw all active effects inside one shared rounded panel. */
    private static void renderEffectsTable(DrawContext draw, MinecraftClient mc) {
        List<StatusEffectInstance> activeEffects = new ArrayList<>(mc.player.getStatusEffects());
        if (activeEffects.isEmpty()) return;

        int iconSize = PvPOverlayConfig.showEffectIcons ? PvPOverlayConfig.effectsIconSize : 0;
        int iconGap = iconSize > 0 ? 5 : 0;
        int rowHeight = Math.max(mc.textRenderer.fontHeight, iconSize);
        int rowGap = PvPOverlayConfig.effectsRowGap;
        int paddingX = 7;
        int paddingY = 6;

        List<EffectRow> rows = new ArrayList<>(activeEffects.size());
        int contentWidth = 0;
        for (StatusEffectInstance effect : activeEffects) {
            Identifier effectId = Registries.STATUS_EFFECT.getId(effect.getEffectType().value());
            String effectName = effectId == null
                    ? Text.translatable(effect.getEffectType().value().getTranslationKey()).getString()
                    : UiLanguage.effectName(effectId.getPath());
            String label = effectName + " " + duration(effect);
            Identifier icon = null;
            if (iconSize > 0 && effectId != null) {
                icon = Identifier.of(effectId.getNamespace(),
                        "textures/mob_effect/" + effectId.getPath() + ".png");
            }
            int textWidth = mc.textRenderer.getWidth(label);
            int rowWidth = (iconSize > 0 ? iconSize + iconGap : 0) + textWidth;
            contentWidth = Math.max(contentWidth, rowWidth);
            rows.add(new EffectRow(icon, label, textWidth));
        }

        int panelWidth = contentWidth + paddingX * 2;
        int panelHeight = paddingY * 2 + rows.size() * rowHeight + (rows.size() - 1) * rowGap;
        float scale = Math.max(25, Math.min(200, PvPOverlayConfig.effectsScale)) / 100.0f;
        int panelColor = alpha(PvPOverlayConfig.effectsBg, PvPOverlayConfig.effectsOpacity);

        // Scale the entire widget (panel, padding, text, icons, gaps and rounded corners).
        draw.getMatrices().push();
        draw.getMatrices().translate(PvPOverlayConfig.effectsX, PvPOverlayConfig.effectsY, 0.0f);
        draw.getMatrices().scale(scale, scale, 1.0f);
        drawRoundedPanel(draw, 0, 0, panelWidth, panelHeight,
                PvPOverlayConfig.effectsCornerRadius, panelColor);

        int rowY = paddingY;
        for (EffectRow row : rows) {
            int contentX = paddingX;
            if (iconSize > 0) {
                if (row.icon() != null) {
                    int iconY = rowY + (rowHeight - iconSize) / 2;
                    draw.drawTexture(RenderLayer::getGuiTextured, row.icon(),
                            contentX, iconY, 0.0f, 0.0f, iconSize, iconSize, 18, 18);
                }
                contentX += iconSize + iconGap;
            }
            int textY = rowY + (rowHeight - mc.textRenderer.fontHeight) / 2;
            draw.drawText(mc.textRenderer, row.text(), contentX, textY,
                    PvPOverlayConfig.effectsTextColor, true);
            rowY += rowHeight + rowGap;
        }
        draw.getMatrices().pop();
    }

    /** Restore the reliable screen-HUD target card from the earlier 1.21.4 build. */
    private static void renderTargetPanel(DrawContext draw, MinecraftClient mc) {
        PlayerEntity target = findTarget(mc);
        if (target == null) return;

        List<HudLine> lines = new ArrayList<>();
        StringBuilder headline = new StringBuilder(target.getName().getString());
        if (PvPOverlayConfig.showTargetHealth) {
            headline.append("   ♥ ")
                    .append(String.format(Locale.ROOT, "%.1f/%.1f", target.getHealth(), target.getMaxHealth()));
        }
        if (PvPOverlayConfig.showTargetPing) {
            int ping = -1;
            if (mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(target.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            headline.append("   ").append(ping < 0 ? UiLanguage.tr("Пинг неизвестен")
                    : (PvPOverlayConfig.language == 1 ? "Ping " + ping + " ms" : "Пинг " + ping + " мс"));
        }
        lines.add(new HudLine(headline.toString(), PvPOverlayConfig.targetColor));

        if (PvPOverlayConfig.showTargetArmor) {
            lines.add(new HudLine(armorLine(target, EquipmentSlot.HEAD, UiLanguage.tr("Шлем")), PvPOverlayConfig.armorColor));
            lines.add(new HudLine(armorLine(target, EquipmentSlot.CHEST, UiLanguage.tr("Нагрудник")), PvPOverlayConfig.armorColor));
            lines.add(new HudLine(armorLine(target, EquipmentSlot.LEGS, UiLanguage.tr("Поножи")), PvPOverlayConfig.armorColor));
            lines.add(new HudLine(armorLine(target, EquipmentSlot.FEET, UiLanguage.tr("Ботинки")), PvPOverlayConfig.armorColor));
        }

        int lineHeight = mc.textRenderer.fontHeight + 2;
        int paddingX = 7;
        int paddingY = 6;
        int panelWidth = 0;
        for (HudLine line : lines) panelWidth = Math.max(panelWidth, mc.textRenderer.getWidth(line.text()));
        panelWidth += paddingX * 2;
        int panelHeight = paddingY * 2 + lines.size() * lineHeight - 2;

        float scale = Math.max(25, Math.min(200, PvPOverlayConfig.targetScale)) / 100.0f;
        int scaledWidth = (int) Math.ceil(panelWidth * scale);
        int panelX = (draw.getScaledWindowWidth() - scaledWidth) / 2 + PvPOverlayConfig.targetOffsetX;
        panelX = Math.max(0, Math.min(draw.getScaledWindowWidth() - scaledWidth, panelX));
        int panelY = Math.max(0, PvPOverlayConfig.targetHudY);

        // Scale the entire target card while keeping its visual centre aligned to the screen.
        draw.getMatrices().push();
        draw.getMatrices().translate(panelX, panelY, 0.0f);
        draw.getMatrices().scale(scale, scale, 1.0f);
        drawRoundedPanel(draw, 0, 0, panelWidth, panelHeight,
                PvPOverlayConfig.effectsCornerRadius,
                alpha(PvPOverlayConfig.targetBg, PvPOverlayConfig.targetOpacity));

        int textY = paddingY;
        for (HudLine line : lines) {
            draw.drawText(mc.textRenderer, line.text(), paddingX, textY,
                    line.color(), true);
            textY += lineHeight;
        }
        draw.getMatrices().pop();
    }

    static PlayerEntity findTarget(MinecraftClient mc) {
        if (targetId == null || System.currentTimeMillis() > targetUntil || mc.world == null) return null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getUuid().equals(targetId) && player != mc.player) return player;
        }
        return null;
    }

    private static String armorLine(PlayerEntity player, EquipmentSlot slot, String name) {
        ItemStack stack = player.getEquippedStack(slot);
        if (stack.isEmpty()) return name + ": " + UiLanguage.tr("нет");

        // Elytra occupies the chest equipment slot, but it is not a chestplate.
        String displayName = slot == EquipmentSlot.CHEST && stack.isOf(Items.ELYTRA)
                ? UiLanguage.tr("Элитры") : name;
        if (!stack.isDamageable()) return displayName + ": —";
        return displayName + ": " + durabilityPercent(stack) + "%";
    }

    /** Compact GPS card with the active vanilla dimension and block coordinates. */
    private static void renderGps(DrawContext draw, MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        String dimension;
        if (mc.world.getRegistryKey().equals(World.OVERWORLD)) {
            dimension = UiLanguage.tr("Верхний мир");
        } else if (mc.world.getRegistryKey().equals(World.NETHER)) {
            dimension = UiLanguage.tr("Незер");
        } else if (mc.world.getRegistryKey().equals(World.END)) {
            dimension = UiLanguage.tr("Энд");
        } else {
            dimension = UiLanguage.tr("Другое измерение");
        }

        var pos = mc.player.getBlockPos();
        String coordinates = "X: " + pos.getX() + "   Y: " + pos.getY() + "   Z: " + pos.getZ();
        int paddingX = 9;
        int paddingY = 6;
        int contentWidth = Math.max(mc.textRenderer.getWidth(dimension), mc.textRenderer.getWidth(coordinates));
        int panelWidth = contentWidth + paddingX * 2 + 4;
        int lineHeight = mc.textRenderer.fontHeight;
        int panelHeight = paddingY * 2 + lineHeight * 2 + 3;
        int x = PvPOverlayConfig.gpsX;
        int y = PvPOverlayConfig.gpsY;

        drawRoundedPanel(draw, x, y, panelWidth, panelHeight, 7,
                alpha(0x101820, PvPOverlayConfig.gpsOpacity));
        // Small gold rail matches the mod's SilverVisualistic visual language.
        draw.fill(x + 4, y + 6, x + 6, y + panelHeight - 6, 0xFFD9C39B);
        draw.drawText(mc.textRenderer, dimension, x + paddingX + 3, y + paddingY,
                0xFFE9CFA6, true);
        draw.drawText(mc.textRenderer, coordinates, x + paddingX + 3,
                y + paddingY + lineHeight + 3, 0xFFF5F0E6, true);
    }

    /** Shows daylight and its sun/moon marker together in one compact top-right panel. */
    private static void renderTimeOfDay(DrawContext draw, MinecraftClient mc) {
        if (mc.world == null) return;

        String label = UiLanguage.tr("Время") + ": " + timeOfDayName(mc.world.getTimeOfDay());
        int paddingX = 7;
        int paddingY = 5;
        boolean showMarker = BetterMinecraftController.isEnabled() && PvPOverlayConfig.betterCelestialMarker;
        int markerArea = showMarker ? 22 : 0;
        int panelWidth = mc.textRenderer.getWidth(label) + paddingX * 2 + markerArea;
        int panelHeight = Math.max(mc.textRenderer.fontHeight, 18) + paddingY * 2;
        int panelX = draw.getScaledWindowWidth() - PvPOverlayConfig.timeX - panelWidth;
        int panelY = PvPOverlayConfig.timeY;

        drawRoundedPanel(draw, panelX, panelY, panelWidth, panelHeight,
                PvPOverlayConfig.timeCornerRadius,
                alpha(PvPOverlayConfig.timeBg, PvPOverlayConfig.timeOpacity));
        if (showMarker) {
            int markerX = panelX + 13;
            int markerY = panelY + panelHeight / 2;
            renderCelestialMarker(draw, mc, markerX, markerY);
        }
        draw.drawText(mc.textRenderer, label, panelX + paddingX + markerArea, panelY + paddingY,
                PvPOverlayConfig.timeTextColor, true);
    }

    /** Soft multi-layer vignette for a cinematic ambience; deliberately not a full-screen shader. */
    private static void renderSoftVignette(DrawContext draw) {
        int strength = Math.max(0, Math.min(100, PvPOverlayConfig.betterVignetteStrength));
        if (strength == 0) return;
        int width = draw.getScaledWindowWidth();
        int height = draw.getScaledWindowHeight();
        int depth = Math.max(24, Math.min(88, Math.min(width, height) / 7));
        int maxAlpha = Math.max(1, strength * 48 / 100);
        for (int i = 0; i < 18; i++) {
            int inset = i * depth / 18;
            int strip = Math.max(1, depth / 18 + 1);
            float fade = (float) (1.0 - (i / 18.0));
            int alpha = Math.round(maxAlpha * fade * fade * 0.72f);
            if (alpha <= 0) continue;
            int color = alpha << 24;
            int top = inset;
            int bottom = height - inset - strip;
            int left = inset;
            int right = width - inset - strip;
            draw.fill(left, top, width - inset, top + strip, color);
            draw.fill(left, bottom, width - inset, bottom + strip, color);
            draw.fill(left, top, left + strip, height - inset, color);
            draw.fill(right, top, right + strip, height - inset, color);
        }
    }

    /** Pixel-style sun/moon marker, placed inside the time-of-day panel. */
    private static void renderCelestialMarker(DrawContext draw, MinecraftClient mc, int x, int y) {
        long time = Math.floorMod(mc.world.getTimeOfDay(), 24000L);
        boolean daytime = time >= 1000L && time < 13000L;
        int gold = 0xFFFFD783;
        if (PvPOverlayConfig.betterCelestialGlow) {
            drawCircleDisc(draw, x, y, 9, daytime ? 0x22FFD783 : 0x222C4770);
            drawCircleDisc(draw, x, y, 7, daytime ? 0x18FFD783 : 0x183C5680);
        }
        drawCircleDisc(draw, x, y, 6, 0xAA18202A);
        if (daytime) {
            drawCircleDisc(draw, x, y, 3, gold);
            draw.fill(x - 1, y - 6, x + 1, y - 4, gold);
            draw.fill(x - 1, y + 4, x + 1, y + 6, gold);
            draw.fill(x - 6, y - 1, x - 4, y + 1, gold);
            draw.fill(x + 4, y - 1, x + 6, y + 1, gold);
            draw.fill(x - 4, y - 4, x - 3, y - 3, gold);
            draw.fill(x + 3, y - 4, x + 4, y - 3, gold);
            draw.fill(x - 4, y + 3, x - 3, y + 4, gold);
            draw.fill(x + 3, y + 3, x + 4, y + 4, gold);
        } else {
            drawCircleDisc(draw, x, y, 5, 0xFFF2E0B6);
            drawCircleDisc(draw, x + 3, y - 3, 5, 0xFF26313B);
            draw.fill(x - 5, y + 4, x - 3, y + 5, 0xFFD8CBAF);
        }
    }

    private static void drawCircleDisc(DrawContext draw, int cx, int cy, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            int halfWidth = (int) Math.floor(Math.sqrt(Math.max(0, radiusSquared - dy * dy)));
            draw.fill(cx - halfWidth, cy + dy, cx + halfWidth + 1, cy + dy + 1, color);
        }
    }

    private static String timeOfDayName(long worldTime) {
        long time = Math.floorMod(worldTime, 24000L);
        if (time < 1000L || time >= 23000L) return UiLanguage.tr("Восход");
        if (time < 11000L) return UiLanguage.tr("День");
        if (time < 13000L) return UiLanguage.tr("Закат");
        return UiLanguage.tr("Ночь");
    }


    private static void renderFreeLookInfo(DrawContext draw, MinecraftClient mc) {
        if (mc.player == null) return;
        var team = mc.player.getScoreboardTeam();
        String prefix = team == null ? "" : team.getPrefix().getString();
        String suffix = team == null ? "" : team.getSuffix().getString();
        String name = prefix + mc.player.getName().getString() + suffix;
        String label = UiLanguage.tr("Свободный просмотр") + ": " + name;
        int width = mc.textRenderer.getWidth(label) + 14;
        int panelHeight = mc.textRenderer.fontHeight + 10;
        int screenWidth = draw.getScaledWindowWidth();
        int screenHeight = draw.getScaledWindowHeight();
        int x = (screenWidth - width) / 2 + PvPOverlayConfig.freeLookHudOffsetX;
        int y = 8 + PvPOverlayConfig.freeLookHudOffsetY;
        x = Math.max(0, Math.min(Math.max(0, screenWidth - width), x));
        y = Math.max(0, Math.min(Math.max(0, screenHeight - panelHeight), y));
        drawRoundedPanel(draw, x, y, width, panelHeight,
                6, 0xAA101820);
        draw.drawText(mc.textRenderer, label, x + 7, y + 5, 0xFFFFFFFF, true);
    }

    private static void renderHotbarWarnings(DrawContext draw, MinecraftClient mc) {
        int screenWidth = draw.getScaledWindowWidth();
        int screenHeight = draw.getScaledWindowHeight();
        ItemStack[] armor = {
                mc.player.getEquippedStack(EquipmentSlot.HEAD), mc.player.getEquippedStack(EquipmentSlot.CHEST),
                mc.player.getEquippedStack(EquipmentSlot.LEGS), mc.player.getEquippedStack(EquipmentSlot.FEET)
        };
        String[] names = PvPOverlayConfig.language == 1
                ? new String[]{"H", "C", "L", "B"} : new String[]{"Ш", "Н", "П", "Б"};
        int y = screenHeight - 58;
        int x = screenWidth / 2 - 90;
        for (int i = 0; i < armor.length; i++) {
            ItemStack stack = armor[i];
            if (!stack.isEmpty() && stack.isDamageable()
                    && durabilityPercent(stack) <= PvPOverlayConfig.warningPercent) {
                draw.fill(x + i * 46, y, x + i * 46 + 42, y + 13, 0xCCFF2020);
                draw.drawCenteredTextWithShadow(mc.textRenderer, names[i] + " " + durabilityPercent(stack) + "%",
                        x + i * 46 + 21, y + 2, 0xFFFFFFFF);
            }
        }
    }

    /** Rounded rectangle assembled from scanlines; no texture or extra mod dependency required. */
    private static void drawRoundedPanel(DrawContext draw, int x, int y, int width, int height,
                                         int requestedRadius, int color) {
        if (width <= 0 || height <= 0) return;
        int radius = Math.max(0, Math.min(requestedRadius, Math.min(width, height) / 2));
        if (radius == 0) {
            draw.fill(x, y, x + width, y + height, color);
            return;
        }

        for (int row = 0; row < height; row++) {
            int inset = 0;
            if (row < radius) {
                double dy = radius - row - 0.5;
                inset = radius - (int) Math.floor(Math.sqrt(radius * radius - dy * dy));
            } else if (row >= height - radius) {
                double dy = row - (height - radius) + 0.5;
                inset = radius - (int) Math.floor(Math.sqrt(radius * radius - dy * dy));
            }
            if (width - inset * 2 > 0) {
                draw.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            }
        }
    }

    private static int alpha(int rgb, int opacity) {
        return ((Math.max(0, Math.min(100, opacity)) * 255 / 100) << 24) | (rgb & 0x00FFFFFF);
    }

    private static String duration(StatusEffectInstance effect) {
        int ticks = effect.getDuration();
        if (ticks < 0) return "∞";
        int seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
