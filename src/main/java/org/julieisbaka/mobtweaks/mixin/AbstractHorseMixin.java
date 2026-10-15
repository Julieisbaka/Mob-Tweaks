package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
abstract class AbstractHorseMixin {
    @Unique
    private int mobTweaks$nextRearWarningTick;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void mobTweaks$warnRiderAboutRearThreat(CallbackInfo ci) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (!(horse.level() instanceof ServerLevel level)
                || !MobTweaksSettings.isEnabled(level, MobTweak.HORSE_REAR_WARNING)
                || !(horse.getControllingPassenger() instanceof Player) || horse.isStanding()
                || horse.tickCount < this.mobTweaks$nextRearWarningTick) {
            return;
        }

        Vec3 forward = horse.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        if (forward.lengthSqr() < 0.5) {
            return;
        }
        boolean threatBehind = level
                .getEntitiesOfClass(Monster.class, horse.getBoundingBox().inflate(8.0), Monster::isAlive).stream()
                .anyMatch(monster -> {
                    Vec3 towardThreat = monster.position().subtract(horse.position()).multiply(1.0, 0.0, 1.0)
                            .normalize();
                    return forward.dot(towardThreat) < -0.5;
                });
        if (threatBehind) {
            horse.setStanding(20);
            horse.playSound(horse.getAmbientStandSound(), 0.8F, 1.0F);
            this.mobTweaks$nextRearWarningTick = horse.tickCount + 60;
        }
    }
}
