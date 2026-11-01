package ru.pvpoverlay.hitfeedback;

import ru.pvpoverlay.PvPOverlayConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Tracks only players hit by the local player. All timestamps are client-local. */
public final class HitTintTracker {
    private static final Map<Integer, TimedTint> ACTIVE = new HashMap<>();

    private HitTintTracker() { }

    public static void mark(int entityId, int rgb) {
        if (!PvPOverlayConfig.hitFeedbackEnabled) return;
        long now = System.currentTimeMillis();
        removeExpired(now);
        ACTIVE.put(entityId, new TimedTint(rgb & 0xFFFFFF, now + PvPOverlayConfig.hitFeedbackDurationMs));
    }

    /** Returns a color for this entity only while the short hit marker is active. */
    public static Integer getActiveColor(int entityId) {
        if (!PvPOverlayConfig.hitFeedbackEnabled) return null;
        TimedTint tint = ACTIVE.get(entityId);
        if (tint == null) return null;
        if (tint.expiresAtMs() <= System.currentTimeMillis()) {
            ACTIVE.remove(entityId);
            return null;
        }
        return tint.rgb();
    }

    public static float strength() {
        return Math.max(0.10F, Math.min(1.0F, PvPOverlayConfig.hitFeedbackStrength / 100.0F));
    }

    public static void removeExpired() {
        removeExpired(System.currentTimeMillis());
    }

    private static void removeExpired(long now) {
        Iterator<Map.Entry<Integer, TimedTint>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAtMs() <= now) iterator.remove();
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private record TimedTint(int rgb, long expiresAtMs) { }
}
