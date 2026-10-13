package org.julieisbaka.mobtweaks.test;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.julieisbaka.mobtweaks.WitherDashGoal;
import org.julieisbaka.mobtweaks.test.mixin.VillagerAccessor;
import org.julieisbaka.mobtweaks.test.mixin.WitherBossAccessor;

public class AdditionalMobGameTests {
    @GameTest
    public void allDyesRecolorShulkersInSurvival(GameTestHelper helper) {
        Shulker shulker = createMob(helper, EntityTypes.SHULKER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (DyeColor color : DyeColor.values()) {
            Item dye = Items.DYE.pick(color);
            ItemStack stack = new ItemStack(dye, 2);
            helper.assertTrue(dye.interactLivingEntity(stack, player, shulker, InteractionHand.MAIN_HAND)
                    .consumesAction(), "Dye interaction must succeed");
            helper.assertTrue(shulker.getColor() == stack.get(DataComponents.DYE), "Each dye must apply its own color");
            helper.assertTrue(stack.getCount() == 1, "Survival dyeing must consume exactly one dye");
        }
        helper.succeed();
    }

    @GameTest
    public void creativeAndSameColorDoNotConsumeDye(GameTestHelper helper) {
        Shulker shulker = createMob(helper, EntityTypes.SHULKER);
        ItemStack dye = new ItemStack(Items.DYE.pick(DyeColor.RED), 2);
        dye.getItem().interactLivingEntity(dye, helper.makeMockServerPlayer(GameType.CREATIVE), shulker, InteractionHand.OFF_HAND);
        helper.assertTrue(shulker.getColor() == DyeColor.RED && dye.getCount() == 2, "Creative must recolor without consuming");
        dye.getItem().interactLivingEntity(dye, helper.makeMockPlayer(GameType.SURVIVAL), shulker, InteractionHand.MAIN_HAND);
        helper.assertTrue(dye.getCount() == 2, "Same-color dye must not be wasted");
        helper.succeed();
    }

    @GameTest
    public void deadShulkerRejectsDye(GameTestHelper helper) {
        Shulker shulker = createMob(helper, EntityTypes.SHULKER);
        shulker.setHealth(0);
        ItemStack dye = new ItemStack(Items.DYE.pick(DyeColor.RED));
        helper.assertTrue(!dye.getItem().interactLivingEntity(dye, helper.makeMockPlayer(GameType.SURVIVAL),
                shulker, InteractionHand.MAIN_HAND).consumesAction(), "Dead shulkers must reject dye");
        helper.assertTrue(shulker.getColor() == null && dye.getCount() == 1, "Rejected dye must not change state");
        helper.succeed();
    }

    @GameTest
    public void shulkerColorSurvivesSaveLoad(GameTestHelper helper) {
        Shulker shulker = createMob(helper, EntityTypes.SHULKER);
        Item cyanDye = Items.DYE.pick(DyeColor.CYAN);
        cyanDye.interactLivingEntity(new ItemStack(cyanDye), helper.makeMockPlayer(GameType.SURVIVAL),
                shulker, InteractionHand.MAIN_HAND);
        Shulker loaded = createMob(helper, EntityTypes.SHULKER);
        load(helper, loaded, save(helper, shulker));
        helper.assertTrue(loaded.getColor() == DyeColor.CYAN, "Dyed color must persist");
        helper.succeed();
    }

    @GameTest
    public void dyeInteractionWorksThroughPlayerOffhand(GameTestHelper helper) {
        Shulker shulker = createMob(helper, EntityTypes.SHULKER);
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DYE.pick(DyeColor.BLUE), 2));
        helper.assertTrue(player.interactOn(shulker, InteractionHand.OFF_HAND, Vec3.ZERO).consumesAction(),
                "Player interaction path must accept offhand dye");
        helper.assertTrue(shulker.getColor() == DyeColor.BLUE, "Offhand interaction must recolor");
        helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).getCount() == 1, "Offhand must consume one dye");
        helper.succeed();
    }

    @GameTest
    public void foodNutritionFundsTimedVillagerHealing(GameTestHelper helper) {
        List<Item> foods = List.of(Items.BREAD, Items.CARROT, Items.POTATO, Items.BEETROOT);
        for (Item food : foods) {
            Villager villager = createMob(helper, EntityTypes.VILLAGER);
            villager.setHealth(2);
            villager.getInventory().setItem(0, new ItemStack(food, 2));
            int nutrition = food.components().get(DataComponents.FOOD).nutrition();
            stepVillager(helper, villager, 79);
            helper.assertTrue(villager.getHealth() == 2, "Must not heal before 80 ticks");
            helper.assertTrue(villager.getInventory().countItem(food) == 1, "Healing must consume one food item");
            stepVillager(helper, villager, 1);
            helper.assertTrue(villager.getHealth() == 3, "Must heal exactly one HP at 80 ticks");
            stepVillager(helper, villager, 80 * (nutrition - 1));
            helper.assertTrue(villager.getHealth() == 2 + nutrition, "Total healing must equal food nutrition");
            helper.assertTrue(villager.getInventory().countItem(food) == 1, "Must finish first food before consuming second");
        }
        helper.succeed();
    }

    @GameTest
    public void healthyVillagerAndNonFoodArePreserved(GameTestHelper helper) {
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        villager.getInventory().setItem(0, new ItemStack(Items.BREAD, 2));
        stepVillager(helper, villager, 160);
        helper.assertTrue(villager.getInventory().countItem(Items.BREAD) == 2, "Healthy villagers must not consume food");
        villager.getInventory().clearContent();
        villager.getInventory().setItem(0, new ItemStack(Items.POPPY, 3));
        villager.getInventory().setItem(1, new ItemStack(Items.WHEAT, 3));
        villager.setHealth(5);
        stepVillager(helper, villager, 160);
        helper.assertTrue(villager.getHealth() == 5, "Non-food items must not heal");
        helper.assertTrue(villager.getInventory().countItem(Items.POPPY) == 3
                && villager.getInventory().countItem(Items.WHEAT) == 3, "Flowers and crops must not be consumed");
        helper.succeed();
    }

    @GameTest
    public void babyHealsAndNoAiVillagerDoesNot(GameTestHelper helper) {
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        villager.setAge(-24000);
        villager.setHealth(5);
        villager.getInventory().setItem(0, new ItemStack(Items.CARROT, 2));
        villager.setNoAi(true);
        stepVillager(helper, villager, 80);
        helper.assertTrue(villager.getHealth() == 5 && villager.getInventory().countItem(Items.CARROT) == 2,
                "NoAI villagers must not consume or heal");
        villager.setNoAi(false);
        stepVillager(helper, villager, 80);
        helper.assertTrue(villager.getHealth() == 6, "Babies must heal too");
        helper.succeed();
    }

    @GameTest
    public void healingBudgetSurvivesSaveLoad(GameTestHelper helper) {
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        villager.setHealth(10);
        villager.getInventory().setItem(0, new ItemStack(Items.BREAD));
        stepVillager(helper, villager, 40);
        CompoundTag tag = save(helper, villager);
        helper.assertTrue(tag.getIntOr("MobTweaksHealingFood", 0) == 5, "Bread must fund five HP");
        Villager loaded = createMob(helper, EntityTypes.VILLAGER);
        load(helper, loaded, tag);
        stepVillager(helper, loaded, 39);
        helper.assertTrue(loaded.getHealth() == 10, "Saved healing timer must retain its elapsed ticks");
        stepVillager(helper, loaded, 1);
        helper.assertTrue(loaded.getHealth() == 11 && loaded.getInventory().isEmpty(), "Saved food must heal without consuming twice");
        helper.succeed();
    }

    @GameTest
    public void healingStopsAtMaxAndRetainsUnusedFood(GameTestHelper helper) {
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        villager.setHealth(19.5F);
        villager.getInventory().setItem(0, new ItemStack(Items.BREAD, 2));
        stepVillager(helper, villager, 240);
        helper.assertTrue(villager.getHealth() == 20, "Healing must clamp to max health");
        helper.assertTrue(villager.getInventory().countItem(Items.BREAD) == 1, "Healthy villagers must not eat another food");
        villager.setHealth(15);
        stepVillager(helper, villager, 80);
        helper.assertTrue(villager.getHealth() == 16 && villager.getInventory().countItem(Items.BREAD) == 1,
                "Unused consumed-food nutrition must remain available");
        helper.succeed();
    }

    @GameTest
    public void sleepingVillagerDoesNotEatAndBreedingFoodIsUnchanged(GameTestHelper helper) {
        Villager villager = createMob(helper, EntityTypes.VILLAGER);
        villager.setHealth(5);
        villager.getInventory().setItem(0, new ItemStack(Items.BREAD, 2));
        villager.startSleeping(villager.blockPosition());
        stepVillager(helper, villager, 1);
        helper.assertTrue(villager.getInventory().countItem(Items.BREAD) == 2 && villager.getHealth() == 5,
                "Sleeping villagers must not eat to heal");
        villager.stopSleeping();
        stepVillager(helper, villager, 80);
        helper.assertTrue(save(helper, villager).getByteOr("FoodLevel", (byte) 0) == 0,
                "Healing must not grant breeding willingness");
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void halfHealthExplosionSpawnsFourEquippedSkeletonsOnce(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(wither.getMaxHealth() / 2 + 1);
        stepWither(helper, wither);
        helper.assertTrue(reinforcements(helper, wither).isEmpty(), "Above half health must not spawn skeletons");
        BlockPos blastPos = wither.blockPosition().above();
        helper.getLevel().setBlockAndUpdate(blastPos, Blocks.STONE.defaultBlockState());
        wither.setHealth(wither.getMaxHealth() / 2);
        stepWither(helper, wither);
        List<WitherSkeleton> skeletons = reinforcements(helper, wither);
        helper.assertTrue(skeletons.size() == 4, "Exactly four reinforcements must spawn at half health");
        helper.assertTrue(helper.getLevel().getBlockState(blastPos).isAir(), "Half-health phase must explode");
        for (WitherSkeleton skeleton : skeletons) {
            helper.assertTrue(skeleton.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.STONE_SWORD),
                    "Reinforcements must have their normal swords");
        }
        wither.setHealth(wither.getMaxHealth());
        stepWither(helper, wither);
        wither.setHealth(100);
        stepWither(helper, wither);
        helper.assertTrue(reinforcements(helper, wither).size() == 4, "Healing and crossing half health again must not repeat");
        skeletons.forEach(Mob::discard);
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void reinforcementPhaseSurvivesReload(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(100);
        stepWither(helper, wither);
        WitherBoss loaded = createMob(helper, EntityTypes.WITHER);
        load(helper, loaded, save(helper, wither));
        stepWither(helper, loaded);
        helper.assertTrue(reinforcements(helper, wither).size() == 4, "Reload must not repeat reinforcements");
        reinforcements(helper, wither).forEach(Mob::discard);
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void damageCrossingHalfHealthTriggersImmediately(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(155);
        wither.hurtServer(helper.getLevel(), wither.damageSources().generic(), 10);
        helper.assertTrue(reinforcements(helper, wither).size() == 4, "Damage crossing the threshold must trigger immediately");
        reinforcements(helper, wither).forEach(Mob::discard);
        helper.succeed();
    }

    @GameTest
    public void spawnInvulnerabilityAndDeathDoNotTriggerPhase(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(100);
        wither.setInvulnerableTicks(220);
        stepWither(helper, wither);
        helper.assertTrue(reinforcements(helper, wither).isEmpty(), "Spawn charging must not summon reinforcements");
        wither.setInvulnerableTicks(0);
        wither.setHealth(0);
        stepWither(helper, wither);
        helper.assertTrue(reinforcements(helper, wither).isEmpty(), "Dead wither must not summon reinforcements");
        helper.succeed();
    }

    @GameTest
    public void dashRequiresFourMainHeadShotsAndHalfHealth(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        Cow target = createMob(helper, EntityTypes.COW);
        target.setPos(wither.getX() + 6, wither.getY(), wither.getZ());
        wither.setTarget(target);
        WitherDashGoal goal = ((WitherBossAccessor) wither).mobTweaks$getDashGoal();
        for (int i = 0; i < 4; i++) {
            wither.performRangedAttack(target, 1);
        }
        helper.assertTrue(!goal.canUse(), "Full-health wither cannot dash");
        wither.setHealth(100);
        for (int i = 0; i < 3; i++) {
            wither.performRangedAttack(target, 1);
        }
        helper.assertTrue(!goal.canUse(), "Three shots must not trigger a dash");
        wither.performRangedAttack(target, 1);
        helper.assertTrue(goal.canUse(), "Four shots at half health must enable a dash");
        goal.start();
        for (int i = 0; i < 9; i++) {
            goal.tick();
            goal.applyMovement();
            helper.assertTrue(wither.getDeltaMovement().equals(Vec3.ZERO), "Dash must have a windup");
        }
        goal.tick();
        goal.applyMovement();
        helper.assertTrue(Math.abs(wither.getDeltaMovement().length() - 1.5) < 0.001, "Dash speed must be 1.5 blocks/tick");
        for (int i = 0; i < 20; i++) {
            goal.tick();
        }
        helper.assertTrue(!goal.canContinueToUse(), "Dash must finish within 20 active ticks");
        goal.stop();
        helper.assertTrue(!goal.canUse(), "Dash must require four new shots");
        helper.succeed();
    }

    @GameTest
    public void dashMovementSurvivesVanillaWitherSteering(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(100);
        CompoundTag state = save(helper, wither);
        state.putBoolean("MobTweaksReinforcementsSpawned", true);
        load(helper, wither, state);
        Cow target = createMob(helper, EntityTypes.COW);
        target.setPos(wither.getX() + 6, wither.getY(), wither.getZ());
        wither.setTarget(target);
        WitherDashGoal goal = ((WitherBossAccessor) wither).mobTweaks$getDashGoal();
        for (int i = 0; i < 4; i++) {
            goal.recordShot();
        }
        double startX = wither.getX();
        for (int i = 0; i < 12; i++) {
            wither.tickCount++;
            wither.aiStep();
        }
        helper.assertTrue(wither.getX() > startX + 1, "Vanilla steering must not cancel dash movement");
        goal.stop();
        helper.succeed();
    }

    @GameTest
    public void dashHitsEachVictimOnceAndSkipsReinforcements(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(100);
        Mob victim = createMob(helper, EntityTypes.IRON_GOLEM);
        helper.getLevel().addFreshEntity(victim);
        WitherSkeleton skeleton = createMob(helper, EntityTypes.WITHER_SKELETON);
        helper.getLevel().addFreshEntity(skeleton);
        wither.setTarget(victim);
        WitherDashGoal goal = new WitherDashGoal(wither);
        goal.start();
        for (int i = 0; i < 11; i++) {
            goal.tick();
        }
        helper.assertTrue(victim.getHealth() == 70, "Dash must deal 30 base damage");
        helper.assertTrue(skeleton.getHealth() == skeleton.getMaxHealth(), "Dash must not hurt reinforcements");
        victim.setInvulnerableTime(0);
        goal.tick();
        helper.assertTrue(victim.getHealth() == 70, "A victim must only be hit once per dash");
        goal.stop();
        victim.discard();
        skeleton.discard();
        helper.succeed();
    }

    @GameTest
    public void dashCancelsWhenHealedOrTargetLost(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(100);
        wither.setTarget(createMob(helper, EntityTypes.COW));
        WitherDashGoal goal = new WitherDashGoal(wither);
        goal.start();
        helper.assertTrue(goal.canContinueToUse(), "Valid dash must continue");
        wither.setHealth(200);
        helper.assertTrue(!goal.canContinueToUse(), "Above-half-health wither must stop dashing");
        goal.stop();
        wither.setHealth(100);
        goal.start();
        wither.setTarget(null);
        helper.assertTrue(!goal.canContinueToUse(), "Lost target must cancel the dash");
        goal.stop();
        helper.assertTrue(wither.getDeltaMovement().equals(Vec3.ZERO), "Cancelled dash must stop moving");
        helper.succeed();
    }

    @GameTest
    public void dashRespectsMobGriefingAndProtectedBlocks(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        wither.setHealth(100);
        Cow target = createMob(helper, EntityTypes.COW);
        target.setPos(wither.getX() + 6, wither.getY(), wither.getZ());
        wither.setTarget(target);
        BlockPos stone = wither.blockPosition().offset(1, 0, 0);
        BlockPos obsidian = wither.blockPosition().offset(1, 1, 0);
        BlockPos bedrock = wither.blockPosition().offset(1, 2, 0);
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(obsidian, Blocks.OBSIDIAN.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
        boolean original = helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        WitherDashGoal goal = new WitherDashGoal(wither);
        try {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false, helper.getLevel().getServer());
            goal.start();
            for (int i = 0; i < 11; i++) {
                goal.tick();
            }
            helper.assertTrue(helper.getLevel().getBlockState(stone).is(Blocks.STONE), "mobGriefing=false must preserve blocks");
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, true, helper.getLevel().getServer());
            goal.tick();
            helper.assertTrue(helper.getLevel().getBlockState(stone).isAir(), "Dash must break ordinary blocks");
            helper.assertTrue(helper.getLevel().getBlockState(obsidian).is(Blocks.OBSIDIAN), "Dash must preserve obsidian");
            helper.assertTrue(helper.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK), "Dash must preserve wither-immune blocks");
        } finally {
            goal.stop();
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void phaseExplosionRespectsMobGriefing(GameTestHelper helper) {
        WitherBoss wither = createMob(helper, EntityTypes.WITHER);
        BlockPos block = wither.blockPosition().above();
        helper.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
        boolean original = helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        try {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false, helper.getLevel().getServer());
            wither.setHealth(100);
            stepWither(helper, wither);
            helper.assertTrue(helper.getLevel().getBlockState(block).is(Blocks.STONE), "Phase explosion must respect mobGriefing");
            helper.assertTrue(reinforcements(helper, wither).size() == 4, "Disabling block damage must not disable reinforcements");
        } finally {
            reinforcements(helper, wither).forEach(Mob::discard);
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    private static <T extends Mob> T createMob(GameTestHelper helper, EntityType<T> type) {
        T mob = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (mob == null) {
            throw new IllegalStateException("Cannot create test mob " + type);
        }
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        mob.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return mob;
    }

    private static void stepVillager(GameTestHelper helper, Villager villager, int ticks) {
        for (int i = 0; i < ticks; i++) {
            ((VillagerAccessor) villager).mobTweaks$serverAiStep(helper.getLevel());
        }
    }

    private static void stepWither(GameTestHelper helper, WitherBoss wither) {
        wither.tickCount = 1;
        ((WitherBossAccessor) wither).mobTweaks$serverAiStep(helper.getLevel());
    }

    private static List<WitherSkeleton> reinforcements(GameTestHelper helper, WitherBoss wither) {
        return helper.getLevel().getEntitiesOfClass(WitherSkeleton.class, wither.getBoundingBox().inflate(6));
    }

    private static CompoundTag save(GameTestHelper helper, Mob mob) {
        ProblemReporter.Collector reporter = new ProblemReporter.Collector();
        TagValueOutput output = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess());
        mob.saveWithoutId(output);
        helper.assertTrue(reporter.isEmpty(), "Saving must report no problems: " + reporter.getReport());
        return output.buildResult();
    }

    private static void load(GameTestHelper helper, Mob mob, CompoundTag tag) {
        ProblemReporter.Collector reporter = new ProblemReporter.Collector();
        mob.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(reporter.isEmpty(), "Loading must report no problems: " + reporter.getReport());
    }
}
