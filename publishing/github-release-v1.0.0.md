One server-controlled **Rider Power** buff for completed Kamen Rider Craft transformations.

Default bonuses: damage ×1.5, maximum health ×1.5, armor +4, armor toughness +2, movement speed ×1.15, and knockback resistance +0.1. Administrators can edit the shared values with `/krcboost edit` or `config/krcboost.json`.

Server checks prevent ordinary players from editing values, duplicate modifier stacking, and health gains from repeated transformation toggles. Forms use KRC's common driver interface, with no list of rider names to maintain.

### Installation

- Minecraft **1.21.1**, Java **21**, NeoForge **21.1.244+** within the 21.1 series.
- Kamen Rider Craft **1.1.3**, GeckoLib **4.9.2**, Player Animation Library **1.1.6+mc.1.21.1** are the tested dependency versions.
- Install `krc-rider-power-1.0.0.jar` on both the client and server. Dependencies are downloaded separately.
- The editor and command messages currently use Traditional Chinese; English configuration instructions are in the README.

### Downloads

- `krc-rider-power-1.0.0.jar`: installable mod.
- `krc-rider-power-1.0.0-sources.jar`: source reference; do not install it as a mod.
- `SHA256SUMS`: checksums for both JARs.
- `krc-rider-power-1.0.0-curseforge-kit.zip`: prepared CurseForge description, project fields, icon, screenshot, changelog, license, and installable JAR. This archive is a submission kit; upload the JAR inside it as the Minecraft mod file.

Validation: 3 unit tests and 8 NeoForge GameTests passed, including all 579 registered KRC driver base suits. The native administrator editor was rendered and captured in an isolated client. See [verification details](https://github.com/win10ogod/krc-rider-power/blob/main/evidence/verification.md).

Released under the MIT license.
