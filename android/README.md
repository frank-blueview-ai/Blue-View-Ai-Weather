# Blue View Weather — Android

Native Android app for [Blue View Weather](https://blueview.ai) — live radar, 7-day forecast, and hourly drill-down. No API key required.

## Features

| Feature | Details |
|---------|---------|
| Current conditions | Temperature, feels-like, humidity, wind, visibility |
| 7-day forecast | Daily cards with high/low, tap a day for hourly detail |
| Hourly breakdown | Filtered by selected day — time, icon, temp, precipitation % |
| Live radar map | RainViewer tiles on a dark Leaflet map, auto-refreshes every 5 min |
| Saved cities | Save as many cities as you like; switch from the title bar or Settings |
| Automatic location | Follows your current city by default, or pin a saved city instead |
| Units | Metric (°C) or Imperial (°F) |
| No API key | Powered by [Open-Meteo](https://open-meteo.com) and [RainViewer](https://rainviewer.com) |

## Requirements

- Android 8.0+ (API 26)
- Internet connection

## Install

### From GitHub Releases

1. Go to [Releases](https://github.com/frank-blueview-ai/Blue-View-Ai-Weather/releases)
2. Download `app-github-release.apk`
3. Enable "Install unknown apps" in Android Settings → Security
4. Open the APK to install

### Build from source

```bash
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/android
./gradlew assembleGithubDebug
adb install app/build/outputs/apk/github/debug/app-github-debug.apk
```

Requires JDK 17 and Android SDK 34.

## Tech stack

- Kotlin + Jetpack Compose
- Material Design 3
- Hilt dependency injection
- Retrofit + kotlinx-serialization for API calls
- DataStore for preferences
- Navigation Compose
- WebView + Leaflet for radar

## Author

**Frank Perez** — frank@blueview.ai

| | |
|--|--|
| Weather OS | [bvos.blueview.ai](https://bvos.blueview.ai) |
| Paper Trail | [mypapertrail.co](https://mypapertrail.co) |
| Read2Me | [read2me.co](https://read2me.co) |
| BlueView | [blueview.ai](https://blueview.ai) |

## License

© 2026 BlueView / Frank Perez. All rights reserved.
