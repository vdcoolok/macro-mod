<div align="center">

# MacroMod

**Macro and pathing mod for Minecraft 26.2**

*Macros, pathfinding, combat and following, all driven from simple `.macro` files*

[![Release](https://img.shields.io/github/v/release/vdcoolok/macro-mod?style=for-the-badge&color=62B47A)](https://github.com/vdcoolok/macro-mod/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-DBB69B?style=for-the-badge)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.161.0+26.2-DBB69B?style=for-the-badge)](https://modrinth.com/mod/fabric-api)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

[![Issues](https://img.shields.io/github/issues/vdcoolok/macro-mod?style=flat)](https://github.com/vdcoolok/macro-mod/issues)
[![Pull Requests](https://img.shields.io/github/issues-pr/vdcoolok/macro-mod?style=flat)](https://github.com/vdcoolok/macro-mod/pulls)
[![Last Commit](https://img.shields.io/github/last-commit/vdcoolok/macro-mod?style=flat)](https://github.com/vdcoolok/macro-mod/commits)

</div>

---

## Table of Contents

- [Features](#features)
- [What the Pathfinder Handles](#what-the-pathfinder-handles)
- [Documentation](#documentation)
- [Commands](#commands)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Features

| Feature | Description |
|---|---|
| **Macro files** | Keys, mouse buttons, chat, commands, loops, waits, keybinds, cycles |
| **Pathfinding** | A* walking to coordinates. Jumps gaps, climbs, breaks through blocks, avoids cliffs and hazards |
| **Precise locks** | `auto goto` / `auto look` snap back the instant you drift |
| **No-look pathing** | Walk somewhere while your camera stays free, or locked on a fixed view |
| **Attacking** | Find, chase and kill mobs by name or category, with spam or crit mode |
| **Following** | Follow any mob or player as it moves |
| **In-game macro editor** | Create, edit and reorder macros from chat, no file juggling |
| **Debug tooling** | Path status, position, chunk cache inspection and live path rendering |

## What the Pathfinder Handles

- [x] Flat walking, diagonals and sprinting with proper run-ups
- [x] 1 block step-ups and safe 3 block drops
- [x] Parkour across 1 to 4 block gaps
- [x] Pillaring up when you carry throwaway blocks
- [x] Water bucket clutches for big falls
- [x] Edge sneaking along cliff ledges
- [x] Breaking through walls when it beats detouring
- [x] Walking on closed trapdoors, bottom slabs and thin snow
- [x] Swimming
- [x] Hazard avoidance: fire, cactus, magma, cobwebs, dripstone, powder snow
- [x] Exploring toward goals in unloaded chunks
- [x] Live path rendering

> Full breakdown in [docs/FEATURES.md](docs/FEATURES.md).

## Documentation

| File | Contents |
|---|---|
| [docs/FEATURES.md](docs/FEATURES.md) | What each feature does and how it works |
| [docs/USAGE.md](docs/USAGE.md) | Installation, every command, full macro syntax |
| [docs/MACROS.md](docs/MACROS.md) | Macro file prefix reference |
| [docs/BUILDING.md](docs/BUILDING.md) | Building from source and troubleshooting |

## Commands

| Command | Description |
|---|---|
| `/macro load <name>` | Runs a macro file |
| `/macro stop` | Stops everything and releases all inputs |
| `/macro attack <hostile\|mobs\|names>` | Starts the kill routine |
| `/macro follow <name>` | Follows a mob or player |
| `/macro combatstop` | Stops attack/follow only |
| `/macro pathdebug walk <x> <y> <z>` | Tests pathfinding to a coordinate |

> The full command tree and macro file syntax are in [docs/USAGE.md](docs/USAGE.md).

## Installation

1. Install **Fabric Loader 0.19.3+** for Minecraft 26.2.
2. Install **Java 25**. Required by 26.2, older versions will not start the game.
3. Place `macro-mod-1.0.0.jar` and `fabric-api-0.161.0+26.2.jar` in `.minecraft/mods/`.
4. Launch the Fabric 26.2 profile and run `/macro` in game.

## Quick Start

Create `macros/test.macro` inside `.minecraft/mods/`:

```macro
goto 100 70 200
look at 153.5 / 14.2
hold left, wait 5s, release left
goto 0 70 0
attack zombie
```

Run it with `/macro load test`. The `macros/` folder is created automatically on the first `/macro` run.

## Roadmap

- [ ] Mining to a target ore from the chunk cache
- [ ] Automatic tool selection and eating
- [ ] Chest interaction and item sorting in macros
- [ ] Multi goal routes (A then B then C in one run)
- [ ] In-game path editor overlay

## Contributing

Issues and pull requests are welcome. Bug reports should include reproduction steps, new features should come with a sample `.macro` demonstrating them.

## License

Released under the [MIT License](LICENSE).
