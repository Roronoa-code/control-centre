# Implementation status

Updated: 11 September 2026, 17:10 (feasibility test in progress).

## Stage 2 — feasibility slice

Written and compiled: Gradle 9.3.1 wrapper, AGP 9.1.1, Kotlin/Compose plugin 2.2.10, JDK 17 (see `docs/build-environment.md`). Manifest, accessibility service configuration, static shortcuts, three page aliases, `PanelAccessibilityService` + `TriggerWindowController` (`TYPE_ACCESSIBILITY_OVERLAY` top-right strip and right-edge handle), `PanelActivity` (three pages, real media volume, persisted counter, battery readout, every other control visibly inert), `SettingsActivity` (service status, trigger toggles, live gesture trace).

Tested: `:app:testDebugUnitTest` 26/26 passed; `:app:lintDebug` 0 errors, 16 warnings.

Device-verified on SM-S938B, One UI 9.0 (`ro.build.version.oneui=90000`), Android 17 / API 37, build `CP2A.260605.016.S938BXXUCZZI4`:

| Check | Result | Evidence |
| --- | --- | --- |
| Install via ADB (wireless) | Yes | `adb install -r` Success, versionName 0.1.0 |
| App launch | Pass | `am start -W` Status ok for `SettingsActivity` (453 ms) and the `ToolsEntry` alias (113 ms); `topResumedActivity` confirmed; screenshots |
| Page swiping | Pass | Injected horizontal swipes Tools → Device → Everyday while the panel window held focus; screenshots of each page |
| Real media volume | Pass | Slider drag: app log `requested=9 observed=9`, `dumpsys audio` STREAM_MUSIC `streamVolume:9`. System-side `cmd media_session volume --stream 3 --set 6`: panel showed "System reports 6 / 15". Restored to 4. |
| Top-right gesture | Not yet tested | Waits for the accessibility service to be enabled manually |
| Side gesture | Not yet tested | Same |
| Samsung panel still accessible | Not yet tested | Same |

Observations: the earlier 4 → 7 volume jump was an AudioService per-device volume resync (a Bluetooth output is attached), not an app or key action. A third-party accessibility service (`com.quarkstudio.glyf`) is already enabled on the phone; note it when interpreting overlay behaviour.

## Next stages

3. Gesture tests on the phone (injected probes, then physical swipes); record the route that works.
4. Functional pages, settings and control backends behind the gate; Shizuku only after the gate.

## Status terminology

Written = source exists. Compiled = an actual build passed. Tested = named tests ran and their output was inspected. Device-verified = observed on the target phone. These are not interchangeable.
