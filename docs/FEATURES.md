# Features

---

## Macro Engine

Macros are plain text files executed on the client tick (20 times per second). Each line, or comma separated run of actions, performs one step: holding keys, clicking, pathfinding, chatting, looping, waiting, binding keys.

When the action list finishes, the macro stops. Unless a snap lock is armed, in which case it idles to keep the lock active.

> **`/macro stop`** halts everything at once: the macro, all binds, pathfinding, attack and follow routines, and releases every key and mouse button the mod pressed.

---

## Pathfinding

Baritone style A* over the chunks you have loaded.

### Movement Capabilities

| Capability | Details |
|---|---|
| Walking | Flat walking, diagonals, sprinting with proper run-ups |
| Climbing | 1 block step-ups, safe drops of up to 3 blocks |
| Parkour | Gaps of 1 to 4 blocks with velocity based edge timing |
| Pillaring | Builds upward when a throwaway block is in the hotbar |
| Bucket clutch | Water bucket for large drops when one is available |
| Edge sneaking | Careful movement along cliff ledges |
| Breaking | Digs through blocks when faster than detouring |
| Slabs and trapdoors | Walks on closed trapdoors and bottom slabs, walks through thin snow (up to 3 layers) |
| Swimming | Water sources are walkable, flowing fluid is avoided |

<details>
<summary><b>Unbreakable blocks (never attempted)</b></summary>

Bedrock, barrier, reinforced deepslate, obsidian, crying obsidian, netherite block, end portal frame, spawners, plus anything your tool cannot break.

</details>

### Safety

| Protection | Behavior |
|---|---|
| Hazard avoidance | Cactus, fire, magma, cobwebs, berry bushes, dripstone, powder snow |
| Fall limits | Drops beyond the safe limit force a detour or a bucket placement |
| Blacklisting | Locations and edges where movement failed are avoided temporarily |

### Cost Model

Every movement carries a **time based cost**: walk ticks, jump penalty, break ticks, fall damage penalties. The chosen route is the *fastest* one, not just the shortest.

Paths are recalculated periodically and swapped for a cheaper one when one exists.

### Unloaded Goals

If the goal lies in ungenerated chunks, the bot walks a straight line toward it in render distance sized steps, packing chunks as it goes, then computes the real path once the goal is cached.

> The active path is rendered in world while pathing.

---

## No Look Pathing

| Action | Behavior |
|---|---|
| `nolookgoto` | Walks the path while your camera stays free. Rotation is swapped to the walking direction only for the instant it needs to be, then restored. Nobody ever sees your head follow the path |
| `nolookautogoto` | Same, but the swap uses your active `auto look` lock, so you appear to be looking at one fixed direction the whole time |

---

## Attacking

`/macro attack` finds the closest matching entity, paths to it, hits it until it dies, then immediately retargets the next closest match. Runs until stopped.

> Every command lives under `/macro`. There is no `/combat` command.

### Target Selection

| Selector | Matches |
|---|---|
| `only` | Restricts the routine to exactly the listed targets, no fuzzy matching. The only place the full mob list is suggested |
| `hostile` | Anything extending `Monster`, so modded hostile mobs work without a hardcoded list |
| `passive` | Anything that is not a `Monster` |
| `all` | Any living entity, the same as bare `/macro attack` |
| `set` | Configures attack mode and interval, not a target |
| `namespace:id` | Exact entity type. `minecraft:zombie`, `minecraft:skeleton`, `minecraft:cow` |
| Names | Fuzzy matched (substring or up to 2 edits). `zom` matches zombie, `irongole` matches iron_golem |

`/macro attack` suggests only `set`, `passive`, `hostile`, `all` and `only`. The mob list lives entirely under `only`.

```
/macro attack
/macro attack only minecraft:zombie
/macro attack only minecraft:zombie minecraft:skeleton minecraft:cow
/macro follow only minecraft:zombie
```

`only` is the readable form. It means attack that and that, nothing else, so a typo can never silently widen the target set, and an unrecognised name is rejected up front instead of turning into a silent no-op later. Names still work with it, `only zombie skeleton` resolves to `minecraft:zombie minecraft:skeleton`.

The full entity list comes from the game registry, not from what is loaded in your world, so it includes mobs you have never seen and modded entities. Tab completion drops the `minecraft:` prefix while you type, so `zom` offers `minecraft:zombie` and `mymod:zom` offers `mymod:zombie`. The list only appears under `only`, the plain form suggests the mobs currently around you.

Each target is its own argument, up to four, so completion keeps working after the first mob instead of dying the way a single greedy argument would.

### Modes

| Mode | Behavior |
|---|---|
| `spam` *(default)* | Hits on an interval, configurable via `/macro attack set spaminterval <ms>` |
| `crit` | Jumps and strikes partway down the fall for the critical |

Both the mode and the interval are remembered between launches, as are `/macro set smoothlook`, `/macro set botview` and `/macro set freecam`. They live in `mods/macros/settings.properties` next to your macro files.

> The bot always faces its target and closes to within 4.45 blocks before swinging, in both modes. Outside that range it keeps walking toward the target and swings as soon as it is back in range. In `crit` mode each attempt is a jump followed by a strike once the player is about halfway down the fall, since striking at the apex happens before any fall distance has accumulated and the critical does not register. If the target backs out of range mid jump the strike is skipped and the bot walks in again.

> Attacking, following, and pathfinding are mutually exclusive. Starting one stops the others.

---

## Following

`/macro follow <name>` continuously paths to the closest entity matching a mob id or player/custom name, updating the goal as the target moves.

> Your view is never forced toward the target.

---

## Free Camera

Detaches the rendering camera from your head by taking over the client camera after the game positions it. This is the one that lets you watch the bot instead of being stuck inside it.

| Mode | Behavior |
|---|---|
| `off` | Vanilla first person |
| `orbit` | Camera sits behind you at `fcamdist` and follows your look direction. WASD still moves you |
| `free` | WASD flies the camera and your movement input is suppressed for that tick. Shift is a speed boost, space goes up, scroll zooms |

F6 cycles the modes. The camera keeps your mouse look, so nothing ever fights you for control. The bot's rotation is applied to your player entity only for the ticks it needs and your real view is put back immediately after, which is what lets the free camera and the bot coexist.

It is gated off during `/macro attack` and `/macro kill`. An attack forces the mode back to `off`, and both the keybind and the command refuse to re-enable it while combat is running, because chasing a target needs your actual view. The gate lifts on `/macro combatstop`.

---

## Input Handling

Keys are pressed by flipping the vanilla `KeyMapping` directly, no synthetic OS events.

While a macro holds a key, opening chat or any screen does **not** release it: vanilla clears key states on screen open, and the mod re-applies its forced states. That is why a held left click keeps mining while you type.

> `press` and `click` hold for one tick before releasing. A same tick press+release is invisible to the game.

---

## Binds and Cycles

`bind R chat "hi"` fires on key press and persists after the macro's action list has ended, as long as the macro is still alive (an idle loop or armed snap lock keeps it alive).

Cycles send the next list item on each run and wrap around.

---

## Snap Locks

| Lock | Behavior |
|---|---|
| `auto goto` *(aliases: `snap goto`, `snap_goto`, ...)* | Re-paths back when you drift more than ~1.5 blocks, then fine walks you onto the exact spot |
| `auto look` *(aliases: `snap look`, `snap_look`, ...)* | Snaps your view back whenever it drifts more than 0.1° |

One of each can be active. Issuing a new one replaces the old.
