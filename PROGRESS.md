# Implementation status

Updated: 11 September 2026, 17:50.

## Active experiment: companion page beside Samsung's Quick Panel (Stage 3)

Goal under test: pull down from the top-right as usual → Samsung's real Quick Panel opens → a horizontal swipe from a small handle reveals one blank companion page (our overlay) → the reverse swipe removes it and the same, still-open Samsung panel is underneath. No extra tap, no replacement of Samsung's opening gesture, no recreated Samsung controls.

Written and compiled (commit `7ee749a`, debug APK SHA-256 `fca42882a4ee5db7ed099f930df0cdb001a95a879cc5f78e946e138b198f5b4f`):

- `companion/QuickPanelDetector`: finds the System UI shade window (`NotificationShade` title, `legacy_window_root` content root) in `AccessibilityService.getWindows()` and checks for the Quick Panel view ids `sec_quick_panel_compose_root`, `quick_settings_container`, `qs_frame`. Read-only, System UI windows only, never reads text. Re-evaluated on `TYPE_WINDOWS_CHANGED` / `TYPE_WINDOW_STATE_CHANGED`; no polling, no PC.
- `companion/CompanionController`: 16×120 dp handle at the right edge (top at 66 % of the screen height, beside Samsung's tile grid) only while the Quick Panel is open and the device is unlocked and interactive; leftward drag slides in a full-screen blank page that follows the finger; rightward drag or the emergency button slides it out; everything is removed when the panel closes, the screen turns off, the service stops or the preference is off.
- Service configuration now declares `canRetrieveWindowContent`, `flagRetrieveInteractiveWindows`, `flagReportViewIds` (needed for window inspection; disclosed in the service description). Android disables the service on this capability change; it must be re-enabled once after the update.
- Tests: `CompanionPolicyTest` 5/5, `DomainTest` 26/26; lint 0 errors.

Device status: installed; verification of Part 1 (original Samsung gestures with the service enabled), detection, the companion round trip and cleanup is in progress and recorded in `docs/companion-test-report.md` when done. Nothing below is claimed until observed.

## Superseded experiment: gesture interception (Stage 2, commits `d2e9875`…`6cf1d52`)

A top-right `TYPE_ACCESSIBILITY_OVERLAY` strip and an always-on right-edge handle that intercepted the opening pull and launched our own `PanelActivity`. Device-verified with injected input (see `docs/gesture-test-report.md`): the strip received the pull before Samsung's status bar, the side handle needed a Back-gesture exclusion rect, media volume worked both ways. **Retired**: it replaced Samsung's opening gesture, which is not the product. The panel activity, page aliases and volume/counter/battery tiles remain in the code but no gesture reaches them.

## Status terminology

Written = source exists. Compiled = an actual build passed. Tested = named tests ran and their output was inspected. Device-verified = observed on the target phone. These are not interchangeable.
