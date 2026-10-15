package org.julieisbaka.mobtweaks;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.SpawnGroupData;

public final class BatColonySpawning {
    public static final int MIN_GROUP_SIZE = 3;
    public static final int MAX_GROUP_SIZE = 6;
    public static final int MAX_PLACEMENT_ATTEMPTS = 48;
    public static final int SELECTION_INTERVAL = 3;

    private BatColonySpawning() {
    }

    public record Colony(int size) implements SpawnGroupData {
        public Colony(RandomSource random) {
            this(MIN_GROUP_SIZE + random.nextInt(MAX_GROUP_SIZE - MIN_GROUP_SIZE + 1));
        }
    }
}
