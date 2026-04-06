# AutoHub — Car Dashboard for Ottocast P3 Pro

Native Kotlin + Jetpack Compose car dashboard app optimized for the **Ottocast P3 Pro** (Android 13, Snapdragon 6225, 8GB RAM).

## Features
- **Drive Tab** — Animated speed/RPM arc gauges with needles, fuel/range/efficiency stats, trip info
- **Vehicle Tab** — Top-down car diagram with tire pressures, fluid levels, service countdown, systems status
- **Climate Tab** — Temperature knob, fan speed, seat heaters, defrost, thermal overview
- Fullscreen immersive landscape mode
- Hardware-accelerated Canvas rendering
- arm64-v8a optimized for Snapdragon 6225

## Build & Install (Phone Only — No Computer Needed)

### Step 1: Create GitHub Repo
1. Open **GitHub mobile app** (or github.com in Safari)
2. Create a **new repository** called `autohub`
3. Upload all files from this project to the repo

### Step 2: Wait for Build
- GitHub Actions will automatically build the APK
- Go to **Actions** tab → click the latest run → wait ~3 min

### Step 3: Download APK
- In the completed Action run, scroll to **Artifacts**
- Download **AutoHub-debug.zip**
- Extract to get `app-debug.apk`

### Step 4: Install on P3 Pro
- Transfer the APK to your P3 Pro via:
  - **USB**: Connect phone to P3 Pro, copy APK to storage
  - **Cloud**: Upload to Google Drive, open on P3 Pro
  - **Direct**: Download the APK directly in P3 Pro's browser
- Open the APK on the P3 Pro → Install → Launch

### Step 5: Enable Unknown Sources (if needed)
On the P3 Pro: Settings → Security → Unknown Sources → Enable

## Tech Stack
- Kotlin 1.9.22
- Jetpack Compose (BOM 2024.01)
- Material3
- Target: Android 13 (API 33)
- ABI: arm64-v8a only
