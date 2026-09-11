# KRC Rider Power

Give every completed Kamen Rider Craft transformation one shared **Rider Power** buff. Server administrators choose the bonuses, and the same settings apply to every eligible player.

## One buff, six configurable bonuses

| Stat | Default bonus |
| --- | --- |
| Damage | ×1.5 |
| Maximum health | ×1.5 |
| Armor | +4 |
| Armor toughness | +2 |
| Movement speed | ×1.15 |
| Knockback resistance | +0.1 |

Damage is multiplied once before armor and resistance calculations for attacks attributed to the transformed player, including melee attacks, projectiles, and KRC skills. Self-damage is excluded.

The buff activates after a complete KRC transformation. It is removed when the player leaves the transformation, loses required equipment, dies, or enters spectator mode. It uses KRC's shared transformation interface, so new forms using that interface can receive the same buff without a separate configuration entry.

## Administrator controls

- `/krcboost edit` opens the editor. Editing requires operator permission level 2.
- `/krcboost reload` reloads `config/krcboost.json` after editing the file.
- `/krcboost status` lets any player view the shared settings and their eligibility.

Ordinary players cannot save their own bonus values. The server checks permissions and transformation state, prevents duplicate modifiers, and preserves health percentage when maximum health changes. Repeatedly transforming does not refill health.

The editor and command messages currently use **Traditional Chinese**. The six JSON fields and their defaults are documented in the [English configuration guide](https://github.com/win10ogod/krc-rider-power#configuration).

## Installation

Use **Minecraft 1.21.1**, **Java 21**, and **NeoForge 21.1.244 or newer within the 21.1 series**. Install this mod on both the client and server, together with:

- [Kamen Rider Craft](https://www.curseforge.com/minecraft/mc-mods/kamen-rider-craft) — tested with 1.1.3.
- [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) — tested with 4.9.2 for NeoForge 1.21.1.
- [Player Animation Library](https://www.curseforge.com/minecraft/mc-mods/playeranimator) — tested with 1.1.6+mc.1.21.1 for NeoForge.

Place `krc-rider-power-1.0.0.jar` in `mods`, then launch the game or server. The configuration file is created on first startup.

Compatibility with future KRC versions depends on their shared transformation interface remaining compatible. Minecraft's normal attribute limits still apply to the resulting stats.

## Source and credits

[Source code](https://github.com/win10ogod/krc-rider-power) · [Report an issue](https://github.com/win10ogod/krc-rider-power/issues) · [MIT license](https://github.com/win10ogod/krc-rider-power/blob/main/LICENSE)

KRC Rider Power is an unofficial addon for Kamen Rider Craft. Thanks to the KRC team for the base mod. The MIT license covers this addon's code and original assets; dependencies retain their own licenses and are distributed separately.
