package org.julieisbaka.mobtweaks;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ZombieJockeyGoal extends Goal {
    private static final TagKey<EntityType<?>> MOUNTS = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("mob_tweaks", "baby_zombie_mounts"));
    private final Zombie zombie;
    private boolean rolled;
    private boolean eligible;
    private Mob mount;
    private int searchTicks;
    private int approachTicks;

    public ZombieJockeyGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public void initialize(EntitySpawnReason reason) {
        if (!this.rolled && this.zombie.isBaby()
                && (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.REINFORCEMENT)) {
            this.rolled = true;
            this.eligible = this.zombie.getRandom().nextInt(100) < 15;
        }
    }

    public void save(ValueOutput output) {
        output.putBoolean("MobTweaksJockeyRolled", this.rolled);
        output.putBoolean("MobTweaksJockeyEligible", this.eligible);
    }

    public void load(ValueInput input) {
        this.rolled = input.getBooleanOr("MobTweaksJockeyRolled", false);
        this.eligible = this.rolled && input.getBooleanOr("MobTweaksJockeyEligible", false);
    }

    public boolean isEligibleMount(Mob candidate) {
        if (candidate == this.zombie || !candidate.isAlive() || candidate.isVehicle() || candidate.isPassenger()
                || candidate.isNoAi()) {
            return false;
        }
        if (candidate instanceof Zombie otherZombie) {
            return !otherZombie.isBaby();
        }
        if (!candidate.is(MOUNTS)) {
            return false;
        }
        if (candidate instanceof Wolf wolf && wolf.isTame()) {
            return false;
        }
        return !(candidate instanceof Cat cat) || !cat.isTame();
    }

    @Override
    public boolean canUse() {
        if (!this.eligible || !this.zombie.isBaby() || !this.zombie.isAlive()
                || this.zombie.isNoAi() || this.zombie.isPassenger()
                || !MobTweaksSettings.isEnabled(this.zombie.level(), MobTweak.BABY_ZOMBIE_JOCKEYS)) {
            return false;
        }
        if (this.searchTicks > 0) {
            this.searchTicks--;
            return false;
        }
        this.searchTicks = reducedTickDelay(20);
        this.mount = this.zombie.level().getEntitiesOfClass(Mob.class,
                        this.zombie.getBoundingBox().inflate(8.0, 4.0, 8.0), this::isEligibleMount).stream()
                .filter(this.zombie::hasLineOfSight)
                .min(Comparator.comparingDouble(this.zombie::distanceToSqr)).orElse(null);
        return this.mount != null;
    }

    @Override
    public void start() {
        this.approachTicks = 100;
        if (this.mount != null && this.zombie.distanceToSqr(this.mount) > 4.0) {
            this.zombie.getNavigation().moveTo(this.mount, 1.0);
        }
        this.tick();
    }

    @Override
    public boolean canContinueToUse() {
        return this.approachTicks > 0 && this.zombie.isAlive() && this.zombie.isBaby()
                && MobTweaksSettings.isEnabled(this.zombie.level(), MobTweak.BABY_ZOMBIE_JOCKEYS)
                && !this.zombie.isPassenger() && this.mount != null && this.isEligibleMount(this.mount)
                && this.zombie.distanceToSqr(this.mount) <= 144.0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.approachTicks--;
        if (this.mount == null || !this.isEligibleMount(this.mount)) {
            return;
        }
        if (this.zombie.distanceToSqr(this.mount) <= 4.0 && this.zombie.hasLineOfSight(this.mount)) {
            this.zombie.startRiding(this.mount, false, true);
        } else if (this.approachTicks % 10 == 0) {
            this.zombie.getNavigation().moveTo(this.mount, 1.0);
        }
    }

    @Override
    public void stop() {
        this.zombie.getNavigation().stop();
        this.mount = null;
        this.approachTicks = 0;
    }
}
