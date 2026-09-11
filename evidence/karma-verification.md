# KRC Rider Power 1.1.0 verification

Verified 2026-09-11 on WSL/Linux with JDK 21, Minecraft 1.21.1, NeoForge 21.1.244, KRC 1.1.3, GeckoLib 4.9.2, and Player Animation Library 1.1.6+mc.1.21.1.

## Automated checks

```bash
python3 -m unittest discover -s scripts/tests -v
./gradlew test runGameTestServer build
```

- **10 Python automation tests passed.** Stable update selection, compatibility filtering, no downgrade, pin handling, concurrent pushes, and published-asset protections remain covered.
- **8 JUnit tests passed**, zero failures or errors. The original three configuration tests are joined by five karma tests covering scaling, bounds, positive reward budgets, clock rollback, disabled rules, strict JSON validation, and file preservation.
- **All 16 required NeoForge GameTests passed.** The Gradle task verified a fresh completion record against all 16 annotated source methods.
- The existing registry coverage still visited **579 KRC driver items**, with **579 complete base suits** receiving the buff at neutral karma. This is a count of driver items, not every possible form.

## Karma behavior exercised

- Tamed wolf, tamed horse, cow, and player death events all leave karma unchanged.
- An actual villager death caused by a mock survival player's damage source subtracts 20 while untransformed. A duplicate death event cannot subtract again.
- An actual zombie death caused by a player-attributed arrow grants one point. Unattributed deaths grant none; repeated eligible kill events stop at the shared 30-point positive budget.
- Spawner, egg, command, dispenser, and summoned monsters grant no points. Conversion to drowned and entity NBT save/load preserve the exclusion. Slime split events also propagate exclusion to children and survive their NBT reload. Trial spawner combat remains eligible.
- An actual vanilla zombie villager cure completed in the test world credits its saved initiating UUID by 10 even with no online player for that UUID.
- Repeated cure events and villager infection/cure cycles do not award more points. Conversion markers survive NBT reload.
- Neutral / maximum good / maximum evil karma gives the expected health and damage. The added armor, toughness, speed, and knockback resistance scale correctly. One hundred transformation cycles preserve wounded health percentage with karma active.
- SavedData NBT round trips preserve both score and reward budget. Player cloning after death and logout do not reset the UUID-based record.
- Canceled deaths and forged player persistent data do not affect karma. `/krcboost karma` has no score-writing subcommands.
- The original eight integration tests still cover permissions, stale editor writes, injected effects, incomplete transformations, player-attributed damage, logout, health ratios, and unrelated modifiers.

These are automated GameTests using the actual Minecraft/KRC runtime, with mock players and some directly posted events. They do not constitute an end-to-end multiplayer play session or exhaustive coverage of every other mod's custom entities and damage sources.

## Editor rendering and release package

```bash
LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1280x900x24' ./gradlew runClient -PuiSmoke build
```

The isolated client opened the updated native editor, saved [rider-power-editor.png](rider-power-editor.png), and exited with `KRC_BUFF_UI_SMOKE_OK`. The capture was visually checked: all six fields, shared-base wording, karma explanation, and buttons are visible without overlap. This verifies rendering; server-side permissions and saving are covered separately by GameTests.

The installable JAR identifies version 1.1.0 and contains the karma implementation, default rule file, translated scoring messages, and MIT license. It excludes upstream KRC classes and the separate development test mod. Release downloads include SHA-256 checksums.

KRC 1.1.3 emits existing recipe/advancement/resource diagnostics during development startup; the server completes all required tests and the client reaches the editor. Raw local logs remain in the ignored `.work` directory. The repository's Build and test workflow runs the same unit and integration tests for pushes to `main`.
