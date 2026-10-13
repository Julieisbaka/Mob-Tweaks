package org.julieisbaka.mobtweaks.test;

import static org.julieisbaka.mobtweaks.test.AdditionalMobGameTests.createMob;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.gamerules.GameRules;

public class RavagerGameTests {
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
        helper.getLevel().setBlockAndUpdate(egg, Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS, 4));
        ravager.aiStep();
        helper.assertTrue(helper.getLevel().getBlockState(egg).isAir(), "Charging ravager must crush all eggs in its path");
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
        helper.assertTrue(helper.getLevel().getBlockState(egg).is(Blocks.TURTLE_EGG), "Stationary ravager must not crush eggs");
        helper.succeed();
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
