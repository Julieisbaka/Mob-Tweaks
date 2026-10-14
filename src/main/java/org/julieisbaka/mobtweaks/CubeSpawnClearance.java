package org.julieisbaka.mobtweaks;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public final class CubeSpawnClearance {
    private CubeSpawnClearance() {
    }

    public static AABB getSpawnBox(EntityType<?> type, ServerLevel level, double x, double y, double z) {
        if (MobTweaksSettings.isEnabled(level, MobTweak.CUBES_SMALL_SPAWNING)
                && (type == EntityTypes.SLIME || type == EntityTypes.MAGMA_CUBE)) {
            double minX = Mth.floor(x);
            double minZ = Mth.floor(z);
            return new AABB(minX, y, minZ, minX + 2.0, y + 2.0, minZ + 2.0);
        }
        return type.getSpawnAABB(x, y, z);
    }
}
