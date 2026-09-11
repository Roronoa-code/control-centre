#!/usr/bin/env bash
# Companion-page probes for the Quick Panel experiment. Injected input only; physical finger
# testing is separate. Requires an authorised device. No device identifiers are printed.
#
# Usage:  ADB_SERIAL=<serial> scripts/companion-probe.sh <step>
# Steps:  open notif in out partial scroll roundtrip back collapse sleep lockedqs outside state
#
# The handle position is read from the live window; if it cannot be parsed the step ABORTS.
# It must never fall back to a hard-coded coordinate (a wrong Y could touch Samsung controls).
set -uo pipefail
: "${ADB_SERIAL:?set ADB_SERIAL to the target device serial}"
ADB=${ADB:-adb}
adb(){ "$ADB" -s "$ADB_SERIAL" "$@"; }
stamp(){ adb shell date +%H:%M:%S.000 | tr -d '\r'; }
after(){ awk -v t="$1" '$1" "$2 >= "09-11 "t'; }  # keep log lines at/after a HH:MM:SS.mmm stamp today
applog(){ adb logcat -d -v time -s CC.Gesture:* 2>&1 | after "$1" | sed -E 's/ +/ /g; s# I/CC.Gesture\([0-9]+\):##' | cut -c1-160; }

# Independent evidence of the native shade: WindowManager view visibility (0x0 = VISIBLE).
shade_vis(){ adb shell dumpsys window windows 2>/dev/null | grep -a -A14 'u0 NotificationShade}' | grep -a -o -E 'mViewVisibility=0x[0-9]|isVisible=[a-z]+' | tr '\n' ' '; }
ours(){ adb shell dumpsys window windows 2>/dev/null | grep -a -o 'ControlCentre:[A-Za-z]*' | sort -u | tr '\n' ' '; }
state(){ echo "   shade: $(shade_vis)| ours: [$(ours)]"; }

# Mid-Y of the companion handle window, or empty. Callers MUST abort on empty.
handle_mid(){
  local block frame
  block=$(adb shell dumpsys window windows 2>/dev/null | grep -a -A60 'u0 ControlCentre:CompanionHandle}') || return 1
  [ -z "$block" ] && return 1
  frame=$(echo "$block" | grep -a -o -E ' frame=\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]' | head -1)
  [ -z "$frame" ] && return 1
  echo "$frame" | awk -F'[][,]' '{print int(($3+$6)/2)}'
}
need_handle(){ local y; y=$(handle_mid) || true; if [ -z "${y:-}" ]; then echo "ABORT: no companion handle window (Quick Panel not detected as open)"; exit 2; fi; echo "$y"; }

# Clean drags with input motionevent so the gesture does not leak to SystemUI like input swipe does.
hold_drag(){ # x0 y x1 -> DOWN, three MOVEs, leaves finger down
  adb shell input motionevent DOWN "$1" "$2"; adb shell input motionevent MOVE $(( ($1+$3)/2 )) "$2"; adb shell input motionevent MOVE "$3" "$2";
}
lift(){ adb shell input motionevent UP "$1" "$2"; }

case "${1:-}" in
  state) state; echo "   handle mid-y: $(handle_mid || echo none)";;
  open)  adb shell input keyevent KEYCODE_HOME; sleep 1; T=$(stamp); echo "## open"; adb shell input swipe 1300 30 1300 1500 300; sleep 2; applog "$T"; state; echo "   handle mid-y: $(handle_mid || echo none)";;
  notif) adb shell input keyevent KEYCODE_HOME; sleep 1; T=$(stamp); echo "## notif"; adb shell input swipe 200 30 200 1500 300; sleep 2; applog "$T"; state; adb shell cmd statusbar collapse; sleep 1;;
  in)    y=$(need_handle); T=$(stamp); echo "## in (handle y=$y, held drag)"; hold_drag 1425 "$y" 500; sleep 0.5; echo "   while held:"; state; lift 500 "$y"; sleep 0.6; echo "   after release:"; applog "$T"; state;;
  out)   T=$(stamp); echo "## out (held drag right)"; hold_drag 400 1500 1200; sleep 0.5; echo "   while held:"; state; lift 1200 1500; sleep 0.8; echo "   after release:"; applog "$T"; state;;
  roundtrip) # open must already be true; in then out, sampling the shade while covered
    y=$(need_handle); T=$(stamp); echo "## roundtrip"
    hold_drag 1425 "$y" 400; sleep 0.5; echo "   COVERED:"; state; lift 400 "$y"; sleep 0.6; echo "   page open:"; state
    hold_drag 400 1500 1200; sleep 0.4; echo "   returning:"; state; lift 1200 1500; sleep 0.8; echo "   RETURNED:"; state
    echo "-- collapse events (should be none):"; adb logcat -d -v time 2>&1 | after "$T" | grep -E "makeExpandedInvisible|animateCollapse" | sed -E 's/ +/ /g' | cut -c1-120 | head -4
    applog "$T";;
  partial) y=$(need_handle); T=$(stamp); echo "## partial"; adb shell input swipe 1425 "$y" 1250 "$y" 200; sleep 1; applog "$T"; state;;
  scroll) T=$(stamp); echo "## scroll (Samsung tile grid still receives touch)"; adb shell input swipe 700 2400 700 2000 250; sleep 0.6; adb shell input swipe 700 2000 700 2400 250; sleep 0.6; adb logcat -d -v time 2>&1 | after "$T" | grep -c "Delivering touch to (5747): action: 0x0" | sed 's/^/   SystemUI DOWN events: /'; state;;
  back) T=$(stamp); echo "## back"; adb shell input keyevent KEYCODE_BACK; sleep 1.2; applog "$T"; state;;
  collapse) T=$(stamp); echo "## collapse"; adb shell cmd statusbar collapse; sleep 1.2; applog "$T"; state;;
  sleep) T=$(stamp); echo "## sleep"; adb shell input keyevent KEYCODE_SLEEP; sleep 2; echo "   after sleep:"; state; adb shell input keyevent KEYCODE_WAKEUP; sleep 2; echo "   after wake:"; state;;
  lockedqs) T=$(stamp); echo "## lockedqs"; adb shell cmd statusbar expand-settings; sleep 2; applog "$T"; state; adb shell cmd statusbar collapse; sleep 1;;
  outside) adb shell input keyevent KEYCODE_HOME; sleep 1; echo "launcher: [$(ours)]"; adb shell am start -W -a android.settings.SETTINGS >/dev/null 2>&1; sleep 1.5; echo "settings app: [$(ours)]"; adb shell input keyevent KEYCODE_HOME;;
  *) echo "unknown step: ${1:-}"; exit 1;;
esac
