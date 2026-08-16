# Word Overlay

Android overlay solver for word-puzzle games.

## What it does
- Floating draggable purple button over other apps.
- Tap button -> captures current screen.
- OCR reads the letter wheel.
- Detects 3+ letter word slots from the white grid.
- Finds valid words from the local Portuguese dictionary.
- Shows answers in a compact overlay panel for 8.5 seconds.
- Works locally after installation; OCR does not require an API key.

## Build
Requires Android Studio / Android SDK and internet access once to download Gradle/ML Kit dependencies.

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Permissions
The app needs:
- Display over other apps
- Screen capture (MediaProjection)
- Notifications (Android 13+)

The floating button intentionally uses a foreground service because Android restricts background execution.
