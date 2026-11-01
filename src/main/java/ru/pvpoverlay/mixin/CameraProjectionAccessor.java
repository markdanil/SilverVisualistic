package ru.pvpoverlay.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.Projection.class)
public interface CameraProjectionAccessor {
    @Accessor("x") Vec3d silvervisualistic$getX();
    @Mutable @Accessor("x") void silvervisualistic$setX(Vec3d value);
}
