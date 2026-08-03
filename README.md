FoCullPointV2 — a lightweight Jetpack Compose app for quickly reviewing, previewing, and managing photos. It helps users inspect image metadata (EXIF), set or preserve focal points, and efficiently cull unwanted shots with fast, Glide-backed rendering. Preferences are stored with DataStore for a predictable review workflow.
Who it's for: photographers, content creators, and anyone who needs a speedy, metadata-aware photo-sorting tool. Run locally in Android Studio or on-device to preview and organize collections.


FoCullPointV2 is an Android app (Jetpack Compose) for handling image-related features and preferences.
Key info
•
ApplicationId: com.example.focullpointv2
•
Compile SDK: 37, Target SDK: 36, Min SDK: 27
•
Compose-enabled; JVM target Java 11
•
Instrumentation runner: androidx.test.runner.AndroidJUnitRunner
Tech
•
Kotlin + Jetpack Compose (Compose BOM)
•
AndroidX (appcompat, core-ktx, activity-compose, lifecycle viewmodel-compose)
•
Material3, Material icons
•
DataStore Preferences, ExifInterface, Glide
•
Testing: JUnit, Espresso
Quick start
1.
Prereqs: Android Studio (Arctic/Chipmunk+), Android SDK for API 36/37, Java 11
2.
From project root on Windows:
◦
gradlew.bat assembleDebug
◦
Open in Android Studio and Run on a device/emulator
3.
Instrumented tests:
◦
gradlew.bat connectedAndroidTest
Notes
•
Release build optimization is disabled in gradle config.
•
Compose tooling is included for debug previews.
