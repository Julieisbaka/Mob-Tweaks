package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.gametest.framework.GameTestHelper;
import org.julieisbaka.mobtweaks.FoxBerryDeliveryGoal;

public class FoxBerryGameTests {
    @GameTest
    public void berryDeliveryCooldownAppliesWhileCarryingBerries(GameTestHelper helper) {
        Fox parent = createMob(helper, EntityTypes.FOX);
        Fox kit = createMob(helper, EntityTypes.FOX);
        kit.setBaby(true);
        kit.setPos(parent.getX() + 1, parent.getY(), parent.getZ());
        parent.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
        helper.getLevel().addFreshEntity(parent);
        helper.getLevel().addFreshEntity(kit);

        FoxBerryDeliveryGoal goal = new FoxBerryDeliveryGoal(parent);
        helper.assertTrue(goal.canUse(), "Carried berries should be delivered to a nearby kit");
        goal.start();
        goal.tick();
        helper.assertTrue(kit.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.SWEET_BERRIES),
                "First delivery should succeed");
        goal.stop();
        kit.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        parent.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));

        parent.tickCount += 599;
        helper.assertTrue(!goal.canUse(), "Carrying berries must not bypass the 600-tick delivery cooldown");
        parent.tickCount++;
        helper.assertTrue(goal.canUse(), "Delivery should resume exactly when the cooldown expires");
        helper.succeed();
    }

    @GameTest
    public void berryHarvestDoesNotOverwriteAnItemPickedUpDuringDelivery(GameTestHelper helper) {
        Fox parent = createMob(helper, EntityTypes.FOX);
        Fox kit = createMob(helper, EntityTypes.FOX);
        kit.setBaby(true);
        kit.setPos(parent.getX() + 1, parent.getY(), parent.getZ());
        helper.getLevel().addFreshEntity(parent);
        helper.getLevel().addFreshEntity(kit);
        BlockPos bush = parent.blockPosition().east();
        helper.getLevel().setBlockAndUpdate(bush,
                Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));

        FoxBerryDeliveryGoal goal = new FoxBerryDeliveryGoal(parent);
        helper.assertTrue(goal.canUse(), "Empty-handed parent should start harvesting for its kit");
        goal.start();
        parent.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CHICKEN));
        helper.assertTrue(!goal.canContinueToUse(), "Picking up another item should cancel harvesting");
        goal.tick();
        helper.assertTrue(parent.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.CHICKEN),
                "Harvesting must not overwrite the parent's new item");
        helper.assertTrue(kit.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),
                "Cancelled delivery must not give the kit berries");
        helper.assertTrue(helper.getLevel().getBlockState(bush).getValue(SweetBerryBushBlock.AGE) == 3,
                "Cancelled harvesting must leave the bush unchanged");
        goal.stop();
        helper.succeed();
    }

    @GameTest
    public void sittingFoxStopsDeliveringBerries(GameTestHelper helper) {
        Fox parent = createMob(helper, EntityTypes.FOX);
        Fox kit = createMob(helper, EntityTypes.FOX);
        kit.setBaby(true);
        kit.setPos(parent.getX() + 1, parent.getY(), parent.getZ());
        parent.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
        helper.getLevel().addFreshEntity(parent);
        helper.getLevel().addFreshEntity(kit);

        FoxBerryDeliveryGoal goal = new FoxBerryDeliveryGoal(parent);
        helper.assertTrue(goal.canUse(), "Standing parent should be able to deliver its berries");
        goal.start();
        parent.setSitting(true);
        helper.assertTrue(!goal.canContinueToUse(), "Sitting should interrupt berry delivery");
        goal.stop();
        helper.assertTrue(parent.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.SWEET_BERRIES),
                "Interrupted delivery must preserve carried berries");
        helper.succeed();
    }

    @GameTest
    public void adultFoxHarvestsAndGivesBerriesToKit(GameTestHelper helper) {
        Fox parent = createMob(helper, EntityTypes.FOX);
        Fox kit = createMob(helper, EntityTypes.FOX);
        kit.setBaby(true);
        kit.setPos(parent.getX() + 1, parent.getY(), parent.getZ());
        helper.getLevel().addFreshEntity(parent);
        helper.getLevel().addFreshEntity(kit);
        BlockPos bush = parent.blockPosition().east();
        helper.getLevel().setBlockAndUpdate(bush,
                Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));

        helper.assertTrue(
                parent.getGoalSelector().getAvailableGoals().stream()
                        .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof FoxBerryDeliveryGoal),
                "Foxes should register the berry-delivery goal");
        FoxBerryDeliveryGoal goal = new FoxBerryDeliveryGoal(parent);
        helper.assertTrue(goal.canUse(), "Adult fox with a nearby kit and ripe berries should start the delivery goal");
        goal.start();
        goal.tick();

        helper.assertTrue(helper.getLevel().getBlockState(bush).getValue(SweetBerryBushBlock.AGE) == 2,
                "Parent fox should pick berries from a ripe bush");
        helper.assertTrue(kit.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.SWEET_BERRIES),
                "Kit should receive and hold the berries");
        helper.assertTrue(parent.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(),
                "Parent should give away the berries it gathered");
        helper.succeed();
    }

    @GameTest
    public void berryDeliveryRespectsMobGriefing(GameTestHelper helper) {
        boolean original = helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        try {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false, helper.getLevel().getServer());
            Fox parent = createMob(helper, EntityTypes.FOX);
            Fox kit = createMob(helper, EntityTypes.FOX);
            kit.setBaby(true);
            kit.setPos(parent.getX() + 1, parent.getY(), parent.getZ());
            helper.getLevel().addFreshEntity(parent);
            helper.getLevel().addFreshEntity(kit);
            BlockPos bush = parent.blockPosition().east();
            helper.getLevel().setBlockAndUpdate(bush,
                    Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));

            helper.assertTrue(!new FoxBerryDeliveryGoal(parent).canUse(),
                    "Foxes must not harvest berries when mobGriefing is disabled");
            helper.assertTrue(helper.getLevel().getBlockState(bush).getValue(SweetBerryBushBlock.AGE) == 3,
                    "Disabling mobGriefing must preserve ripe bushes");
            parent.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES));
            FoxBerryDeliveryGoal goal = new FoxBerryDeliveryGoal(parent);
            helper.assertTrue(goal.canUse(), "Disabling mobGriefing must still allow delivery of carried berries");
            goal.start();
            goal.tick();
            helper.assertTrue(kit.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.SWEET_BERRIES),
                    "Carried berries should reach the kit without harvesting");
            goal.stop();
        } finally {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void parentDoesNotReplaceFoodAlreadyHeldByKit(GameTestHelper helper) {
        Fox parent = createMob(helper, EntityTypes.FOX);
        Fox kit = createMob(helper, EntityTypes.FOX);
        kit.setBaby(true);
        kit.setPos(parent.getX() + 1, parent.getY(), parent.getZ());
        kit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CHICKEN));
        helper.assertTrue(kit.isBaby(), "Test fox should be a kit");
        helper.assertTrue(kit.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.CHICKEN),
                "Test kit should already be carrying food");
        helper.getLevel().addFreshEntity(parent);
        helper.getLevel().addFreshEntity(kit);
        BlockPos bush = parent.blockPosition().east();
        helper.getLevel().setBlockAndUpdate(bush,
                Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));

        helper.assertTrue(!new FoxBerryDeliveryGoal(parent).canUse(),
                "Parent should not try to replace an item the kit is already carrying");
        helper.assertTrue(kit.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.CHICKEN), "Kit's food must be preserved");
        helper.succeed();
    }
}
