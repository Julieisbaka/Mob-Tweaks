![Mob Tweaks - familiar mobs, fresh behavior](docs/images/banner.png)

# Mob Tweaks

**Small changes. More personality. A few new surprises.**

Mob Tweaks gives familiar Minecraft mobs new behaviors: foxes bring berries to
their kits, villagers heal with food, horses warn their riders, and withers put
up a tougher fight. Keep the changes you enjoy and switch off the rest.

[Report a bug](https://github.com/Julieisbaka/Mob-Tweaks/issues) ·
[Changelog](CHANGELOG.md)

## What's different?

### A little more life

- **Bat colonies:** Bats appear less often, but spawn in small colonies instead
  of scattered encounters. Roosting bats scatter when nearby blocks break or
  an explosion goes off.
- **Fox families:** Adult foxes gather sweet berries and bring them to nearby kits.
- **Village care:** Baby villagers accept iron golem poppies, and injured villagers
  eat stored food to slowly recover health.
- **Colorful shulkers:** Right-click a shulker with dye to change its shell color.
- **A warning from your horse:** Ridden horses rear and neigh when a hostile mob
  is behind them, without bucking you off.

### Smarter friends and foes

- **Protective golems:** Iron golems try to step between villagers and incoming
  hostile projectiles.
- **Skeleton cover:** Skeletons, strays, and bogged seek cover while reloading bows.
- **Defensive endermen:** At half health, endermen can place their carried block
  between themselves and an attacker.
- **Vex recall:** Vexes return to their evoker when they lose their target or
  stray too far away.

### New challenges

- **Tougher withers:** Difficulty-based health, adjusted birth explosions, a
  one-time explosion with four wither skeletons at half health, and a dash attack.
- **Raid illusioners:** One illusioner joins each raid wave from wave 5 onward.
- **More baby zombie jockeys:** Eligible babies have a 15% chance to ride nearby
  animals or adult zombies.
- **Slimes and magma cubes:** Target snow golems and fit into smaller spawn spaces.
- **Ravagers:** Crush turtle eggs while charging through them.

<details>
<summary>Show the numbers and finer details</summary>

- Villagers recover **half a heart every 4 seconds** while healing from food.
  Bread funds up to 5 HP of healing, carrots 3 HP, and potatoes or beetroots 1 HP.
- Withers start with **300 HP on Easy, 450 HP on Normal, and 600 HP on Hard**.
  Their birth explosions deal up to 37, 72, or 108 HP respectively to an
  unprotected player. At half health, they can dash after four main-head shots.
- Naturally spawned baby zombies and reinforcements can become jockeys. Mounts
  include wild wolves, ocelots, wild cats, pigs, sheep, cows, mooshrooms, foxes,
  pandas, llamas, spiders, cave spiders, horses and their variants, and adult
  zombies and their variants.
- Slimes and magma cubes can spawn in **2x2x2 spaces**, choosing a smaller size
  when needed.
- Fox berry deliveries have a **30-second cooldown**.
- Bat colony spawning allows **one in three vanilla bat-group selections** to
  proceed, targeting **3-6 bats** per colony. Limited space can produce smaller groups.
- Roosting bats react to block breaking and explosions within **12 blocks**,
  flee for **3 seconds**, then return to normal behavior.
- Timings assume the game is running at 20 ticks per second.

</details>

## Install

You'll need **Minecraft Java Edition 26.3**, **Java 25 or newer**,
**Fabric Loader 0.19.5 or newer**, and **Fabric API for 26.3**
(tested with 0.162.0+26.3). Forge and NeoForge are not supported.

1. Set up [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for the same
   Minecraft version.
3. Download the Mob Tweaks mod JAR from its Modrinth project page or you can [build it from source](CONTRIBUTING.md).
4. Place both JARs in your Minecraft instance's `mods` folder. For a dedicated
   server, use the server's `mods` folder instead.
5. Launch the game or server. All tweaks are on by default.

Use the regular mod JAR, not the `-sources.jar` archive.
**Back up your world before adding or updating mods.**

## Make it your own

Don't want a particular change? Turn it off without losing the others.

**In your world**, use a gamerule command. For example, to disable fox berry
deliveries:

```mcfunction
/gamerule mob_tweaks:fox_berry_delivery false
```

Replace `false` with `true` to turn it back on. You'll need cheats enabled or
permission to change gamerules on the server.

**For the whole game instance or server**, edit `config/mob-tweaks.properties`
after the first launch, then restart:

```properties
fox_berry_delivery=false
```

A tweak must be enabled in both places to run. A world command cannot override
a disabled configuration entry.

<details>
<summary>All settings</summary>

Use these keys in the properties file or after `/gamerule mob_tweaks:`.
Every setting defaults to `true`.

| Setting | Controls |
| --- | --- |
| `raid_illusioners` | Illusioners in later raid waves |
| `baby_villager_flowers` | Baby villagers accepting golem poppies |
| `dyeable_shulkers` | Dyeing shulker shells |
| `villager_food_healing` | Villagers healing with stored food |
| `wither_difficulty_health` | Difficulty-based health for newly created withers |
| `wither_birth_explosion` | Adjusted wither birth explosion damage |
| `wither_half_health_reinforcements` | Half-health explosion and reinforcements |
| `wither_dash` | Wither dash attacks |
| `baby_zombie_jockeys` | Extra baby zombie mounts |
| `cubes_attack_snow_golems` | Slimes and magma cubes attacking snow golems |
| `cubes_small_spawning` | Smaller spawn spaces for slimes and magma cubes |
| `ravager_turtle_eggs` | Ravagers crushing turtle eggs |
| `fox_berry_delivery` | Foxes gathering and delivering berries |
| `vex_recall` | Vexes returning to their evoker |
| `iron_golem_projectile_guard` | Golems trying to intercept hostile projectiles |
| `horse_rear_warning` | Mounted horses warning about threats behind them |
| `skeleton_cover` | Bow-wielding skeletons seeking reload cover |
| `enderman_block_defense` | Endermen placing defensive blocks |
| `bat_disturbance_flee` | Roosting bats fleeing block breaking and explosions |
| `bat_colony_spawning` | Less frequent bat encounters with small colonies |

Use only `true` or `false` in the properties file. Missing keys are added
automatically; invalid values prevent startup and produce an error.

</details>

The vanilla `minecraft:mob_griefing` rule also controls fox harvesting, ravager
egg destruction, enderman block placement, and wither block destruction.
Foxes can still deliver berries they already hold when it is off.

Turning a tweak off won't undo changes already made, such as a dyed shulker's
color or a wither's maximum health.

## Need help or have an idea?

Check [existing issues](https://github.com/Julieisbaka/Mob-Tweaks/issues), then
use the bug-report or feature-request form. For bugs, include your mod and
Minecraft versions, what happened, and how to reproduce it. Remove personal
information from logs before sharing them.

Want to contribute code? See [Contributing](CONTRIBUTING.md).

## License

Code and original project artwork are available under the [MIT License](LICENSE).
