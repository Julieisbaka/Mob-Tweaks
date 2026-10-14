package org.julieisbaka.mobtweaks;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class IronGolemProjectileGuardGoal extends Goal {
    private static final double THREAT_RANGE = 16.0;
    private static final int GUARD_TICKS = 20;

    private final IronGolem golem;
    private Villager villager;
    private Projectile projectile;
    private Vec3 guardPosition;
    private int guardUntil;

    public IronGolemProjectileGuardGoal(IronGolem golem) {
        this.golem = golem;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(this.golem.level() instanceof ServerLevel level)
                || !MobTweaksSettings.isEnabled(level, MobTweak.IRON_GOLEM_PROJECTILE_GUARD) || !this.golem.isAlive()
                || this.golem.isNoAi()) {
            return false;
        }

        AABB searchArea = this.golem.getBoundingBox().inflate(THREAT_RANGE);
        var villagers = level.getEntitiesOfClass(Villager.class, searchArea, Villager::isAlive);
        var projectiles = level.getEntitiesOfClass(Projectile.class, searchArea,
                candidate -> candidate.isAlive() && candidate.getOwner() instanceof Monster);

        double closestThreat = Double.MAX_VALUE;
        for (Projectile candidate : projectiles) {
            Vec3 velocity = candidate.getDeltaMovement();
            if (velocity.lengthSqr() < 1.0E-4) {
                continue;
            }

            Vec3 start = candidate.position();
            Vec3 end = start.add(velocity.normalize().scale(THREAT_RANGE));
            for (Villager protectedVillager : villagers) {
                var impact = protectedVillager.getBoundingBox().inflate(0.2).clip(start, end);
                if (impact.isEmpty()) {
                    continue;
                }

                double distance = candidate.distanceToSqr(this.golem);
                if (distance < closestThreat) {
                    closestThreat = distance;
                    this.villager = protectedVillager;
                    this.projectile = candidate;
                    Vec3 towardShooter = start.subtract(protectedVillager.position()).normalize();
                    double separation = this.golem.getBbWidth() / 2.0 + protectedVillager.getBbWidth() / 2.0 + 0.15;
                    this.guardPosition = protectedVillager.position().add(towardShooter.scale(separation));
                }
            }
        }

        return this.villager != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.golem.isAlive()
                && MobTweaksSettings.isEnabled(this.golem.level(), MobTweak.IRON_GOLEM_PROJECTILE_GUARD)
                && this.villager != null && this.villager.isAlive() && this.golem.tickCount < this.guardUntil;
    }

    @Override
    public void start() {
        this.guardUntil = this.golem.tickCount + GUARD_TICKS;
        this.moveBetweenThreatAndVillager();
    }

    @Override
    public void tick() {
        if (this.projectile != null && this.projectile.isAlive() && this.villager.isAlive()) {
            Vec3 towardShooter = this.projectile.position().subtract(this.villager.position()).normalize();
            double separation = this.golem.getBbWidth() / 2.0 + this.villager.getBbWidth() / 2.0 + 0.15;
            this.guardPosition = this.villager.position().add(towardShooter.scale(separation));
        }
        this.moveBetweenThreatAndVillager();
    }

    @Override
    public void stop() {
        this.golem.getMoveControl().setWait();
        this.villager = null;
        this.projectile = null;
        this.guardPosition = null;
    }

    private void moveBetweenThreatAndVillager() {
        if (this.guardPosition != null) {
            this.golem.getMoveControl().setWantedPosition(this.guardPosition.x, this.guardPosition.y,
                    this.guardPosition.z, 1.0);
            this.golem.getLookControl().setLookAt(this.villager, 30.0F, 30.0F);
        }
    }
}
