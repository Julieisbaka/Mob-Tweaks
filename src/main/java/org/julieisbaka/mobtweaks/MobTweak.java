package org.julieisbaka.mobtweaks;

public enum MobTweak {
    RAID_ILLUSIONERS("raid_illusioners"),
    BABY_VILLAGER_FLOWERS("baby_villager_flowers"),
    DYEABLE_SHULKERS("dyeable_shulkers"),
    VILLAGER_FOOD_HEALING("villager_food_healing"),
    WITHER_DIFFICULTY_HEALTH("wither_difficulty_health"),
    WITHER_BIRTH_EXPLOSION("wither_birth_explosion"),
    WITHER_HALF_HEALTH_REINFORCEMENTS("wither_half_health_reinforcements"),
    WITHER_DASH("wither_dash"),
    BABY_ZOMBIE_JOCKEYS("baby_zombie_jockeys"),
    CUBES_ATTACK_SNOW_GOLEMS("cubes_attack_snow_golems"),
    CUBES_SMALL_SPAWNING("cubes_small_spawning"),
    RAVAGER_TURTLE_EGGS("ravager_turtle_eggs"),
    FOX_BERRY_DELIVERY("fox_berry_delivery"),
    VEX_RECALL("vex_recall"),
    IRON_GOLEM_PROJECTILE_GUARD("iron_golem_projectile_guard"),
    HORSE_REAR_WARNING("horse_rear_warning"),
    SKELETON_COVER("skeleton_cover"),
    ENDERMAN_BLOCK_DEFENSE("enderman_block_defense");

    private final String key;

    MobTweak(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }
}
