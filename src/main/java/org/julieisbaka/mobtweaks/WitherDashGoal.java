package org.julieisbaka.mobtweaks;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class WitherDashGoal extends Goal {
    private final WitherBoss wither;
    private final Set<UUID> hitEntities = new HashSet<>();
    private int shots;
    private int windupTicks;
    private int dashTicks;
    private boolean active;
    private Vec3 direction = Vec3.ZERO;

    public WitherDashGoal(WitherBoss wither) {
        this.wither = wither;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    public void recordShot() {
        if (!this.active && this.wither.isPowered() && this.wither.getInvulnerableTicks() == 0
                && MobTweaksSettings.isEnabled(this.wither.level(), MobTweak.WITHER_DASH)) {
            this.shots = Math.min(4, this.shots + 1);
        }
    }

    @Override
    public boolean canUse() {
        if (!this.wither.isPowered()) {
            this.shots = 0;
        }
        LivingEntity target = this.wither.getTarget();
        return MobTweaksSettings.isEnabled(this.wither.level(), MobTweak.WITHER_DASH)
                && this.shots >= 4 && this.wither.isAlive() && this.wither.isPowered()
                && this.wither.getInvulnerableTicks() == 0 && target != null
                && target.isAlive() && this.wither.canAttack(target);
    }

    @Override
    public void start() {
        this.shots = 0;
        this.windupTicks = 10;
        this.dashTicks = 20;
        this.active = true;
        this.hitEntities.clear();
        this.wither.getNavigation().stop();
        this.wither.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.wither.getTarget();
        return MobTweaksSettings.isEnabled(this.wither.level(), MobTweak.WITHER_DASH)
                && this.active && this.dashTicks > 0 && this.wither.isAlive() && this.wither.isPowered()
                && this.wither.getInvulnerableTicks() == 0 && target != null && target.isAlive()
                && this.wither.canAttack(target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.wither.getTarget();
        if (target == null) {
            this.stop();
            return;
        }
        if (this.windupTicks > 0) {
            this.wither.getLookControl().setLookAt(target, 180.0F, 180.0F);
            if (--this.windupTicks == 0) {
                this.direction = target.getBoundingBox().getCenter()
                        .subtract(this.wither.getBoundingBox().getCenter()).normalize().scale(1.5);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.wither.level();
        this.breakBlocks(level);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                this.wither.getBoundingBox().expandTowards(this.direction).inflate(0.2))) {
            if (victim != this.wither && !victim.is(EntityTypeTags.WITHER_FRIENDS)
                    && this.wither.canAttack(victim) && this.wither.hasLineOfSight(victim)
                    && this.hitEntities.add(victim.getUUID())
                    && victim.hurtServer(level, this.wither.damageSources().mobAttack(this.wither), 30.0F)) {
                int effectTicks = switch (level.getDifficulty()) {
                    case NORMAL -> 200;
                    case HARD -> 800;
                    default -> 0;
                };
                if (effectTicks > 0) {
                    victim.addEffect(new MobEffectInstance(MobEffects.WITHER, effectTicks, 1), this.wither);
                }
                victim.knockback(1.0, -this.direction.x, -this.direction.z,
                        this.wither.damageSources().mobAttack(this.wither), 30.0F);
            }
        }
        this.dashTicks--;
        if (this.wither.horizontalCollision && this.dashTicks < 19 || this.dashTicks == 0) {
            this.stop();
        }
    }

    private void breakBlocks(ServerLevel level) {
        if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
        }
        BlockPos center = this.wither.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(3, 6, 3))) {
            BlockState state = level.getBlockState(pos);
            if (WitherBoss.canDestroy(state) && !state.is(Blocks.OBSIDIAN) && !state.is(Blocks.CRYING_OBSIDIAN)) {
                level.destroyBlock(pos, true, this.wither);
            }
        }
    }

    public void applyMovement() {
        if (this.active) {
            if (!this.canContinueToUse()) {
                this.stop();
                return;
            }
            this.wither.setDeltaMovement(this.windupTicks > 0 ? Vec3.ZERO : this.direction);
        }
    }

    @Override
    public void stop() {
        this.active = false;
        this.dashTicks = 0;
        this.direction = Vec3.ZERO;
        this.hitEntities.clear();
        this.wither.setDeltaMovement(Vec3.ZERO);
    }
}
