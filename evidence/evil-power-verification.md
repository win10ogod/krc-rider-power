# KRC Rider Power 1.2.0 verification

Verified 2026-09-11 on WSL/Linux with JDK 21, Minecraft 1.21.1, NeoForge 21.1.244, KRC 1.1.3, GeckoLib 4.9.2, and Player Animation Library 1.1.6+mc.1.21.1.

## Automated checks

```bash
python3 -m unittest discover -s scripts/tests -v
./gradlew test runGameTestServer build
```

- **10 Python automation tests passed.** Update selection, pin handling, concurrent pushes, and published-asset protections remain covered.
- **12 JUnit tests passed**, with zero failures or errors. Four new power-profile tests check neutral/good/evil endpoints, monotonic evil damage and health, custom endpoints and score limits, disabled rules, old JSON compatibility, and invalid new settings.
- **All 20 required NeoForge GameTests passed.** The fresh completion record matches the 20 annotated test methods.
- The original registry coverage still visited **579 KRC driver items**, with **579 complete base suits** receiving the normal buff at neutral score. This does not count every possible form.

## Evil power behavior exercised

- Maximum evil gives ×2.5 damage and ×0.5 maximum health with defaults. Maximum good retains ×2 for both. Existing secondary bonus scaling remains covered.
- One hundred switches between maximum evil and maximum good keep exactly the selected effect and preserve 40% wounded health: `4/10 ↔ 16/40`. Injected opposite effects and level-255 effects are removed or normalized to level I.
- Melee, player-attributed arrow, and actual KRC rider-kick damage sources receive one evil multiplier before defenses. Self-damage and attacks after removing the driver receive no multiplier.
- Logout clears the evil health reduction before saving. Reconstructing a player with the **same GameProfile** and loading the saved NBT preserves signed karma, the KRC transformation, and wounded health. Disabling the buff restores the underlying maximum health; death does not revive the player.
- A separate configuration test batch loads disabled karma rules from disk and verifies normal base power without deleting the negative score. Re-enabling with custom endpoints applies ×3 damage and ×0.25 maximum health while preserving the wounded ratio. Original files and active rules are restored in a `finally` block.
- Existing tests continue to cover animal/PVP neutrality, actual cures and kills, repeated rewards, spawn exclusions, conversion/splitting markers, score and reward-budget persistence, administrator permissions, and stale editor writes.

GameTests use the actual Minecraft/KRC runtime with mock players and some directly posted events. They are not an end-to-end multiplayer play session or exhaustive coverage of every other mod's custom entities and damage sources.

## Client, protocol, and package

```bash
LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1280x900x24' ./gradlew runClient -PuiSmoke build
```

The isolated client opened the updated editor, captured [rider-power-editor.png](rider-power-editor.png), and exited with `KRC_BUFF_UI_SMOKE_OK`. The screenshot was visually checked: fields, the evil health/damage explanation, and buttons are visible without overlap. This verifies editor rendering, not a multiplayer interaction or the in-world effect HUD.

The new 18×18 evil effect PNG was generated from the original vector source in [artwork/evil-rider-power.svg](../artwork/evil-rider-power.svg) and visually inspected. English, Traditional Chinese, and Simplified Chinese effect names and scoring-message resources parse successfully.

The required payload protocol is now `2`. The installed NeoForge 21.1.244 source (`NetworkComponentNegotiator`) explicitly rejects mismatched versions. This compatibility guard was checked in source; an old-client/new-server handshake was not run live.

The installable JAR identifies 1.2.0 and contains the evil profile, effect icon, rule defaults, translations, and MIT license. It excludes upstream KRC classes and the separate development test mod. Release downloads include SHA-256 checksums.

Existing KRC recipe/advancement/resource diagnostics still appear during development startup. The server completes all required tests and the client reaches the editor. Local raw logs remain in ignored `.work`; GitHub's Build and test workflow repeats the unit and integration checks on pushes to `main`.
