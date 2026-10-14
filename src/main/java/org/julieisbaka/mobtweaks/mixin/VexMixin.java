package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.world.entity.monster.Vex;
import org.julieisbaka.mobtweaks.VexRecallGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Vex.class)
abstract class VexMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void mobTweaks$addRecallGoal(CallbackInfo ci) {
        Vex vex = (Vex) (Object) this;
        vex.getGoalSelector().addGoal(1, new VexRecallGoal(vex));
    }
}
