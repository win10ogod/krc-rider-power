Evil karma now grants **Evil Rider Power**, trading maximum health for greater damage. Good deeds continue to strengthen **Rider Power**. Only one power is active at a time.

| Default state | Damage | Maximum health |
| --- | --- | --- |
| Evil karma 100 (signed score −100) | ×2.5 | ×0.5 |
| Evil karma 50 (signed score −50) | ×2 | ×1 |
| Neutral | ×1.5 | ×1.5 |
| Merit 100 (signed score +100) | ×2 | ×2 |

Switching powers preserves your current health percentage. Evil power reduces maximum health rather than dealing periodic damage. Extra armor, toughness, movement speed, and knockback resistance decrease as evil karma grows.

Use `/krcboost karma` to see merit, evil karma, the selected power, and damage/health multipliers. Administrators can configure `evilDamageBonusScale` (default 3) and `evilHealthMultiplier` (default 0.5) in `config/krcboost-karma.json`, then run `/krcboost reload`.

Existing 1.1.0 scores and settings carry forward; missing new fields use defaults. Animals, including tamed animals, and PVP remain neutral. Existing scoring and positive reward limits remain in place.

**Update both client and server to 1.2.0.** The new protocol requires the new effect registration and messages on both sides. Requires Minecraft 1.21.1, Java 21, NeoForge 21.1.244, KRC 1.1.3, GeckoLib 4.9.2, and Player Animation Library 1.1.6+mc.1.21.1.

Install `krc-rider-power-1.2.0.jar`. The sources JAR is for reference; the CurseForge kit contains publishing materials. MIT licensed; KRC is not bundled.

[Karma guide](https://github.com/win10ogod/krc-rider-power/blob/main/docs/karma.md) · [Verification](https://github.com/win10ogod/krc-rider-power/blob/main/evidence/evil-power-verification.md) · [Changelog](https://github.com/win10ogod/krc-rider-power/blob/main/CHANGELOG.md)
