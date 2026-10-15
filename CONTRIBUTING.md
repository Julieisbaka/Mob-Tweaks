# Contributing to Mob Tweaks

For bugs or feature ideas, check [existing issues](https://github.com/Julieisbaka/Mob-Tweaks/issues)
and use the appropriate issue form.

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

## Pull requests

Keep changes focused, add or update game tests for behavior changes, and run
`build` before opening a pull request. Update the [README](README.md) and
[changelog](CHANGELOG.md) when changing documented behavior.

## Artwork

The original pixel-art icon and matching README banner are covered by the
project's [MIT License](LICENSE). They use custom pixel lettering and no
third-party game textures or fonts.

- Mod icon: `src/main/resources/assets/mob_tweaks/icon.png` (512x512 PNG).
- README banner: `docs/images/banner.png` (1200x400 PNG).

Keep the icon path in `fabric.mod.json` in sync if you move or rename it.
