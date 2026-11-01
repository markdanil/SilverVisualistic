package ru.pvpoverlay.hitfeedback.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.IdentityHashMap;
import java.util.Map;

/** Applies a temporary color blend only while the locally-hit player's model is rendered. */
public final class TintedVertexConsumerProvider implements VertexConsumerProvider {
    private final VertexConsumerProvider delegate;
    private final int tintRgb;
    private final float strength;
    private final Map<RenderLayer, VertexConsumer> wrappers = new IdentityHashMap<>();

    public TintedVertexConsumerProvider(VertexConsumerProvider delegate, int tintRgb, float strength) {
        this.delegate = delegate;
        this.tintRgb = tintRgb & 0xFFFFFF;
        this.strength = Math.max(0.0F, Math.min(1.0F, strength));
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return wrappers.computeIfAbsent(layer,
                key -> new TintedVertexConsumer(delegate.getBuffer(key), tintRgb, strength));
    }

    private static final class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int tintR;
        private final int tintG;
        private final int tintB;
        private final float strength;

        private TintedVertexConsumer(VertexConsumer delegate, int tintRgb, float strength) {
            this.delegate = delegate;
            this.tintR = (tintRgb >> 16) & 0xFF;
            this.tintG = (tintRgb >> 8) & 0xFF;
            this.tintB = tintRgb & 0xFF;
            this.strength = strength;
        }

        @Override public VertexConsumer vertex(float x, float y, float z) { delegate.vertex(x, y, z); return this; }
        @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
            delegate.color(blend(red, tintR), blend(green, tintG), blend(blue, tintB), alpha);
            return this;
        }
        @Override public VertexConsumer texture(float u, float v) { delegate.texture(u, v); return this; }
        @Override public VertexConsumer overlay(int u, int v) { delegate.overlay(u, v); return this; }
        @Override public VertexConsumer light(int u, int v) { delegate.light(u, v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }

        private int blend(int original, int tint) {
            float safeOriginal = Math.max(0, Math.min(255, original));
            return Math.max(0, Math.min(255,
                    Math.round(safeOriginal * (1.0F - strength) + tint * strength)));
        }
    }
}
