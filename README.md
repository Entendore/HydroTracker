# HydroTracker

Offline-first hydration tracker (Jetpack Compose). Features: quick logging with
custom cups, 14 session templates with sip/recovery/pacing reminders, hot-day &
active-day goal boosts, streaks/badges/insights, 53 cited health tips, home-screen
widget, Quick Settings tile, CSV export, onboarding, and a one-time Pro IAP.

## Run
Open this folder in Android Studio → Sync → Run.
(No Gradle wrapper? Run `gradle wrapper --gradle-version 8.9` or let Studio create it.)

## Tests
./gradlew test                       # unit tests (JVM + Robolectric)
./gradlew connectedDebugAndroidTest  # worker + Compose UI tests (emulator required)

## Notes
- No INTERNET permission: all data stays on-device.
- For the IAP route, create in-app product `pro_unlock` ($4.99) in Play Console.
- Known open items: string extraction to resources, widget dark-mode variant.
