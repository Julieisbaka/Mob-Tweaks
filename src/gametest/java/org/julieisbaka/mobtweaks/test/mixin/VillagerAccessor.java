package org.julieisbaka.mobtweaks.test.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerAccessor {
    @Invoker("customServerAiStep")
    void mobTweaks$serverAiStep(ServerLevel level);
}
