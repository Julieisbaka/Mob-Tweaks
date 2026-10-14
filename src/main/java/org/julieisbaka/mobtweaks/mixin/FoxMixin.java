package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.world.entity.animal.fox.Fox;
import org.julieisbaka.mobtweaks.FoxBerryDeliveryGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fox.class)
abstract class FoxMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void mobTweaks$addBerryDeliveryGoal(CallbackInfo ci) {
        Fox fox = (Fox) (Object) this;
        fox.getGoalSelector().addGoal(6, new FoxBerryDeliveryGoal(fox));
    }
}
