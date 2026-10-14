package org.julieisbaka.mobtweaks;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public class FoxBerryDeliveryGoal extends Goal {
    private static final int SEARCH_COOLDOWN = 200;
    private static final int DELIVERY_COOLDOWN = 600;

    private final Fox fox;
    private Fox baby;
    private BlockPos berryBush;
    private int nextSearchTick;
    private boolean delivered;

    public FoxBerryDeliveryGoal(Fox fox) {
        this.fox = fox;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(this.fox.level() instanceof ServerLevel level)) {
            return false;
        }
        if (this.fox.isBaby()
                || !this.fox.isAlive()
                || this.fox.getTarget() != null
                || this.fox.isAggressive()
                || this.fox.isSleeping()
                || this.fox.isSitting()
                || !MobTweaksSettings.isEnabled(level, MobTweak.FOX_BERRY_DELIVERY)) {
            return false;
        }

        ItemStack heldItem = this.fox.getItemBySlot(EquipmentSlot.MAINHAND);
        boolean carryingBerries = heldItem.is(Items.SWEET_BERRIES);
        if (!carryingBerries && this.fox.tickCount < this.nextSearchTick) {
            return false;
        }

        this.baby = this.findNearbyBaby();
        if (this.baby == null) {
            this.nextSearchTick = this.fox.tickCount + SEARCH_COOLDOWN;
            return false;
        }

        if (carryingBerries) {
            this.berryBush = null;
            return true;
        }
        if (!heldItem.isEmpty()
                || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }

        this.berryBush = this.findRipeBerryBush();
        if (this.berryBush == null) {
            this.nextSearchTick = this.fox.tickCount + SEARCH_COOLDOWN;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.delivered
                && this.fox.isAlive()
                && this.baby != null
                && this.baby.isAlive()
                && this.baby.isBaby()
                && this.baby.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                && this.fox.getTarget() == null
                && MobTweaksSettings.isEnabled(this.fox.level(), MobTweak.FOX_BERRY_DELIVERY)
                && !this.fox.isAggressive()
                && !this.fox.isSleeping();
    }

    @Override
    public void start() {
        this.delivered = false;
        this.moveTowardCurrentTarget();
    }

    @Override
    public void stop() {
        this.fox.getNavigation().stop();
        if (!this.delivered && !this.fox.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.SWEET_BERRIES)) {
            this.nextSearchTick = this.fox.tickCount + SEARCH_COOLDOWN;
        }
    }

    @Override
    public void tick() {
        ItemStack heldItem = this.fox.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!heldItem.is(Items.SWEET_BERRIES)) {
            if (!(this.fox.level() instanceof ServerLevel level)
                    || !MobTweaksSettings.isEnabled(level, MobTweak.FOX_BERRY_DELIVERY)
                    || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                this.delivered = true;
                return;
            }

            if (!this.isRipeBerryBush(level, this.berryBush)) {
                this.berryBush = this.findRipeBerryBush();
            }
            if (this.berryBush == null) {
                this.delivered = true;
                return;
            }

            Vec3 bushCenter = Vec3.atBottomCenterOf(this.berryBush);
            if (this.fox.position().distanceToSqr(bushCenter) <= 2.25) {
                var state = level.getBlockState(this.berryBush);
                int age = state.getValue(SweetBerryBushBlock.AGE);
                if (age < 2) {
                    this.berryBush = null;
                    return;
                }

                int count = age == 3 ? 2 + this.fox.getRandom().nextInt(2) : 1 + this.fox.getRandom().nextInt(2);
                level.setBlock(this.berryBush, state.setValue(SweetBerryBushBlock.AGE, age - 1), 2);
                this.fox.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.SWEET_BERRIES, count));
            } else {
                this.fox.getNavigation().moveTo(bushCenter.x, bushCenter.y, bushCenter.z, 1.2);
                return;
            }
        }

        heldItem = this.fox.getItemBySlot(EquipmentSlot.MAINHAND);
        if (this.baby == null || !this.baby.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            this.delivered = true;
            return;
        }

        if (this.fox.distanceToSqr(this.baby) <= 4.0) {
            this.baby.setItemSlot(EquipmentSlot.MAINHAND, heldItem.copy());
            this.fox.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.nextSearchTick = this.fox.tickCount + DELIVERY_COOLDOWN;
            this.delivered = true;
        } else {
            this.fox.getNavigation().moveTo(this.baby, 1.2);
        }
    }

    private Fox findNearbyBaby() {
        return this.fox.level().getEntitiesOfClass(
                        Fox.class,
                        this.fox.getBoundingBox().inflate(16.0),
                        candidate -> candidate.isBaby()
                                && candidate.isAlive()
                                && candidate.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty())
                .stream()
                .min((first, second) -> Double.compare(this.fox.distanceToSqr(first), this.fox.distanceToSqr(second)))
                .orElse(null);
    }

    private BlockPos findRipeBerryBush() {
        BlockPos origin = this.fox.blockPosition();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-12, -4, -12), origin.offset(12, 4, 12))) {
            if (!this.isRipeBerryBush((ServerLevel) this.fox.level(), pos)) {
                continue;
            }

            double distance = this.fox.distanceToSqr(Vec3.atBottomCenterOf(pos));
            if (distance < closestDistance) {
                closest = pos.immutable();
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean isRipeBerryBush(ServerLevel level, BlockPos pos) {
        return pos != null
                && level.getBlockState(pos).is(Blocks.SWEET_BERRY_BUSH)
                && level.getBlockState(pos).getValue(SweetBerryBushBlock.AGE) >= 2;
    }

    private void moveTowardCurrentTarget() {
        if (this.fox.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.SWEET_BERRIES)) {
            if (this.baby != null) {
                this.fox.getNavigation().moveTo(this.baby, 1.2);
            }
        } else if (this.berryBush != null) {
            Vec3 bushCenter = Vec3.atBottomCenterOf(this.berryBush);
            this.fox.getNavigation().moveTo(bushCenter.x, bushCenter.y, bushCenter.z, 1.2);
        }
    }
}
