package ru.pvpoverlay;

import net.minecraft.client.gui.hud.ChatHudLine;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/** Lightweight UI animations used by Better Minecraft. */
public final class BetterMinecraftController {
    private static final Map<ChatHudLine.Visible, Long> CHAT_LINE_STARTS = new IdentityHashMap<>();
    private static boolean hotbarInitialized;
    private static float hotbarCurrentX;
    private static float hotbarStartX;
    private static int hotbarTargetX = Integer.MIN_VALUE;
    private static long hotbarTransitionStartNanos;

    private BetterMinecraftController() { }

    public static boolean isEnabled() {
        return PvPOverlayConfig.betterMinecraftEnabled;
    }

    /** Registers the exact visible chat row instance, not the whole chat or message text. */
    public static synchronized void onChatLineCreated(ChatHudLine.Visible line) {
        if (line == null || !isEnabled() || !PvPOverlayConfig.betterChatAnimation) return;
        long now = System.nanoTime();
        CHAT_LINE_STARTS.put(line, now);

        long retentionNanos = Math.max(5_000_000_000L,
                ((long) PvPOverlayConfig.betterChatDurationMs + 1500L) * 1_000_000L);
        Iterator<Map.Entry<ChatHudLine.Visible, Long>> iterator = CHAT_LINE_STARTS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > retentionNanos) iterator.remove();
        }
        while (CHAT_LINE_STARTS.size() > 256) {
            Iterator<ChatHudLine.Visible> keys = CHAT_LINE_STARTS.keySet().iterator();
            if (!keys.hasNext()) break;
            keys.next();
            keys.remove();
        }
    }

    /** Negative X offset applied only while this one newly-created line slides in. */
    public static synchronized int chatLineOffset(ChatHudLine.Visible line, int scaledScreenWidth) {
        if (!isEnabled() || !PvPOverlayConfig.betterChatAnimation || line == null) return 0;
        Long start = CHAT_LINE_STARTS.get(line);
        if (start == null) return 0;

        double durationMs = clamp(PvPOverlayConfig.betterChatDurationMs, 120, 3000);
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        double progress = clamp(elapsedMs / durationMs, 0.0, 1.0);
        if (progress >= 1.0) {
            CHAT_LINE_STARTS.remove(line);
            return 0;
        }
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        return (int) Math.round(-Math.min(90, scaledScreenWidth * 0.12) * (1.0 - eased));
    }

    public static boolean useRoundedHotbar() {
        return isEnabled() && PvPOverlayConfig.betterHotbarPanelEnabled;
    }

    /** Smoothly moves the selected-slot plate; all item stacks stay at vanilla coordinates. */
    public static int animateHotbarSelectionX(int targetX) {
        if (!isEnabled() || !PvPOverlayConfig.betterHotbarAnimation) {
            hotbarInitialized = false;
            hotbarTargetX = Integer.MIN_VALUE;
            return targetX;
        }
        long now = System.nanoTime();
        if (!hotbarInitialized) {
            hotbarInitialized = true;
            hotbarCurrentX = targetX;
            hotbarStartX = targetX;
            hotbarTargetX = targetX;
            hotbarTransitionStartNanos = now;
            return targetX;
        }
        if (targetX != hotbarTargetX) {
            hotbarStartX = hotbarCurrentX;
            hotbarTargetX = targetX;
            hotbarTransitionStartNanos = now;
        }
        double durationMs = clamp(PvPOverlayConfig.betterHotbarDurationMs, 90, 500);
        double elapsedMs = (now - hotbarTransitionStartNanos) / 1_000_000.0;
        double progress = clamp(elapsedMs / durationMs, 0.0, 1.0);
        double eased = 1.0 - Math.pow(1.0 - progress, 3.0);
        hotbarCurrentX = (float) (hotbarStartX + (hotbarTargetX - hotbarStartX) * eased);
        if (progress >= 1.0) hotbarCurrentX = hotbarTargetX;
        return Math.round(hotbarCurrentX);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
