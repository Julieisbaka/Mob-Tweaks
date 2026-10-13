package org.julieisbaka.mobtweaks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;

public final class WitherBirthExplosionCalculator extends EntityBasedExplosionDamageCalculator {
    public WitherBirthExplosionCalculator(Entity source) {
        super(source);
    }

    @Override
    public float getEntityDamageAmount(Explosion explosion, Entity entity, float exposure) {
        double distance = entity.position().distanceTo(explosion.center()) / (explosion.radius() * 2.0);
        double impact = Math.max(0.0, 1.0 - distance) * exposure;
        return (float) ((impact * impact + impact) / 2.0 * 71.0 + 1.0);
    }
}
