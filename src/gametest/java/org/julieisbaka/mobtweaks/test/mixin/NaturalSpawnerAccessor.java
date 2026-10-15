package org.julieisbaka.mobtweaks.test.mixin;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NaturalSpawner.class)
public interface NaturalSpawnerAccessor {
    @Invoker("getRandomSpawnMobAt")
    static Optional<MobSpawnSettings.SpawnerData> mobTweaks$selectSpawn(ServerLevel level, StructureManager structures,
            ChunkGenerator generator, MobCategory category, RandomSource random, BlockPos pos) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
