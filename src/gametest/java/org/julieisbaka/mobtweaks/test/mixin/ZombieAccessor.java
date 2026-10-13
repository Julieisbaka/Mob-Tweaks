package org.julieisbaka.mobtweaks.test.mixin;

import net.minecraft.world.entity.monster.zombie.Zombie;
import org.julieisbaka.mobtweaks.ZombieJockeyGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Zombie.class)
public interface ZombieAccessor {
    @Accessor("mobTweaks$jockeyGoal")
    ZombieJockeyGoal mobTweaks$getJockeyGoal();
}
