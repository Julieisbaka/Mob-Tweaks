package org.julieisbaka.mobtweaks;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Vex;

public class VexRecallGoal extends Goal {
    private static final double RECALL_DISTANCE_SQUARED = 32.0 * 32.0;
    private static final double ARRIVAL_DISTANCE_SQUARED = 3.0 * 3.0;

    private final Vex vex;
    private LivingEntity owner;

    public VexRecallGoal(Vex vex) {
        this.vex = vex;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.owner = this.vex.getOwner();
        if (this.owner == null || !this.owner.isAlive() || !this.vex.isAlive()
                || !MobTweaksSettings.isEnabled(this.vex.level(), MobTweak.VEX_RECALL)) {
            return false;
        }

        double distance = this.vex.distanceToSqr(this.owner);
        return distance > RECALL_DISTANCE_SQUARED
                || (this.vex.getTarget() == null && distance > ARRIVAL_DISTANCE_SQUARED);
    }

    @Override
    public boolean canContinueToUse() {
        return this.owner != null && this.owner.isAlive() && this.vex.isAlive()
                && MobTweaksSettings.isEnabled(this.vex.level(), MobTweak.VEX_RECALL)
                && this.vex.distanceToSqr(this.owner) > ARRIVAL_DISTANCE_SQUARED;
    }

    @Override
    public void start() {
        if (this.vex.distanceToSqr(this.owner) > RECALL_DISTANCE_SQUARED) {
            this.vex.setTarget(null);
            this.vex.setIsCharging(false);
        }
        this.moveTowardOwner();
    }

    @Override
    public void tick() {
        if (this.vex.distanceToSqr(this.owner) > RECALL_DISTANCE_SQUARED) {
            this.vex.setTarget(null);
            this.vex.setIsCharging(false);
        }
        this.moveTowardOwner();
    }

    @Override
    public void stop() {
        this.vex.getMoveControl().setWait();
    }

    private void moveTowardOwner() {
        this.vex.getMoveControl().setWantedPosition(this.owner.getX(), this.owner.getY(), this.owner.getZ(), 1.0);
        this.vex.getLookControl().setLookAt(this.owner, 30.0F, 30.0F);
    }
}
