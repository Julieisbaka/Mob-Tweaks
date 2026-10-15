# Mob Tweaks

A Fabric mod by **JulieISBaka** that adds configurable mob behaviors to
Minecraft Java Edition **26.3**.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft Java Edition | 26.3 |
| Java | 25 or newer (Java 25 recommended for development) |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | Built and tested with 0.162.0+26.3 |
| Mob Tweaks | 0.1.0 |

This project targets Fabric, not Forge or NeoForge.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3.
2. Install the matching [Fabric API](https://modrinth.com/mod/fabric-api).
3. Obtain the Mob Tweaks mod JAR from the repository's
   [Releases](https://github.com/Julieisbaka/Mob-Tweaks/releases), if a release
   is available, or [build it from source](#building-and-testing).
4. Put the mod JAR and Fabric API in the Minecraft instance's `mods` directory
   (or the server's `mods` directory for a dedicated server).
5. Start the game or server. Mob Tweaks creates its configuration on first startup.

Back up existing worlds before adding or updating mods.

## Mob changes

- **Illusioners:** One joins each raid wave from wave 5 onward.
- **Baby villagers:** Accept iron golem poppies into their inventories.
- **Villagers:** Eat stored food to heal half a heart every 4 seconds.
  Bread restores up to 5 HP, carrots 3 HP, and potatoes or beetroots 1 HP.
- **Shulkers:** Right-click with a dye to change their shell color.
- **Withers:**
  - Spawn with 300 HP on Easy, 450 HP on Normal, and 600 HP on Hard.
  - Birth explosions deal up to 37 HP on Easy, 72 HP on Normal, and 108 HP
    on Hard to an unprotected player.
  - At half health, explode and summon four wither skeletons once.
  - Dash toward their target after four main-head shots while at half health.
- **Baby zombies:** Naturally spawned babies and reinforcements have a 15%
  chance to ride wild wolves, ocelots, wild cats, pigs, sheep, cows,
  mooshrooms, foxes, pandas, llamas, spiders, cave spiders, horses and their
  variants, or adult zombies and their variants.
- **Slimes and magma cubes:** Attack snow golems and can spawn in 2x2x2
  spaces, choosing a smaller size when needed.
- **Ravagers:** Crush turtle eggs when charging through them.
- **Foxes:** Bring sweet berries to nearby kits.
  Deliveries have a 30-second cooldown at 20 ticks per second (600 ticks).
- **Vexes:** Return to their evoker when they lose their target or wander too far away.
- **Iron golems:** Try to move between villagers and incoming hostile projectiles.
- **Horses:** Rear and neigh when a hostile mob approaches from behind, without bucking the rider.
- **Skeletons, strays, and bogged:** Seek cover while reloading their bows.
- **Endermen:** At half health, place a carried block between themselves and an attacker.

## Configuration

Every tweak is enabled by default and has two controls:

- **Global settings:** Edit `config/mob-tweaks.properties` in the game or server
  directory, then restart. Set a key to `true` or `false`. Missing keys are added
  with `true`; invalid boolean values cause startup to fail with an error.
- **World settings:** Use `/gamerule mob_tweaks:<key> false` to disable a tweak
  in the world, or `true` to enable it. Commands require permission to change
  gamerules.

Both controls must be `true` for a tweak to run. A world gamerule cannot override
a globally disabled setting.

For example, to disable fox deliveries globally:

```properties
fox_berry_delivery=false
```

Or disable them in the world:

```mcfunction
/gamerule mob_tweaks:fox_berry_delivery false
```

| Key | Behavior |
| --- | --- |
| `raid_illusioners` | Add an illusioner to raid waves from wave 5 onward |
| `baby_villager_flowers` | Let baby villagers accept iron golem poppies |
| `dyeable_shulkers` | Recolor shulkers with dyes |
| `villager_food_healing` | Let villagers consume stored food to heal |
| `wither_difficulty_health` | Scale newly created withers' health with difficulty |
| `wither_birth_explosion` | Adjust wither birth explosion damage |
| `wither_half_health_reinforcements` | Trigger a one-time explosion and reinforcements at half health |
| `wither_dash` | Let half-health withers dash after four main-head shots |
| `baby_zombie_jockeys` | Let eligible baby zombies ride nearby mobs |
| `cubes_attack_snow_golems` | Let slimes and magma cubes target and damage snow golems |
| `cubes_small_spawning` | Allow slimes and magma cubes to fit smaller spawn spaces |
| `ravager_turtle_eggs` | Let charging ravagers crush turtle eggs |
| `fox_berry_delivery` | Let adult foxes harvest and deliver berries to kits |
| `vex_recall` | Recall vexes to their evoker |
| `iron_golem_projectile_guard` | Let iron golems try to intercept hostile projectiles |
| `horse_rear_warning` | Warn mounted riders about hostile mobs behind their horse |
| `skeleton_cover` | Let bow-wielding skeletons seek cover while reloading |
| `enderman_block_defense` | Let low-health endermen place carried blocks defensively |

The vanilla `minecraft:mob_griefing` gamerule also controls fox berry harvesting,
ravager egg destruction, enderman defensive block placement, and wither block
destruction. Foxes can still deliver berries they already hold when it is disabled.
Disabling a tweak does not undo changes already made, such as shulker colors or
a wither's maximum health.

## Building and testing

Install JDK 25 and make sure `JAVA_HOME` points to it. The Gradle wrapper downloads
the required Gradle version; no system Gradle installation is needed. The first
build needs internet access to resolve Minecraft and Fabric dependencies.

On Windows (PowerShell):

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```sh
sh gradlew build
```

`build` compiles the mod, runs the Fabric server game tests through `check`, and
creates archives in `build/libs`. Install `mob-tweaks-0.1.0.jar`, not the
`-sources.jar` archive.

To run just the game-test task on Windows:

```powershell
.\gradlew.bat runGameTest
```

On Linux or macOS, use `sh gradlew runGameTest`. Game tests run in a separate
development server and do not use your normal Minecraft saves.

## Reporting bugs and contributing

Search [existing issues](https://github.com/Julieisbaka/Mob-Tweaks/issues) before
opening a report. Use the bug-report form and include versions, reproduction
steps, relevant settings, other installed mods, and sanitized logs or crash reports.
Use the feature-request form for new ideas.

For code contributions, keep changes focused, add or update game tests for
behavior changes, and run `build` before opening a pull request. Update this README
and the [changelog](CHANGELOG.md) when changing documented behavior.

## License

Mob Tweaks is licensed under the [MIT License](LICENSE).
