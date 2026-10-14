package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.AABB;
import org.julieisbaka.mobtweaks.CubeSpawnClearance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @Redirect(method = "isValidSpawnPostitionForType", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB mobTweaks$checkSpawnPositionBox(
            EntityType<?> type, double x, double y, double z, ServerLevel level, MobCategory category,
            StructureManager structures, ChunkGenerator generator, MobSpawnSettings.SpawnerData spawnData,
            BlockPos.MutableBlockPos pos, double distance) {
        return CubeSpawnClearance.getSpawnBox(type, level, x, y, z);
    }

    @Redirect(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB mobTweaks$checkChunkGenerationSpawnBox(
            EntityType<?> type, double x, double y, double z, ServerLevelAccessor level,
            BlockPos pos, ChunkPos chunkPos, RandomSource random) {
        return CubeSpawnClearance.getSpawnBox(type, level.getLevel(), x, y, z);
    }
}
