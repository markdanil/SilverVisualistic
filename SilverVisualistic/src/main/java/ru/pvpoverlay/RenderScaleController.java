package ru.pvpoverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11;
import ru.pvpoverlay.mixin.MinecraftClientFramebufferAccessor;

/** Renders the 3D world into a smaller framebuffer, then scales it to the native window size. */
public final class RenderScaleController {
    private static SimpleFramebuffer scaledTarget;
    private static Framebuffer nativeTarget;
    private static boolean active;
    private static int scaledWidth;
    private static int scaledHeight;

    private RenderScaleController() { }

    public static void begin(MinecraftClient client) {
        if (active || client == null || client.world == null || !PvPOverlayConfig.optimizationEnabled
                || !PvPOverlayConfig.optimizationRenderScaleEnabled
                || PvPOverlayConfig.optimizationRenderScalePercent >= 100) return;

        int percent = Math.max(50, Math.min(100, PvPOverlayConfig.optimizationRenderScalePercent));
        int width = Math.max(320, client.getWindow().getFramebufferWidth() * percent / 100);
        int height = Math.max(240, client.getWindow().getFramebufferHeight() * percent / 100);
        if (scaledTarget == null || width != scaledWidth || height != scaledHeight) {
            if (scaledTarget != null) scaledTarget.delete();
            scaledTarget = new SimpleFramebuffer(width, height, true);
            scaledTarget.setTexFilter(GL11.GL_LINEAR);
            scaledWidth = width;
            scaledHeight = height;
        }

        nativeTarget = client.getFramebuffer();
        scaledTarget.clear();
        scaledTarget.beginWrite(true);
        ((MinecraftClientFramebufferAccessor) client).silvervisualistic$setFramebuffer(scaledTarget);
        active = true;
    }

    public static void end(MinecraftClient client) {
        if (!active || client == null || nativeTarget == null || scaledTarget == null) return;
        try {
            scaledTarget.endWrite();
            ((MinecraftClientFramebufferAccessor) client).silvervisualistic$setFramebuffer(nativeTarget);
            nativeTarget.beginWrite(true);
            scaledTarget.draw(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
            nativeTarget.beginWrite(true);
        } finally {
            ((MinecraftClientFramebufferAccessor) client).silvervisualistic$setFramebuffer(nativeTarget);
            active = false;
            nativeTarget = null;
        }
    }
}
