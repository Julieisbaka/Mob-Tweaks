package org.julieisbaka.mobtweaks.test.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Bat.class)
public interface BatAccessor {
    @Invoker("customServerAiStep")
    void mobTweaks$serverAiStep(ServerLevel level);
}
