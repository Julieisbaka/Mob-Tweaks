# Mob Tweaks

A Fabric mod for Minecraft Java Edition **26.3**, by **JulieISBaka**.

Mod ID: `mob_tweaks`  
Java package / Maven group: `org.julieisbaka`

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

## Installation

1. Install Fabric Loader **0.19.5 or newer** for Minecraft **26.3**.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 26.3.
3. Place `mob-tweaks-1.0.0.jar` from `build/libs` in your instance's `mods`
   directory. Do not install the `-sources.jar`.

For single-player, install in the client instance. For multiplayer, install
on the Fabric server; clients do not need Mob Tweaks because both changes use
vanilla entities and inventory data.

Minecraft 26.3 and this project require **Java 25**.

## Building and testing

From this directory in Windows PowerShell:

```powershell
.\gradlew.bat build
```

This compiles the mod, creates the JAR and sources JAR in `build/libs`, and
runs 11 server-side Fabric GameTests in a separate development world.

To run only the gameplay tests:

```powershell
.\gradlew.bat runGameTest
```

Tests cover early, late, and bonus raid waves; correct raid membership and
bow equipment; completed and interrupted offers; inventory stacking and
capacity; adult and out-of-range recipients; and copper golem compatibility.

Test-only code is in `src/gametest` and is not shipped in the mod JAR.
