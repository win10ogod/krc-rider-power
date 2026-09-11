# KRC Rider Power

Power up your Kamen Rider Craft transformations with **Rider Power** and **Evil Rider Power**. Administrators customize six shared base values. Good deeds strengthen your bonuses; evil karma trades maximum health for higher damage. Only one power is active at a time.

## Transform to activate

Complete a KRC transformation to receive the buff automatically. The bonuses are removed when you leave the transformation, lose required equipment, die, or enter spectator mode.

New forms can use the same settings when they follow KRC's supported transformation system, without requiring a separate rider or form configuration.

## Default bonuses at neutral karma

| Stat | Default bonus |
| --- | --- |
| Damage | ×1.5 |
| Maximum health | ×1.5 |
| Armor | +4 |
| Armor toughness | +2 |
| Movement speed | ×1.15 |
| Knockback resistance | +0.1 |

All six values are configurable. Damage is multiplied once before armor and resistance calculations for attacks attributed to the transformed player, including melee attacks, projectiles, and supported KRC skills. Self-damage is excluded. Minecraft's normal attribute limits still apply.

## Let your deeds shape your power

Karma starts at **0**, with a default range of **−100 to +100**.

| Action | Karma change |
| --- | --- |
| Complete a first eligible zombie villager cure | +10 |
| Defeat an eligible hostile monster | +1 |
| Kill a villager or wandering trader | −20 |
| Kill an animal, including a tamed pet, or another player | 0 |

Negative scores activate **Evil Rider Power**, displayed as positive evil karma (業力). Positive scores represent merit (功德). With default settings:

| State | Damage | Maximum health |
| --- | --- | --- |
| Evil karma 100 (score −100) | ×2.5 | ×0.5 |
| Evil karma 50 (score −50) | ×2 | ×1 |
| Neutral | ×1.5 | ×1.5 |
| Merit 100 (score +100) | ×2 | ×2 |

Evil power increases damage as it reduces maximum health. Extra armor, toughness, speed, and knockback resistance decrease along the evil path. Your current health percentage is preserved when powers switch, and the two effects cannot stack.

Deeds count even while untransformed. Scores survive death and logout. To limit farming, positive gains share a **30-point budget per 24,000 server game ticks** (20 minutes at normal tick speed). Ordinary spawner, egg, command, dispenser, and mob-summoned targets do not give positive points; trial chamber combat can. Infected villagers and repeated cures do not grant rescue points. Naturally spawned mob farms can still earn points within the same budget.

## Shared settings for multiplayer

Administrators manage one set of base bonuses and karma rules for the whole server. Ordinary players cannot change their own values, and repeated transformations do not stack the buff or refill health. When maximum health changes, your current health percentage is preserved.

- `/krcboost edit` — Open the in-game configuration screen.
- `/krcboost reload` — Reload the base bonuses in `config/krcboost.json` and rules in `config/krcboost-karma.json`.
- `/krcboost status` — View shared base settings, your karma, and transformation eligibility.
- `/krcboost karma` — View your signed score, merit, evil karma, power type, and damage/health multipliers.

Editing and reloading require **operator permission level 2**. The status and karma commands are available to everyone. Both configuration files are created on first startup. Administrators can disable karma scaling and scoring while keeping the shared base bonuses and saved scores.

The evil damage and health endpoints are configurable through `evilDamageBonusScale` (default 3) and `evilHealthMultiplier` (default 0.5). Existing rule files and scores carry forward; missing new fields use defaults.

The editor and command messages currently use **Traditional Chinese**. An [English configuration guide](https://github.com/win10ogod/krc-rider-power#configuration) explains the JSON settings.

## Requirements and installation

- **Minecraft 1.21.1**
- **NeoForge 21.1.244 or newer within the 21.1 series**
- **Java 21**
- [Kamen Rider Craft](https://www.curseforge.com/minecraft/mc-mods/kamen-rider-craft)
- [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib)
- [Player Animation Library](https://www.curseforge.com/minecraft/mc-mods/player-animation-library)

Install this mod and its dependencies on **both the client and server**. Update both sides to 1.2.0; older addon clients cannot join the new protocol. Place the mod JAR in the `mods` folder, launch the game, and complete a KRC transformation.

Version 1.2.0 was tested with **KRC 1.1.3**, **GeckoLib 4.9.2**, and **Player Animation Library 1.1.6+mc.1.21.1**, using NeoForge 21.1.244.

## Updates and source code

[GitHub releases](https://github.com/win10ogod/krc-rider-power/releases) include automated compatibility builds for newer KRC releases on Minecraft 1.21.1 / NeoForge. Each candidate must pass the build and automated tests before a compatibility prerelease is published. Future KRC interface changes may require an addon update.

[Source code](https://github.com/win10ogod/krc-rider-power) · [Report an issue](https://github.com/win10ogod/krc-rider-power/issues) · [MIT license](https://github.com/win10ogod/krc-rider-power/blob/main/LICENSE)

KRC Rider Power is an unofficial addon for Kamen Rider Craft. Thanks to the KRC team for the base mod.
