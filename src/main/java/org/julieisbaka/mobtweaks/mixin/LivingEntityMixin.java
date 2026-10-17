package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ravager;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void mobTweaks$ignorePlantDamage(ServerLevel level, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Ravager
                && (source.is(DamageTypes.SWEET_BERRY_BUSH) || source.is(DamageTypes.CACTUS))
                && MobTweaksSettings.isEnabled(level, MobTweak.RAVAGER_PLANT_DAMAGE_IMMUNITY)) {
            cir.setReturnValue(false);
        }
    }
}
