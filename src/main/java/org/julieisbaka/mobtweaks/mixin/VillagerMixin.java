package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
abstract class VillagerMixin extends AbstractVillager {
    @Unique
    private int mobTweaks$healingFood;
    @Unique
    private int mobTweaks$healingTicks;

    protected VillagerMixin(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void mobTweaks$healFromFood(ServerLevel level, CallbackInfo ci) {
        if (!this.isAlive() || this.isNoAi() || this.isSleeping() || this.getHealth() >= this.getMaxHealth()) {
            this.mobTweaks$healingTicks = 0;
            return;
        }
        if (this.mobTweaks$healingFood == 0) {
            for (int slot = 0; slot < this.getInventory().getContainerSize(); slot++) {
                ItemStack stack = this.getInventory().getItem(slot);
                FoodProperties food = stack.get(DataComponents.FOOD);
                if (stack.has(DataComponents.VILLAGER_FOOD) && food != null && food.nutrition() > 0) {
                    this.mobTweaks$healingFood = food.nutrition();
                    this.getInventory().removeItem(slot, 1);
                    this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                    break;
                }
            }
        }
        if (this.mobTweaks$healingFood > 0 && ++this.mobTweaks$healingTicks >= 80) {
            this.heal(1.0F);
            this.mobTweaks$healingFood--;
            this.mobTweaks$healingTicks = 0;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void mobTweaks$saveHealing(ValueOutput output, CallbackInfo ci) {
        output.putInt("MobTweaksHealingFood", this.mobTweaks$healingFood);
        output.putInt("MobTweaksHealingTicks", this.mobTweaks$healingTicks);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void mobTweaks$loadHealing(ValueInput input, CallbackInfo ci) {
        this.mobTweaks$healingFood = Math.max(0, input.getIntOr("MobTweaksHealingFood", 0));
        this.mobTweaks$healingTicks = Math.clamp(input.getIntOr("MobTweaksHealingTicks", 0), 0, 79);
    }
}
