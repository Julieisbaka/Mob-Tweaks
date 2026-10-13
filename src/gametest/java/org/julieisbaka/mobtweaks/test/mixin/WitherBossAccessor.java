package org.julieisbaka.mobtweaks.test.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.julieisbaka.mobtweaks.WitherDashGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WitherBoss.class)
public interface WitherBossAccessor {
    @Invoker("customServerAiStep")
    void mobTweaks$serverAiStep(ServerLevel level);

    @Accessor("mobTweaks$dashGoal")
    WitherDashGoal mobTweaks$getDashGoal();
}
