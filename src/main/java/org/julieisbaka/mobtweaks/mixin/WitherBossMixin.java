package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.julieisbaka.mobtweaks.WitherDashGoal;
import org.julieisbaka.mobtweaks.WitherBirthExplosionCalculator;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherBoss.class)
abstract class WitherBossMixin extends Monster {
    @Unique
    private boolean mobTweaks$reinforcementsSpawned;
    @Unique
    private WitherDashGoal mobTweaks$dashGoal;

    protected WitherBossMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mobTweaks$setSpawnHealth(EntityType<? extends WitherBoss> type, Level level, CallbackInfo ci) {
        double health = switch (level.getDifficulty()) {
            case NORMAL -> 450.0;
            case HARD -> 600.0;
            default -> 300.0;
        };
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        this.setHealth(this.getMaxHealth());
    }

    @ModifyArg(method = "customServerAiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;heal(F)V", ordinal = 0), index = 0)
    private float mobTweaks$scaleSpawnChargeHealing(float amount) {
        return amount * this.getMaxHealth() / 300.0F;
    }

    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void mobTweaks$birthExplosionDamage(ServerLevel level, Entity source, double x, double y, double z,
                                                float radius, boolean fire, Level.ExplosionInteraction interaction) {
        level.explode(source, null, new WitherBirthExplosionCalculator(source),
                new Vec3(x, y, z), radius, fire, interaction);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void mobTweaks$registerDash(CallbackInfo ci) {
        this.mobTweaks$dashGoal = new WitherDashGoal((WitherBoss) (Object) this);
        this.goalSelector.addGoal(1, this.mobTweaks$dashGoal);
    }

    @Inject(method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V", at = @At("TAIL"))
    private void mobTweaks$countMainHeadShots(LivingEntity target, float power, CallbackInfo ci) {
        this.mobTweaks$dashGoal.recordShot();
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            shift = At.Shift.AFTER))
    private void mobTweaks$overrideDashMovement(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.mobTweaks$dashGoal.applyMovement();
        }
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void mobTweaks$checkHalfHealth(ServerLevel level, CallbackInfo ci) {
        this.mobTweaks$enterHalfHealthPhase(level);
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void mobTweaks$checkDamagedHealth(ServerLevel level, DamageSource source, float damage,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            this.mobTweaks$enterHalfHealthPhase(level);
        }
    }

    @Unique
    private void mobTweaks$enterHalfHealthPhase(ServerLevel level) {
        WitherBoss wither = (WitherBoss) (Object) this;
        if (this.mobTweaks$reinforcementsSpawned || !this.isAlive()
                || wither.getInvulnerableTicks() > 0 || !wither.isPowered()) {
            return;
        }
        this.mobTweaks$reinforcementsSpawned = true;
        level.explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, Level.ExplosionInteraction.MOB);
        for (int i = 0; i < 4; i++) {
            WitherSkeleton skeleton = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
            if (skeleton == null) {
                throw new IllegalStateException("Mob Tweaks could not create wither reinforcements");
            }
            double angle = i * Math.PI / 2.0;
            skeleton.setPos(this.getX() + Math.cos(angle) * 4, this.getY(), this.getZ() + Math.sin(angle) * 4);
            skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(skeleton.blockPosition()), EntitySpawnReason.EVENT, null);
            skeleton.setTarget(this.getTarget());
            level.addFreshEntityWithPassengers(skeleton);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void mobTweaks$savePhase(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("MobTweaksReinforcementsSpawned", this.mobTweaks$reinforcementsSpawned);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void mobTweaks$loadPhase(ValueInput input, CallbackInfo ci) {
        this.mobTweaks$reinforcementsSpawned = input.getBooleanOr("MobTweaksReinforcementsSpawned", false);
    }
}
