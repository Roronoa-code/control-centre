# Companion-page experiment — test report

Device: Samsung SM-S938B (Galaxy S25 Ultra), One UI 9.0 (`ro.build.version.oneui=90000`), Android 17 / API 37, build `CP2A.260605.016.S938BXXUCZZI4`, 1440×3120 @ 600 dpi. Wireless ADB.

Tested build: `versionName=0.2.0-companion+be83205`, APK SHA-256 `ea09f52144f3bff57628844f0c33e9597096f3a999bbf8360b5f169e371dc994`, commit `be83205`. The service logs its own `build=` string at connect, so the installed build is identifiable on-device.

Two input methods are kept separate below. **Injected** = `adb shell input motionevent` / `input swipe` (used for automated evidence). **Physical** = a finger on the glass (not yet performed; the owner must do these).

## Mechanism (how the open Quick Panel is recognised)

`AccessibilityService.getWindows()` is scanned for the System UI shade window (title `NotificationShade`, content root id `legacy_window_root`). Inside it, the presence of the Samsung view id `com.android.systemui:id/sec_quick_panel_compose_root` decides "Quick Panel open". Read-only; System UI windows only; no text is read. Re-evaluated on `TYPE_WINDOWS_CHANGED` / `TYPE_WINDOW_STATE_CHANGED` events — no polling, no PC, no manual "panel open" mode.

Device finding that shaped this: `qs_frame` and `quick_settings_container` are **also** present while the *notifications* panel is open on this firmware, so they cannot gate the handle. Only `sec_quick_panel_compose_root` is exclusive to the Quick Panel. The committed detector uses that id alone; the other two are logged as diagnostics but decide nothing.

## Automated results (injected input)

| Check | Result | Evidence |
| --- | --- | --- |
| Quick Panel detection | PASS | Top-right pull → log `quickPanel=true ids=[sec_quick_panel_compose_root, …]`, handle window created. |
| Notifications not misdetected | PASS | Top-left pull → `shade visible=true` but `quickPanel=false` (only `notification_stack_scroller`, `qs_frame`, `quick_settings_container` present); **no handle** shown. |
| No handle outside the Quick Panel | PASS | On the launcher and inside the Settings app, `ControlCentre:*` windows = none. |
| Companion opens without an extra tap | PASS | One drag from the handle creates and slides in the page; no intermediate tap/launch. `PanelActivity` is never started. |
| Samsung panel stays expanded **while covered** | PASS | Held drag (finger not lifted): with `ControlCentre:CompanionPage` present, `NotificationShade` stays `mViewVisibility=0x0` (VISIBLE) at every sample — covered, page-open, returning, and after return. **Zero** `makeExpandedInvisible` / `animateCollapse` events during the round trip. Screenshot `42-companion-over-panel.png` shows the blank page half-covering the still-lit Samsung tiles. |
| Reverse swipe restores the same panel | PASS | Rightward drag removes `CompanionPage`; `NotificationShade` still `0x0`; the same expanded panel (not a reopened one — no expand event) is underneath. |
| Samsung controls usable after the round trip | PASS (touch reaches SystemUI) | After returning, a vertical drag in the tile area delivers touches to pid 5747 (`NotificationShade`); tiles receive input. Actually toggling a tile is left to physical testing to avoid changing device state. |
| Back while companion open | PASS (safe) | `KEYCODE_BACK` collapses the whole shade to the launcher and our windows are removed. No trap, no leftover window. Back is a full dismiss, not a page-only close; the emergency button is the page-only close. |
| Screen-off cleanup | PASS | With the page open, `KEYCODE_SLEEP` → `ControlCentre:*` windows = none. |
| Lock-screen suppression | PASS | After wake the focus is the lock screen; expanding the Quick Panel there logs `quickPanel=true` but the handle is **suppressed** (`isKeyguardLocked`), windows = none. Screenshot `44-lockscreen-qs.png`. |
| Native dismissal cleanup | PASS | `cmd statusbar collapse` → windows removed, shade `0x4`. |
| Build / tests / lint | PASS | `assembleDebug` OK; unit tests 31/31 (`CompanionPolicyTest` 5, `DomainTest` 26); lint 0 errors. |

The critical earlier scare — the panel collapsing when the page appeared — was traced to `adb shell input swipe`, whose synthetic trajectory leaks to SystemUI and triggers a Back/collapse. With `input motionevent` (a clean press-move-release) the panel does not collapse. A real finger is a clean stream like `motionevent`, so this is expected to match physical use, but that must be confirmed on the glass.

## Gesture area (exact)

- Handle window: `TYPE_ACCESSIBILITY_OVERLAY`, right edge, **16 dp wide × 120 dp tall**, top at 66 % of screen height. On this device that is screen px `frame=[1380,2059][1440,2509]` (a 60×450 px strip on the far right, roughly level with the lower tile rows). It exists **only** while the Quick Panel is open. This is a narrow dedicated strip, not "swipe anywhere".
- Companion page: full-screen `TYPE_ACCESSIBILITY_OVERLAY`, opaque content that slides in from the right; touches on it never reach the panel underneath (separate window, no touch forwarding).

## Permissions

Accessibility service with `canRetrieveWindowContent="true"` plus `flagRetrieveInteractiveWindows` and `flagReportViewIds` (needed to see the System UI window ids; disclosed in the service description). No overlay permission, no notification access, no `WRITE_SECURE_SETTINGS`, no Shizuku. The Shizuku libraries are still on the classpath from the previous experiment but no Shizuku code runs.

## NOT TESTED (requires a finger; injected input cannot stand in)

- Physical top-right pull opening the real Quick Panel with the service enabled, from the launcher and from inside another app.
- Physical swipe from the handle into the companion, and the physical reverse swipe, with the panel confirmed still expanded during the cover.
- **Ten complete physical round trips** for reliability.
- Partial/cancelled physical horizontal swipes.
- Whether the 16 dp strip is comfortable to hit with a thumb and does not fight Samsung's own edge/scroll gestures under a finger.
- Physically toggling a Samsung tile before and after a round trip.

## Limitations / open points

- The handle sits at a fixed 66 % height on the right. If the owner's most-used tiles are there, the strip may crowd them; position is a tuning value, not yet validated by feel.
- Back closes everything (panel + companion), not just the companion. Acceptable as a safety exit; the intended page-close is the reverse swipe or the emergency button.
- Detection depends on the Samsung id `sec_quick_panel_compose_root`. A future One UI update could rename it; the detector should be revalidated after system updates (it fails safe: unknown id → no handle, never a false panel).
