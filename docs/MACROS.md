# Writing Macros

A `.macro` file is plain text, one action per line. Place files in `.minecraft/mods/macros/` and run them with `/macro load <name>`. The folder is created on the first `/macro` run.

This page lists every action prefix you can use in a macro file. Full command descriptions are in [USAGE.md](USAGE.md).

---

## Grammar

- Newlines and commas both separate actions
- `"quotes"` group words into one argument, commas inside quotes are safe
- `|` separates items in a `cycle` or `bind ... cycle` list
- `#` starts a comment

---

## Action Prefixes

### Movement

| Prefix | Syntax |
|---|---|
| `goto` | `goto "x y z"` |
| `gotohere` | `gotohere` |
| `auto goto` | `auto goto "x y z"` (alias `snap goto`) |
| `nolookgoto` | `nolookgoto "x y z"` |
| `nolookautogoto` | `nolookautogoto "x y z"` |
| `pathstop` | `pathstop` |

### View

| Prefix | Syntax |
|---|---|
| `look at` | `look at "yaw / pitch"` (alias `look yaw pitch`) |
| `lookhere` | `lookhere` |
| `smoothlookat` | `smoothlookat "yaw / pitch"` (aliases `smoothlook`, `smooth_look`, `lookatsmooth`) |
| `smoothlookhere` | `smoothlookhere` |
| `auto look` | `auto look "yaw / pitch"` (alias `snap look`) |

### Combat

| Prefix | Syntax |
|---|---|
| `attack` | `attack hostile` / `attack passive` / `attack mobs` / `attack <name> [name...]` (alias `kill`) |
| `follow` | `follow <name>` |
| `combatstop` | `combatstop` (aliases `stopcombat`, `stopattack`, `unfollow`) |

### Keys and Mouse

| Prefix | Syntax |
|---|---|
| `hold` | `hold <key>` or `hold <left\|right\|middle>` |
| `release` | `release <key>` or `release <button>` |
| `press` | `press <key>` |
| `click` | `click <left\|right\|middle>` |

### Chat

| Prefix | Syntax |
|---|---|
| `chat` | `chat "message"` (alias `say`) |
| `cmd` | `cmd "/command"` (alias `command`) |

### Cycles and Binds

| Prefix | Syntax |
|---|---|
| `cycle chat` | `cycle chat "msg" \| "msg" \| ...` |
| `cycle cmd` | `cycle cmd "/cmd" \| "/cmd" \| ...` |
| `bind` | `bind <key> chat\|cmd "msg"` or `bind <key> cycle chat\|cmd "a" \| "b"` |

### Timing and Loops

| Prefix | Syntax |
|---|---|
| `wait` | `wait 500ms` / `wait 2s` / `wait 1m` (bare number = milliseconds) |
| `loop` | `loop` (infinite) or `loop <count>` |
| `endloop` | `endloop` |

---

## Smooth Looking

`look at` snaps the view instantly. `smoothlookat` turns the head toward the target over a few ticks instead, and the macro waits for the turn to finish before continuing.

Pathing head turning follows the global setting: `/macro set smoothlook true` turns smoothing on for every automatic head movement, `/macro set smoothlook false` turns it off. Smoothing is on by default. `auto look` and `auto lookhere` are always instant, because they exist to correct drift immediately.

## Values

| Value | Format |
|---|---|
| Coordinates | `x y z`, decimals allowed, quotes recommended. `~` and `~-5` relative forms work in `/macro action add` |
| Yaw / pitch | `yaw / pitch` or `yaw pitch`. Yaw `0` south, `90` west, `180` north, `-90` east. Pitch `-90` up, `0` horizon, `90` down |
| Wait time | `500ms`, `2s`, `1m`, or bare milliseconds |
| Key names | `w a s d space shift ctrl alt tab enter escape backspace delete up down arrow_left arrow_right`, `f1` to `f25`, letters, digits, `inventory`, `q`, `f`, `t`, hotbar `1` to `9` |
| Mouse buttons | `left`, `right`, `middle` |

---

## Skeleton

A commented template showing the common structure:

```macro
# ── move somewhere ─────────────────────
goto "0 70 0"

# ── aim the camera ─────────────────────
look at "0 / 0"

# ── act ────────────────────────────────
hold left, wait 5s, release left

# ── keep alive so binds stay armed ─────
loop
  wait 1s
endloop
```
