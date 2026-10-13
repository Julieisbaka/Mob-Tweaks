package org.julieisbaka.mobtweaks.test.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OfferFlowerGoal.class)
public interface OfferFlowerGoalAccessor {
    @Accessor("entity")
    void mobTweaks$setRecipient(LivingEntity entity);

    @Accessor("tick")
    void mobTweaks$setTick(int tick);
}
