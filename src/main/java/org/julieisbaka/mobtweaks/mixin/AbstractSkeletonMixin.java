package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.julieisbaka.mobtweaks.SkeletonCoverGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
abstract class AbstractSkeletonMixin extends Monster {
    @Unique
    private SkeletonCoverGoal mobTweaks$coverGoal;

    protected AbstractSkeletonMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void mobTweaks$registerCoverGoal(CallbackInfo ci) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        this.mobTweaks$coverGoal = new SkeletonCoverGoal(skeleton);
        skeleton.getGoalSelector().addGoal(2, this.mobTweaks$coverGoal);
    }

    @Inject(method = "performRangedAttack", at = @At("TAIL"))
    private void mobTweaks$beginBowReloadCover(LivingEntity target, float velocity, CallbackInfo ci) {
        if (this.mobTweaks$coverGoal != null
                && this.level() instanceof ServerLevel level
                && MobTweaksSettings.isEnabled(level, MobTweak.SKELETON_COVER)) {
            this.mobTweaks$coverGoal.beginReload();
        }
    }
}
