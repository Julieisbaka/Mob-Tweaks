package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.world.entity.animal.golem.IronGolem;
import org.julieisbaka.mobtweaks.IronGolemProjectileGuardGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolem.class)
abstract class IronGolemMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void mobTweaks$addProjectileGuardGoal(CallbackInfo ci) {
        IronGolem golem = (IronGolem) (Object) this;
        golem.getGoalSelector().addGoal(1, new IronGolemProjectileGuardGoal(golem));
    }
}
