<div align="center">

# Nudge

**Reminders that won't let you forget.**

A native Android reminder app whose notifications stay pinned until you actually deal with the task.

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)

</div>

## Features

| | |
|---|---|
| **Sticky notifications** | When a reminder is due it rings and stays pinned. Swipe it away and it comes right back — it only clears when you tick ✓ or cross ✕ it in the app. |
| **Active & Completed** | Active tasks are grouped into *Ringing now*, *Today*, *Tomorrow*, *This week* and *Later*. Finished ones move to Completed, where you can restore or delete them. |
| **Quick scheduling** | One-tap picks like *In 15 min*, *Tonight*, *Tomorrow morning* or *This weekend*, plus a full date and time picker. |
| **Edit anytime** | Tap a task to change its title, notes, date or time. |
| **Repeat & priority** | Once, daily, weekdays, weekly or monthly. Low, medium or high priority. |
| **Undo** | Every done, skip and delete can be undone from the snackbar. |
| **Reliable alarms** | Exact alarms that survive reboots, app updates and time zone changes. |
| **Light & dark** | Follows your system theme, with a fluid, animated UI. |

## Install

Download the APK onto an Android phone (8.0 or newer), open it and allow installing from unknown sources. On first launch, allow notifications and tap **Allow** on the battery card so reminders are never delayed.

## Build

Requires JDK 17 and the Android SDK (API 35).

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`. To sign it, add a `keystore.properties` file at the project root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Without it the release build is unsigned.

## Tech

Kotlin · Jetpack Compose (Material 3) · Room · AlarmManager · Core SplashScreen
