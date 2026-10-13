package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyeItem.class)
abstract class DyeItemMixin {
    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void mobTweaks$dyeShulker(ItemStack stack, Player player, LivingEntity target,
                                      InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        DyeColor color = stack.get(DataComponents.DYE);
        if (target instanceof Shulker shulker && shulker.isAlive() && !stack.isEmpty() && color != null) {
            if (shulker.getColor() != color && !shulker.level().isClientSide()) {
                shulker.setComponent(DataComponents.SHULKER_COLOR, color);
                shulker.level().playSound(null, shulker, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                stack.consume(1, player);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
