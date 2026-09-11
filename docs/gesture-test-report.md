# Gesture test report — feasibility pass

Device: Samsung SM-S938B (Galaxy S25 Ultra), One UI 9.0 (`ro.build.version.oneui=90000`), Android 17 / API 37, build `CP2A.260605.016.S938BXXUCZZI4`, 1440×3120 @ 600 dpi (density 3.75). Wireless ADB. Debug APK from commit `8f17781` (SHA-256 `83569a9ab0fad7b81c40235721636d30a677b4005e87293544051b4de558a2df`).

Test date: 11 September 2026, 17:10–17:14. Input method for this pass: `adb shell input swipe` (goes through the same InputDispatcher window targeting as a finger). **Physical finger swipes have not yet been recorded**; see "Still open".

## Setup observed

- Accessibility service enabled manually; `Service connected`, `Trigger windows: TOP, SIDE` logged 90 ms later. The service reconnected by itself after an `adb install -r` update.
- `dumpsys window windows`: `ControlCentre:TOP` is a 360×180 px (96×48 dp) `ACCESSIBILITY_OVERLAY` at the top-right; `ControlCentre:SIDE` is 60×360 px (16×96 dp) at the right edge, y 1224–1584. Both are above Samsung's `StatusBar` (`ty=STATUS_BAR`, 128 px tall) and `NotificationShade` windows in the z-order.
- Another accessibility service (`com.quarkstudio.glyf`) was already enabled; no interaction observed.

## Results

| Probe | Input | What happened | Verdict |
| --- | --- | --- | --- |
| Top-right pull | (1380,30) → (1380,700), 300 ms | Our overlay received DOWN, MOVE, UP (dy 179 dp). SystemUI `QuickPanelLog` shows **no** `dispatchTouchEvent ACTION_DOWN` and no `makeExpandedVisible` during the gesture. Panel opened on Everyday; `Displayed … +64 ms`. Repeated after reinstall: same. | Pass (injected) |
| Top-right pull with rail | (1380,30) → (1080,420) | `page=tools progress=100%` during the pull; UP `dx=-80dp dy=104dp commit=tools`; panel opened on Tools. | Pass (injected) |
| Native top-left pull | (200,30) → (200,900) | SystemUI received the DOWN at (200,30), `ShadeControllerImpl.makeExpandedVisible`; notification shade opened (screenshot); our app logged nothing. Collapsed with `cmd statusbar collapse`. | Native shade intact |
| Side handle, straight in (before fix) | (1425,1404) → (1050,1404) | `SIDE DOWN` then `SIDE CANCEL` 17 ms later; `InputManagerService: pilferPointers` at the same instant = Samsung's edge Back gesture stole the pointer. Focus stayed on the launcher. | Fail → fixed |
| Side handle, diagonal up (before fix) | (1425,1404) → (1120,1100) | Same cancellation, same `pilferPointers`. | Fail → fixed |
| Side handle, diagonal down (before fix) | (1425,1404) → (1120,1710) | Not stolen; `commit=device`; panel opened on Device (`+51 ms`). | Pass |
| Side handle, all three (after `systemGestureExclusionRects` on the handle) | as above | Straight → Everyday, up → Tools (`+67 ms`), down → Device (`+73 ms`). No `pilferPointers`. | Pass (injected) |

Launch path: `PanelAccessibilityService.startActivity` → `PanelActivity` (own task, translucent, `noHistory`). No background-activity-launch block was logged; the service is system-bound, which is a documented exemption.

## What this establishes

- On this One UI 9 build a `TYPE_ACCESSIBILITY_OVERLAY` window placed over the top-right status-bar area receives the pull-down before Samsung's status bar, for injected input. Samsung's shade did not move underneath.
- The native notification pull from the top-left is unaffected. Samsung's split Quick Panel is still reachable from the part of the right half that is left of our 96 dp strip (not yet exercised).
- The right-edge handle needs the gesture-exclusion rect; without it the Back gesture wins for near-horizontal swipes.

## Still open (needs a finger, not ADB)

1. Physical top-right pulls starting at the glass edge, at least 10 in a row, from the launcher, Settings, a browser and a video app. Record which page opened and whether the shade flashed.
2. Physical top-left and top-centre pulls afterwards (notifications and Samsung Quick Panel).
3. Physical side-handle swipes and whether the Back gesture still works just above/below the handle.
4. Touch-rejection behaviour at the very edge (y = 0) with a case on the phone.

## How to reproduce the injected probes

```powershell
adb -s <serial> shell input swipe 1380 30 1380 700 300     # top-right pull -> Everyday
adb -s <serial> shell input swipe 1380 30 1080 420 300     # pull + rail -> Tools
adb -s <serial> shell input swipe 200 30 200 900 300       # native shade (top-left)
adb -s <serial> shell input swipe 1425 1404 1050 1404 200  # side handle -> Everyday
adb -s <serial> logcat -d -s CC.Gesture:*
```
