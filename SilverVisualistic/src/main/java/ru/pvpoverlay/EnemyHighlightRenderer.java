package ru.pvpoverlay;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Independent one-target highlight for players and living mobs.
 * Uses only Fabric and vanilla rendering APIs; no SoupAPI/SoupVisuals/ZakoOpt hooks.
 */
public final class EnemyHighlightRenderer {
    private static final int RING_SEGMENTS = 64;
    private static final int SPHERE_LATITUDE_SEGMENTS = 14;
    private static final int SPHERE_LONGITUDE_SEGMENTS = 28;
    private static final int ORB_TRAIL_SEGMENTS = 16;
    private static final Vec3d LIGHT_DIRECTION = new Vec3d(-0.35, 0.72, 0.60).normalize();

    private EnemyHighlightRenderer() { }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!PvPOverlayConfig.enemyHighlightEnabled || client.player == null || client.world == null
                || context.matrixStack() == null || context.camera() == null) return;

        LivingEntity target = selectTarget(client);
        if (!isValidTarget(client, target)) return;

        double maxDistance = PvPOverlayConfig.enemyHighlightMaxDistance;
        if (client.player.squaredDistanceTo(target) > maxDistance * maxDistance) return;

        float tickDelta = context.tickCounter().getTickDelta(true);
        Vec3d targetPos = target.getLerpedPos(tickDelta);
        Vec3d cameraPos = context.camera().getPos();
        Vec3d towardCamera = cameraPos.subtract(targetPos).normalize();
        if (towardCamera.lengthSquared() < 1.0E-6) towardCamera = new Vec3d(0, 0, 1);

        VertexConsumerProvider consumers = context.consumers();
        VertexConsumerProvider.Immediate fallback = null;
        if (consumers == null) {
            fallback = client.getBufferBuilders().getEntityVertexConsumers();
            consumers = fallback;
        }

        int alpha = Math.max(20, Math.min(255, PvPOverlayConfig.enemyHighlightOpacity * 255 / 100));
        double phase = gradientPhase();
        double rotation = System.currentTimeMillis() / 1000.0
                * (0.25 + PvPOverlayConfig.enemyHighlightGradientSpeed / 100.0 * 1.8);
        float scale = Math.max(0.5f, Math.min(1.8f, PvPOverlayConfig.enemyHighlightScale / 100.0f));

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        try {
            // All world-render vertices are camera-relative in Fabric's shared provider.
            matrices.translate(targetPos.x - cameraPos.x, targetPos.y - cameraPos.y, targetPos.z - cameraPos.z);
            switch (PvPOverlayConfig.enemyHighlightStyle) {
                case 1 -> renderRingCylinder(consumers, matrices, target, scale, alpha, phase, rotation);
                case 2 -> renderOrbitingSpheres(consumers, matrices, target, towardCamera, scale, alpha, phase, rotation);
                default -> renderCenteredMarker(consumers, matrices, target, towardCamera, scale, alpha, phase, rotation);
            }
        } finally {
            matrices.pop();
        }
        if (fallback != null) fallback.draw(RenderLayer.getDebugQuads());
    }

    /** Crosshair selection accepts every living entity; last-target mode uses a timed UUID. */
    private static LivingEntity selectTarget(MinecraftClient client) {
        if (PvPOverlayConfig.enemyHighlightTargetMode == 0
                && client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living
                && isValidTarget(client, living)) {
            PvPOverlayClient.rememberHighlightTarget(living);
            return living;
        }
        return PvPOverlayClient.findHighlightTarget(client);
    }

    private static boolean isValidTarget(MinecraftClient client, LivingEntity target) {
        if (target == null || target == client.player || !target.isAlive() || target.isSpectator()) return false;
        return !(target instanceof PlayerEntity playerTarget) || !playerTarget.isTeammate(client.player);
    }

    /** A stable square face, like one side of a cube. Geometry never spins or skews with the gradient. */
    private static void renderCenteredMarker(VertexConsumerProvider consumers, MatrixStack matrices,
                                             LivingEntity target, Vec3d towardCamera,
                                             float scale, int alpha, double phase, double rotation) {
        VertexConsumer quads = consumers.getBuffer(RenderLayer.getDebugQuads());
        MatrixStack.Entry entry = matrices.peek();

        Vec3d normal = towardCamera.lengthSquared() < 1.0E-6
                ? new Vec3d(0, 0, 1) : towardCamera.normalize();
        Vec3d right = new Vec3d(normal.z, 0.0, -normal.x);
        if (right.lengthSquared() < 1.0E-6) right = new Vec3d(1, 0, 0);
        else right = right.normalize();
        // Build the second axis from the face normal so the marker is a true
        // square billboard: looking up/down cannot stretch it into a trapezoid.
        Vec3d up = normal.crossProduct(right).normalize();

        // The marker is a square plane in world space, held at a fixed orientation relative
        // to the viewer. Its edges remain horizontal/vertical and are never rotated by animation.
        float half = (float) Math.max(0.22, target.getWidth() * 0.54) * scale;
        Vec3d faceCenter = normal.multiply(Math.max(0.34, target.getWidth() * 1.04))
                .add(0.0, target.getHeight() * 0.55, 0.0);
        Vec3d topLeft = faceCenter.add(right.multiply(-half)).add(up.multiply(half));
        Vec3d topRight = faceCenter.add(right.multiply(half)).add(up.multiply(half));
        Vec3d bottomRight = faceCenter.add(right.multiply(half)).add(up.multiply(-half));
        Vec3d bottomLeft = faceCenter.add(right.multiply(-half)).add(up.multiply(-half));

        float thickness = 0.014f + PvPOverlayConfig.enemyHighlightMarkerThickness * 0.008f;
        Vec3d[] points = {topLeft, topRight, bottomRight, bottomLeft, topLeft};
        for (int i = 0; i < 4; i++) {
            int colorA = colorAt(phase + i * 0.25);
            int colorB = colorAt(phase + (i + 1) * 0.25);
            drawThickSegment(quads, entry, points[i], points[i + 1], normal,
                    colorA, colorB, Math.max(8, alpha / 4), thickness * 2.5f);
            drawThickSegment(quads, entry, points[i], points[i + 1], normal,
                    colorA, colorB, alpha, thickness);
        }
    }

    /** Three horizontal rings plus a continuously coloured translucent cylinder. */
    private static void renderRingCylinder(VertexConsumerProvider consumers, MatrixStack matrices,
                                           LivingEntity target, float scale, int alpha,
                                           double phase, double rotation) {
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer quads = consumers.getBuffer(RenderLayer.getDebugQuads());
        float radius = (float) Math.max(0.42, target.getWidth() * 0.82) * scale;
        float bottom = 0.025f;
        float middle = Math.max(0.18f, target.getHeight() * 0.50f);
        float top = Math.max(middle + 0.12f, target.getHeight() - 0.025f);
        int fogAlpha = Math.max(4, Math.min(110, alpha * PvPOverlayConfig.enemyHighlightCylinderOpacity / 100));

        drawCylinderBand(quads, entry, radius, bottom, middle, rotation, phase, fogAlpha);
        drawCylinderBand(quads, entry, radius, middle, top, rotation, phase + 0.19, fogAlpha);
        float ringWidth = 0.012f + PvPOverlayConfig.enemyHighlightMarkerThickness * 0.0025f;
        drawRing(quads, entry, radius, bottom, rotation, phase, alpha, ringWidth);
        drawRing(quads, entry, radius, middle, -rotation * 0.72, phase + 0.33, alpha, ringWidth);
        drawRing(quads, entry, radius, top, rotation * 0.56, phase + 0.66, alpha, ringWidth);
    }

    private static void drawCylinderBand(VertexConsumer quads, MatrixStack.Entry entry,
                                         float radius, float lowerY, float upperY,
                                         double rotation, double phase, int alpha) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / RING_SEGMENTS + rotation;
            double a1 = Math.PI * 2.0 * (i + 1) / RING_SEGMENTS + rotation;
            double u0 = i / (double) RING_SEGMENTS;
            double u1 = (i + 1.0) / RING_SEGMENTS;
            Vec3d p0 = new Vec3d(Math.cos(a0) * radius, lowerY, Math.sin(a0) * radius);
            Vec3d p1 = new Vec3d(Math.cos(a1) * radius, lowerY, Math.sin(a1) * radius);
            Vec3d p2 = new Vec3d(Math.cos(a1) * radius, upperY, Math.sin(a1) * radius);
            Vec3d p3 = new Vec3d(Math.cos(a0) * radius, upperY, Math.sin(a0) * radius);
            int c0 = colorAt(phase + u0);
            int c1 = colorAt(phase + u1);
            int a = Math.max(2, alpha / 2);
            vertex(quads, entry, p0, c0, a);
            vertex(quads, entry, p1, c1, a);
            vertex(quads, entry, p2, colorAt(phase + u1 + 0.12), a);
            vertex(quads, entry, p3, colorAt(phase + u0 + 0.12), a);
        }
    }

    private static void drawRing(VertexConsumer quads, MatrixStack.Entry entry,
                                 float radius, float y, double rotation, double phase,
                                 int alpha, float thickness) {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / RING_SEGMENTS + rotation;
            double a1 = Math.PI * 2.0 * (i + 1) / RING_SEGMENTS + rotation;
            Vec3d from = new Vec3d(Math.cos(a0) * radius, y, Math.sin(a0) * radius);
            Vec3d to = new Vec3d(Math.cos(a1) * radius, y, Math.sin(a1) * radius);
            drawThickSegment(quads, entry, from, to, new Vec3d(0, 1, 0),
                    colorAt(phase + i / (double) RING_SEGMENTS),
                    colorAt(phase + (i + 1.0) / RING_SEGMENTS), alpha, thickness);
        }
    }

    /**
     * Every orb has its own inclined orbital plane, like electrons around a nucleus.
     * Translucent tapered trails are drawn first so the filled orb remains crisp.
     */
    private static void renderOrbitingSpheres(VertexConsumerProvider consumers, MatrixStack matrices,
                                              LivingEntity target, Vec3d viewDirection,
                                              float scale, int alpha, double phase, double rotation) {
        int[] palette = PvPOverlayConfig.getEnemyHighlightColors();
        int count = Math.max(1, palette.length);
        float size = Math.max(0.065f, 0.075f + PvPOverlayConfig.enemyHighlightOrbSize / 290.0f) * scale;
        float orbit = (float) Math.max(0.50, target.getWidth() * 0.82) * scale + size * 1.6f;
        VertexConsumer quads = consumers.getBuffer(RenderLayer.getDebugQuads());
        MatrixStack.Entry entry = matrices.peek();
        int orbAlpha = Math.max(1, Math.min(210,
                alpha * PvPOverlayConfig.enemyHighlightOrbOpacity / 100));
        float centerY = target.getHeight() * 0.52f;
        Vec3d[] orbitUs = new Vec3d[count];
        Vec3d[] orbitVs = new Vec3d[count];
        Vec3d[] centers = new Vec3d[count];
        double[] angles = new double[count];

        // Build separate orbital planes and draw every tail before the spheres for clean layering.
        for (int i = 0; i < count; i++) {
            double node = i * 2.399963229728653;
            double inclination = 0.48 + (i % 4) * 0.235;
            Vec3d axis = new Vec3d(Math.sin(inclination) * Math.cos(node),
                    Math.cos(inclination), Math.sin(inclination) * Math.sin(node)).normalize();
            Vec3d reference = Math.abs(axis.y) > 0.92 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
            orbitUs[i] = reference.crossProduct(axis).normalize();
            orbitVs[i] = axis.crossProduct(orbitUs[i]).normalize();
            double orbSpeed = 0.78 + i * 0.13;
            angles[i] = rotation * orbSpeed + Math.PI * 2.0 * i / count;
            centers[i] = orbitPoint(orbitUs[i], orbitVs[i], centerY, orbit, angles[i]);

            if (PvPOverlayConfig.enemyHighlightOrbTrailsEnabled) {
                drawOrbTrail(quads, entry, orbitUs[i], orbitVs[i], centerY, orbit, angles[i],
                        size, palette[i] & 0xFFFFFF, orbAlpha, viewDirection);
            }
        }

        // Spheres are drawn last so their silhouettes stay crisp above the translucent comet tails.
        for (int i = 0; i < count; i++) {
            matrices.push();
            matrices.translate(centers[i].x, centers[i].y, centers[i].z);
            MatrixStack.Entry sphereEntry = matrices.peek();
            drawSolidSphere(quads, sphereEntry, size, palette[i] & 0xFFFFFF, orbAlpha,
                    PvPOverlayConfig.enemyHighlightOrbGlow);
            matrices.pop();
        }
    }

    private static Vec3d orbitPoint(Vec3d u, Vec3d v, float centerY,
                                    float orbitRadius, double angle) {
        return new Vec3d(0.0, centerY, 0.0)
                .add(u.multiply(Math.cos(angle) * orbitRadius))
                .add(v.multiply(Math.sin(angle) * orbitRadius));
    }

    /** Draw a short, fading comet tail that follows each orb along its own orbital plane. */
    private static void drawOrbTrail(VertexConsumer quads, MatrixStack.Entry entry,
                                     Vec3d orbitU, Vec3d orbitV, float centerY,
                                     float orbitRadius, double currentAngle, float orbSize,
                                     int color, int orbAlpha, Vec3d viewDirection) {
        float configuredLength = Math.max(10, Math.min(100, PvPOverlayConfig.enemyHighlightOrbTrailLength)) / 100.0f;
        double trailArc = 0.38 + configuredLength * 1.28;
        float baseWidth = orbSize * (0.30f + configuredLength * 0.52f);
        int trailOpacity = Math.max(5, Math.min(100, PvPOverlayConfig.enemyHighlightOrbTrailOpacity));

        for (int segment = 0; segment < ORB_TRAIL_SEGMENTS; segment++) {
            double t0 = segment / (double) ORB_TRAIL_SEGMENTS;
            double t1 = (segment + 1.0) / ORB_TRAIL_SEGMENTS;
            Vec3d from = orbitPoint(orbitU, orbitV, centerY, orbitRadius,
                    currentAngle - t0 * trailArc);
            Vec3d to = orbitPoint(orbitU, orbitV, centerY, orbitRadius,
                    currentAngle - t1 * trailArc);
            double fade = 1.0 - (t0 + t1) * 0.5;
            float width = Math.max(0.0015f, baseWidth * (float) Math.pow(fade, 1.35));
            int segmentAlpha = Math.max(1, (int) (orbAlpha * trailOpacity / 100.0 * Math.pow(fade, 1.65)));
            if (segmentAlpha <= 1) continue;
            int bright = shade(color, 0.92f + (float) (fade * 0.22));
            drawThickSegment(quads, entry, from, to, viewDirection,
                    bright, color, segmentAlpha, width);
        }
    }

    private static void drawSolidSphere(VertexConsumer quads, MatrixStack.Entry entry,
                                        float radius, int baseColor, int alpha, int glow) {
        // A fully filled latitude/longitude mesh: no wireframe or internal line geometry.
        float ambient = 0.48f + Math.max(0, Math.min(1, glow / 100.0f)) * 0.18f;
        for (int lat = 0; lat < SPHERE_LATITUDE_SEGMENTS; lat++) {
            double theta0 = Math.PI * lat / SPHERE_LATITUDE_SEGMENTS;
            double theta1 = Math.PI * (lat + 1) / SPHERE_LATITUDE_SEGMENTS;
            for (int lon = 0; lon < SPHERE_LONGITUDE_SEGMENTS; lon++) {
                double phi0 = Math.PI * 2.0 * lon / SPHERE_LONGITUDE_SEGMENTS;
                double phi1 = Math.PI * 2.0 * (lon + 1) / SPHERE_LONGITUDE_SEGMENTS;
                Vec3d n00 = sphereNormal(theta0, phi0);
                Vec3d n01 = sphereNormal(theta0, phi1);
                Vec3d n11 = sphereNormal(theta1, phi1);
                Vec3d n10 = sphereNormal(theta1, phi0);
                Vec3d p00 = n00.multiply(radius);
                Vec3d p01 = n01.multiply(radius);
                Vec3d p11 = n11.multiply(radius);
                Vec3d p10 = n10.multiply(radius);

                vertex(quads, entry, p00, shade(baseColor, ambient + (float) Math.max(0.0, n00.dotProduct(LIGHT_DIRECTION)) * 0.48f), alpha);
                vertex(quads, entry, p01, shade(baseColor, ambient + (float) Math.max(0.0, n01.dotProduct(LIGHT_DIRECTION)) * 0.48f), alpha);
                vertex(quads, entry, p11, shade(baseColor, ambient + (float) Math.max(0.0, n11.dotProduct(LIGHT_DIRECTION)) * 0.48f), alpha);
                vertex(quads, entry, p10, shade(baseColor, ambient + (float) Math.max(0.0, n10.dotProduct(LIGHT_DIRECTION)) * 0.48f), alpha);
            }
        }
    }

    private static Vec3d rotateInPlane(Vec3d right, Vec3d up, double x, double y, double clockwiseAngle) {
        double cos = Math.cos(clockwiseAngle);
        double sin = Math.sin(clockwiseAngle);
        double rotatedX = x * cos + y * sin;
        double rotatedY = -x * sin + y * cos;
        return right.multiply(rotatedX).add(up.multiply(rotatedY));
    }

    private static Vec3d sphereNormal(double theta, double phi) {
        double sin = Math.sin(theta);
        return new Vec3d(sin * Math.cos(phi), Math.cos(theta), sin * Math.sin(phi));
    }

    private static int shade(int rgb, float brightness) {
        float b = Math.max(0.25f, Math.min(1.35f, brightness));
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * b));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * b));
        int bl = Math.min(255, Math.round((rgb & 255) * b));
        return (r << 16) | (g << 8) | bl;
    }

    private static void drawThickSegment(VertexConsumer quads, MatrixStack.Entry entry,
                                         Vec3d from, Vec3d to, Vec3d viewDirection,
                                         int colorFrom, int colorTo, int alpha, float width) {
        Vec3d direction = to.subtract(from);
        Vec3d side = direction.crossProduct(viewDirection);
        if (side.lengthSquared() < 1.0E-8) side = direction.crossProduct(new Vec3d(0, 1, 0));
        if (side.lengthSquared() < 1.0E-8) side = new Vec3d(1, 0, 0);
        side = side.normalize().multiply(width * 0.5);
        vertex(quads, entry, from.subtract(side), colorFrom, alpha);
        vertex(quads, entry, from.add(side), colorFrom, alpha);
        vertex(quads, entry, to.add(side), colorTo, alpha);
        vertex(quads, entry, to.subtract(side), colorTo, alpha);
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry entry,
                               Vec3d pos, int rgb, int alpha) {
        consumer.vertex(entry, (float) pos.x, (float) pos.y, (float) pos.z)
                .color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                        Math.max(0, Math.min(255, alpha)));
    }

    private static double gradientPhase() {
        double speed = Math.max(1, Math.min(100, PvPOverlayConfig.enemyHighlightGradientSpeed));
        return (System.currentTimeMillis() * speed / 500000.0) % 1.0;
    }

    private static int colorAt(double progress) {
        int[] colors = PvPOverlayConfig.getEnemyHighlightColors();
        if (colors.length == 1) return colors[0];
        double p = progress - Math.floor(progress);
        double scaled = p * colors.length;
        int index = (int) Math.floor(scaled) % colors.length;
        int next = (index + 1) % colors.length;
        float amount = (float) (scaled - Math.floor(scaled));
        return lerpColor(colors[index], colors[next], amount);
    }

    private static int lerpColor(int from, int to, float amount) {
        int r1 = (from >> 16) & 255, g1 = (from >> 8) & 255, b1 = from & 255;
        int r2 = (to >> 16) & 255, g2 = (to >> 8) & 255, b2 = to & 255;
        int r = Math.round(r1 + (r2 - r1) * amount);
        int g = Math.round(g1 + (g2 - g1) * amount);
        int b = Math.round(b1 + (b2 - b1) * amount);
        return (r << 16) | (g << 8) | b;
    }
}
