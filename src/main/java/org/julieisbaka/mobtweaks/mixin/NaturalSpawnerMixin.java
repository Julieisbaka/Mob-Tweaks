package org.julieisbaka.mobtweaks.mixin;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import org.julieisbaka.mobtweaks.CubeSpawnClearance;
import org.julieisbaka.mobtweaks.BatColonySpawning;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @Inject(method = "getRandomSpawnMobAt", at = @At("RETURN"), cancellable = true)
    private static void mobTweaks$reduceBatGroupFrequency(ServerLevel level, StructureManager structures,
            ChunkGenerator generator, MobCategory category, RandomSource random, BlockPos pos,
            CallbackInfoReturnable<Optional<MobSpawnSettings.SpawnerData>> cir) {
        Optional<MobSpawnSettings.SpawnerData> selected = cir.getReturnValue();
        if (selected.isPresent() && selected.get().type() == EntityTypes.BAT
                && MobTweaksSettings.isEnabled(level, MobTweak.BAT_COLONY_SPAWNING)
                && random.nextInt(BatColonySpawning.SELECTION_INTERVAL) != 0) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Redirect(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;count()Lnet/minecraft/util/valueproviders/IntProvider;"))
    private static IntProvider mobTweaks$allowColonyPlacementRetries(MobSpawnSettings.SpawnerData data,
            MobCategory category, ServerLevel level, ChunkAccess chunk, BlockPos start,
            NaturalSpawner.SpawnPredicate predicate, NaturalSpawner.AfterSpawnCallback callback) {
        // The bat's shared colony size ends the cluster; this only budgets
        // valid-placement attempts.
        return data.type() == EntityTypes.BAT && MobTweaksSettings.isEnabled(level, MobTweak.BAT_COLONY_SPAWNING)
                ? ConstantInt.of(BatColonySpawning.MAX_PLACEMENT_ATTEMPTS)
                : data.count();
    }

    @Redirect(method = "isValidSpawnPostitionForType", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB mobTweaks$checkSpawnPositionBox(EntityType<?> type, double x, double y, double z,
            ServerLevel level, MobCategory category, StructureManager structures, ChunkGenerator generator,
            MobSpawnSettings.SpawnerData spawnData, BlockPos.MutableBlockPos pos, double distance) {
        return CubeSpawnClearance.getSpawnBox(type, level, x, y, z);
    }

    @Redirect(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;getSpawnAABB(DDD)Lnet/minecraft/world/phys/AABB;"))
    private static AABB mobTweaks$checkChunkGenerationSpawnBox(EntityType<?> type, double x, double y, double z,
            ServerLevelAccessor level, BlockPos pos, ChunkPos chunkPos, RandomSource random) {
        return CubeSpawnClearance.getSpawnBox(type, level.getLevel(), x, y, z);
    }
}
