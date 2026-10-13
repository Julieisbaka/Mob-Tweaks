package org.julieisbaka.mobtweaks.test.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Raid.class)
public interface RaidAccessor {
    @Accessor("groupsSpawned")
    void mobTweaks$setGroupsSpawned(int groupsSpawned);

    @Invoker("spawnGroup")
    void mobTweaks$spawnGroup(ServerLevel level, BlockPos pos);
}
