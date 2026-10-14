package org.julieisbaka.mobtweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.illager.Illusioner;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Raid.class)
abstract class RaidMixin {
    @Shadow
    private int groupsSpawned;

    @Shadow
    public abstract void joinRaid(ServerLevel level, int groupNumber, Raider raider, BlockPos pos, boolean exists);

    @Inject(method = "spawnGroup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;updateBossbar()V"))
    private void mobTweaks$spawnIllusioner(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        // Vanilla has incremented the wave number but has not updated the boss bar yet.
        if (this.groupsSpawned < 5 || !MobTweaksSettings.isEnabled(level, MobTweak.RAID_ILLUSIONERS)) {
            return;
        }

        Illusioner illusioner = EntityTypes.ILLUSIONER.create(level, EntitySpawnReason.EVENT);
        if (illusioner == null) {
            throw new IllegalStateException("Mob Tweaks could not create a raid illusioner");
        }
        this.joinRaid(level, this.groupsSpawned, illusioner, pos, false);
    }
}
