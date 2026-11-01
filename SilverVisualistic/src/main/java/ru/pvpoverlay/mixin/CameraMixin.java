package ru.pvpoverlay.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.pvpoverlay.FreeLookController;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void pvpoverlay$orbitCamera(net.minecraft.world.BlockView area, Entity focusedEntity,
                                        boolean thirdPerson, boolean inverseView, float tickDelta,
                                        CallbackInfo ci) {
        if (!FreeLookController.isActive() || !(focusedEntity instanceof ClientPlayerEntity player)
                || player.getWorld() == null) return;

        Vec3d eye = player.getCameraPosVec(tickDelta);
        Vec3d look = player.getRotationVec(tickDelta).normalize();
        Vec3d desired = eye.subtract(look.multiply(FreeLookController.getDistance()))
                .add(0.0, 0.12, 0.0);

        // Keep the camera on the visible side of blocks: no wall clipping or x-ray view.
        HitResult hit = player.getWorld().raycast(new RaycastContext(
                eye, desired, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, player));
        Vec3d cameraPos = hit.getType() == HitResult.Type.MISS
                ? desired : hit.getPos().subtract(look.multiply(0.12));

        setPos(cameraPos.x, cameraPos.y, cameraPos.z);
        setRotation(player.getYaw(tickDelta), player.getPitch(tickDelta));
    }
}
