package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.julieisbaka.mobtweaks.BatColonySpawning;
import org.julieisbaka.mobtweaks.BatDisturbance;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksGameRules;
import org.julieisbaka.mobtweaks.test.mixin.BatAccessor;
import org.julieisbaka.mobtweaks.test.mixin.NaturalSpawnerAccessor;

public class BatGameTests {
    @GameTest(padding = 16)
    public void breakingABlockWakesBatsAndMakesThemFlee(GameTestHelper helper) {
        Bat bat = roostingBat(helper);
        try {
            BlockPos broken = bat.blockPosition().east(4);
            helper.getLevel().setBlockAndUpdate(broken, Blocks.STONE.defaultBlockState());
            helper.getLevel().destroyBlock(broken, false);
            helper.assertTrue(!bat.isResting(), "A real block-break event should wake a nearby roosting bat");
            ((BatAccessor) bat).mobTweaks$serverAiStep(helper.getLevel());
            helper.assertTrue(bat.getDeltaMovement().x < 0.0, "Bat should fly away from the broken block");
        } finally {
            bat.discard();
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void explosionsWakeNearbyRoostingBats(GameTestHelper helper) {
        Bat bat = roostingBat(helper);
        try {
            helper.getLevel().explode(null, bat.getX() + 6.0, bat.getY(), bat.getZ(), 1.0F, false,
                    Level.ExplosionInteraction.NONE);
            helper.assertTrue(bat.isAlive() && !bat.isResting(), "A nearby explosion should wake the bat");
            ((BatAccessor) bat).mobTweaks$serverAiStep(helper.getLevel());
            helper.assertTrue(bat.getDeltaMovement().x < 0.0, "Bat should flee away from the explosion");
        } finally {
            bat.discard();
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void batsIgnoreDistantAndUnrelatedEvents(GameTestHelper helper) {
        Bat bat = roostingBat(helper);
        try {
            helper.getLevel().gameEvent(GameEvent.STEP, bat.position().add(2.0, 0.0, 0.0),
                    GameEvent.Context.of(bat));
            helper.assertTrue(bat.isResting(), "Ordinary footsteps should not trigger this tweak");
            helper.getLevel().gameEvent(GameEvent.BLOCK_DESTROY, bat.position().add(13.0, 0.0, 0.0),
                    GameEvent.Context.of(Blocks.STONE.defaultBlockState()));
            helper.assertTrue(bat.isResting(), "Block breaking beyond 12 blocks should not disturb the bat");
            bat.setNoAi(true);
            helper.getLevel().gameEvent(GameEvent.EXPLODE, bat.position().add(2.0, 0.0, 0.0),
                    GameEvent.Context.of(bat));
            helper.assertTrue(bat.isResting(), "NoAI bats must not react to disturbances");
        } finally {
            bat.discard();
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void disturbanceToggleWorksIndependentlyOfColonySpawning(GameTestHelper helper) {
        var rules = helper.getLevel().getGameRules();
        var rule = MobTweaksGameRules.get(MobTweak.BAT_DISTURBANCE_FLEE);
        boolean original = rules.get(rule);
        Bat bat = roostingBat(helper);
        try {
            rules.set(rule, false, helper.getLevel().getServer());
            helper.getLevel().gameEvent(GameEvent.BLOCK_DESTROY, bat.position().add(2.0, 0.0, 0.0),
                    GameEvent.Context.of(Blocks.STONE.defaultBlockState()));
            helper.assertTrue(bat.isResting(), "Disabled disturbance rule should leave the bat roosting");
            helper.assertTrue(rules.get(MobTweaksGameRules.get(MobTweak.BAT_COLONY_SPAWNING)),
                    "Disturbance toggle must not change colony spawning");
            rules.set(rule, true, helper.getLevel().getServer());
            helper.getLevel().gameEvent(GameEvent.BLOCK_DESTROY, bat.position().add(2.0, 0.0, 0.0),
                    GameEvent.Context.of(Blocks.STONE.defaultBlockState()));
            helper.assertTrue(!bat.isResting(), "Re-enabling the rule should activate the existing listener");
            rules.set(rule, false, helper.getLevel().getServer());
            BatDisturbance listener = disturbanceListener(bat);
            bat.setResting(true);
            listener.tick(helper.getLevel());
            helper.assertTrue(bat.isResting(), "Disabling the rule must cancel an active flee");
        } finally {
            rules.set(rule, original, helper.getLevel().getServer());
            bat.discard();
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void disturbedBatsResumeNormalBehaviorAfterThreeSeconds(GameTestHelper helper) {
        Bat bat = roostingBat(helper);
        try {
            helper.getLevel().gameEvent(GameEvent.BLOCK_DESTROY, bat.position().add(2.0, 0.0, 0.0),
                    GameEvent.Context.of(Blocks.STONE.defaultBlockState()));
            BatDisturbance listener = disturbanceListener(bat);
            for (int tick = 0; tick < 60; tick++) {
                bat.setResting(true);
                listener.tick(helper.getLevel());
                helper.assertTrue(!bat.isResting(), "Bat should remain in flight during its 60-tick escape");
                helper.assertTrue(helper.getLevel().noCollision(bat,
                        bat.getBoundingBox().expandTowards(bat.getDeltaMovement())),
                        "Escape movement must not cross solid blocks");
            }
            bat.setResting(true);
            listener.tick(helper.getLevel());
            helper.assertTrue(bat.isResting(), "After 60 ticks the tweak should no longer prevent roosting");
        } finally {
            bat.discard();
        }
        helper.succeed();
    }

    @GameTest
    public void naturallySpawnedBatsShareAThreeToSixBatColonyTarget(GameTestHelper helper) {
        Set<Integer> observedSizes = new HashSet<>();
        RandomSource seeds = RandomSource.create(9823490L);
        for (int seed = 0; seed < 100; seed++) {
            Bat first = createMob(helper, EntityTypes.BAT);
            first.getRandom().setSeed(seeds.nextLong());
            SpawnGroupData data = first.finalizeSpawn(helper.getLevel(),
                    helper.getLevel().getCurrentDifficultyAt(first.blockPosition()), EntitySpawnReason.NATURAL, null);
            helper.assertTrue(data instanceof BatColonySpawning.Colony, "Natural bats should share colony data");
            int size = first.getMaxSpawnClusterSize();
            helper.assertTrue(size >= 3 && size <= 6, "Colony target must be between three and six");
            observedSizes.add(size);
            Bat second = createMob(helper, EntityTypes.BAT);
            SpawnGroupData secondData = second.finalizeSpawn(helper.getLevel(),
                    helper.getLevel().getCurrentDifficultyAt(second.blockPosition()), EntitySpawnReason.NATURAL, data);
            helper.assertTrue(secondData == data && second.getMaxSpawnClusterSize() == size,
                    "Every bat in a natural group should use the same colony target");
        }
        helper.assertTrue(observedSizes.equals(Set.of(3, 4, 5, 6)), "All four colony sizes should be possible");
        helper.succeed();
    }

    @GameTest
    public void colonySpawningDoesNotAffectOtherSpawnReasonsOrDisabledBats(GameTestHelper helper) {
        Bat bat = createMob(helper, EntityTypes.BAT);
        int vanillaLimit = bat.getMaxSpawnClusterSize();
        for (EntitySpawnReason reason : new EntitySpawnReason[] {
                EntitySpawnReason.COMMAND, EntitySpawnReason.SPAWN_ITEM_USE, EntitySpawnReason.SPAWNER }) {
            SpawnGroupData data = bat.finalizeSpawn(helper.getLevel(),
                    helper.getLevel().getCurrentDifficultyAt(bat.blockPosition()), reason, null);
            helper.assertTrue(!(data instanceof BatColonySpawning.Colony)
                    && bat.getMaxSpawnClusterSize() == vanillaLimit,
                    "Non-natural spawns must keep vanilla behavior: " + reason);
        }
        var rule = MobTweaksGameRules.get(MobTweak.BAT_COLONY_SPAWNING);
        boolean original = helper.getLevel().getGameRules().get(rule);
        try {
            helper.getLevel().getGameRules().set(rule, false, helper.getLevel().getServer());
            SpawnGroupData data = bat.finalizeSpawn(helper.getLevel(),
                    helper.getLevel().getCurrentDifficultyAt(bat.blockPosition()), EntitySpawnReason.NATURAL, null);
            helper.assertTrue(!(data instanceof BatColonySpawning.Colony)
                    && bat.getMaxSpawnClusterSize() == vanillaLimit,
                    "Disabled colony spawning must restore vanilla natural group limits");
            helper.assertTrue(helper.getLevel().getGameRules().get(
                    MobTweaksGameRules.get(MobTweak.BAT_DISTURBANCE_FLEE)),
                    "Colony toggle must not change disturbance behavior");
        } finally {
            helper.getLevel().getGameRules().set(rule, original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void batGroupSelectionsAreReducedToOneThird(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 3, 3));
        var level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        RandomSource random = RandomSource.create(12345L);
        int accepted = 0;
        for (int attempt = 0; attempt < 6000; attempt++) {
            var result = NaturalSpawnerAccessor.mobTweaks$selectSpawn(level, level.structureManager(), generator,
                    MobCategory.AMBIENT, random, pos);
            if (result.isPresent()) {
                helper.assertTrue(result.get().type() == EntityTypes.BAT, "Test biome should select bats");
                accepted++;
            }
        }
        helper.assertTrue(accepted >= 1800 && accepted <= 2200,
                "About one third of 6000 bat-group selections should proceed, got " + accepted);
        var rule = MobTweaksGameRules.get(MobTweak.BAT_COLONY_SPAWNING);
        boolean original = level.getGameRules().get(rule);
        try {
            level.getGameRules().set(rule, false, level.getServer());
            for (int attempt = 0; attempt < 100; attempt++) {
                helper.assertTrue(NaturalSpawnerAccessor.mobTweaks$selectSpawn(level, level.structureManager(),
                        generator, MobCategory.AMBIENT, random, pos).isPresent(),
                        "Disabled colony rule must not discard vanilla bat groups");
            }
        } finally {
            level.getGameRules().set(rule, original, level.getServer());
        }
        helper.succeed();
    }

    @GameTest(padding = 48, maxTicks = 200)
    public void naturalSpawnerCreatesColoniesAndHonorsPlacementPredicate(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(3, 3, 3));
        for (BlockPos floor : BlockPos.betweenClosed(center.offset(-32, -1, -32), center.offset(32, -1, 32))) {
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(floor.above(4), Blocks.STONE.defaultBlockState());
        }
        helper.startSequence()
                .thenWaitUntil(
                        () ->                         helper.assertTrue(level.getMaxLocalRawBrightness(center) == 0,
                                "Test cave must be dark, got " + level.getMaxLocalRawBrightness(center)))
                .thenExecute(() -> {
                    ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
                    player.setPos(center.getX() + 48.5, center.getY(), center.getZ() + 0.5);
                    Connection connection = new Connection(PacketFlow.SERVERBOUND);
                    EmbeddedChannel channel = new EmbeddedChannel(connection);
                    new ServerGamePacketListenerImpl(level.getServer(), connection, player,
                            CommonListenerCookie.createInitial(player.getGameProfile(), false));
                    level.addNewPlayer(player);
                    AABB area = new AABB(center).inflate(40);
                    try {
                        var chunk = level.getChunk(center);
                        int[] callbacks = { 0 };
                        level.getRandom().setSeed(12345L);
                        for (int attempt = 0; attempt < 20; attempt++) {
                            NaturalSpawner.spawnCategoryForPosition(MobCategory.AMBIENT, level, chunk, center,
                                    (type, spawnLevel, pos, spawnChunk) -> false,
                                    (mob, spawnChunk) -> callbacks[0]++);
                        }
                        helper.assertTrue(level.getEntitiesOfClass(Bat.class, area).isEmpty() && callbacks[0] == 0,
                                "Placement predicate rejection must prevent all colony spawns");
                        for (int attempt = 0; attempt < 40 && callbacks[0] == 0; attempt++) {
                            NaturalSpawner.spawnCategoryForPosition(MobCategory.AMBIENT, level, chunk, center,
                                    (type, spawnLevel, pos, spawnChunk) -> true,
                                    (mob, spawnChunk) -> callbacks[0]++);
                        }
                        var bats = level.getEntitiesOfClass(Bat.class, area);
                        helper.assertTrue(bats.size() >= 3 && bats.size() <= 6,
                                "Open dark cave should produce a colony of 3-6 bats, got " + bats.size());
                        helper.assertTrue(callbacks[0] == bats.size(),
                                "Every spawned bat must run vanilla spawn accounting");
                        helper.assertTrue(bats.stream().allMatch(bat -> level.noCollision(bat, bat.getBoundingBox())),
                                "Colony bats must fit valid empty positions");
                    } finally {
                        level.getEntitiesOfClass(Bat.class, area).forEach(Bat::discard);
                        player.discard();
                        channel.finishAndReleaseAll();
                    }
                }).thenSucceed();
    }

    private static Bat roostingBat(GameTestHelper helper) {
        Bat bat = createMob(helper, EntityTypes.BAT);
        helper.getLevel().setBlockAndUpdate(bat.blockPosition().above(), Blocks.STONE.defaultBlockState());
        bat.setResting(true);
        helper.getLevel().addFreshEntity(bat);
        return bat;
    }

    private static BatDisturbance disturbanceListener(Bat bat) {
        AtomicReference<BatDisturbance> result = new AtomicReference<>();
        bat.updateDynamicGameEventListener((listener, level) -> {
            if (listener.getListener() instanceof BatDisturbance disturbance) {
                result.set(disturbance);
            }
        });
        if (result.get() == null) {
            throw new IllegalStateException("Bat disturbance listener was not registered");
        }
        return result.get();
    }
}
