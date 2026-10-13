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
- Healing does not add to the vanilla breeding food level. Food consumed for
  healing is no longer available for breeding or sharing.
- Poppies, wheat, seeds, and other non-food inventory items are not consumed.

This is a custom food-healing mechanic, not an exact Bedrock port. The
[Minecraft Wiki's healing documentation](https://minecraft.wiki/w/Healing)
describes Bedrock villagers healing after sleep rather than eating to heal.

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

## Installation

1. Install Fabric Loader **0.19.5 or newer** for Minecraft **26.3**.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.3.
3. Place `mob-tweaks-1.1.0.jar` from `build/libs` in your instance's `mods`
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
runs 32 server-side Fabric GameTests in a separate development world.

To run only the gameplay tests:

```powershell
.\gradlew.bat runGameTest
```
