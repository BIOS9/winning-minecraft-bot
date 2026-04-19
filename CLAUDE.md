# Y-Rush Winner Bot — Claude Code Instructions

## Project Overview

A **client-side Fabric mod** for Minecraft 1.21.11 that plays the **Y-Rush minigame** autonomously.
Y-Rush: reach a target Y coordinate before opponent players/bots. Games last up to ~4 minutes.
The target Y is 10–50 blocks above or below spawn, chosen randomly each round.

This bot takes a **classical AI approach** — time-weighted decision trees, A* pathfinding, and
heuristic strategy selection. No machine learning.

The mod loads into a normal Minecraft client and **takes over the player's controls** when
activated, so the owner can spectate and record a screencapture of the bot playing as them.
It must also work in a headless-mc client for automated match running.

---

## Tech Stack

| Layer            | Choice                                                                 |
|------------------|------------------------------------------------------------------------|
| Language         | Java 21                                                                |
| Mod loader       | Fabric (latest stable for MC 1.21.11)                                  |
| Build            | Gradle with Fabric Loom                                                |
| Pathfinding      | Custom A* (see Navigator section). Baritone is an *optional* drop-in.  |
| CI               | GitHub Actions                                                         |
| Dependency mgmt  | Dependabot                                                             |

**IMPORTANT — version pinning:** Before writing any `build.gradle` or `gradle.properties`,
look up the correct versions for Minecraft 1.21.11:
- Fabric Loader version
- Yarn mappings version
- Fabric API version

Do not guess or hallucinate version strings. Check https://fabricmc.net/develop/ or the
Fabric meta API at https://meta.fabricmc.net/v2/versions.

---

## Package & Mod Metadata

- **Root package:** `ciaassured.yrushwinner`
- **Mod ID:** `yrushwinner`
- **Mod name:** Y-Rush Winner
- **Main class:** `ciaassured.yrushwinner.YRushWinnerMod`
- **Client init class:** `ciaassured.yrushwinner.YRushWinnerClient`

---

## Repository Layout

```
yrush-winner/
├── CLAUDE.md
├── build.gradle
├── gradle.properties          ← mc version, loader version, yarn version, fabric-api version
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/ciaassured/yrushwinner/
│   │   │   ├── YRushWinnerMod.java
│   │   │   ├── YRushWinnerClient.java
│   │   │   ├── game/
│   │   │   │   ├── GameDetector.java
│   │   │   │   ├── GameState.java
│   │   │   │   └── RoundInfo.java
│   │   │   ├── strategy/
│   │   │   │   ├── Choice.java                ← core interface
│   │   │   │   ├── ChoiceResult.java          ← SUCCESS / FAILED_RETRY / FAILED_ABORT
│   │   │   │   ├── BotContext.java            ← shared runtime state passed to choices
│   │   │   │   ├── BotController.java         ← state machine + tree walker
│   │   │   │   ├── EnvironmentScanner.java    ← scans chunk data, builds candidate tree
│   │   │   │   ├── DecisionTree.java          ← tree of Choice nodes, supports backtracking
│   │   │   │   └── choices/
│   │   │   │       ├── CompositeChoice.java   ← sequences choices, sums time estimates
│   │   │   │       ├── ClimbHillChoice.java
│   │   │   │       ├── TowerUpChoice.java
│   │   │   │       ├── DescendCaveChoice.java
│   │   │   │       ├── DigDownChoice.java
│   │   │   │       ├── SwimDownChoice.java
│   │   │   │       ├── GatherBlocksChoice.java
│   │   │   │       └── CraftPickaxeChoice.java
│   │   │   ├── navigation/
│   │   │   │   ├── Navigator.java             ← interface
│   │   │   │   ├── AStarNavigator.java        ← primary implementation
│   │   │   │   ├── NavNode.java
│   │   │   │   └── TimeCostModel.java         ← all time constants live here
│   │   │   ├── input/
│   │   │   │   ├── InputController.java       ← intercepts + injects Fabric inputs
│   │   │   │   └── BotToggleKeybind.java      ← keybind to enable/disable bot mid-game
│   │   │   ├── network/
│   │   │   │   └── YRushPacketHandler.java    ← receives custom server packet with targetY
│   │   │   ├── safety/
│   │   │   │   └── AirManager.java            ← breath timer, interrupts executor if needed
│   │   │   └── util/
│   │   │       ├── InventoryUtil.java
│   │   │       ├── BlockUtil.java
│   │   │       ├── ChunkScanner.java          ← helpers for scanning loaded chunk data
│   │   │       └── ChatUtil.java
│   │   └── resources/
│   │       ├── fabric.mod.json
│   │       └── yrushwinner.mixins.json
│   ├── test/
│   │   └── java/ciaassured/yrushwinner/
│   │       ├── navigation/
│   │       │   └── AStarNavigatorTest.java
│   │       ├── strategy/
│   │       │   ├── DecisionTreeTest.java
│   │       │   └── CompositeChoiceTest.java
│   │       ├── safety/
│   │       │   └── AirManagerTest.java
│   │       └── timecost/
│   │           └── TimeCostModelTest.java
│   └── calibration/
│       └── java/ciaassured/yrushwinner/calibration/
│           ├── CalibrationRunner.java
│           ├── CalibrationReport.java
│           └── scenarios/
│               ├── MiningScenario.java
│               ├── MovementScenario.java
│               └── TowerScenario.java
├── .github/
│   ├── dependabot.yml
│   └── workflows/
│       ├── build.yml
│       └── release.yml
└── docs/
    └── calibration-results/   ← committed calibration report outputs
```

---

## The Game

### How a round works
1. Server teleports the player to a random location (surface, cave, underwater, any biome).
2. A custom server packet arrives with `targetY` (an integer). See Network section.
3. The 4-minute game timer starts immediately. No pre-game analysis window.
4. First player to reach `targetY` wins. Dying = eliminated (spectator mode, no respawn).
5. Rounds are independent. There are infinite rounds; the game restarts automatically.

### Starting inventory
- **Surface / exposed spawn:** empty inventory.
- **Cave spawn (detected server-side):** one wooden pickaxe in inventory.
  This is significant — crafting a pickaxe from scratch is slow, so cave spawns
  have a different optimal strategy profile.

### Environment variability
The world is randomly generated vanilla Minecraft terrain (possibly amplified). The bot may
spawn on a mountain, in a cave, underwater, in any biome. It cannot predict the environment
before it arrives.

### Sabotage
Out of scope for the initial version. Do not implement.

---

## Core Design Philosophy

### Time is the only currency
All costs are in **seconds of real time**, not blocks or distance. The bot does not optimise
for shortest path — it optimises for fastest path to `targetY`.

### Plan completely before committing
The `EnvironmentScanner` builds a complete composite plan before the bot takes its first step.
"Climb hill" is not a strategy. "Grab 8 dirt blocks (est. 4.2s) → sprint to hill base (est. 6.1s)
→ climb hill to Y+18 (est. 9.0s) → tower 12 blocks (est. 7.8s) = 27.1s total" is a strategy.

The scanner evaluates several complete composite plans and picks the lowest total time estimate.

### Exception-driven replanning only
Replanning only happens when execution genuinely fails — hitting an unexpected lava lake,
a cave that dead-ends, a block that cannot be broken. It is not periodic. When failure occurs,
the `DecisionTree` backtracks to the next-best candidate plan rather than rebuilding from scratch.
If no candidates remain, a full replan is triggered. There is no replan limit — if the bot is
genuinely stuck it must find another path.

---

## The Choice Interface

Every action the bot can take implements `Choice`. This is the central abstraction.

```java
package ciaassured.yrushwinner.strategy;

import java.time.Duration;

public interface Choice {
    /**
     * Estimate how long this choice will take to execute, in seconds.
     * Called during planning — must be fast (no blocking, no world mutation, no movement).
     * All world data needed must be passed in at construction time.
     */
    Duration estimateTime();

    /**
     * Execute this choice to completion. Blocking — does not return until
     * the choice succeeds or fails. Runs on the bot execution thread, not
     * the game tick thread.
     *
     * @param ctx shared runtime state (player, world, input controller, etc.)
     * @return result indicating success or failure mode
     */
    ChoiceResult execute(BotContext ctx);
}
```

```java
public enum ChoiceResult {
    SUCCESS,
    FAILED_RETRY,   // this choice failed but sibling branches may work — backtrack in tree
    FAILED_ABORT    // this entire strategy is unrecoverable — replan from root
}
```

`CompositeChoice` sequences multiple `Choice` instances and sums their `estimateTime()` values.
This is how compound strategies (gather + climb + tower) are represented.

---

## TimeCostModel

All time constants live in one place for easy tuning. These are based on Minecraft wiki values
and will be refined by the calibration suite. Every constant must have a comment citing its
source (wiki page or formula).

```java
package ciaassured.yrushwinner.navigation;

public final class TimeCostModel {

    // Movement speeds (blocks per second)
    // Source: https://minecraft.wiki/w/Sprinting
    public static final double SPRINT_SPEED_BPS = 5.612;
    public static final double WALK_SPEED_BPS   = 4.317;
    public static final double SWIM_SPEED_BPS   = 2.2;    // horizontal underwater
    public static final double SINK_SPEED_BPS   = 1.5;    // descending in water
    public static final double JUMP_PENALTY_S   = 0.5;    // extra time per jump in navigation

    // Block placement (tower jumping rhythm)
    // Source: empirical — to be validated by calibration suite
    public static final double PLACE_BLOCK_S    = 0.25;

    // Mining times (seconds per block) — barehanded
    // Source: https://minecraft.wiki/w/Breaking
    // Only list block types the bot will actually mine barehanded
    public static final double MINE_DIRT_BARE_S    = 0.75;
    public static final double MINE_GRAVEL_BARE_S  = 0.9;
    public static final double MINE_SAND_BARE_S    = 0.75;
    public static final double MINE_LEAVES_BARE_S  = 0.3;
    public static final double MINE_WOOD_BARE_S    = 3.0;

    // Mining times — wooden pickaxe
    public static final double MINE_STONE_WOOD_PICK_S  = 1.15;
    public static final double MINE_DIRT_WOOD_PICK_S   = 0.6;
    // Ores are impractically slow with wooden pickaxe — treat as impassable
    public static final double MINE_ORE_WOOD_PICK_S    = 15.0;

    // Crafting times (seconds per step, including placing crafting table)
    public static final double CRAFT_PLANKS_S          = 0.5;
    public static final double PLACE_CRAFTING_TABLE_S  = 0.5;
    public static final double CRAFT_STICKS_S          = 0.5;
    public static final double CRAFT_WOOD_PICKAXE_S    = 0.5;
    // Total time to craft a wooden pickaxe from scratch (assumes 1 log already in hand)
    public static final double TOTAL_CRAFT_PICKAXE_FROM_LOG_S =
            CRAFT_PLANKS_S + PLACE_CRAFTING_TABLE_S + CRAFT_STICKS_S + CRAFT_WOOD_PICKAXE_S;

    // Breathing
    // Source: https://minecraft.wiki/w/Oxygen
    public static final double MAX_AIR_TICKS    = 300.0;  // 15 seconds at 20 tps
    public static final double AIR_DANGER_TICKS = 60.0;   // AirManager triggers below this

    private TimeCostModel() {}
}
```

**When adding a new constant:** add it here with a source comment, add a unit test in
`TimeCostModelTest.java`, and add a calibration scenario to validate it against real gameplay.

---

## EnvironmentScanner

Runs once at game start. Must complete within ~300ms.

**Data sources (all synchronous, no movement required):**
- Loaded chunk data via `ClientWorld` — full 3D block data within render distance
- Player position and current Y level
- Inventory contents (empty vs has wooden pickaxe)
- `targetY` from the custom packet
- Whether the player is currently underwater (block at eye level is water)

**What it scans for:**
- Nearest cave entrance (opening leading below surface)
- Highest reachable hill within a radius cutoff (see below)
- Nearest wood log blocks
- Nearest dirt/gravel/sand deposits
- Whether the direct path to targetY is air (tower), stone (dig), or water (swim)

**Radius cutoff heuristic:** Do not consider terrain features whose travel time alone
would consume more time than the estimated savings. Compute dynamically:
`maxRadius = (estimatedSavingsVsTower) / (1.0 / SPRINT_SPEED_BPS)`

**Output:** A ranked list of complete `Choice` instances (typically 3–6 candidates),
sorted ascending by `estimateTime()`. The lowest-time choice is executed first.

---

## Navigator (A*)

Navigator is an interface so Baritone can be plugged in later:

```java
package ciaassured.yrushwinner.navigation;

import java.util.List;
import net.minecraft.util.math.BlockPos;

public interface Navigator {
    /**
     * Find a time-optimal path from start to goal.
     * Edge weights are seconds (from TimeCostModel), not distance.
     * Returns ordered waypoints, or empty list if no path found.
     */
    List<BlockPos> findPath(BlockPos start, BlockPos goal, BotContext ctx);
}
```

`AStarNavigator` implementation notes:
- Edge weight = time cost to enter adjacent block (sprint, swim, mine if solid)
- Heuristic = Euclidean distance / SPRINT_SPEED_BPS (admissible lower bound)
- Blocks too slow to mine (ores with wood pick, etc.) are treated as impassable
- Do not over-engineer the first pass — get a working implementation, iterate later

---

## InputController

The bot controls the player by injecting inputs through Fabric's input system, making it
indistinguishable from a human player to the server.

- Set key binding pressed states each tick for movement (W/A/S/D, jump, sneak, sprint)
- Control look direction by setting yaw/pitch on the player entity each tick
- Block breaking: use `ClientPlayerInteractionManager.attackBlock()`
- Block placing: use `ClientPlayerInteractionManager.interactBlock()`
- A keybind (default: `B`) toggles the bot on/off at any time without disconnecting

The bot's blocking `Choice.execute()` calls run on a **dedicated bot thread**.
All input injection must be posted back to the main game thread safely using
`MinecraftClient.getInstance().execute(runnable)`.

---

## AirManager

Cross-cutting concern that can interrupt any executing `Choice`.

- Monitors `player.getAir()` each tick from the main thread
- When air drops below `TimeCostModel.AIR_DANGER_TICKS`, signals the bot thread to pause
- Emergency air-finding routine:
  1. Check for air pockets within 2 blocks
  2. If none: mine the block directly above, place a block to seal water out, breathe
  3. Resume the paused choice once air is restored
- If health drops to critical levels, signals `FAILED_ABORT` to trigger a full replan

Implement the pause/resume mechanism carefully to avoid deadlocks between the bot thread
and the main tick thread.

---

## Network — Custom Packet

`YRushPacketHandler` registers a custom payload listener on the client.

Assumed packet format (server side not yet implemented — use this spec):
```
Channel ID: yrushwinner:game_start
Payload:
  targetY: int32   (signed, can be negative)
```

When received, create a `RoundInfo` and fire the game start event to `GameDetector`.

Also parse chat messages as a secondary fallback signal for game start/end announcements,
since the packet implementation on the server side may not always be present.

---

## GameDetector

Listens for game start and end signals.

**Game start:** custom packet (primary) or chat message matching "Reach Y: <n>" pattern.
**Game end:** player enters spectator mode (eliminated/lost) or win message in chat.

`RoundInfo` fields: `targetY`, `spawnPos`, `startTimeMs`, `hasWoodenPickaxe`.

---

## BotController (State Machine)

States: `IDLE → SCANNING → EXECUTING → REPLANNING → FINISHED`

```
IDLE
  └─ on GameStartEvent → SCANNING

SCANNING
  └─ EnvironmentScanner.scan() → builds ranked DecisionTree
  └─ → EXECUTING

EXECUTING
  └─ DecisionTree.executeNext() on bot thread
  └─ SUCCESS → targetY reached? → FINISHED, else continue
  └─ FAILED_RETRY → DecisionTree.backtrack() → try next sibling branch → EXECUTING
  └─ FAILED_ABORT → REPLANNING

REPLANNING
  └─ Re-run EnvironmentScanner with current world state
  └─ → EXECUTING

FINISHED
  └─ release input control, log round result, → IDLE
```

---

## Concrete Choice Implementations

All live in `strategy/choices/`. Do not put strategy logic in `BotController` directly.

### GatherBlocksChoice
Collects N blocks of a target material (wood logs, dirt, etc.).
- `estimateTime()`: nearest N blocks × (mine time + travel time per block)
- Returns `FAILED_RETRY` if fewer than N blocks are reachable in loaded chunks

### CraftPickaxeChoice
Crafts a wooden pickaxe. Precondition: at least 1 log in inventory.
- Sequence: craft planks → place crafting table → craft sticks → craft pickaxe
- `estimateTime()`: `TOTAL_CRAFT_PICKAXE_FROM_LOG_S`

### ClimbHillChoice
Navigates to a hill and climbs it.
- `estimateTime()`: travel time to base + vertical climb time
- May mine or place individual blocks en route to make steps passable
- Returns `FAILED_RETRY` if hill top is insufficient for the remaining Y delta

### TowerUpChoice
Towers straight up by jump-placing blocks.
- `estimateTime()`: blocksNeeded × PLACE_BLOCK_S
- Precondition: sufficient blocks in inventory (planned before execute is called)

### DigDownChoice
Digs a 1×2 shaft downward.
- `estimateTime()`: per-block mine time based on tool and expected block types
- Navigates around ores and blocks that are too slow to mine
- Coordinates with AirManager if water is encountered

### DescendCaveChoice
Navigates to a cave entrance and descends through it using A*.
- `estimateTime()`: travel to entrance + A* path time through cave
- Returns `FAILED_RETRY` if cave does not reach targetY

### SwimDownChoice
Swims and/or mines downward through water.
- `estimateTime()`: depth × (1 / SINK_SPEED_BPS)
- Coordinates with AirManager for breath management

### CompositeChoice
Sequences an ordered list of `Choice` instances.
- `estimateTime()`: sum of all children's estimates
- `execute()`: runs children in order, propagates first failure result

---

## GitHub Actions

### `build.yml`
Triggers: push to any branch, pull requests to main.
1. Set up Java 21
2. `./gradlew build`
3. `./gradlew test`
4. Upload test results as workflow artifact

### `release.yml`
Triggers: tags matching `v*` (e.g. `v1.0.0`).
1. Set up Java 21
2. `./gradlew build`
3. `./gradlew test` — fail the release if any tests fail
4. Create GitHub Release with the tag name
5. Upload the built jar from `build/libs/` as a release asset

### `dependabot.yml`
```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: "/"
    schedule:
      interval: weekly
  - package-ecosystem: github-actions
    directory: "/"
    schedule:
      interval: weekly
```

---

## Calibration Suite

Located in `src/calibration/`. Run manually against a real Minecraft instance, not in CI.

**Purpose:** Validate that `TimeCostModel` constants match real in-game timing.

**How it works:**
1. `CalibrationRunner` connects as a bot and executes isolated actions
   (mine 10 stone blocks with wood pick, sprint 50 blocks, tower 20 blocks, etc.)
2. Records wall-clock time per action
3. `CalibrationReport` compares actual vs estimated:
   ```
   Action                        Estimated    Actual    Error
   Mine stone (wood pick) ×10    11.50s       12.3s     +6.9%
   Sprint 50 blocks               8.91s        8.7s     -2.4%
   Tower 20 blocks                5.00s        5.6s    +12.0%  ← needs tuning
   ```
4. Adjust constants in `TimeCostModel` manually. Target: < 5% error on all actions.
5. Commit updated constants and calibration reports to `docs/calibration-results/`.

---

## Unit Test Scope

Tests must run without a Minecraft runtime (pure Java).

**In scope for unit tests:**
- `AStarNavigator` — mock world as a 3D boolean array, verify path cost and correctness
- `TimeCostModel` — verify constant values against cited wiki sources
- `CompositeChoice` — mock children, verify time summation and result propagation
- `DecisionTree` — verify backtracking: FAILED_RETRY → next sibling, FAILED_ABORT → replan
- `AirManager` — mock air ticks, verify interrupt threshold and recovery sequence

**Not unit-testable (calibration or integration only):**
- `InputController` (requires Fabric runtime)
- `EnvironmentScanner` (requires loaded chunk data)
- Any `Choice.execute()` (requires world and player)

---

## Conventions

- All time values are **`double` seconds** unless the variable name ends in `Ticks` (then `int` at 20 tps).
- `estimateTime()` must be **pure, fast, and side-effect-free**. No world access. No blocking.
  All world data needed for estimation is passed to the `Choice` constructor at scan time.
- Log with SLF4J via `LoggerFactory.getLogger(YRushWinnerMod.MOD_ID)`.
  Use `LOGGER.debug()` for per-tick spam; `LOGGER.info()` for strategy decisions and replans.
- Do not use `Thread.sleep()` for timing on the bot thread. Use tick-counted polling loops.
- All `Choice` implementations go in `strategy/choices/`.

## Git Workflow

- **Never commit directly to `main`.** All changes go through a branch + pull request.
- **Branch naming:** `<type>/<short-description>` — e.g. `feat/tower-up-choice`, `fix/astar-heuristic`, `chore/update-deps`.
- **Conventional commits** — every commit message must follow the format:
  ```
  <type>(<optional scope>): <short description>

  <optional body>
  ```
  Valid types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`.
  Examples:
  - `feat(strategy): add TowerUpChoice with jump-place loop`
  - `fix(astar): correct heuristic admissibility for vertical movement`
  - `test(safety): add AirManager edge-case coverage`
  - `chore(deps): bump fabric-api to 0.142.0+1.21.11`
- **PR titles** follow the same conventional-commit format as the primary commit.
- **No force-pushes to main.** Feature branches may be force-pushed before review if needed.

## What Not To Do

- **Do not guess Minecraft/Fabric/Yarn version strings.** Look them up before writing gradle files.
- **Do not make Baritone a hard dependency.** It goes behind the `Navigator` interface only.
- **Do not implement sabotage.** Explicitly out of scope.
- **Do not use ML anywhere.** All decisions are deterministic and explainable.
- **Do not run scanning or pathfinding on the game tick thread.** Only input injection touches the main thread.
- **Do not add unnecessary abstractions on the first pass.** Get it working, then refactor.

