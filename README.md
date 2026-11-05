# RiseLock — Wake-Up Mission & Alarm Clock (Android)

**RiseLock** is an Android alarm application designed with a **guaranteed wake-up architecture** inspired by the warm, minimalist editorial design aesthetic of Claude.

Unlike standard system alarms that are easily swiped away or silenced in half-sleep, RiseLock is built with an **un-ignorable wake-up loop**:
- **No Dismiss or Snooze buttons** during active missions.
- **Persistent Full-Screen Takeover**: Rings directly over the Android lock screen.
- **Step Challenge**: Forces you to take 20 to 100 physical steps (monitored via device hardware step detector & accelerometer peak analysis).
- **Photo Match Challenge**: Forces you to physically walk to a registered spot (bathroom sink, kitchen stove, coffee maker) and capture a live matching photo. **Camera only — direct CameraX feed with zero gallery or file selection allowed.**
- **Bypasses Doze Mode**: Scheduled using `AlarmManager.setAlarmClock()`.
- **Max Volume Enforcement**: Rings on `AudioAttributes.USAGE_ALARM` with max volume lock and intercepts volume down keypresses.
- **Reboot Resilience**: Auto-reschedules all alarms after device reboots via `BootReceiver`.

---

## Design System (Claude-Inspired Editorial Warmth)

- **Canvas / Background**: Warm Alabaster (`#FAF8F5`)
- **Surfaces & Cards**: Warm Stone (`#EFECE6` / `#F7F4EE`)
- **Text Primary**: Deep Warm Charcoal (`#1A1A18`)
- **Text Secondary**: Muted Taupe (`#6E6B63`)
- **Interactive Accent**: Warm Terracotta / Rust (`#D97757`)
- **Dividers & Borders**: Hairline Warm Gray (`#E2DFD7`)
- **Typography**: Clean serif clock headings combined with legible modern body type.

---

## Project Structure

```
WakeLockAlarm/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/antigravity/riselock/
│       │   ├── MainActivity.kt               # Bottom navigation (Alarms, Timer, Missions)
│       │   ├── AlarmApplication.kt          # High-priority notification channels
│       │   ├── model/
│       │   │   ├── Alarm.kt                 # Alarm entity & trigger timestamp calculation
│       │   │   └── MissionType.kt           # NONE, STEPS, PHOTO_MATCH
│       │   ├── data/
│       │   │   └── AlarmRepository.kt       # Persistent JSON-backed storage
│       │   ├── service/
│       │   │   ├── AlarmScheduler.kt        # AlarmManager.setAlarmClock exact scheduler
│       │   │   ├── AlarmReceiver.kt         # Wakes CPU and launches RingingActivity
│       │   │   ├── BootReceiver.kt          # Reschedules alarms on device reboot
│       │   │   └── AlarmAudioService.kt     # Un-cancellable foreground audio loop
│       │   ├── mission/
│       │   │   ├── StepDetectorHelper.kt    # Step sensor with accelerometer peak fallback
│       │   │   └── ImageSimilarityMatcher.kt# dHash & 16x16 luminance similarity engine
│       │   └── ui/
│       │       ├── theme/                   # Claude warm palette & typography
│       │       ├── components/              # Warm cards, terracotta switches & buttons
│       │       └── screens/
│       │           ├── AlarmListScreen.kt   # Live clock, upcoming badge, alarm cards
│       │           ├── AlarmEditSheet.kt    # Hour/minute picker, repeat days, mission config
│       │           ├── TimerScreen.kt       # Circular sweep countdown & presets
│       │           ├── MissionsScreen.kt    # Interactive step tester & photo spot manager
│       │           ├── PhotoCaptureScreen.kt# Live CameraX feed (NO gallery picker)
│       │           └── RingingActivity.kt   # Lockscreen takeover with mission verification
```

---

## How to Open & Run in Android Studio

1. Open **Android Studio**.
2. Select **File > Open...** and choose `C:\All_Projects\WakeLockAlarm`.
3. Let Gradle sync dependencies (Jetpack Compose, CameraX, Kotlin Coroutines).
4. Connect an Android phone (or launch an Emulator).
5. Press **Run (Shift + F10)**.

### Testing Alarms & Missions

1. **Step Challenge**:
   - Create an alarm set for 1–2 minutes in the future.
   - Choose **👣 Steps** and set 20 or 30 steps.
   - Lock your phone screen.
   - When the alarm triggers, the screen wakes up automatically. The ringtone will loop at max volume. Walk around your room until the step counter hits your target — the alarm will then stop!

2. **Photo Match Challenge**:
   - Go to the **Missions** tab.
   - Tap **Bathroom Sink** or **Kitchen Counter** and take a reference photo using the in-app camera.
   - Set an alarm with **📷 Photo Match**.
   - When the alarm fires, tap "Open Camera & Match". Point at your spot and capture. When verified, the alarm turns off.

3. **Battery Optimization Exemption (Crucial for Xiaomi / Samsung / OnePlus)**:
   - On heavily customized OEM Android skins, disable battery optimization for RiseLock so the OS doesn't kill background services during deep overnight sleep.

