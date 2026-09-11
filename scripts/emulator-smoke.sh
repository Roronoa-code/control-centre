#!/usr/bin/env bash
# CI emulator smoke: install the built debug APK, launch each page entry, keep screenshots and logs as evidence.
set -euo pipefail
mkdir -p emulator-evidence
adb wait-for-device
adb install -r evidence/app/build/outputs/apk/debug/app-debug.apk | tee emulator-evidence/install.log
adb shell am start -W -n com.mani.controlcentre/.ui.SettingsActivity | tee emulator-evidence/launch-settings.log
sleep 2
adb exec-out screencap -p > emulator-evidence/settings.png
for entry in EverydayEntry ToolsEntry DeviceEntry; do
  adb shell am start -W -n "com.mani.controlcentre/.access.$entry" | tee "emulator-evidence/launch-$entry.log"
  sleep 2
  adb exec-out screencap -p > "emulator-evidence/$entry.png"
done
adb logcat -d -s CC.Gesture AndroidRuntime > emulator-evidence/logcat.txt
! grep -q "FATAL EXCEPTION" emulator-evidence/logcat.txt
