# Usage

---

## Installation

1. Install **Fabric Loader 0.19.3+** for Minecraft 26.2.
2. Install **Java 25**. Minecraft 26.2 does not run on Java 21.
3. Place `fabric-api-0.161.0+26.2.jar` and `macro-mod-1.0.0.jar` in `.minecraft/mods/`.

---

## Commands

### Core

| Command | Description |
|---|---|
| `/macro` | Help and list of your macros |
| `/macro load <name>` | Runs `macros/<name>.macro` |
| `/macro stop` | Stops the macro, releases all keys, stops pathing/attack/follow, clears binds |

### Macro Editor

| Command | Description |
|---|---|
| `/macro create <name>` | Creates an empty macro and enters edit mode |
| `/macro edit <name>` | Opens an existing macro for editing |
| `/macro action add <action>` | Appends a line. Supports `~` relative coordinates and angles, and `here` shortcuts |
| `/macro action remove <n>` | Removes line *n* |
| `/macro action list` | Shows all lines |
| `/macro action move <from> <to>` | Reorders lines |
| `/macro save` | Saves during editing |
| `/macro exit` | Saves and leaves edit mode |

### Combat

| Command | Description |
|---|---|
| `/macro attack <hostile\|mobs\|names>` | Starts the kill routine, names are fuzzy matched |
| `/macro attack set attackmode <spam\|crit>` | Selects the attack style |
| `/macro attack set spaminterval <ms>` | Sets the hit interval for spam mode |
| `/macro follow <name>` | Follows a mob or player by name |
| `/macro combatstop` | Stops attack/follow without stopping the macro |

### Pathfinding

| Command | Description |
|---|---|
| `/macro pathdebug walk <x> <y> <z>` | Tests pathfinding to a coordinate |
| `/macro pathdebug pathxz <x> <z>` | Tests pathfinding to X/Z (any Y) |
| `/macro pathdebug status` / `here` / `cache` / `stop` | Debug info |

> **Shortcuts** available in `/macro action add`: `gotohere`, `lookhere`, `autogotohere`, `autolookhere`, plus `~` prefixes on coordinates and angles.

---

## Macro File Location

```
.minecraft/mods/macros/
    test.macro
    farm.macro
```

The folder is created on the first `/macro` run. Files can also be edited with any text editor while the game is open. `/macro load` reads them fresh every run.

---

## Macro Syntax

### Rules

- Newlines and commas both separate actions
- `"quotes"` group words into one argument, commas inside quotes are safe
- `\|` separates items in a `cycle` or `bind ... cycle` list
- `#` starts a comment

### Movement

| Line | Description |
|---|---|
| `goto "100 70 200"` | Pathfinds to the coordinates. Walks, jumps, climbs, breaks |
| `gotohere` | Pathfinds to your current position (useful when recording with `action add`) |
| `auto goto "100 70 200"` | Position lock, re-paths back when you drift |
| `nolookgoto "100 70 200"` | Pathfinds while your camera stays free |
| `nolookautogoto "100 70 200"` | Pathfinds while the server sees you looking at your `auto look` lock |
| `pathstop` | Stops pathfinding, keeps the macro running |

<details>
<summary><b>Accepted aliases</b></summary>

| Alias | Same as |
|---|---|
| `walk`, `walkto`, `pathfind`, `go` | `goto` |
| `stopwalking`, `stopgoto` | `pathstop` |
| `nolookwalk`, `nolookgo` | `nolookgoto` |
| `nolookautowalk`, `nolocksnapgoto` | `nolookautogoto` |

</details>

### View

| Line | Description |
|---|---|
| `look at "153.5 / 14.2"` | Sets the view once. Also accepts `look 153.5 14.2` without the slash |
| `lookhere` | Records your current view |
| `auto look "153.5 / 14.2"` | Look lock, snaps back when the view drifts |

> **Yaw:** `0` = south (+Z) · `90` = west (−X) · `180` = north (−Z) · `-90` = east (+X)
> **Pitch:** `-90` = straight up · `0` = horizon · `90` = straight down

### Combat

| Line | Description |
|---|---|
| `attack hostile` | Kills the closest hostile mobs until stopped |
| `attack mobs` | Kills the closest living entity |
| `attack zombie skeleton` | Kills the closest zombie or skeleton (fuzzy names) |
| `follow <name>` | Follows a mob or player by name |
| `combatstop` | Stops attack/follow |

> In a macro file, `attack`/`follow` start their routine and the macro continues with the next actions. The routine runs independently each tick. End it later with `combatstop`, or `/macro stop` to stop everything.

<details>
<summary><b>Accepted aliases</b></summary>

| Alias | Same as |
|---|---|
| `kill` | `attack` |
| `stopcombat`, `stopattack`, `unfollow` | `combatstop` |

</details>

### Keys and Mouse

| Line | Description |
|---|---|
| `hold w` / `release w` | Holds or releases a key. Long form `key hold w` also works |
| `press space` | One tick tap |
| `hold left` / `release left` | Mouse buttons: `left`, `right`, `middle`. Long form `mouse hold left` |
| `click left` | Single click |

> **Key names:** `w a s d space shift ctrl alt tab enter escape backspace delete up down arrow_left arrow_right f1` to `f25`, single letters and digits, plus `inventory`, `q`, `f`, `t`, and hotbar slots `1` to `9`.

### Chat

| Line | Description |
|---|---|
| `chat "hello"` | Sends a chat message |
| `cmd "/spawn"` | Runs a command, leading slash optional |

<details>
<summary><b>Accepted aliases</b></summary>

| Alias | Same as |
|---|---|
| `say` | `chat` |
| `command` | `cmd` |

</details>

### Cycles and Binds

```macro
cycle chat "hi" | "bye" | "hello"
bind R chat "one message"
bind G cycle cmd "/home" | "/spawn"
```

- **Cycles** advance one item per run and wrap around
- **Binds** fire on key press and remain active after the macro's action list ends, as long as the macro is still alive

### Timing and Loops

```macro
wait 500ms
wait 2s
wait 1m
loop 5
loop
endloop
```

- A bare `wait 500` means milliseconds
- Loops nest freely, `loop` without a number is infinite

<details>
<summary><b>Legacy forms (still supported)</b></summary>

`key_hold`, `key_release`, `key_press`, `mouse_hold`, `mouse_release`, `mouse_click`, `snap_goto`, `snap_look`

</details>

### Full Example

```macro
# walk to the farm, harvest, then fight anything that came close
goto "12048.7 71.0 -12.3"
look at "12060.2 / 2.4"
hold left, wait 10s, release left
goto "12076.1 71.0 -12.3"
attack hostile
```

> Macro file action prefix reference in [MACROS.md](MACROS.md).
