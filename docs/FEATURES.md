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
| Slabs and trapdoors | Walks on closed trapdoors, bottom slabs, thin snow (up to 3 layers) |
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

### Target Selection

| Selector | Matches |
|---|---|
| `hostile` | Anything extending `Monster`, so modded hostile mobs work without a hardcoded list |
| `mobs` | Any living entity |
| Names | Fuzzy matched (substring or up to 2 edits). `zom` matches zombie, `irongole` matches iron_golem |

### Modes

| Mode | Behavior |
|---|---|
| `spam` *(default)* | Hits on an interval, configurable via `/macro attack set spaminterval <ms>` |
| `crit` | Approaches to 2 or 3 blocks, jumps, strikes mid fall for the critical |

> The bot faces its target while attacking. Attacking, following, and pathfinding are mutually exclusive. Starting one stops the others.

---

## Following

`/macro follow <name>` continuously paths to the closest entity matching a mob id or player/custom name, updating the goal as the target moves.

> Your view is never forced toward the target.

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
