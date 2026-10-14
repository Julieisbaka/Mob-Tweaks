package org.julieisbaka.mobtweaks.test.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Enderman.class)
public interface EndermanAccessor {
    @Invoker("customServerAiStep")
    void mobTweaks$serverAiStep(ServerLevel level);
}
