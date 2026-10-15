package org.julieisbaka.mobtweaks;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SkeletonCoverGoal extends Goal {
    private static final int COVER_TICKS = 12;
    private static final int COVER_RANGE = 6;

    private final AbstractSkeleton skeleton;
    private int reloadUntil;
    private BlockPos coverPosition;

    public SkeletonCoverGoal(AbstractSkeleton skeleton) {
        this.skeleton = skeleton;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public void beginReload() {
        this.reloadUntil = this.skeleton.tickCount + COVER_TICKS;
        this.coverPosition = this.findCoverPosition();
    }

    @Override
    public boolean canUse() {
        return this.skeleton.level() instanceof ServerLevel
                && MobTweaksSettings.isEnabled(this.skeleton.level(), MobTweak.SKELETON_COVER)
                && this.skeleton.isAlive() && this.skeleton.getTarget() != null && this.skeleton.getTarget().isAlive()
                && this.skeleton.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW)
                && this.skeleton.tickCount < this.reloadUntil && this.coverPosition != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.moveIntoCover();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.skeleton.tickCount % 4 == 0) {
            this.moveIntoCover();
        }
        LivingEntity target = this.skeleton.getTarget();
        if (target != null) {
            this.skeleton.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        this.skeleton.getNavigation().stop();
        this.coverPosition = null;
    }

    private BlockPos findCoverPosition() {
        if (!(this.skeleton.level() instanceof ServerLevel level)) {
            return null;
        }
        LivingEntity target = this.skeleton.getTarget();
        if (target == null || !target.isAlive()) {
            return null;
        }
        if (!this.skeleton.hasLineOfSight(target)) {
            return this.skeleton.blockPosition();
        }

        BlockPos origin = this.skeleton.blockPosition();
        BlockPos bestPosition = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-COVER_RANGE, -1, -COVER_RANGE),
                origin.offset(COVER_RANGE, 1, COVER_RANGE))) {
            if (!level.isEmptyBlock(candidate) || !level.isEmptyBlock(candidate.above())
                    || level.getBlockState(candidate.below()).getCollisionShape(level, candidate.below()).isEmpty()) {
                continue;
            }

            Vec3 eyePosition = new Vec3(candidate.getX() + 0.5, candidate.getY() + this.skeleton.getEyeHeight(),
                    candidate.getZ() + 0.5);
            BlockHitResult sight = level.clip(new ClipContext(eyePosition, target.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.skeleton));
            if (sight.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            double distance = this.skeleton.distanceToSqr(Vec3.atBottomCenterOf(candidate));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPosition = candidate.immutable();
            }
        }
        return bestPosition;
    }

    private void moveIntoCover() {
        if (this.coverPosition != null) {
            boolean pathFound = this.skeleton.getNavigation().moveTo(this.coverPosition.getX() + 0.5,
                    this.coverPosition.getY(), this.coverPosition.getZ() + 0.5, 1.0);
            if (!pathFound) {
                this.skeleton.getMoveControl().setWantedPosition(this.coverPosition.getX() + 0.5,
                        this.coverPosition.getY(), this.coverPosition.getZ() + 0.5, 1.0);
            }
        }
    }
}
