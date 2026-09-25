# Building

Requires **JDK 25** and Git.

---

## Build

```bash
git clone https://github.com/vdcoolok/macro-mod.git
cd macro-mod
./build.sh
```

The script:

1. Generates the Gradle wrapper if it is missing
2. Builds the project
3. Copies `macro-mod-1.0.0.jar` into the project root

Drop that jar into `.minecraft/mods/` alongside Fabric API.

### Manual Commands

| Command | Purpose |
|---|---|
| `./gradlew build` | Build the jar (output in `build/libs/`) |
| `./gradlew runClient` | Launch a dev client with the mod loaded |
| `./gradlew -version` | Verify the JDK being used |

---

## Toolchain

| Component | Version |
|---|---|
| Minecraft | 26.2 |
| Loom | 1.17 |
| Yarn mappings | None. Since 26.1 the game ships unobfuscated |

All versions live in `gradle.properties`.

---

## Project Structure

```
macro-mod/
├── build.gradle
├── gradle.properties
├── build.sh
├── LICENSE
├── README.md
├── docs/                          # FEATURES, USAGE, MACROS, BUILDING
└── src/main/
    ├── java/com/example/macromod/
    │   ├── MacroMod.java            # client entrypoint, tick hook, chunk caching
    │   ├── MacroCommand.java        # /macro command tree
    │   ├── MacroParser.java         # .macro files to actions
    │   ├── MacroExecutor.java       # action loop, loops, binds, snap locks
    │   ├── MacroAction.java         # action data model
    │   ├── CombatController.java    # attack targeting and hits
    │   ├── FollowController.java    # follow targeting and repathing
    │   ├── TargetRegistry.java      # entity type categorization
    │   ├── FuzzyMatcher.java        # Levenshtein + substring matching
    │   ├── BackgroundInputHandler.java / ForcedInputState.java
    │   ├── NolookController.java    # camera swap during pathing
    │   ├── mixin/                   # KeyboardHandler, KeyMapping, LocalPlayer, GameRenderer
    │   └── path/
    │       ├── behavior/            # PathingBehavior: goal handling, recalc, exploration
    │       ├── calc/                # A* finder, path, executor, CalculationContext
    │       ├── cache/               # chunk packer and block cache
    │       ├── movement/            # one class per movement type
    │       ├── goal/                # GoalBlock, GoalXZ
    │       └── render/              # path rendering and rotation control
    └── resources/
        ├── fabric.mod.json
        └── macromod.mixins.json
```

---

## Troubleshooting

<details>
<summary><b>Build fails with "cannot find symbol" on Minecraft classes</b></summary>

Wrong Java version. Minecraft 26.2 requires **JDK 25**. Verify with `./gradlew -version` and `java -version`.

</details>

<details>
<summary><b>"Macro file not found"</b></summary>

The file must be in `.minecraft/mods/macros/` and end in `.macro`. Run `/macro` once and it prints the folder path.

</details>

<details>
<summary><b>"Failed to parse macro"</b></summary>

Check the game log for the exact line. Common causes:

- A missing `endloop`
- A typo'd directive, logged as `[MacroMod] Unknown command: <name>`
- Commas inside an unquoted string

</details>

<details>
<summary><b>Macro runs but nothing happens</b></summary>

Make sure it actually started. You should see the green **"Running macro"** message. A lone `wait` does nothing visible.

</details>

<details>
<summary><b>Keys stay stuck after stopping</b></summary>

`/macro stop` releases everything. If the game was force closed mid macro, key states reset on relaunch anyway.

</details>

<details>
<summary><b>Game crashes on launch</b></summary>

Almost always a Java version mismatch. **26.2 requires Java 25**.

</details>

<details>
<summary><b>Pathfinding refuses to start</b></summary>

The goal must be within loaded chunks, or reachable in a straight line so the bot can explore toward it. `/macro pathdebug status` shows whether it is calculating.

</details>
