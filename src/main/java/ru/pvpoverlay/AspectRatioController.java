package ru.pvpoverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import ru.pvpoverlay.mixin.CameraProjectionAccessor;

import java.util.Map;
import java.util.WeakHashMap;

/** Aspect-ratio helpers shared by the render matrix and camera projection mixins. */
public final class AspectRatioController {
    private static final Map<Camera.Projection, Float> APPLIED_CAMERA_RATIOS = new WeakHashMap<>();

    private AspectRatioController() { }

    /** Returns 0 when vanilla projection should be used. */
    public static float getTargetAspectRatio() {
        return switch (PvPOverlayConfig.aspectRatioMode) {
            case 1 -> 16.0f / 10.0f;
            case 2 -> 16.0f / 9.0f;
            case 3 -> 4.0f / 3.0f;
            case 4 -> (float) Math.max(1, PvPOverlayConfig.aspectRatioCustomWidth)
                    / (float) Math.max(1, PvPOverlayConfig.aspectRatioCustomHeight);
            default -> 0.0f;
        };
    }

    /** Adjusts the camera's frustum rectangle to match the same virtual ratio as the projection matrix. */
    public static void adjustCameraProjection(Camera.Projection projection) {
        if (projection == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null || client.getWindow().getFramebufferHeight() <= 0) return;
        float actual = (float) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight();
        if (!Float.isFinite(actual) || actual <= 0.0f) return;

        float target = getTargetAspectRatio();
        CameraProjectionAccessor accessor = (CameraProjectionAccessor) (Object) projection;
        synchronized (APPLIED_CAMERA_RATIOS) {
            Float previous = APPLIED_CAMERA_RATIOS.remove(projection);
            if (target <= 0.0f) {
                if (previous != null && previous > 0.0f) {
                    accessor.silvervisualistic$setX(accessor.silvervisualistic$getX().multiply(1.0 / previous));
                }
                return;
            }

            float factor = target / actual;
            float correction = previous == null ? factor : factor / previous;
            Vec3d adjustedHalfWidth = accessor.silvervisualistic$getX().multiply(correction);
            accessor.silvervisualistic$setX(adjustedHalfWidth);
            APPLIED_CAMERA_RATIOS.put(projection, factor);
        }
    }
}
