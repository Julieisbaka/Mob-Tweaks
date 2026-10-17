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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
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
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive() || this.isNoAi() || this.isImmobile()
                || this.getTarget() == null || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
        }
        boolean crushEggs = MobTweaksSettings.isEnabled(level, MobTweak.RAVAGER_TURTLE_EGGS);
        boolean trampleBlocks = MobTweaksSettings.isEnabled(level, MobTweak.RAVAGER_TRAMPLING);
        if (!crushEggs && !trampleBlocks) {
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
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(path.minX), Mth.floor(path.minY), Mth.floor(path.minZ),
                Mth.floor(path.maxX), Mth.floor(path.maxY), Mth.floor(path.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (crushEggs && state.is(Blocks.TURTLE_EGG) && level.destroyBlock(pos, false, this)) {
                level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
            } else if (trampleBlocks && mobTweaks$isTrampleable(state)) {
                level.destroyBlock(pos, false, this);
            }
        }
    }

    @Unique
    private static boolean mobTweaks$isTrampleable(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.LEAVES) || block == Blocks.CHORUS_FLOWER || block == Blocks.CHORUS_PLANT
                || block == Blocks.AZALEA || block == Blocks.FLOWERING_AZALEA || block == Blocks.CARVED_PUMPKIN
                || block == Blocks.JACK_O_LANTERN || block == Blocks.MELON || block == Blocks.PUMPKIN;
    }
}
