
<div align="center">

# ⚡ MacroMod

**A powerful client-side macro system for Minecraft 26.2**

*Hold keys, click mice, teleport with sub-millimeter precision, lock your view, chat, run commands, and loop it all — driven entirely from simple text fil ues.*

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-DBB69B?style=for-the-badge)](https://fabricmc.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-0.161.0+26.2-DBB69B?style=for-the-badge)](https://modrinth.com/mod/fabric-api)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

</div>

---

## 📖 Table of Contents

- [What is MacroMod?](#-what-is-macromod)
- [Features](#-features)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Macro File Location](#-macro-file-location)
- [Command Reference](#-command-reference)
- [Syntax Reference](#-syntax-reference)
  - [The Rules](#the-rules)
  - [Movement & View](#movement--view)
  - [Keys](#keys)
  - [Mouse](#mouse)
  - [Chat & Commands](#chat--commands)
  - [Cycles](#cycles)
  - [Binds](#binds)
  - [Timing](#timing)
  - [Loops](#loops)
- [Examples](#-examples)
- [How It Works](#-how-it-works)
- [Building from Source](#-building-from-source)
- [Project Structure](#-project-structure)
- [Troubleshooting](#-troubleshooting)
- [FAQ](#-faq)
- [Limitations](#-limitations)
- [License](#-license)

---

## 🎯 What is MacroMod?

**MacroMod** is a lightweight, client-side Fabric mod that runs plain-text `.macro` files as in-game action sequences. It chains actions with simple comma-separated syntax, so a whole behaviour reads like English:

```macro
hold w, hold d, wait 10s, release d, wait 10s, release w
```

That one line holds W, holds D, waits ten seconds, releases D, waits ten more, and releases W. No GUI, no config menus — just type `/macro <filename>` in chat and watch it run.

---

## ✨ Features

- 🎮 **Hold, release, or press any key or mouse button**
- 📍 **Precise movement** — `.001` block precision via `goto`
- 👁️ **Precise look** — `.1` degree precision via `look at`
- 🔒 **Snap-back locks** — position and view snap back the instant they drift
- 💬 **Send chat messages and commands**
- 🔁 **Cycle** through lists of chats or commands on each pass
- ⌨️ **Keybinds** — press a key in-game to send a message or advance a cycle
- 🕒 **Waits** with human-friendly units — `500ms`, `0.5s`, `2s`, `1m`
- 🔂 **Loops** — finite (`loop 5`) or infinite (`loop`)
- 📝 **One-line chaining** — commas separate actions, so complex sequences stay readable

---

## 📋 Requirements

| Component | Version |
|---|---|
| **Minecraft** | `26.2` |
| **Fabric Loader** | `0.19.3` or newer |
| **Fabric API** | `0.161.0+26.2` or newer |
| **Java** | **25** (mandatory — 26.2 does not run on Java 21) |

> ⚠️ **Important:** Minecraft 26.1+ ships **unobfuscated** with official Mojang names. This mod builds directly against those names — there are **no Yarn mappings** involved.

---

## 🚀 Installation

1. Install **Fabric Loader 0.19.3+** for Minecraft 26.2 via the [Fabric installer](https://fabricmc.net/use/installer/).
2. Install **Java 25** from [Adoptium](https://adoptium.net/) or your package manager. Verify with `java -version`.
3. Drop these into `.minecraft/mods/`:
   - `macro-mod-1.0.0.jar` (this mod)
   - `fabric-api-0.161.0+26.2.jar` ([download](https://modrinth.com/mod/fabric-api))
4. Launch the **Fabric 26.2** profile.

That's it. The mod is **client-side only** — you don't need it installed on any server.

---

## 🏁 Quick Start

1. Launch Minecraft and join any world (singleplayer or multiplayer).
2. Run `/macro` in chat — the mod auto-creates your `macros/` folder.
3. Create a file at `.minecraft/mods/macros/patrol.macro`:

   ```macro
   loop
     goto "100 64 200", look at "0 / 0", wait 1s
     goto "150.5 70.25 -300.75", look at "180 / 45", wait 1s
   endloop
   ```

4. Back in-game, run `/macro patrol`.
5. Stop any time with `/macro stop`.

---

## 📁 Macro File Location

```
.minecraft/
└── mods/
    └── macros/          ← your .macro files go here
        ├── patrol.macro
        ├── farm.macro
        └── demo.macro
```

The folder is **created automatically** the first time you run `/macro`. The `.macro` extension is optional on the command line:

```bash
/macro patrol          # ✅ resolves to patrol.macro
/macro patrol.macro    # ✅ also works
```

---

## 🕹️ Command Reference

| Command | Description |
|---|---|
| `/macro` | Shows usage and confirms the macros folder location |
| `/macro <filename>` | Runs the specified macro file |
| `/macro stop` | Immediately stops the running macro, releases all keys, and clears binds |

The command is **client-side only** — it never touches the server and works on any server you can join.

---

## 📝 Syntax Reference

### The Rules

Everything you need to know about the syntax, in four bullets:

- **Commas and newlines both separate actions.** `hold w, wait 1s, release w` is exactly the same as three lines.
- **`"quotes"` group a phrase into one argument.** Spaces and commas inside quotes are safe.
- **`|` separates items** inside a `cycle` or `bind ... cycle` list. Do not use commas for that — commas mean "next action."
- **`#` starts a comment.** Anything after it on that line is ignored.

That's the entire grammar. Every directive below fits into that model.

---

### Movement & View

| Directive | Description |
|---|---|
| `goto "10 50 20"` | Teleport once to exact coordinates (`.001` precision) |
| `goto 10 50 20` | Same, without quotes |
| `auto goto "10 50 20"` | Snap-back **position lock** — snap back instantly if you move away |
| `snap goto "10 50 20"` | Alias for `auto goto` |
| `snap_goto 10 50 20` | Legacy alias, still supported |
| `look at "153.5 / 14.2"` | Set view once (`.1` precision) |
| `look "153.5 / 14.2"` | Same, without `at` |
| `look 153.5 14.2` | Same, without quotes or slash |
| `auto look "153.5 / 14.2"` | Snap-back **look lock** — snap back instantly if your view drifts |
| `snap look "153.5 / 14.2"` | Alias for `auto look` |
| `snap_look 153.5 14.2` | Legacy alias, still supported |
| `nolookgoto "10 50 20"` | Pathfind to coordinates **without touching your camera** — walk anywhere while your view stays free. Others see you looking where you're looking |
| `nolookautogoto "10 50 20"` | Same, but the server sees you looking at your active `auto look` direction for the whole walk — perfect for mining while repositioning |

> **Yaw:** `0` = south, `90` = west, `180` = north, `-90`/`270` = east
> **Pitch:** `-90` = straight up, `0` = horizon, `90` = straight down

---

### Keys

| Directive | Description |
|---|---|
| `hold w` | Hold a key down |
| `release w` | Release a held key |
| `press space` | Press and release in the same tick (one-shot) |

**Supported key names:** `w`, `a`, `s`, `d`, `space`, `shift`, `ctrl`, `alt`, `tab`, `enter`, `escape`, `backspace`, `delete`, `up`, `down`, `arrow_left`, `arrow_right`, `f1`–`f25`, single letters `a`–`z`, single digits `0`–`9`.

Long forms (`key hold w`, `key release w`, `key press w`) also work. Legacy underscore forms (`key_hold`, `key_release`, `key_press`) still parse.

---

### Mouse

| Directive | Description |
|---|---|
| `hold left` / `hold right` / `hold middle` | Hold a mouse button |
| `release left` / `release right` / `release middle` | Release a mouse button |
| `click left` / `click right` / `click middle` | Click a mouse button once |

Long forms (`mouse hold left`, `mouse release left`, `mouse click left`) also work, as do legacy underscore forms (`mouse_hold`, `mouse_release`, `mouse_click`).

---

### Chat & Commands

| Directive | Description |
|---|---|
| `chat "hello there"` | Send a chat message |
| `say "hello there"` | Alias for `chat` |
| `cmd "/spawn"` | Run a command (leading slash is optional) |
| `command "/gamemode creative"` | Alias for `cmd` |

---

### Cycles

A **cycle** sends the next message in the list every time it runs. When it reaches the end, it wraps back to the start.

| Directive | Description |
|---|---|
| `cycle chat "hi" \| "bye" \| "hello"` | Sends `hi`, then `bye`, then `hello`, then `hi` again, each time it runs |
| `cycle cmd "/home" \| "/spawn" \| "/warp base"` | Runs the next command each time it runs |

Cycles inside a loop advance naturally on each pass. Cycles bound to a key (see below) advance on each key press.

> **Note:** Use `|` between items, **not commas** — commas end the current action.

---

### Binds

A **bind** triggers when you press a specific key in-game. Binds persist until `/macro stop` or you exit the game. They work even if the macro's main action list has finished — as long as a `loop / wait / endloop` keeps the macro alive.

| Directive | Description |
|---|---|
| `bind R chat "single message"` | Press R → send that message |
| `bind key R chat "single message"` | Same; the word `key` is optional |
| `bind G cmd "/spawn"` | Press G → run that command |
| `bind R cycle chat "hi" \| "bye" \| "hello"` | Press R → send `hi`, next press → `bye`, next → `hello`, then back to `hi` |
| `bind G cycle cmd "/home" \| "/spawn"` | Press G → cycle through commands |

**Supported bind keys:** letters (`A`–`Z`), digits (`0`–`9`), `F1`–`F25`, `space`, `shift`, `ctrl`, `alt`, `tab`, `enter`, `escape`, `backspace`, `delete`, `up`, `down`, `arrow_left`, `arrow_right`.

---

### Timing

| Directive | Description |
|---|---|
| `wait 500ms` | Wait milliseconds |
| `wait 0.5s` | Wait seconds (decimals OK) |
| `wait 2s` | Wait seconds |
| `wait 1m` | Wait minutes |
| `wait 500` | Bare number = milliseconds |

---

### Loops

| Directive | Description |
|---|---|
| `loop` | Infinite loop — runs until `/macro stop` |
| `loop 5` | Finite loop — runs 5 times |
| `endloop` | End the loop block |

Loops can be nested freely.

---

## 💡 Examples

Now that you know the syntax, here's what it can actually do. Copy any of these into a `.macro` file and run it.

### One-line movement chains

```macro
hold w, wait 20s, release w
```

```macro
hold w, hold d, wait 10s, release d, wait 10s, release w
```

### Sprint-jump-attack burst

```macro
hold ctrl, hold w, press space, click left, wait 500ms, click left, wait 500ms, click left, release w, release ctrl
```

### Infinite patrol between two precise points

```macro
loop
  goto "1174.129 54.230 12.320", look at "153.5 / 14.2", wait 1s
  goto "1180.001 55.999 -5.750", look at "-45.0 / -30.5", wait 1s
endloop
```

### Position + view lock (AFK / PvP)

```macro
# Lock position and view
auto goto "1174.129 54.230 12.320", auto look "153.5 / 14.2"

# Hold forward + attack for 5 seconds
hold w, hold left, wait 5s, release w, release left

# Keep the locks armed forever
loop, wait 1s, endloop
```

### Mining loop with intervals

```macro
loop 10
  goto "200 60 300", look at "0 / 90"
  hold left, wait 2s, release left
  goto "202.5 60 302.5", look at "180 / 90"
  hold left, wait 2s, release left
endloop
```

### Nested loops

```macro
# Outer loop runs 3 times, inner loop runs 5 times each
loop 3
  hold w
  loop 5
    press space, wait 500ms
  endloop
  release w, wait 2s
endloop
```

### Chat and command binds

```macro
# Press R to cycle greetings
bind R cycle chat "hi" | "bye" | "hello"

# Press G to cycle commands
bind G cycle cmd "/home" | "/spawn" | "/warp base"

# Press F to send a single message
bind F chat "single message"

# Keep the macro alive so binds stay armed
loop, wait 1s, endloop
```

### Standalone cycle (advances each time the line runs)

```macro
loop 6
  cycle chat "hi" | "bye" | "hello"
  wait 1s
endloop
# Sends: hi, bye, hello, hi, bye, hello
```

### Full demo file

```macro
# ── Locks ──────────────────────────────────
auto goto "1174.129 54.230 12.320"
auto look "153.5 / 14.2"

# ── Binds ──────────────────────────────────
bind R cycle chat "hi" | "bye" | "hello"
bind G cycle cmd "/home" | "/spawn" | "/warp base"
bind F chat "single message"

# ── Scheduled behavior ─────────────────────
hold w, hold d, wait 10s, release d, wait 10s, release w

# ── Keep alive forever ─────────────────────
loop
  wait 1s
endloop
```

---

## 🔬 How It Works

### Client Tick Loop

The macro engine runs on Fabric's `END_CLIENT_TICK` event — **20 times per second**. Each tick it:

1. Processes any scheduled key/mouse releases.
2. Checks all active **binds** and fires them if their key was just pressed.
3. Checks `snap_goto` / `snap_look` locks and corrects any drift beyond tolerance.
4. If the current `wait` has expired, advances to the next action and executes it.

### Precision Guarantees

| Feature | Precision | Mechanism |
|---|---|---|
| `goto` coordinates | **0.001 blocks** | `LocalPlayer#setPos(double, double, double)` |
| `look` angles | **0.1 degrees** | `LocalPlayer#setYaw` / `setPitch` |
| `snap_goto` tolerance | **0.001 blocks** | Per-tick position delta check |
| `snap_look` tolerance | **0.1 degrees** | Per-tick rotation delta check |

### Input Simulation

Key and mouse states are injected via `KeyMapping.set(key, pressed)`, which flips the vanilla key binding directly. The game reads this every tick as if you were physically holding the key — no OS-level synthetic input events are used.

`press` and `click` schedule a hold on the current tick and a release on the next tick, so Minecraft actually registers them (a hold+release within the same tick would be invisible to the game).

### Loops, Binds, and Locks

- **Loops** are tracked with an explicit stack. Infinite loops (`count == -1`) reset the index to the loop start; finite loops decrement a counter.
- **Binds** are registered the first time their `bind` action runs, and then persist independently of the action list until `/macro stop`.
- **Snap locks** are stored as single references (`activeSnapGoto`, `activeSnapLook`). Only one of each can be active at a time — issuing a new one replaces the previous.
- **Nolook pathing** decouples look from movement: during each tick the player rotation is the walking direction (so movement impulses follow the path), and right before the movement packet is sent the rotation swaps to your camera (`nolookgoto`) or your `auto look` lock (`nolookautogoto`), then swaps back. Your camera never snaps, and other players never see your head follow the path. While pathing, the keyboard is locked to the bot's inputs so stray key presses can't derail it.
- If the macro action list reaches the end while any snap lock is active, the macro stays running (idling) so the locks remain armed. Otherwise it stops.

---

## 🔨 Building from Source

### Prerequisites

- **JDK 25** (mandatory)
- Git

### Build

```bash
git clone https://github.com/vdcoolok/macro-mod.git
cd macro-mod
./build.sh
```

The built jar appears in the project root: `./macro-mod-1.0.0.jar`.

### Manual build

```bash
./gradlew build
# → build/libs/macro-mod-1.0.0.jar
```

### Dev run

```bash
./gradlew runClient          # launch a dev instance with the mod
./gradlew idea               # generate IntelliJ project files
./gradlew eclipse            # generate Eclipse project files
```

---

## 🗂️ Project Structure

```
macro-mod/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── build.sh
├── LICENSE
├── README.md
├── .gitignore
└── src/
    └── main/
        ├── java/
        │   └── com/example/macromod/
        │       ├── MacroMod.java        # Client entrypoint + tick hook
        │       ├── MacroCommand.java    # Registers the /macro command
        │       ├── MacroParser.java     # Parses .macro files → actions
        │       ├── MacroExecutor.java   # Runs actions, loops, snaps, binds
        │       ├── MacroAction.java     # Action data model
        │       └── MacroBinding.java    # Keybind state
        └── resources/
            └── fabric.mod.json
```

---

## 🛠️ Troubleshooting

<details>
<summary><b>"Macro file not found"</b></summary>

Make sure the file is inside `.minecraft/mods/macros/` and ends in `.macro`. The mod auto-creates this folder on first run — if you're not sure where it is, run `/macro` once and check the chat for the folder path in the usage hint.
</details>

<details>
<summary><b>"Failed to parse macro"</b></summary>

Check the game log for the exact line that failed. Common causes:

- Missing `endloop` for a `loop`
- A typo in a directive (the parser logs `[MacroMod] Unknown command: <name>`)
- Commas inside an unquoted string (use quotes: `chat "hello, world"`)
- Commas used to separate cycle items instead of `|`
</details>

<details>
<summary><b>Nothing happens when I run the macro</b></summary>

- Verify the macro started — you should see a green "Running macro" message.
- Check that the actions produce visible changes (e.g. `wait 10s` alone does nothing visible).
- If you're on a server with **anti-cheat**, large `goto` teleports may be rejected. Try smaller deltas.
- Confirm Fabric API version matches (`0.161.0+26.2`).
</details>

<details>
<summary><b>Game crashes on launch</b></summary>

Almost always a Java version mismatch. **26.2 requires Java 25.** Verify with `java -version` and reinstall from [Adoptium](https://adoptium.net/) if needed.
</details>

<details>
<summary><b>Keys stay stuck after macro stops</b></summary>

`/macro stop` explicitly releases every key and mouse button the macro pressed. If you force-quit the game mid-macro, keys reset naturally when the game closes.
</details>

<details>
<summary><b>Binds don't fire</b></summary>

Binds persist as long as the macro is running. If the macro's action list has finished and no snap locks are active, the macro stops and the binds are cleared. Add this to the end of your macro to keep it alive:

```macro
loop
  wait 1s
endloop
```
</details>

<details>
<summary><b>Macro doesn't build / build fails</b></summary>

- Confirm Java 25: `./gradlew -version`
- Refresh dependencies: `./gradlew --refresh-dependencies`
- Ensure `fabric_version=0.161.0+26.2` in `gradle.properties`
- Confirm Loom is `1.17-SNAPSHOT` in `gradle.properties`
</details>

---

## ❓ FAQ

<details>
<summary><b>Is this allowed on servers?</b></summary>

MacroMod is a **client-side automation tool**. Many servers consider automation to be cheating — check your server's rules before using it. Anti-cheat systems will typically reject large `goto` teleports anyway.
</details>

<details>
<summary><b>Does it work in singleplayer?</b></summary>

Yes — it works everywhere, because everything it does is local to your client.
</details>

<details>
<summary><b>Does the server need the mod?</b></summary>

No. It's client-only. Servers don't need anything installed.
</details>

<details>
<summary><b>Can I use decimals with commas?</b></summary>

No. Use periods: `1174.129`, not `1174,129`.
</details>

<details>
<summary><b>How precise can my coordinates be?</b></summary>

The parser reads values as Java `double`s — practically 15+ significant digits. In practice, Minecraft's chunk/entity system caps useful precision around `.001`, which is what the example macros use.
</details>

<details>
<summary><b>Can I have multiple snap locks at once?</b></summary>

No — only one `snap_goto` and one `snap_look` can be active. Issuing a new one replaces the previous lock.
</details>

<details>
<summary><b>Why Java 25?</b></summary>

Minecraft 26.2 was compiled against Java 25. Running it with Java 21 or lower will fail at class-load time.
</details>

<details>
<summary><b>Why no Yarn mappings?</b></summary>

Starting with Minecraft 26.1, Mojang ships the game **unobfuscated** with original class/method/field names. Fabric Loom 1.17+ uses these names directly, so the `mappings` line was removed from `build.gradle`.
</details>

<details>
<summary><b>Can I contribute?</b></summary>

Yes! PRs are welcome. Fork, branch, and open a pull request. Please include reproduction steps for bug fixes and a sample `.macro` for new features.
</details>

---

## ⚠️ Limitations

- **Client-side movement only.** `goto` calls `LocalPlayer#setPos`, which moves your local client. Servers with strict anti-cheat will rubber-band you back.
- **Not a cheat bypass.** This is an automation and scripting tool, not an exploit.
- **No scripting language.** The macro format is intentionally simple — no variables, conditionals, or arithmetic.
- **No GUI editor.** Everything is text files. Edit them in your favorite editor.
- **Single active macro.** Running `/macro` while another is running stops the previous one and starts the new one.
- **One `snap_goto`, one `snap_look`.** Multiple simultaneous locks are not supported.

---

## 📜 License

Released under the **MIT License**. See [`LICENSE`](LICENSE) for details.

You're free to use, modify, redistribute, and ship this in modpacks — attribution is appreciated but not required.

---

<div align="center">

**Built for Minecraft 26.2 · Fabric Loader 0.19.3+ · Fabric API 0.161.0+26.2 · Java 25**

*If MacroMod saved you time, consider ⭐ starring the repo — it helps others find it.*

</div>