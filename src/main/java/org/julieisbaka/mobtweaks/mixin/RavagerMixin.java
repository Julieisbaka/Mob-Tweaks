package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ravager.class)
abstract class RavagerMixin extends Raider {
    @Unique
    private AABB mobTweaks$beforeMovement;

    protected RavagerMixin(EntityType<? extends Raider> type, Level level) {
        super(type, level);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void mobTweaks$rememberMovement(CallbackInfo ci) {
        this.mobTweaks$beforeMovement = this.getBoundingBox();
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void mobTweaks$crushEggs(CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive() || this.isNoAi()
                || this.isImmobile() || this.getTarget() == null
                || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
        }
        AABB before = this.mobTweaks$beforeMovement;
        AABB after = this.getBoundingBox();
        double dx = after.minX - before.minX;
        double dz = after.minZ - before.minZ;
        if (dx * dx + dz * dz < 1.0E-6) {
            return;
        }
        AABB path = before.minmax(after).inflate(0.0, 0.25, 0.0);
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(path.minX), Mth.floor(path.minY), Mth.floor(path.minZ),
                Mth.floor(path.maxX), Mth.floor(path.maxY), Mth.floor(path.maxZ))) {
            if (level.getBlockState(pos).is(Blocks.TURTLE_EGG) && level.destroyBlock(pos, false, this)) {
                level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        }
    }
}
