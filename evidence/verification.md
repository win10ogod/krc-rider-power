# Release verification

Date: 2026-09-11. Environment: WSL/Linux, JDK 21, Minecraft 1.21.1, NeoForge 21.1.244.

## Runtime and package

KRC 1.1.3, GeckoLib 4.9.2, and Player Animation Library 1.1.6+mc.1.21.1 were used. The pinned Modrinth KRC download has the same SHA-512 as the original development JAR:

`f3deeb1d8a4ac99f2aaf3619b7e19891e075b2287717f2170a736b7c1ee6d6fc5c53df43c5c6c81763e54078b46520859aaf80b79c123896b929e590525051f0`

The release JAR includes the MIT license and this addon's code and resources. It excludes upstream KRC classes and the separate development test mod. Release assets include their own `SHA256SUMS` file.

## Tests

```bash
python3 -m unittest discover -s scripts/tests -v
./gradlew test runGameTestServer build
```

- 10 Python automation tests passed: stable release selection, compatible Minecraft/loader filtering, no downgrade, unchanged dependencies, unsafe identifier rejection, precise property updates, concurrent push protection, refusal to commit unrelated changes, push failure handling, and no overwrite of published assets.
- 3 JUnit configuration tests passed, with zero failures or errors.
- All 8 required NeoForge GameTests passed.
- All 579 registered KRC `RiderDriverItem` entries were visited; 579 complete base suits received the shared buff. This counts driver items, not distinct riders or every possible form.
- 200 repeated updates did not stack modifiers. 100 transformation cycles preserved the health ratio.
- Invalid transformation state, incomplete equipment, and an injected high effect level did not grant extra bonuses.
- Logout/save/reload, death, disabling the buff, and unrelated modifiers were checked.
- Ordinary-player writes and stale administrator writes were rejected; authorized administrator writes succeeded.
- Melee, projectile, and KRC kick damage events were multiplied once and stopped receiving the multiplier when transformation eligibility ended.

The Gradle GameTest task verifies a fresh completion record against the number of test methods in source. Missing tests or an early server exit cannot pass solely because the Minecraft process returned exit code zero.

## Editor rendering

```bash
LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1280x900x24' ./gradlew runClient -PuiSmoke build
```

An isolated Minecraft client opened the native administrator editor with test data, rendered it, captured [rider-power-editor.png](rider-power-editor.png), and exited with `KRC_BUFF_UI_SMOKE_OK`. This verifies rendering. Permission and save behavior were exercised separately through server GameTests; the screenshot run is not a multiplayer interaction test.

Raw development logs are kept outside the source repository. GitHub Actions retains workflow logs and compatibility diagnostics for future builds.
