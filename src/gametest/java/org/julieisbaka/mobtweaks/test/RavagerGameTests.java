package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.gamerules.GameRules;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksGameRules;

public class RavagerGameTests {
    private static final List<Block> TRAMPLE_BLOCKS = List.of(
            Blocks.CARVED_PUMPKIN, Blocks.CHORUS_FLOWER, Blocks.CHORUS_PLANT, Blocks.AZALEA,
            Blocks.OAK_LEAVES, Blocks.JACK_O_LANTERN, Blocks.MELON, Blocks.PUMPKIN, Blocks.FLOWERING_AZALEA);

    @GameTest
    public void ravagerHasSeventyFivePercentKnockbackResistance(GameTestHelper helper) {
        Ravager ravager = createMob(helper, EntityTypes.RAVAGER);
        helper.assertTrue(ravager.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.75,
                "Ravager knockback resistance must be 75 percent");
        helper.succeed();
    }

    @GameTest
    public void chargingRavagerCrushesEntireEggClutch(GameTestHelper helper) {
        Ravager ravager = chargingRavager(helper);
        BlockPos egg = ravager.blockPosition();
        helper.getLevel().setBlockAndUpdate(egg,
                Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, 4));
        ravager.aiStep();
        helper.assertTrue(helper.getLevel().getBlockState(egg).isAir(),
                "Charging ravager must crush all eggs in its path");
        helper.succeed();
    }

    @GameTest
    public void eggCrushingRespectsMobGriefing(GameTestHelper helper) {
        boolean original = helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        try {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false, helper.getLevel().getServer());
            Ravager ravager = chargingRavager(helper);
            BlockPos egg = ravager.blockPosition();
            helper.getLevel().setBlockAndUpdate(egg, Blocks.TURTLE_EGG.defaultBlockState());
            ravager.aiStep();
            helper.assertTrue(helper.getLevel().getBlockState(egg).is(Blocks.TURTLE_EGG),
                    "mobGriefing=false must preserve eggs");
        } finally {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void stationaryRavagerDoesNotCrushEggs(GameTestHelper helper) {
        Ravager ravager = createMob(helper, EntityTypes.RAVAGER);
        ravager.setNoGravity(true);
        ravager.setTarget(createMob(helper, EntityTypes.COW));
        BlockPos egg = ravager.blockPosition();
        helper.getLevel().setBlockAndUpdate(egg, Blocks.TURTLE_EGG.defaultBlockState());
        ravager.aiStep();
        helper.assertTrue(helper.getLevel().getBlockState(egg).is(Blocks.TURTLE_EGG),
                "Stationary ravager must not crush eggs");
        helper.succeed();
    }

    @GameTest
    public void chargingRavagerTramplesFragileBlocks(GameTestHelper helper) {
        Ravager ravager = chargingRavager(helper);
        double x = ravager.getX();
        double y = ravager.getY();
        double z = ravager.getZ();
        BlockPos pos = ravager.blockPosition();
        for (Block block : TRAMPLE_BLOCKS) {
            ravager.setPos(x, y, z);
            ravager.setDeltaMovement(0.4, 0, 0);
            helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            ravager.aiStep();
            helper.assertTrue(helper.getLevel().getBlockState(pos).isAir(),
                    "Charging ravager must trample " + block);
        }
        helper.succeed();
    }

    @GameTest
    public void tramplingRespectsMobGriefingAndItsGameRule(GameTestHelper helper) {
        boolean originalMobGriefing = helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING);
        boolean originalTrampling = helper.getLevel().getGameRules()
                .get(MobTweaksGameRules.get(MobTweak.RAVAGER_TRAMPLING));
        try {
            Ravager ravager = chargingRavager(helper);
            double x = ravager.getX();
            double y = ravager.getY();
            double z = ravager.getZ();
            BlockPos pos = ravager.blockPosition();
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, false, helper.getLevel().getServer());
            helper.getLevel().setBlockAndUpdate(pos, Blocks.PUMPKIN.defaultBlockState());
            ravager.aiStep();
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.PUMPKIN),
                    "mobGriefing=false must preserve blocks");

            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, true, helper.getLevel().getServer());
            helper.getLevel().getGameRules().set(MobTweaksGameRules.get(MobTweak.RAVAGER_TRAMPLING), false,
                    helper.getLevel().getServer());
            ravager.setPos(x, y, z);
            ravager.setDeltaMovement(0.4, 0, 0);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.PUMPKIN.defaultBlockState());
            ravager.aiStep();
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.PUMPKIN),
                    "Disabled trampling game rule must preserve blocks");
        } finally {
            helper.getLevel().getGameRules().set(GameRules.MOB_GRIEFING, originalMobGriefing,
                    helper.getLevel().getServer());
            helper.getLevel().getGameRules().set(MobTweaksGameRules.get(MobTweak.RAVAGER_TRAMPLING),
                    originalTrampling, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void eggCrushingRemainsIndependentOfTrampling(GameTestHelper helper) {
        boolean originalTrampling = helper.getLevel().getGameRules()
                .get(MobTweaksGameRules.get(MobTweak.RAVAGER_TRAMPLING));
        try {
            helper.getLevel().getGameRules().set(MobTweaksGameRules.get(MobTweak.RAVAGER_TRAMPLING), false,
                    helper.getLevel().getServer());
            Ravager ravager = chargingRavager(helper);
            BlockPos egg = ravager.blockPosition();
            helper.getLevel().setBlockAndUpdate(egg, Blocks.TURTLE_EGG.defaultBlockState());
            ravager.aiStep();
            helper.assertTrue(helper.getLevel().getBlockState(egg).isAir(),
                    "Egg crushing must remain enabled when trampling is disabled");
        } finally {
            helper.getLevel().getGameRules().set(MobTweaksGameRules.get(MobTweak.RAVAGER_TRAMPLING),
                    originalTrampling, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public void ravagerIgnoresPlantDamage(GameTestHelper helper) {
        Ravager ravager = createMob(helper, EntityTypes.RAVAGER);
        List<DamageSource> plantDamage = List.of(helper.getLevel().damageSources().sweetBerryBush(),
                helper.getLevel().damageSources().cactus());
        for (DamageSource source : plantDamage) {
            float health = ravager.getHealth();
            boolean applied = ravager.hurtServer(helper.getLevel(), source, 1.0F);
            helper.assertTrue(!applied && ravager.getHealth() == health,
                    "Ravagers must not take sweet berry bush or cactus damage");
        }
        helper.succeed();
    }

    @GameTest
    public void ravagerDoesNotGetWitheredByWitherRoses(GameTestHelper helper) {
        Ravager ravager = createMob(helper, EntityTypes.RAVAGER);
        BlockPos pos = ravager.blockPosition();
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WITHER_ROSE.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!ravager.hasEffect(MobEffects.WITHER),
                    "Ravagers must not get the Wither effect from wither roses");
            helper.succeed();
        });
    }

    private static Ravager chargingRavager(GameTestHelper helper) {
        Ravager ravager = createMob(helper, EntityTypes.RAVAGER);
        ravager.setNoGravity(true);
        var target = createMob(helper, EntityTypes.COW);
        target.setPos(ravager.getX() + 5, ravager.getY(), ravager.getZ());
        ravager.setTarget(target);
        ravager.setDeltaMovement(0.4, 0, 0);
        return ravager;
    }
}
