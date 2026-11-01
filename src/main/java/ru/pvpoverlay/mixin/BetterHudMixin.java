package ru.pvpoverlay.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.pvpoverlay.BetterMinecraftController;
import ru.pvpoverlay.PvPOverlayConfig;

import java.util.function.Function;

@Mixin(InGameHud.class)
public abstract class BetterHudMixin {
    @Shadow @Final private static Identifier HOTBAR_TEXTURE;
    @Shadow @Final private static Identifier HOTBAR_SELECTION_TEXTURE;

    @Redirect(
            method = "renderHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"),
            require = 0
    )
    private void silvervisualistic$drawRoundedHotbar(DrawContext context,
                                                        Function<Identifier, RenderLayer> renderLayers,
                                                        Identifier sprite, int x, int y, int width, int height) {
        if (BetterMinecraftController.isEnabled() && HOTBAR_TEXTURE.equals(sprite)) {
            int opacity = Math.max(0, Math.min(100, PvPOverlayConfig.betterHotbarOpacity));
            if (BetterMinecraftController.useRoundedHotbar()) {
                int radius = Math.max(0, Math.min(PvPOverlayConfig.betterHotbarCornerRadius, Math.min(width, height) / 2));
                int bg = withOpacity(PvPOverlayConfig.betterHotbarColor, opacity);
                int border = withOpacity(PvPOverlayConfig.betterHotbarHighlightColor, Math.min(70, opacity));
                rounded(context, x, y, width, height, radius, border);
                rounded(context, x + 1, y + 1, width - 2, height - 2, Math.max(0, radius - 1), bg);
            } else {
                // The user may keep the vanilla hotbar plate but still use the separators.
                context.drawGuiTexture(renderLayers, sprite, x, y, width, height);
            }
            if (PvPOverlayConfig.betterHotbarSeparatorsEnabled) {
                // Nine 20px slots inside the vanilla 182px hotbar texture.
                // Subtle 1px separators sit behind item icons and the selected-slot plate.
                int separator = withOpacity(PvPOverlayConfig.betterHotbarHighlightColor, Math.min(35, opacity / 3));
                for (int slot = 1; slot < 9; slot++) {
                    int separatorX = x + slot * 20;
                    context.fill(separatorX, y + 5, separatorX + 1, y + height - 5, separator);
                }
            }
            return;
        }

        if (HOTBAR_SELECTION_TEXTURE.equals(sprite) && BetterMinecraftController.useRoundedHotbar()) {
            int animatedX = BetterMinecraftController.animateHotbarSelectionX(x);
            int radius = Math.max(0, Math.min(PvPOverlayConfig.betterHotbarCornerRadius - 1, Math.min(width, height) / 2));
            int accent = PvPOverlayConfig.betterHotbarHighlightColor & 0xFFFFFF;
            rounded(context, animatedX, y, width, height, radius, 0xAA000000 | accent);
            rounded(context, animatedX + 1, y + 1, width - 2, height - 2, Math.max(0, radius - 1), 0x55000000 | (accent & 0x00FFFFFF));
            return;
        }

        if (HOTBAR_SELECTION_TEXTURE.equals(sprite)) {
            x = BetterMinecraftController.animateHotbarSelectionX(x);
        }
        context.drawGuiTexture(renderLayers, sprite, x, y, width, height);
    }

    private static int withOpacity(int rgb, int opacityPercent) {
        int alpha = Math.max(0, Math.min(255, Math.round(opacityPercent * 2.55f)));
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static void rounded(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        int rr = r * r;
        for (int row = 0; row < height; row++) {
            int dy = row < r ? r - row - 1 : (row >= height - r ? row - (height - r) : 0);
            int inset = dy == 0 ? 0 : r - (int) Math.floor(Math.sqrt(Math.max(0, rr - dy * dy)));
            context.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }
}
