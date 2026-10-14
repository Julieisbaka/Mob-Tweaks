package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.julieisbaka.mobtweaks.IronGolemProjectileGuardGoal;
import org.julieisbaka.mobtweaks.VexRecallGoal;

public class VexAndGolemGameTests {
    @GameTest
    public void vexReturnsToOwnerAfterLosingTarget(GameTestHelper helper) {
        Evoker evoker = createMob(helper, EntityTypes.EVOKER);
        Vex vex = createMob(helper, EntityTypes.VEX);
        vex.setOwner(evoker);
        vex.setPos(evoker.getX() + 12, evoker.getY(), evoker.getZ());
        helper.getLevel().addFreshEntity(evoker);
        helper.getLevel().addFreshEntity(vex);

        VexRecallGoal goal = new VexRecallGoal(vex);
        helper.assertTrue(goal.canUse(), "Targetless vex away from its owner should return");
        goal.start();
        helper.assertTrue(goal.canContinueToUse(), "Vex should continue returning until near its owner");
        goal.tick();
        helper.assertTrue(vex.getNavigation().isInProgress(), "Returning vex should navigate to its owner");
        goal.stop();
        helper.succeed();
    }

    @GameTest
    public void distantVexAbandonsTargetToReturnToOwner(GameTestHelper helper) {
        Evoker evoker = createMob(helper, EntityTypes.EVOKER);
        Vex vex = createMob(helper, EntityTypes.VEX);
        vex.setOwner(evoker);
        vex.setPos(evoker.getX() + 40, evoker.getY(), evoker.getZ());
        vex.setTarget(createMob(helper, EntityTypes.COW));
        helper.getLevel().addFreshEntity(evoker);
        helper.getLevel().addFreshEntity(vex);

        VexRecallGoal goal = new VexRecallGoal(vex);
        helper.assertTrue(goal.canUse(), "Vex more than 32 blocks from its owner should return");
        goal.start();
        helper.assertTrue(vex.getTarget() == null, "Distant vex should stop chasing its target");
        helper.assertTrue(goal.canContinueToUse(), "Distant vex should stay in recall mode");
        goal.stop();
        helper.succeed();
    }

    @GameTest
    public void nearbyTargetlessVexDoesNotNeedRecall(GameTestHelper helper) {
        Evoker evoker = createMob(helper, EntityTypes.EVOKER);
        Vex vex = createMob(helper, EntityTypes.VEX);
        vex.setOwner(evoker);
        vex.setPos(evoker.getX() + 2, evoker.getY(), evoker.getZ());
        helper.getLevel().addFreshEntity(evoker);
        helper.getLevel().addFreshEntity(vex);

        helper.assertTrue(!new VexRecallGoal(vex).canUse(), "Vex already beside its owner should not recall");
        helper.succeed();
    }

    @GameTest
    public void golemMovesBetweenVillagerAndHostileArrow(GameTestHelper helper) {
        IronGolem golem = createMob(helper, EntityTypes.IRON_GOLEM);
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        Skeleton skeleton = createMob(helper, EntityTypes.SKELETON);
        AbstractArrow arrow = (AbstractArrow) EntityTypes.ARROW.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (arrow == null) {
            throw new IllegalStateException("Could not create test arrow");
        }
        arrow.setOwner(skeleton);
        arrow.setPos(villager.getX() + 8, villager.getEyeY(), villager.getZ());
        arrow.setDeltaMovement(-1, 0, 0);
        golem.setPos(villager.getX() + 2, villager.getY(), villager.getZ() + 2);
        helper.getLevel().addFreshEntity(golem);
        helper.getLevel().addFreshEntity(villager);
        helper.getLevel().addFreshEntity(skeleton);
        helper.getLevel().addFreshEntity(arrow);

        IronGolemProjectileGuardGoal goal = new IronGolemProjectileGuardGoal(golem);
        helper.assertTrue(goal.canUse(), "Golem should detect a hostile arrow aimed at a nearby villager");
        goal.start();
        goal.tick();
        helper.assertTrue(golem.getNavigation().isInProgress(), "Golem should move to intercept the arrow");
        helper.assertTrue(
                golem.getNavigation().getTargetPos().getX() > villager.blockPosition().getX(),
                "Golem should move toward the incoming arrow's side of the villager");
        goal.stop();
        golem.discard();
        villager.discard();
        skeleton.discard();
        arrow.discard();
        helper.succeed();
    }

    @GameTest
    public void golemIgnoresPlayerProjectiles(GameTestHelper helper) {
        IronGolem golem = createMob(helper, EntityTypes.IRON_GOLEM);
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        AbstractArrow arrow = (AbstractArrow) EntityTypes.ARROW.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (arrow == null) {
            throw new IllegalStateException("Could not create test arrow");
        }
        arrow.setOwner(helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        arrow.setPos(villager.getX() + 8, villager.getEyeY(), villager.getZ());
        arrow.setDeltaMovement(-1, 0, 0);
        helper.getLevel().addFreshEntity(golem);
        helper.getLevel().addFreshEntity(villager);
        helper.getLevel().addFreshEntity(arrow);

        helper.assertTrue(
                !new IronGolemProjectileGuardGoal(golem).canUse(),
                "Golem should not intercept projectiles fired by players");
        golem.discard();
        villager.discard();
        arrow.discard();
        helper.succeed();
    }

    @GameTest
    public void registeredGoalsAreAttachedToBothMobTypes(GameTestHelper helper) {
        Vex vex = createMob(helper, EntityTypes.VEX);
        IronGolem golem = createMob(helper, EntityTypes.IRON_GOLEM);
        helper.assertTrue(
                vex.getGoalSelector().getAvailableGoals().stream()
                        .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof VexRecallGoal),
                "Vexes should register the recall goal");
        helper.assertTrue(
                golem.getGoalSelector().getAvailableGoals().stream()
                        .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof IronGolemProjectileGuardGoal),
                "Iron golems should register the projectile guard goal");
        helper.succeed();
    }
}
