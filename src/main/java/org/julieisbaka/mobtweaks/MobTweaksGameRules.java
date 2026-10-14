package org.julieisbaka.mobtweaks;

import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class MobTweaksGameRules {
    private static final Map<MobTweak, GameRule<Boolean>> RULES = new EnumMap<>(MobTweak.class);

    private MobTweaksGameRules() {
    }

    public static void register() {
        for (MobTweak tweak : MobTweak.values()) {
            GameRule<Boolean> rule = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.MOBS)
                    .buildAndRegister(Identifier.fromNamespaceAndPath("mob_tweaks", tweak.key()));
            RULES.put(tweak, rule);
        }
    }

    public static GameRule<Boolean> get(MobTweak tweak) {
        GameRule<Boolean> rule = RULES.get(tweak);
        if (rule == null) {
            throw new IllegalStateException("Mob Tweaks game rule has not been registered: " + tweak.key());
        }
        return rule;
    }
}
