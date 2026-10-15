package org.julieisbaka.mobtweaks.mixin;

import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import org.julieisbaka.mobtweaks.BatColonySpawning;
import org.julieisbaka.mobtweaks.BatDisturbance;
import org.julieisbaka.mobtweaks.MobTweak;
import org.julieisbaka.mobtweaks.MobTweaksSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bat.class)
abstract class BatMixin extends AmbientCreature {
    @Unique
    private final DynamicGameEventListener<BatDisturbance> mobTweaks$disturbance = new DynamicGameEventListener<>(
            new BatDisturbance((Bat) (Object) this));
    @Unique
    private int mobTweaks$colonySize;

    protected BatMixin(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
    }

    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> action) {
        super.updateDynamicGameEventListener(action);
        if (this.level() instanceof ServerLevel level) {
            action.accept(this.mobTweaks$disturbance, level);
        }
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void mobTweaks$fleeDisturbance(ServerLevel level, CallbackInfo ci) {
        this.mobTweaks$disturbance.getListener().tick(level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (reason == EntitySpawnReason.NATURAL
                && MobTweaksSettings.isEnabled(level, MobTweak.BAT_COLONY_SPAWNING)) {
            BatColonySpawning.Colony colony = result instanceof BatColonySpawning.Colony existing
                    ? existing
                    : new BatColonySpawning.Colony(this.getRandom());
            this.mobTweaks$colonySize = colony.size();
            return colony;
        }
        this.mobTweaks$colonySize = 0;
        return result;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return this.mobTweaks$colonySize > 0
                && MobTweaksSettings.isEnabled(this.level(), MobTweak.BAT_COLONY_SPAWNING)
                        ? this.mobTweaks$colonySize
                        : super.getMaxSpawnClusterSize();
    }
}
