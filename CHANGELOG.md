# Changelog

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
