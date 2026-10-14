package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.julieisbaka.mobtweaks.SnowGolemTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCubeMob.class)
abstract class CubeMobMixin extends Mob {
    protected CubeMobMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Shadow
    protected abstract boolean canDealDamage();

    @Shadow
    protected abstract void dealDamage(LivingEntity target);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mobTweaks$targetSnowGolems(EntityType<? extends AbstractCubeMob> type, Level level, CallbackInfo ci) {
        if ((Object) this instanceof Slime || (Object) this instanceof MagmaCube) {
            this.targetSelector.addGoal(3, new SnowGolemTargetGoal(this));
        }
    }

    @Inject(method = "push", at = @At("TAIL"))
    private void mobTweaks$damageSnowGolem(Entity entity, CallbackInfo ci) {
        if (((Object) this instanceof Slime || (Object) this instanceof MagmaCube)
                && entity instanceof SnowGolem snowGolem && this.canDealDamage()
                && MobTweaksSettings.isEnabled(this.level(), MobTweak.CUBES_ATTACK_SNOW_GOLEMS)) {
            this.dealDamage(snowGolem);
        }
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void mobTweaks$fitSpawnSpace(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!((Object) this instanceof Slime || (Object) this instanceof MagmaCube)
                || reason == EntitySpawnReason.CONVERSION || reason == EntitySpawnReason.LOAD
                || reason == EntitySpawnReason.DIMENSION_TRAVEL
                || !MobTweaksSettings.isEnabled(level, MobTweak.CUBES_SMALL_SPAWNING)) {
            return;
        }
        AbstractCubeMob cube = (AbstractCubeMob) (Object) this;
        if (!level.noCollision(cube, cube.getBoundingBox())) {
            this.setPos(Mth.floor(this.getX()) + 1.0, this.getY(), Mth.floor(this.getZ()) + 1.0);
            while (cube.getSize() > 1 && !level.noCollision(cube, cube.getBoundingBox())) {
                cube.setSize(cube.getSize() / 2, true);
            }
        }
    }
}
