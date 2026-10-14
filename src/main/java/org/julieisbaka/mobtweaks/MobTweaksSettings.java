package org.julieisbaka.mobtweaks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class MobTweaksSettings {
    private MobTweaksSettings() {
    }

    public static boolean isEnabled(Level level, MobTweak tweak) {
        if (!MobTweaksConfig.isEnabled(tweak)) {
            return false;
        }
        return !(level instanceof ServerLevel serverLevel)
                || serverLevel.getGameRules().get(MobTweaksGameRules.get(tweak));
    }

    public static boolean isEnabled(ServerLevel level, MobTweak tweak) {
        return MobTweaksConfig.isEnabled(tweak) && level.getGameRules().get(MobTweaksGameRules.get(tweak));
    }

    public static boolean isEnabled(ServerLevelAccessor level, MobTweak tweak) {
        return isEnabled(level.getLevel(), tweak);
    }
}
