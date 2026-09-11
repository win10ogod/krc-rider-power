# Changelog

## 1.2.0 — 2026-09-11

- Add Evil Rider Power for negative signed karma: greater evil karma raises damage while reducing maximum health. Neutral and positive scores use Rider Power; only one effect can be active at a time.
- With defaults, evil karma 100 (score −100) gives ×2.5 damage and ×0.5 maximum health; neutral remains ×1.5 for both, and merit 100 (score +100) remains ×2 for both.
- Configure the evil endpoints through `evilDamageBonusScale` (default 3) and `evilHealthMultiplier` (default 0.5). Existing 1.1.0 files and saved scores carry forward without deletion or conversion.
- Show merit, evil karma, selected power, and damage/health multipliers in status commands. Add the localized Evil Rider Power name and an original effect icon.
- Preserve health percentage across good/evil switches, logout, death, and disabling rules; reject or remove injected opposite effects and effect levels.
- Retain animal and PVP neutrality, scoring rules, server-owned progress, and positive reward limits. Evil armor, toughness, speed, and knockback bonuses still decrease with negative score.
- Require the new client/server protocol so an older client cannot join without the new effect registration and messages.

Requires Minecraft 1.21.1, Java 21, and NeoForge. Tested with KRC 1.1.3 and NeoForge 21.1.244. Update both client and server to 1.2.0.

## 1.1.0 — 2026-09-11

- Add server-owned karma that strengthens or weakens the existing Rider Power bonus. All players share administrator-controlled base values, with personal bonus strength earned through deeds.
- Award +10 for an eligible completed zombie villager cure and +1 for an eligible hostile monster kill; subtract 20 for killing a villager or wandering trader. Animals, including tamed animals, and PVP remain neutral.
- Default to a −100…+100 score range and 30 positive points per 24,000 server game ticks. Administrators can configure or disable karma in `config/krcboost-karma.json`.
- Preserve scores and reward budgets across death, logout, and world saves. Credit completed cures to their initiating player even when offline.
- Exclude artificial spawn sources, infected villagers, and repeated cures from positive rewards; preserve exclusions through conversions and slime splitting.
- Add the read-only `/krcboost karma` command, scoring action-bar messages, and personal karma information in `/krcboost status`.
- Keep one Rider Power effect and preserve current health percentage when karma changes maximum health.

Tested with Minecraft 1.21.1, KRC 1.1.3, NeoForge 21.1.244, GeckoLib 4.9.2, and Player Animation Library 1.1.6+mc.1.21.1. Existing base configuration remains valid; new karma settings and neutral starting scores are created automatically. Update both client and server to 1.1.0.

## 1.0.0 — 2026-09-11

Initial release for Minecraft 1.21.1 and NeoForge.

- Add one shared Rider Power buff for players with a completed Kamen Rider Craft transformation.
- Configure damage, maximum health, armor, armor toughness, movement speed, and knockback resistance.
- Provide an administrator editor through `/krcboost edit`, plus status and reload commands.
- Validate editing permissions and transformation eligibility on the server.
- Prevent modifier stacking and preserve health percentage when bonuses change.
- Support forms through KRC's shared driver interface without maintaining a list of rider names.

Tested with KRC 1.1.3, NeoForge 21.1.244, GeckoLib 4.9.2, and Player Animation Library 1.1.6+mc.1.21.1. Install on both the client and server. The in-game editor and command messages currently use Traditional Chinese.
