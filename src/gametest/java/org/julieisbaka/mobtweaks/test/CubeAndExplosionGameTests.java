package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;

import java.util.List;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.julieisbaka.mobtweaks.CubeSpawnClearance;
import org.julieisbaka.mobtweaks.test.mixin.WitherBossAccessor;

public class CubeAndExplosionGameTests {
    @GameTest
    public void cubesDamageSnowGolemsOnContact(GameTestHelper helper) {
        for (EntityType<? extends AbstractCubeMob> type : types()) {
            AbstractCubeMob cube = createMob(helper, type);
            cube.setSize(2, true);
            SnowGolem golem = createMob(helper, EntityTypes.SNOW_GOLEM);
            float before = golem.getHealth();
            cube.push(golem);
            helper.assertTrue(golem.getHealth() < before, "Cube contact must damage snow golem: " + type);
        }
        helper.succeed();
    }

    @GameTest
    public void tinySlimeAndNoAiCubesKeepDamageRules(GameTestHelper helper) {
        AbstractCubeMob slime = createMob(helper, EntityTypes.SLIME);
        slime.setSize(1, true);
        SnowGolem golem = createMob(helper, EntityTypes.SNOW_GOLEM);
        slime.push(golem);
        helper.assertTrue(golem.getHealth() == golem.getMaxHealth(), "Tiny slimes must remain harmless");
        AbstractCubeMob magma = createMob(helper, EntityTypes.MAGMA_CUBE);
        magma.setSize(1, true);
        magma.push(golem);
        helper.assertTrue(golem.getHealth() < golem.getMaxHealth(), "Tiny magma cubes must damage snow golems");
        for (EntityType<? extends AbstractCubeMob> type : types()) {
            AbstractCubeMob cube = createMob(helper, type);
            cube.setSize(2, true);
            cube.setNoAi(true);
            SnowGolem target = createMob(helper, EntityTypes.SNOW_GOLEM);
            cube.push(target);
            helper.assertTrue(target.getHealth() == target.getMaxHealth(), "NoAI cubes must remain harmless");
        }
        helper.succeed();
    }

    @GameTest
    public void cubesTargetSnowGolems(GameTestHelper helper) {
        for (EntityType<? extends AbstractCubeMob> type : types()) {
            AbstractCubeMob cube = createMob(helper, type);
            cube.setSize(2, true);
            SnowGolem target = createMob(helper, EntityTypes.SNOW_GOLEM);
            target.setPos(cube.getX() + 1, cube.getY(), cube.getZ());
            helper.getLevel().addFreshEntity(target);
            for (int i = 0; i < 50 && cube.getTarget() == null; i++) {
                cube.tickCount++;
                cube.aiStep();
            }
            helper.assertTrue(cube.getTarget() == target, "Cube AI must target snow golems: " + type);
            target.discard();
        }
        helper.succeed();
    }

    @GameTest
    public void cubesSpawnAndFitInsideTwoBlockRoom(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(2, 2, 2))) {
            boolean inside = pos.getX() >= origin.getX() && pos.getX() < origin.getX() + 2
                    && pos.getY() >= origin.getY() && pos.getY() < origin.getY() + 2
                    && pos.getZ() >= origin.getZ() && pos.getZ() < origin.getZ() + 2;
            helper.getLevel().setBlockAndUpdate(pos, inside ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
        }
        for (EntityType<? extends AbstractCubeMob> type : types()) {
            AABB spawnBox = CubeSpawnClearance.getSpawnBox(
                    type, helper.getLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            helper.assertTrue(spawnBox.getXsize() == 2 && spawnBox.getYsize() == 2 && spawnBox.getZsize() == 2,
                    "Spawn footprint must be exactly 2x2x2");
            helper.assertTrue(helper.getLevel().noCollision(spawnBox), "Actual two-block room must pass spawn clearance");
            boolean reducedLarge = false;
            for (int i = 0; i < 30; i++) {
                AbstractCubeMob cube = createMob(helper, type);
                cube.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                cube.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(origin), EntitySpawnReason.NATURAL, null);
                helper.assertTrue(helper.getLevel().noCollision(cube, cube.getBoundingBox()), "Spawned cube must actually fit in room");
                helper.assertTrue(cube.getSize() <= 2, "Large cubes must choose a size that fits");
                reducedLarge |= cube.getSize() == 2;
            }
            helper.assertTrue(reducedLarge, "Room must support medium cubes, not just tiny ones");
            helper.getLevel().setBlockAndUpdate(origin.above(), Blocks.STONE.defaultBlockState());
            helper.assertTrue(!helper.getLevel().noCollision(spawnBox), "A blocked two-block room must fail clearance");
            helper.getLevel().setBlockAndUpdate(origin.above(), Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest
    public void largeCubesStillSpawnInOpenSpace(GameTestHelper helper) {
        for (EntityType<? extends AbstractCubeMob> type : types()) {
            boolean sawLarge = false;
            for (int i = 0; i < 40; i++) {
                AbstractCubeMob cube = createMob(helper, type);
                cube.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(cube.blockPosition()),
                        EntitySpawnReason.NATURAL, null);
                sawLarge |= cube.getSize() == 4;
            }
            helper.assertTrue(sawLarge, "Large cubes must still spawn in open space: " + type);
        }
        helper.succeed();
    }

    @GameTest(padding = 16)
    public void actualBirthExplosionDealsExactBedrockDamage(GameTestHelper helper) {
        Difficulty original = helper.getLevel().getDifficulty();
        try {
            for (Difficulty difficulty : List.of(Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD)) {
                helper.getLevel().getServer().getWorldData().setDifficulty(difficulty);
                WitherBoss wither = createMob(helper, EntityTypes.WITHER);
                wither.setInvulnerableTicks(1);
                wither.tickCount = 1;
                ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
                player.setHealth(200);
                player.setPos(wither.getX(), wither.getEyeY(), wither.getZ());
                Connection connection = new Connection(PacketFlow.SERVERBOUND);
                EmbeddedChannel channel = new EmbeddedChannel(connection);
                new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                        CommonListenerCookie.createInitial(player.getGameProfile(), false));
                for (int i = 0; i < 60; i++) {
                    player.connection.tickClientLoadTimeout();
                }
                helper.getLevel().addNewPlayer(player);
                try {
                    ((WitherBossAccessor) wither).mobTweaks$serverAiStep(helper.getLevel());
                    float expected = switch (difficulty) {
                        case EASY -> 37;
                        case HARD -> 108;
                        default -> 72;
                    };
                    helper.assertTrue(Math.abs((200 - player.getHealth()) - expected) < 0.001,
                            "Birth explosion must deal exactly " + expected + " HP on " + difficulty
                                    + ", got " + (200 - player.getHealth()));
                } finally {
                    player.discard();
                    channel.finishAndReleaseAll();
                }
            }
        } finally {
            helper.getLevel().getServer().getWorldData().setDifficulty(original);
        }
        helper.succeed();
    }

    private static List<EntityType<? extends AbstractCubeMob>> types() {
        return List.of(EntityTypes.SLIME, EntityTypes.MAGMA_CUBE);
    }
}
