# Implementation status

Updated: 11 September 2026, 17:15.

## Stage 2 — feasibility slice (device-verified with injected input; finger tests pending)

Written and compiled: Gradle 9.3.1 wrapper, AGP 9.1.1, Kotlin/Compose plugin 2.2.10, JDK 17 (see `docs/build-environment.md`). Manifest, accessibility service configuration, static shortcuts, three page aliases, `PanelAccessibilityService` + `TriggerWindowController` (`TYPE_ACCESSIBILITY_OVERLAY` top-right strip and right-edge handle with a system-gesture exclusion rect), `PanelActivity` (three pages, real media volume, persisted counter, battery readout, every other control visibly inert), `SettingsActivity` (service status, trigger toggles, live gesture trace).

Tested: `:app:testDebugUnitTest` 26/26 passed; `:app:lintDebug` 0 errors, 16 warnings. Debug APK SHA-256 `83569a9ab0fad7b81c40235721636d30a677b4005e87293544051b4de558a2df` (commit `8f17781`).

Device-verified on SM-S938B, One UI 9.0, Android 17 / API 37, build `CP2A.260605.016.S938BXXUCZZI4` (details and evidence in `docs/gesture-test-report.md`):

| Check | Result |
| --- | --- |
| Install via ADB (wireless) | Yes |
| App launch | Pass (Settings and the Tools alias; resumed; screenshots) |
| Page swiping | Pass (injected swipes Tools → Device → Everyday) |
| Real media volume | Pass both ways (`dumpsys audio` confirms slider changes; system-side change reflected in the panel) |
| Top-right gesture | Pass with injected input; Samsung's shade received no touch. Physical finger pulls not yet recorded. |
| Side gesture | Pass with injected input after the Back-gesture exclusion fix (all three directions). Physical swipes not yet recorded. |
| Samsung panel still accessible | Yes: top-left pull opened the notification shade normally during the test. |

Manual permission enabled: accessibility service only. Nothing else.

## Direction note

The owner's stated goal (11 Sep, 17:13) is extra pages inside Samsung's own Quick Panel that reuse Samsung's tiles. That is not achievable by a third-party app without root or SystemUI modification, which this project excludes. What the verified build gives instead: Samsung's panel untouched, plus a separate panel with its own pages one gesture away (top-right pull or edge handle), whose controls must be re-implemented. Decision pending.

## Next stages

3. Physical finger tests (see the report's "Still open" list); then decide the direction.
4. If continuing: Samsung-like styling, drag preview, ordinary system controls; Shizuku-backed controls last.

## Status terminology

Written = source exists. Compiled = an actual build passed. Tested = named tests ran and their output was inspected. Device-verified = observed on the target phone. These are not interchangeable.
