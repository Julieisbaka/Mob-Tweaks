package org.julieisbaka.mobtweaks;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.SnowGolem;

public final class SnowGolemTargetGoal extends NearestAttackableTargetGoal<SnowGolem> {
    private final Mob cube;

    public SnowGolemTargetGoal(Mob cube) {
        super(cube, SnowGolem.class, true);
        this.cube = cube;
    }

    @Override
    public boolean canUse() {
        return MobTweaksSettings.isEnabled(this.cube.level(), MobTweak.CUBES_ATTACK_SNOW_GOLEMS)
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return MobTweaksSettings.isEnabled(this.cube.level(), MobTweak.CUBES_ATTACK_SNOW_GOLEMS)
                && super.canContinueToUse();
    }
}
