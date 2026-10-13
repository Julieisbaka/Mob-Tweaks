package org.julieisbaka.mobtweaks.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.julieisbaka.mobtweaks.test.mixin.OfferFlowerGoalAccessor;
import org.julieisbaka.mobtweaks.test.mixin.RaidAccessor;

public class MobTweaksGameTests {
    @GameTest
    public void earlyWavesHaveNoIllusioners(GameTestHelper helper) {
        for (int wave = 1; wave <= 4; wave++) {
            checkWave(helper, Difficulty.NORMAL, wave, false, 0);
        }
        helper.succeed();
    }

    @GameTest
    public void laterWavesHaveExactlyOneIllusioner(GameTestHelper helper) {
        checkWave(helper, Difficulty.NORMAL, 5, false, 1);
        for (int wave = 5; wave <= 7; wave++) {
            checkWave(helper, Difficulty.HARD, wave, false, 1);
        }
        helper.succeed();
    }

    @GameTest
    public void bonusWavesRespectWaveThreshold(GameTestHelper helper) {
        checkWave(helper, Difficulty.EASY, 4, true, 0);
        checkWave(helper, Difficulty.NORMAL, 6, true, 1);
        checkWave(helper, Difficulty.HARD, 8, true, 1);
        helper.succeed();
    }

    @GameTest
    public void babyReceivesExactlyOnePoppyAfterOffer(GameTestHelper helper) {
        IronGolem golem = createMob(helper, EntityTypes.IRON_GOLEM);
        Villager baby = createBaby(helper);
        OfferFlowerGoal goal = offer(golem, baby);
        helper.assertTrue(baby.getInventory().isEmpty(), "No flower before the offer finishes");
        while (goal.canContinueToUse()) {
            goal.tick();
        }
        goal.stop();
        helper.assertTrue(baby.getInventory().countItem(Items.POPPY) == 1, "Baby must receive one poppy");
        helper.assertTrue(baby.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).isEmpty(),
                "Flower must be stored in inventory, not equipment");
        helper.assertTrue(golem.getOfferFlowerTick() == 0, "Offer animation must stop");
        goal.stop();
        helper.assertTrue(baby.getInventory().countItem(Items.POPPY) == 1, "Stopping twice must not duplicate flowers");
        helper.succeed();
    }

    @GameTest
    public void flowersStackWithoutReplacingFood(GameTestHelper helper) {
        Villager baby = createBaby(helper);
        baby.getInventory().setItem(0, new ItemStack(Items.BREAD, 12));
        baby.getInventory().setItem(1, new ItemStack(Items.POPPY, 3));
        completeOffer(createMob(helper, EntityTypes.IRON_GOLEM), baby);
        helper.assertTrue(baby.getInventory().countItem(Items.POPPY) == 4, "Flower must stack");
        helper.assertTrue(baby.getInventory().countItem(Items.BREAD) == 12, "Food must remain unchanged");
        helper.succeed();
    }

    @GameTest
    public void fullInventoryIsNotOverwritten(GameTestHelper helper) {
        Villager baby = createBaby(helper);
        for (int slot = 0; slot < baby.getInventory().getContainerSize(); slot++) {
            baby.getInventory().setItem(slot, new ItemStack(Items.BREAD, 64));
        }
        completeOffer(createMob(helper, EntityTypes.IRON_GOLEM), baby);
        helper.assertTrue(baby.getInventory().countItem(Items.POPPY) == 0, "Full inventory cannot accept a flower");
        helper.assertTrue(baby.getInventory().countItem(Items.BREAD) == 64 * baby.getInventory().getContainerSize(),
                "Full inventory must remain unchanged");
        helper.succeed();
    }

    @GameTest
    public void interruptedOfferDoesNotGiveFlower(GameTestHelper helper) {
        Villager baby = createBaby(helper);
        OfferFlowerGoal goal = offer(createMob(helper, EntityTypes.IRON_GOLEM), baby);
        goal.tick();
        goal.stop();
        helper.assertTrue(baby.getInventory().isEmpty(), "Interrupted offers must not give a flower");
        helper.succeed();
    }

    @GameTest
    public void adultVillagerDoesNotReceiveFlower(GameTestHelper helper) {
        Villager adult = createMob(helper, EntityTypes.VILLAGER);
        adult.setAge(0);
        completeOffer(createMob(helper, EntityTypes.IRON_GOLEM), adult);
        helper.assertTrue(adult.getInventory().isEmpty(), "Adults must not receive flowers");
        helper.succeed();
    }

    @GameTest
    public void distantBabyDoesNotReceiveFlower(GameTestHelper helper) {
        IronGolem golem = createMob(helper, EntityTypes.IRON_GOLEM);
        Villager baby = createBaby(helper);
        baby.setPos(golem.getX() + 20, golem.getY(), golem.getZ());
        completeOffer(golem, baby);
        helper.assertTrue(baby.getInventory().isEmpty(), "Out-of-range baby must not receive a flower");
        helper.succeed();
    }

    @GameTest
    public void copperGolemGiftingIsPreserved(GameTestHelper helper) {
        CopperGolem copperGolem = createMob(helper, EntityTypes.COPPER_GOLEM);
        completeOffer(createMob(helper, EntityTypes.IRON_GOLEM), copperGolem);
        helper.assertTrue(copperGolem.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA).is(Items.POPPY),
                "Vanilla copper golem gifting must still work");
        helper.succeed();
    }

    private static void checkWave(GameTestHelper helper, Difficulty difficulty, int wave, boolean bonus, int expected) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        Raid raid = new Raid(pos, difficulty);
        raid.setRaidOmenLevel(bonus ? 2 : 1);
        RaidAccessor accessor = (RaidAccessor) raid;
        accessor.mobTweaks$setGroupsSpawned(wave - 1);
        accessor.mobTweaks$spawnGroup(helper.getLevel(), pos);
        long count = raid.getAllRaiders().stream().filter(Illusioner.class::isInstance).count();
        helper.assertTrue(count == expected, "Wave " + wave + " must contain " + expected + " illusioners");
        for (Raider raider : raid.getAllRaiders()) {
            if (raider instanceof Illusioner illusioner) {
                helper.assertTrue(illusioner.getCurrentRaid() == raid, "Illusioner must belong to the raid");
                helper.assertTrue(illusioner.getWave() == wave, "Illusioner must belong to the correct wave");
                helper.assertTrue(illusioner.canJoinRaid(), "Illusioner raid membership must be enabled");
                helper.assertTrue(illusioner.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW),
                        "Illusioner must have its normal bow");
                helper.assertTrue(raid.getHealthOfLivingRaiders() >= illusioner.getHealth(),
                        "Raid health must include the illusioner");
            }
            raider.discard();
        }
        helper.assertTrue(raid.getGroupsSpawned() == wave, "Wave progression must remain unchanged");
    }

    private static <T extends Mob> T createMob(GameTestHelper helper, EntityType<T> type) {
        T mob = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (mob == null) {
            throw new IllegalStateException("Cannot create test mob " + type);
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return mob;
    }

    private static Villager createBaby(GameTestHelper helper) {
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        villager.setAge(-24000);
        return villager;
    }

    private static OfferFlowerGoal offer(IronGolem golem, LivingEntity recipient) {
        OfferFlowerGoal goal = new OfferFlowerGoal(golem);
        ((OfferFlowerGoalAccessor) goal).mobTweaks$setRecipient(recipient);
        goal.start();
        return goal;
    }

    private static void completeOffer(IronGolem golem, LivingEntity recipient) {
        OfferFlowerGoal goal = offer(golem, recipient);
        ((OfferFlowerGoalAccessor) goal).mobTweaks$setTick(0);
        goal.stop();
    }
}
