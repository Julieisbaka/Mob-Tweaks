package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksConfig;
import org.julieisbaka.mobtweaks.MobTweaksGameRules;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.julieisbaka.mobtweaks.CubeSpawnClearance;
import org.julieisbaka.mobtweaks.SkeletonCoverGoal;
import org.julieisbaka.mobtweaks.test.mixin.EndermanAccessor;

public class MobTweakSettingsAndBehaviorGameTests {
    @GameTest
    public void everyTweakHasAnEnabledWorldRuleAndConfigDefault(GameTestHelper helper) {
        for (MobTweak tweak : MobTweak.values()) {
            GameRule<Boolean> rule = MobTweaksGameRules.get(tweak);
            helper.assertTrue(helper.getLevel().getGameRules().get(rule), "World rule should default on: " + tweak);
            helper.assertTrue(MobTweaksConfig.isEnabled(tweak), "Config setting should default on: " + tweak);
            helper.assertTrue(MobTweaksSettings.isEnabled(helper.getLevel(), tweak), "Tweak should default on: " + tweak);
        }
        helper.succeed();
    }

    @GameTest
    public void worldRuleDisablesOnlyItsOwnTweak(GameTestHelper helper) {
        MobTweak disabled = MobTweak.VEX_RECALL;
        GameRule<Boolean> rule = MobTweaksGameRules.get(disabled);
        try {
            helper.getLevel().getGameRules().set(rule, false, helper.getLevel().getServer());
            helper.assertTrue(!MobTweaksSettings.isEnabled(helper.getLevel(), disabled), "Disabled rule must disable its tweak");
            helper.assertTrue(
                    MobTweaksSettings.isEnabled(helper.getLevel(), MobTweak.RAVAGER_TURTLE_EGGS),
                    "Other tweaks must remain enabled");
        } finally {
            helper.getLevel().getGameRules().set(rule, true, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void smallCubeSpawnRuleCanBeDisabled(GameTestHelper helper) {
        GameRule<Boolean> rule = MobTweaksGameRules.get(MobTweak.CUBES_SMALL_SPAWNING);
        try {
            helper.getLevel().getGameRules().set(rule, false, helper.getLevel().getServer());
            var spawnBox = CubeSpawnClearance.getSpawnBox(
                    EntityTypes.SLIME, helper.getLevel(), 4.5, 3.0, 4.5);
            helper.assertTrue(spawnBox.getXsize() > 2.0, "Disabled rule must restore normal slime clearance");
        } finally {
            helper.getLevel().getGameRules().set(rule, true, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void horseWarnsWithARearAndKeepsRiderMounted(GameTestHelper helper) {
        AbstractHorse horse = createMob(helper, EntityTypes.HORSE);
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        helper.getLevel().addFreshEntity(horse);

        ServerPlayer rider = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        EmbeddedChannel channel = connectPlayer(helper, rider);
        try {
            helper.assertTrue(rider.startRiding(horse, true, true), "Test rider should mount horse");
            Vec3 forward = horse.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
            Monster attacker = createMob(helper, EntityTypes.ZOMBIE);
            attacker.setPos(
                    horse.getX() - forward.x * 4.0,
                    horse.getY(),
                    horse.getZ() - forward.z * 4.0);
            helper.getLevel().addFreshEntity(attacker);

            horse.clearStanding();
            horse.aiStep();
            helper.assertTrue(horse.isStanding(), "Horse should rear when a hostile approaches from behind");
            helper.assertTrue(rider.getVehicle() == horse, "Warning rear must not buck the rider off");
            attacker.discard();
        } finally {
            rider.stopRiding();
            rider.discard();
            horse.discard();
            channel.finishAndReleaseAll();
        }
        helper.succeed();
    }

    @GameTest
    public void horseRearWarningCanBeDisabled(GameTestHelper helper) {
        GameRule<Boolean> rule = MobTweaksGameRules.get(MobTweak.HORSE_REAR_WARNING);
        try {
            helper.getLevel().getGameRules().set(rule, false, helper.getLevel().getServer());
            AbstractHorse horse = createMob(helper, EntityTypes.HORSE);
            horse.setTamed(true);
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            helper.getLevel().addFreshEntity(horse);
            ServerPlayer rider = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            EmbeddedChannel channel = connectPlayer(helper, rider);
            try {
                rider.startRiding(horse, true, true);
                Vec3 forward = horse.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
                Monster attacker = createMob(helper, EntityTypes.ZOMBIE);
                attacker.setPos(
                        horse.getX() - forward.x * 4.0,
                        horse.getY(),
                        horse.getZ() - forward.z * 4.0);
                helper.getLevel().addFreshEntity(attacker);

                horse.aiStep();
                helper.assertTrue(!horse.isStanding(), "Disabled warning must not make the horse rear");
                attacker.discard();
            } finally {
                rider.stopRiding();
                rider.discard();
                horse.discard();
                channel.finishAndReleaseAll();
            }
        } finally {
            helper.getLevel().getGameRules().set(rule, true, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void bowSkeletonMovesBehindCoverAfterShooting(GameTestHelper helper) {
        Skeleton skeleton = createMob(helper, EntityTypes.SKELETON);
        Villager target = createMob(helper, EntityTypes.VILLAGER);
        target.setPos(skeleton.getX() + 6.0, skeleton.getY(), skeleton.getZ());
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setTarget(target);
        helper.getLevel().addFreshEntity(skeleton);
        helper.getLevel().addFreshEntity(target);
        int floorY = BlockPos.containing(skeleton.getX(), skeleton.getY(), skeleton.getZ()).getY() - 1;
        for (int x = BlockPos.containing(skeleton.getX(), skeleton.getY(), skeleton.getZ()).getX() - 4;
             x <= BlockPos.containing(skeleton.getX(), skeleton.getY(), skeleton.getZ()).getX() + 6; x++) {
            for (int z = BlockPos.containing(skeleton.getX(), skeleton.getY(), skeleton.getZ()).getZ() - 4;
                 z <= BlockPos.containing(skeleton.getX(), skeleton.getY(), skeleton.getZ()).getZ() + 6; z++) {
                helper.getLevel().setBlockAndUpdate(new BlockPos(x, floorY, z), Blocks.STONE.defaultBlockState());
            }
        }
        int eyeY = net.minecraft.util.Mth.floor(skeleton.getEyeY());
        for (int x = net.minecraft.util.Mth.floor(skeleton.getX()); x <= net.minecraft.util.Mth.floor(target.getX()); x++) {
            for (int y = eyeY - 1; y <= eyeY + 1; y++) {
                helper.getLevel().setBlockAndUpdate(
                        new BlockPos(x, y, net.minecraft.util.Mth.floor(skeleton.getZ())),
                        Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos wall = BlockPos.containing(skeleton.getX() + 3.0, skeleton.getY(), skeleton.getZ() + 3.0);
        helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(wall.above(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(skeleton.hasLineOfSight(target), "Test wall should leave the skeleton's current path exposed");

        SkeletonCoverGoal goal = skeleton.getGoalSelector().getAvailableGoals().stream()
                .map(wrappedGoal -> wrappedGoal.getGoal())
                .filter(SkeletonCoverGoal.class::isInstance)
                .map(SkeletonCoverGoal.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Skeleton cover goal was not registered"));
        skeleton.performRangedAttack(target, 1.0F);
        helper.assertTrue(goal.canUse(), "Skeleton should find nearby cover after firing its bow");
        goal.start();
        BlockPos pathTarget = skeleton.getNavigation().getTargetPos();
        boolean navigationStarted = pathTarget != null && !pathTarget.equals(skeleton.blockPosition());
        boolean steeringStarted = skeleton.getMoveControl().hasWanted()
                && (skeleton.getMoveControl().getWantedX() != skeleton.getX()
                || skeleton.getMoveControl().getWantedZ() != skeleton.getZ());
        helper.assertTrue(navigationStarted || steeringStarted,
                "Skeleton should move toward the block that hides it from its target");
        goal.tick();
        goal.stop();
        skeleton.discard();
        target.discard();
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void skeletonCoverCanBeDisabled(GameTestHelper helper) {
        GameRule<Boolean> rule = MobTweaksGameRules.get(MobTweak.SKELETON_COVER);
        try {
            helper.getLevel().getGameRules().set(rule, false, helper.getLevel().getServer());
            Skeleton skeleton = createMob(helper, EntityTypes.SKELETON);
            Villager target = createMob(helper, EntityTypes.VILLAGER);
            target.setPos(skeleton.getX() + 6.0, skeleton.getY(), skeleton.getZ());
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            skeleton.setTarget(target);
            helper.getLevel().addFreshEntity(skeleton);
            helper.getLevel().addFreshEntity(target);
            BlockPos wall = BlockPos.containing(skeleton.getX() + 2.0, skeleton.getY(), skeleton.getZ());
            helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(wall.above(), Blocks.STONE.defaultBlockState());

            SkeletonCoverGoal goal = new SkeletonCoverGoal(skeleton);
            goal.beginReload();
            helper.assertTrue(!goal.canUse(), "Disabled feature must prevent cover-seeking");
            skeleton.discard();
            target.discard();
        } finally {
            helper.getLevel().getGameRules().set(rule, true, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void lowHealthEndermanPlacesItsCarriedBlockBetweenItselfAndAttacker(GameTestHelper helper) {
        Enderman enderman = createMob(helper, EntityTypes.ENDERMAN);
        Mob attacker = createMob(helper, EntityTypes.COW);
        attacker.setPos(enderman.getX() + 5.0, enderman.getY(), enderman.getZ());
        enderman.setHealth(enderman.getMaxHealth() / 2.0F);
        enderman.setCarriedBlock(Blocks.STONE.defaultBlockState());
        enderman.setTarget(attacker);
        helper.getLevel().addFreshEntity(enderman);
        helper.getLevel().addFreshEntity(attacker);
        BlockPos expectedBlock = BlockPos.containing(enderman.getX() + 1.25, enderman.getY(), enderman.getZ());

        ((EndermanAccessor) enderman).mobTweaks$serverAiStep(helper.getLevel());
        helper.assertTrue(helper.getLevel().getBlockState(expectedBlock).is(Blocks.STONE),
                "Enderman should place its held block in the attacker's path");
        helper.assertTrue(enderman.getCarriedBlock() == null, "Placed block should leave the enderman's hands");
        enderman.discard();
        attacker.discard();
        helper.succeed();
    }

    @GameTest
    public void endermanBlockDefenseRespectsMobGriefing(GameTestHelper helper) {
        boolean original = helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        try {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false, helper.getLevel().getServer());
            Enderman enderman = createMob(helper, EntityTypes.ENDERMAN);
            Mob attacker = createMob(helper, EntityTypes.COW);
            attacker.setPos(enderman.getX() + 5.0, enderman.getY(), enderman.getZ());
            enderman.setHealth(enderman.getMaxHealth() / 2.0F);
            enderman.setCarriedBlock(Blocks.STONE.defaultBlockState());
            enderman.setTarget(attacker);
            helper.getLevel().addFreshEntity(enderman);
            helper.getLevel().addFreshEntity(attacker);
            BlockPos expectedBlock = BlockPos.containing(enderman.getX() + 1.25, enderman.getY(), enderman.getZ());

            ((EndermanAccessor) enderman).mobTweaks$serverAiStep(helper.getLevel());
            helper.assertTrue(helper.getLevel().getBlockState(expectedBlock).isAir(),
                    "Enderman must not place blocks when mobGriefing is disabled");
            helper.assertTrue(enderman.getCarriedBlock().is(Blocks.STONE), "Disabled placement must retain its block");
            enderman.discard();
            attacker.discard();
        } finally {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    private static EmbeddedChannel connectPlayer(GameTestHelper helper, ServerPlayer player) {
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        for (int i = 0; i < 60; i++) {
            player.connection.tickClientLoadTimeout();
        }
        helper.getLevel().addNewPlayer(player);
        return channel;
    }
}
