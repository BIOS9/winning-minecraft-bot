# Y-Rush Winner

A client-side Fabric mod for Minecraft 1.21.11 that plays the **Y-Rush** minigame autonomously.

Y-Rush: reach a target Y coordinate before opponent players. Games last up to ~4 minutes. The target Y is a fixed number of blocks above or below spawn, announced in chat each round.

## How it works

When a round starts the server sends a chat message such as `CLIMB 17 BLOCKS` or `DIG DOWN 25 BLOCKS`. The mod parses this, computes the absolute target Y from the player's current position, and runs A\* pathfinding to find a time-optimal route through loaded terrain. The planned path is rendered in-world as a coloured line (green → red) that passes through walls for visibility.

**Core design:**
- Classical AI — time-weighted A\*, heuristic strategy selection. No machine learning.
- All costs are in **seconds of real time**, not block distance.
- Pathfinder waits for chunks to load before treating them as passable, rather than assuming unloaded terrain is open air.
- Dependency injection via Google Guice. Services start/stop cleanly with the client lifecycle.

## Controls

| Input | Action |
|-------|--------|
| `B` | Debug: plan a path to Y=100 from current position |
| `/goto <y>` | Plan and render a path to the given Y level |
| `/clearpath` | Clear the rendered path |

## Requirements

- Minecraft 1.21.11
- Fabric Loader ≥ 0.19.2
- Fabric API

## Building

```bash
./gradlew build
```

The built jar is placed in `build/libs/`.

## Calibration

Time constants in `TimeCostModel` are sourced from the Minecraft wiki and validated by the calibration suite in `src/calibration/`. Run calibration manually against a real instance to tune constants to < 5% error. Commit updated results to `docs/calibration-results/`.

## Disclaimer

This project was developed with significant assistance from [Claude](https://claude.ai) (Anthropic). AI was used for architecture decisions, code generation, and iterative debugging throughout the development process.
