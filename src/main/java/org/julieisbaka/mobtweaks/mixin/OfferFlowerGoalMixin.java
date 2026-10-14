package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OfferFlowerGoal.class)
abstract class OfferFlowerGoalMixin {
    @Shadow
    @Final
    private IronGolem golem;

    @Shadow
    private LivingEntity entity;

    @Shadow
    private int tick;

    @Inject(method = "stop", at = @At("HEAD"))
    private void mobTweaks$giveFlowerToBaby(CallbackInfo ci) {
        if (this.tick != 0
                || !(this.entity instanceof Villager villager)
                || !villager.isAlive()
                || !villager.isBaby()
                || !(this.golem.level() instanceof ServerLevel level)
                || !MobTweaksSettings.isEnabled(level, MobTweak.BABY_VILLAGER_FLOWERS)
                || !this.golem.getBoundingBox().inflate(6.0, 2.0, 6.0).intersects(villager.getBoundingBox())) {
            return;
        }

        ItemStack flower = new ItemStack(Items.POPPY);
        if (villager.getInventory().canAddItem(flower)) {
            villager.getInventory().addItem(flower);
        }
    }
}
