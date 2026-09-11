KRC Rider Power now lets your deeds strengthen or weaken the same universal Rider Power buff.

- Cure an eligible zombie villager: **+10 karma**.
- Defeat an eligible hostile monster: **+1 karma**.
- Kill a villager or wandering trader: **−20 karma**.
- **Animals, including tamed animals, and PVP are neutral.**

Karma starts at 0, defaults to a range of −100 to +100, and scales the extra bonus. Default damage becomes ×1 / ×1.5 / ×2 at −100 / 0 / +100. There is still one Rider Power effect, and changing maximum health preserves the current health percentage.

Scores and reward budgets survive death and logout. Positive gains share a 30-point budget per 24,000 server game ticks. Artificial spawn sources, infected villagers, and repeated cures cannot provide positive rewards; trial chamber combat remains eligible.

Players can check `/krcboost karma`. Administrators can edit or disable the rules in `config/krcboost-karma.json` and use `/krcboost reload`. Existing base settings remain valid. See the [karma guide](https://github.com/win10ogod/krc-rider-power/blob/main/docs/karma.md) and [changelog](https://github.com/win10ogod/krc-rider-power/blob/main/CHANGELOG.md).

Requires **Minecraft 1.21.1, Java 21, NeoForge 21.1.244**, Kamen Rider Craft **1.1.3**, GeckoLib **4.9.2**, and Player Animation Library **1.1.6+mc.1.21.1**. Install `krc-rider-power-1.1.0.jar` on **both client and server**. The sources JAR is for reference; the CurseForge kit contains publishing materials.

MIT licensed. This addon does not bundle KRC. The editor and commands use Traditional Chinese.
