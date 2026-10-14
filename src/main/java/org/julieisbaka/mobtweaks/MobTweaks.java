package org.julieisbaka.mobtweaks;

import net.fabricmc.api.ModInitializer;

public final class MobTweaks implements ModInitializer {
    @Override
    public void onInitialize() {
        MobTweaksConfig.load();
        MobTweaksGameRules.register();
    }
}
