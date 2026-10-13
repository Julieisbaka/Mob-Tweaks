package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.julieisbaka.mobtweaks.ZombieJockeyGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
abstract class ZombieMixin extends Monster {
    @Unique
    private ZombieJockeyGoal mobTweaks$jockeyGoal;

    protected ZombieMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void mobTweaks$registerJockeyGoal(EntityType<? extends Zombie> type, Level level, CallbackInfo ci) {
        this.mobTweaks$jockeyGoal = new ZombieJockeyGoal((Zombie) (Object) this);
        this.goalSelector.addGoal(1, this.mobTweaks$jockeyGoal);
    }

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void mobTweaks$rollJockey(ServerLevelAccessor level, DifficultyInstance difficulty,
                                     EntitySpawnReason reason, SpawnGroupData groupData,
                                     CallbackInfoReturnable<SpawnGroupData> cir) {
        this.mobTweaks$jockeyGoal.initialize(reason);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void mobTweaks$saveJockey(ValueOutput output, CallbackInfo ci) {
        this.mobTweaks$jockeyGoal.save(output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void mobTweaks$loadJockey(ValueInput input, CallbackInfo ci) {
        this.mobTweaks$jockeyGoal.load(input);
    }
}
