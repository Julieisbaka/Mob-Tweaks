package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import org.julieisbaka.mobtweaks.CubeSpawnClearance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnUtil.class)
abstract class SpawnUtilMixin {
    @Redirect(method = "trySpawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB mobTweaks$checkTrySpawnBox(EntityType<?> checkedType, double x, double y, double z,
            EntityType<?> type, EntitySpawnReason reason, ServerLevel level, BlockPos pos, int horizontalRange,
            int verticalRange, int attempts, SpawnUtil.Strategy strategy, boolean finalizeSpawn) {
        return CubeSpawnClearance.getSpawnBox(checkedType, level, x, y, z);
    }
}
