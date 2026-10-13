package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;
import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.load;
import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.save;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.storage.TagValueOutput;
import org.julieisbaka.mobtweaks.ZombieJockeyGoal;
import org.julieisbaka.mobtweaks.test.mixin.WitherBossAccessor;
import org.julieisbaka.mobtweaks.test.mixin.ZombieAccessor;

public class JockeyAndHealthGameTests {
    @GameTest
    public void witherSpawnHealthMatchesEveryDifficulty(GameTestHelper helper) {
        Difficulty original = helper.getLevel().getDifficulty();
        try {
            for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)) {
                setDifficulty(helper, difficulty);
                WitherBoss wither = createMob(helper, EntityTypes.WITHER);
                float expected = healthFor(difficulty);
                helper.assertTrue(wither.getMaxHealth() == expected && wither.getHealth() == expected,
                        difficulty + " wither must have " + expected + " HP");
                wither.setHealth(expected / 2 + 1);
                helper.assertTrue(!wither.isPowered(), "Armor must not activate above half health");
                wither.setHealth(expected / 2);
                helper.assertTrue(wither.isPowered(), "Armor must activate at difficulty-scaled half health");
                wither.makeInvulnerable();
                helper.assertTrue(wither.getHealth() == expected / 3 && wither.getInvulnerableTicks() == 220,
                        "Built wither spawn charging must use scaled health");
                for (int tick = 1; tick <= 220; tick++) {
                    wither.tickCount = tick;
                    ((WitherBossAccessor) wither).mobTweaks$serverAiStep(helper.getLevel());
                }
                helper.assertTrue(wither.getHealth() == expected && wither.getInvulnerableTicks() == 0,
                        "Built wither must finish its charge at full difficulty-scaled health");
            }
        } finally {
            setDifficulty(helper, original);
        }
        helper.succeed();
    }

    @GameTest
    public void witherSpawnHealthPersistsWhenDifficultyChanges(GameTestHelper helper) {
        Difficulty original = helper.getLevel().getDifficulty();
        try {
            setDifficulty(helper, Difficulty.HARD);
            WitherBoss wither = createMob(helper, EntityTypes.WITHER);
            wither.setHealth(400);
            CompoundTag tag = save(helper, wither);
            setDifficulty(helper, Difficulty.EASY);
            helper.assertTrue(wither.getMaxHealth() == 600 && wither.getHealth() == 400,
                    "Changing difficulty must not change existing withers");
            WitherBoss loaded = createMob(helper, EntityTypes.WITHER);
            load(helper, loaded, tag);
            helper.assertTrue(loaded.getMaxHealth() == 600 && loaded.getHealth() == 400,
                    "Reload at a different difficulty must retain saved health");
            helper.assertTrue(createMob(helper, EntityTypes.WITHER).getMaxHealth() == 300,
                    "New withers must use the new difficulty");
            WitherBoss legacy = createMob(helper, EntityTypes.WITHER);
            legacy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);
            legacy.setHealth(250);
            CompoundTag oldSave = save(helper, legacy);
            setDifficulty(helper, Difficulty.HARD);
            WitherBoss oldLoaded = createMob(helper, EntityTypes.WITHER);
            load(helper, oldLoaded, oldSave);
            helper.assertTrue(oldLoaded.getMaxHealth() == 300 && oldLoaded.getHealth() == 250,
                    "Existing 300-HP withers must not be upgraded on reload");
        } finally {
            setDifficulty(helper, original);
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void everyDifficultyUsesCorrectReinforcementThreshold(GameTestHelper helper) {
        Difficulty original = helper.getLevel().getDifficulty();
        try {
            for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)) {
                setDifficulty(helper, difficulty);
                WitherBoss wither = createMob(helper, EntityTypes.WITHER);
                wither.setHealth(healthFor(difficulty) / 2 + 1);
                wither.tickCount = 1;
                ((WitherBossAccessor) wither).mobTweaks$serverAiStep(helper.getLevel());
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(Mob.class,
                        wither.getBoundingBox().inflate(6), mob -> mob.is(EntityTypes.WITHER_SKELETON)).isEmpty(),
                        "Reinforcements must wait for the scaled threshold");
                wither.setHealth(healthFor(difficulty) / 2);
                ((WitherBossAccessor) wither).mobTweaks$serverAiStep(helper.getLevel());
                List<Mob> skeletons = helper.getLevel().getEntitiesOfClass(Mob.class,
                        wither.getBoundingBox().inflate(6), mob -> mob.is(EntityTypes.WITHER_SKELETON));
                helper.assertTrue(skeletons.size() == 4, "Threshold must summon four skeletons on " + difficulty);
                skeletons.forEach(Mob::discard);
            }
        } finally {
            setDifficulty(helper, original);
        }
        helper.succeed();
    }

    @GameTest
    public void jockeyRollHasExactlyFifteenSuccessfulOutcomes(GameTestHelper helper) {
        Zombie baby = createMob(helper, EntityTypes.ZOMBIE);
        baby.setBaby(true);
        boolean[] seen = new boolean[100];
        int successes = 0;
        for (long seed = 0; seed < 2000; seed++) {
            int expectedRoll = RandomSource.create(seed).nextInt(100);
            baby.getRandom().setSeed(seed);
            ZombieJockeyGoal goal = new ZombieJockeyGoal(baby);
            goal.initialize(EntitySpawnReason.NATURAL);
            CompoundTag state = saveGoal(helper, goal);
            helper.assertTrue(state.getBooleanOr("MobTweaksJockeyRolled", false), "Natural baby must roll");
            boolean eligible = state.getBooleanOr("MobTweaksJockeyEligible", false);
            helper.assertTrue(eligible == (expectedRoll < 15), "Only outcomes 0-14 may enable jockeys");
            if (!seen[expectedRoll]) {
                seen[expectedRoll] = true;
                if (eligible) {
                    successes++;
                }
            }
            goal.initialize(EntitySpawnReason.REINFORCEMENT);
            helper.assertTrue(saveGoal(helper, goal).getBooleanOr("MobTweaksJockeyEligible", false) == eligible,
                    "Repeated initialization must not reroll");
        }
        for (boolean outcome : seen) {
            helper.assertTrue(outcome, "Test must exercise all 100 random outcomes");
        }
        helper.assertTrue(successes == 15, "Exactly 15 of 100 outcomes must grant jockey ability");
        helper.succeed();
    }

    @GameTest
    public void excludedSpawnReasonsAndAdultsDoNotRoll(GameTestHelper helper) {
        for (EntitySpawnReason reason : EntitySpawnReason.values()) {
            Zombie zombie = createMob(helper, EntityTypes.ZOMBIE);
            zombie.setBaby(true);
            ZombieJockeyGoal goal = ((ZombieAccessor) zombie).mobTweaks$getJockeyGoal();
            goal.initialize(reason);
            helper.assertTrue(saveGoal(helper, goal).getBooleanOr("MobTweaksJockeyRolled", false)
                            == (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.REINFORCEMENT),
                    "Only natural and reinforcement babies may roll: " + reason);
        }
        Zombie adult = createMob(helper, EntityTypes.ZOMBIE);
        ZombieJockeyGoal goal = ((ZombieAccessor) adult).mobTweaks$getJockeyGoal();
        goal.initialize(EntitySpawnReason.NATURAL);
        helper.assertTrue(!saveGoal(helper, goal).getBooleanOr("MobTweaksJockeyRolled", false), "Adults must not roll");
        helper.succeed();
    }

    @GameTest
    public void naturalZombieVariantsReceiveSpawnRoll(GameTestHelper helper) {
        for (EntityType<? extends Zombie> type : zombieTypes()) {
            Zombie baby = createMob(helper, type);
            baby.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(baby.blockPosition()),
                    EntitySpawnReason.NATURAL, new Zombie.ZombieGroupData(true, false));
            helper.assertTrue(baby.isBaby(), "Spawn data must create a baby");
            helper.assertTrue(save(helper, baby).getBooleanOr("MobTweaksJockeyRolled", false),
                    "Every natural baby zombie variant must receive a roll: " + type);
            baby.stopRiding();
        }
        helper.succeed();
    }

    @GameTest
    public void allListedMountTypesCanBeRidden(GameTestHelper helper) {
        for (EntityType<? extends Mob> type : mountTypes()) {
            Zombie baby = eligibleBaby(helper);
            Mob mount = createMob(helper, type);
            helper.getLevel().addFreshEntity(mount);
            ZombieJockeyGoal goal = ((ZombieAccessor) baby).mobTweaks$getJockeyGoal();
            helper.assertTrue(goal.isEligibleMount(mount), "Listed mount must be eligible: " + type);
            helper.assertTrue(goal.canUse(), "Goal must find a nearby listed mount: " + type);
            goal.start();
            helper.assertTrue(baby.getVehicle() == mount, "Baby must ride listed mount: " + type);
            helper.assertTrue(mount.getControllingPassenger() == baby, "Baby must control mount: " + type);
            goal.stop();
            baby.stopRiding();
            mount.discard();
        }
        helper.succeed();
    }

    @GameTest
    public void tamedPetsOccupiedDeadAndUnlistedMountsAreRejected(GameTestHelper helper) {
        Zombie baby = eligibleBaby(helper);
        ZombieJockeyGoal goal = ((ZombieAccessor) baby).mobTweaks$getJockeyGoal();
        Wolf wolf = createMob(helper, EntityTypes.WOLF);
        wolf.setTame(true, false);
        Cat cat = createMob(helper, EntityTypes.CAT);
        cat.setTame(true, false);
        helper.assertTrue(!goal.isEligibleMount(wolf) && !goal.isEligibleMount(cat), "Tamed wolves and cats must be excluded");
        Zombie otherBaby = createMob(helper, EntityTypes.HUSK);
        otherBaby.setBaby(true);
        helper.assertTrue(!goal.isEligibleMount(otherBaby), "Baby zombies cannot be mounts");
        for (EntityType<? extends Mob> type : List.of(EntityTypes.CHICKEN, EntityTypes.CAMEL,
                EntityTypes.VILLAGER, EntityTypes.IRON_GOLEM, EntityTypes.SKELETON)) {
            helper.assertTrue(!goal.isEligibleMount(createMob(helper, type)), "Unlisted mount must be excluded: " + type);
        }
        Mob cow = createMob(helper, EntityTypes.COW);
        Zombie rider = createMob(helper, EntityTypes.ZOMBIE);
        rider.startRiding(cow);
        helper.assertTrue(!goal.isEligibleMount(cow), "Occupied mounts must be excluded");
        rider.stopRiding();
        cow.setHealth(0);
        helper.assertTrue(!goal.isEligibleMount(cow), "Dead mounts must be excluded");
        helper.assertTrue(!goal.isEligibleMount(baby), "A baby cannot ride itself");
        helper.succeed();
    }

    @GameTest
    public void jockeyEligibilityPersistsForSuccessAndFailure(GameTestHelper helper) {
        for (boolean eligible : List.of(true, false)) {
            Zombie baby = eligibleBaby(helper);
            CompoundTag state = save(helper, baby);
            state.putBoolean("MobTweaksJockeyEligible", eligible);
            Zombie loaded = createMob(helper, EntityTypes.ZOMBIE);
            load(helper, loaded, state);
            ZombieJockeyGoal goal = ((ZombieAccessor) loaded).mobTweaks$getJockeyGoal();
            goal.initialize(EntitySpawnReason.REINFORCEMENT);
            helper.assertTrue(save(helper, loaded).getBooleanOr("MobTweaksJockeyEligible", false) == eligible,
                    "Save/load and repeated spawn initialization must preserve success or failure");
        }
        helper.succeed();
    }

    @GameTest
    public void eligibleBabyRetriesWithoutRerollWhenMountArrives(GameTestHelper helper) {
        Zombie baby = eligibleBaby(helper);
        ZombieJockeyGoal goal = ((ZombieAccessor) baby).mobTweaks$getJockeyGoal();
        helper.assertTrue(!goal.canUse(), "No nearby mount must not start the goal");
        Mob cow = createMob(helper, EntityTypes.COW);
        helper.getLevel().addFreshEntity(cow);
        boolean found = false;
        for (int i = 0; i < 25; i++) {
            if (goal.canUse()) {
                found = true;
                break;
            }
        }
        helper.assertTrue(found, "Eligible baby must retry and find a newly available mount");
        goal.start();
        helper.assertTrue(baby.getVehicle() == cow, "Retry must actually mount the cow");
        baby.stopRiding();
        goal.stop();
        cow.discard();
        helper.succeed();
    }

    @GameTest
    public void existingChickenJockeyIsNotReplaced(GameTestHelper helper) {
        Zombie baby = eligibleBaby(helper);
        Mob chicken = createMob(helper, EntityTypes.CHICKEN);
        helper.assertTrue(baby.startRiding(chicken), "Test must create a chicken jockey");
        Mob cow = createMob(helper, EntityTypes.COW);
        helper.getLevel().addFreshEntity(cow);
        ZombieJockeyGoal goal = ((ZombieAccessor) baby).mobTweaks$getJockeyGoal();
        helper.assertTrue(!goal.canUse() && baby.getVehicle() == chicken, "Existing chicken jockeys must not be replaced");
        baby.stopRiding();
        cow.discard();
        helper.succeed();
    }

    @GameTest
    public void grownZombieAndFailedRollDoNotMount(GameTestHelper helper) {
        Zombie zombie = eligibleBaby(helper);
        Mob cow = createMob(helper, EntityTypes.COW);
        helper.getLevel().addFreshEntity(cow);
        ZombieJockeyGoal goal = ((ZombieAccessor) zombie).mobTweaks$getJockeyGoal();
        zombie.setBaby(false);
        helper.assertTrue(!goal.canUse(), "Adults must not use baby jockey ability");
        zombie.setBaby(true);
        CompoundTag state = save(helper, zombie);
        state.putBoolean("MobTweaksJockeyEligible", false);
        load(helper, zombie, state);
        helper.assertTrue(!goal.canUse(), "Failed roll must not mount even when a mount exists");
        cow.discard();
        helper.succeed();
    }

    @GameTest
    public void realZombieAiMountsNearbyCow(GameTestHelper helper) {
        Zombie baby = eligibleBaby(helper);
        Mob cow = createMob(helper, EntityTypes.COW);
        helper.getLevel().addFreshEntity(cow);
        baby.aiStep();
        helper.assertTrue(baby.getVehicle() == cow, "Normal zombie AI must start the jockey goal and mount");
        baby.stopRiding();
        ZombieJockeyGoal goal = ((ZombieAccessor) baby).mobTweaks$getJockeyGoal();
        goal.stop();
        cow.discard();
        helper.succeed();
    }

    @GameTest
    public void approachingBabyStopsIfMountBecomesInvalid(GameTestHelper helper) {
        Zombie baby = eligibleBaby(helper);
        Mob cow = createMob(helper, EntityTypes.COW);
        cow.setPos(baby.getX() + 3, baby.getY(), baby.getZ());
        helper.getLevel().addFreshEntity(cow);
        ZombieJockeyGoal goal = ((ZombieAccessor) baby).mobTweaks$getJockeyGoal();
        helper.assertTrue(goal.canUse(), "Must find a mount within search range");
        goal.start();
        helper.assertTrue(!baby.isPassenger() && goal.canContinueToUse(), "Distant mount must require approaching");
        cow.setHealth(0);
        helper.assertTrue(!goal.canContinueToUse(), "Approach must stop if the mount dies");
        goal.stop();
        cow.discard();
        helper.succeed();
    }

    @GameTest
    public void jockeyMountRelationshipIsSerialized(GameTestHelper helper) {
        Zombie baby = eligibleBaby(helper);
        Mob cow = createMob(helper, EntityTypes.COW);
        helper.getLevel().addFreshEntity(cow);
        ZombieJockeyGoal goal = ((ZombieAccessor) baby).mobTweaks$getJockeyGoal();
        helper.assertTrue(goal.canUse(), "Baby must find cow");
        goal.start();
        CompoundTag mountData = save(helper, cow);
        helper.assertTrue(mountData.getListOrEmpty("Passengers").size() == 1,
                "Mount save must include its baby zombie passenger");
        CompoundTag passenger = mountData.getListOrEmpty("Passengers").getCompoundOrEmpty(0);
        helper.assertTrue(passenger.getBooleanOr("MobTweaksJockeyEligible", false)
                        && passenger.getBooleanOr("IsBaby", false),
                "Saved passenger must retain baby status and jockey eligibility");
        baby.stopRiding();
        goal.stop();
        cow.discard();
        helper.succeed();
    }

    private static List<EntityType<? extends Zombie>> zombieTypes() {
        return List.of(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED,
                EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ZOMBIFIED_PIGLIN);
    }

    private static List<EntityType<? extends Mob>> mountTypes() {
        return List.of(EntityTypes.WOLF, EntityTypes.OCELOT, EntityTypes.CAT, EntityTypes.PIG,
                EntityTypes.SHEEP, EntityTypes.COW, EntityTypes.MOOSHROOM, EntityTypes.FOX, EntityTypes.PANDA,
                EntityTypes.LLAMA, EntityTypes.TRADER_LLAMA, EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER,
                EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE, EntityTypes.SKELETON_HORSE,
                EntityTypes.ZOMBIE_HORSE, EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED,
                EntityTypes.ZOMBIE_VILLAGER, EntityTypes.ZOMBIFIED_PIGLIN);
    }

    private static Zombie eligibleBaby(GameTestHelper helper) {
        Zombie zombie = createMob(helper, EntityTypes.ZOMBIE);
        zombie.setBaby(true);
        CompoundTag tag = save(helper, zombie);
        tag.putBoolean("MobTweaksJockeyRolled", true);
        tag.putBoolean("MobTweaksJockeyEligible", true);
        load(helper, zombie, tag);
        return zombie;
    }

    private static CompoundTag saveGoal(GameTestHelper helper, ZombieJockeyGoal goal) {
        ProblemReporter.Collector reporter = new ProblemReporter.Collector();
        TagValueOutput output = TagValueOutput.createWithoutContext(reporter);
        goal.save(output);
        helper.assertTrue(reporter.isEmpty(), "Saving goal must not report problems: " + reporter.getReport());
        return output.buildResult();
    }

    private static float healthFor(Difficulty difficulty) {
        return switch (difficulty) {
            case NORMAL -> 450;
            case HARD -> 600;
            default -> 300;
        };
    }

    private static void setDifficulty(GameTestHelper helper, Difficulty difficulty) {
        helper.getLevel().getServer().getWorldData().setDifficulty(difficulty);
        helper.assertTrue(helper.getLevel().getDifficulty() == difficulty, "Test difficulty must change");
    }
}
