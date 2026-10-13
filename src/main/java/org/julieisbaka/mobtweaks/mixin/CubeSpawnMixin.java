package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
abstract class CubeSpawnMixin {
    @Inject(method = "getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;", at = @At("HEAD"), cancellable = true)
    private void mobTweaks$smallerSpawnSpace(double x, double y, double z, CallbackInfoReturnable<AABB> cir) {
        if ((Object) this == EntityTypes.SLIME || (Object) this == EntityTypes.MAGMA_CUBE) {
            // Align the two-block footprint with block boundaries, not a block's center.
            double minX = Mth.floor(x);
            double minZ = Mth.floor(z);
            cir.setReturnValue(new AABB(minX, y, minZ, minX + 2, y + 2, minZ + 2));
        }
    }
}
