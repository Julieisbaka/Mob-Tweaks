package org.julieisbaka.mobtweaks;

import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

public final class BatDisturbance implements GameEventListener {
    private static final int RANGE = 12;
    private static final int MIN_FLEE_TICKS = 55;
    private static final int MAX_FLEE_TICKS = 65;

    private final Bat bat;
    private final PositionSource positionSource;
    private Vec3 disturbancePosition = Vec3.ZERO;
    private int fleeTicks;

    public BatDisturbance(Bat bat) {
        this.bat = bat;
        this.positionSource = new EntityPositionSource(bat, 0.0F);
    }

    @Override
    public PositionSource getListenerSource() {
        return this.positionSource;
    }

    @Override
    public int getListenerRadius() {
        return RANGE;
    }

    @Override
    public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context,
            Vec3 sourcePosition) {
        if ((!event.is(GameEvent.BLOCK_DESTROY.key()) && !event.is(GameEvent.EXPLODE.key()))
                || !this.bat.isAlive() || this.bat.isNoAi()
                || (!this.bat.isResting() && this.fleeTicks == 0)
                || !MobTweaksSettings.isEnabled(level, MobTweak.BAT_DISTURBANCE_FLEE)
                || this.bat.position().distanceToSqr(sourcePosition) > RANGE * RANGE) {
            return false;
        }

        if (this.bat.isResting() && !this.bat.isSilent()) {
            level.levelEvent(null, 1025, this.bat.blockPosition(), 0);
        }
        this.bat.setResting(false);
        this.disturbancePosition = sourcePosition;
        this.fleeTicks = MIN_FLEE_TICKS + this.bat.getRandom().nextInt(MAX_FLEE_TICKS - MIN_FLEE_TICKS + 1);
        return true;
    }

    public void tick(ServerLevel level) {
        if (this.fleeTicks == 0) {
            return;
        }
        if (!this.bat.isAlive() || this.bat.isNoAi()
                || !MobTweaksSettings.isEnabled(level, MobTweak.BAT_DISTURBANCE_FLEE)) {
            this.fleeTicks = 0;
            return;
        }

        this.fleeTicks--;
        this.bat.setResting(false);
        Vec3 away = this.bat.position().subtract(this.disturbancePosition).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            double angle = this.bat.getRandom().nextDouble() * Math.PI * 2.0;
            away = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        away = away.normalize().scale(0.5);
        Vec3[] escapeDirections = {
                away.add(0.0, -0.1, 0.0),
                away.add(0.0, 0.2, 0.0),
                new Vec3(-away.z, 0.0, away.x),
                new Vec3(away.z, 0.0, -away.x),
                new Vec3(0.0, -0.3, 0.0)
        };
        for (Vec3 direction : escapeDirections) {
            Vec3 movement = direction.scale(0.5);
            if (level.hasChunk(SectionPos.blockToSectionCoord(this.bat.getX() + movement.x),
                    SectionPos.blockToSectionCoord(this.bat.getZ() + movement.z))
                    && level.noCollision(this.bat, this.bat.getBoundingBox().expandTowards(movement))) {
                this.bat.setDeltaMovement(movement);
                return;
            }
        }
        this.bat.setDeltaMovement(Vec3.ZERO);
    }
}
