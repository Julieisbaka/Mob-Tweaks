package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Enderman.class)
abstract class EndermanMixin {
    @Unique
    private int mobTweaks$blockPlaceCooldown;

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void mobTweaks$placeBlockAgainstAttacker(ServerLevel level, CallbackInfo ci) {
        Enderman enderman = (Enderman) (Object) this;
        if (this.mobTweaks$blockPlaceCooldown > 0) {
            this.mobTweaks$blockPlaceCooldown--;
            return;
        }
        if (!MobTweaksSettings.isEnabled(level, MobTweak.ENDERMAN_BLOCK_DEFENSE)
                || !level.getGameRules().get(GameRules.MOB_GRIEFING)
                || enderman.getHealth() > enderman.getMaxHealth() / 2.0F) {
            return;
        }

        LivingEntity attacker = enderman.getTarget();
        if (attacker == null || !attacker.isAlive()) {
            attacker = enderman.getLastHurtByMob();
        }
        if (attacker == null || !attacker.isAlive()
                || enderman.distanceToSqr(attacker) < 4.0
                || enderman.distanceToSqr(attacker) > 64.0) {
            return;
        }

        BlockState carried = enderman.getCarriedBlock();
        if (carried == null || carried.isAir()) {
            return;
        }
        Vec3 towardAttacker = attacker.position().subtract(enderman.position())
                .multiply(1.0, 0.0, 1.0).normalize();
        BlockPos position = BlockPos.containing(
                enderman.getX() + towardAttacker.x * 1.25,
                enderman.getY(),
                enderman.getZ() + towardAttacker.z * 1.25);
        BlockState existing = level.getBlockState(position);
        AABB placementBounds = new AABB(
                position.getX(), position.getY(), position.getZ(),
                position.getX() + 1.0, position.getY() + 1.0, position.getZ() + 1.0);
        if (!existing.isAir()
                || carried.getCollisionShape(level, position).isEmpty()
                || !carried.canSurvive(level, position)
                || !level.noCollision(enderman, placementBounds)
                || !level.setBlock(position, carried, 3)) {
            return;
        }

        enderman.setCarriedBlock(null);
        this.mobTweaks$blockPlaceCooldown = 100;
    }
}
