# Mob Tweaks

A Fabric mod for Minecraft Java Edition **26.3**, by **JulieISBaka**.

## Features

### Illusioners in raids

- Adds exactly one illusioner to every raid wave from wave 5 onward,
  including bonus waves.
- Waves 1-4 are unchanged. Easy raids therefore do not spawn illusioners,
  including their fourth, bonus wave.
- Existing raiders are not replaced.
- Illusioners spawn with their normal bow, spells, and AI, and are registered
  as full raid participants: their health counts toward the boss bar and they
  must be defeated to complete the wave.

### Baby villagers accept iron golem flowers

- When an iron golem completes its normal flower offer to a living baby
  villager nearby, one poppy goes directly into the villager's inventory.
- Uses the vanilla gift range: the golem's bounding box expanded by 6 blocks
  horizontally and 2 vertically.
- Poppies stack with existing poppies and do not replace food or other items.
  If the inventory has no room, the baby does not receive the flower.
- The poppy stays in the normal saved villager inventory; it is not equipped
  in the villager's hand and is not spawned as a dropped item.
- Adult villagers do not accept flowers. Interrupted offers do not give a
  flower. Vanilla copper golem gifting is unchanged.
- This extends the existing randomized, daytime gifting routine rather than
  making golems offer flowers constantly.

### Dye live shulkers

- Right-click a living shulker with any of the 16 dyes, in either hand.
- Its shell immediately changes to that dye's color using vanilla synced
  entity data. The color is saved with the shulker.
- Consumes one dye in Survival; Creative consumes none. Applying the same
  color again does not waste dye. Dead shulkers cannot be dyed.
- Existing sheep dyeing is unchanged.

### Villagers eat to heal

- Injured villagers, including babies, consume food from their inventory.
  There is no hunger meter or hunger prerequisite.
- Healing follows the player's normal regeneration cadence: **1 health point
  (half a heart) every 80 ticks (4 seconds)**.
- One food item funds healing equal to its player food nutrition: bread
  restores up to 5 HP, carrots 3 HP, potatoes 1 HP, and beetroots 1 HP.
  Only edible items with the vanilla villager-food component qualify.
- Healthy, sleeping, dead, and NoAI villagers do not eat for healing.
  Healing never exceeds maximum health.
- Unused nutrition and progress toward the next heal survive saving/reloading.
  If a villager reaches full health, unused nutrition is retained for future
  injuries, but the current heal timer resets.
- Poppies, wheat, seeds, and other non-food inventory items are not consumed.

### Wither half-health reinforcements and dash

- When an active, living wither reaches **50% health or below**, it creates
  a **power-7 explosion** and spawns exactly **4 wither skeletons** with their
  normal stone swords. They spawn after the explosion and inherit its target.
- The explosion/reinforcements happen **once per wither**, even if it heals,
  drops below half health again, or the world is saved and reloaded.
- The initial invulnerable spawning charge and lethal damage do not trigger
  this additional explosion.
- At half health or below, the wither can dash toward its target after every
  **4 main-head skull shots**. Side-head shots do not advance that counter.
- Dashes have a 10-tick windup, then move in a fixed direction at approximately
  **1.5 blocks/tick**, for at most **20 ticks**. They stop on horizontal
  collisions, loss of a valid target, or healing above half health.
- A dash deals **30 base damage**, with normal Java mob-attack difficulty
  scaling for players and normal armor/resistance handling. Each victim can
  be struck only once per dash. Wither-friendly mobs, including the summoned
  wither skeletons, are not struck.
- Successful dash hits apply Wither II for 10 seconds on Normal or 40 seconds
  on Hard, and no Wither effect on Easy.
- Dashes break ordinary blocks in a **6x8x6** volume around the wither,
  preserving obsidian, crying obsidian, and wither-immune blocks. Both the
  phase explosion and dash block damage respect `minecraft:mob_griefing`.

### Difficulty-based wither health

- New withers have **300 HP on Easy**, **450 HP on Normal**, and **600 HP on
  Hard**. Peaceful uses the 300-HP fallback; peaceful monster removal still
  applies.
- Health is set when the wither is created. Changing difficulty later does
  not change an existing wither's current or maximum health.
- Saved withers retain their attributes and current health on reload,
  including existing 300-HP withers from older versions.
- The armor, reinforcement, and dash threshold remains at 50% of actual
  maximum health: 150 HP on Easy, 225 HP on Normal, and 300 HP on Hard.
- Withers built from skulls still undergo their vanilla spawn charge,
  starting at one-third of maximum health. Charge healing scales with maximum
  health so they enter combat at full health; combat regeneration is unchanged.

### Baby zombie jockeys

- Naturally spawned and reinforcement baby zombies have a **15% chance** to
  gain the ability to ride additional mobs. This is one saved roll per baby,
  not a new chance each tick or search.
- Applies to zombies, husks, drowned, zombie villagers, and zombified piglins.
- Successful babies seek the nearest visible available mount within their
  bounding box expanded by 8 blocks horizontally and 4 vertically, approach
  it, and mount when within 2 blocks.
- They keep checking periodically if no mount is available and can seek a
  new mount after dismounting. Both successful and failed rolls persist.
- Supported mounts:
  - Wild wolves, ocelots, and wild cats; tamed wolves/cats are excluded.
  - Pigs, sheep, cows, mooshrooms, foxes, and pandas.
  - Llamas and trader llamas.
  - Spiders and cave spiders.
  - Horses, donkeys, mules, skeleton horses, and zombie horses.
  - Adult zombies and their variants listed above.
- Dead, occupied, already-riding, and NoAI mounts are excluded. Babies do not
  steal a player's mount or stack onto an existing jockey.
- Vanilla rider control handles movement and combat once mounted. This
  feature uses existing mobs, rather than spawning additional mounts.
- Command, spawn-egg, spawner, conversion, and other non-natural/non-
  reinforcement spawn reasons do not receive the additional roll.
- Existing chicken and drowned/nautilus jockeys are preserved.
- The non-zombie mount list is the `mob_tweaks:baby_zombie_mounts` entity-type
  tag in `src/main/resources/data/mob_tweaks/tags/entity_type/`.

## Installation

1. Install Fabric Loader **0.19.5 or newer** for Minecraft **26.3**.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.3.
3. Place `mob-tweaks-1.2.0.jar` from `build/libs` in your instance's `mods`
   directory. Do not install the `-sources.jar`.
4. Remove an older Mob Tweaks JAR when upgrading; do not install two versions.

For single-player, install in the client instance. For multiplayer, install
on the Fabric server; clients do not need Mob Tweaks because all changes use
vanilla entities and inventory data.

Minecraft 26.3 and this project require **Java 25**.

## Building and testing

From this directory in Windows PowerShell:

```powershell
.\gradlew.bat build
```

This compiles the mod, creates the JAR and sources JAR in `build/libs`, and
runs 47 server-side Fabric GameTests in a separate development world.

To run only the gameplay tests:

```powershell
.\gradlew.bat runGameTest
```

New tests cover exact wither spawn health and half-health thresholds on every
difficulty, health persistence across difficulty changes, all 100 jockey
roll outcomes (exactly 15 successes), all supported mount types and zombie
variants, exclusions, retries, vanilla AI mounting, and saved passenger data.
