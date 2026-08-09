# Blue View AI Weather

**Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.**

Live radar, 7-day forecast, and hourly drill-down — no API key required. Powered by
[Open-Meteo](https://open-meteo.com) and [RainViewer](https://rainviewer.com).

This repo contains all three Blue View Weather apps, one per platform:

| Platform | Folder | Stack |
|---|---|---|
| 🖥 Desktop (Linux / macOS / Windows) | [`desktop/`](desktop/) | Python, PyQt6 |
| 🤖 Android | [`android/`](android/) | Kotlin, Jetpack Compose |
| 🍎 iOS | [`ios/`](ios/) | Swift, SwiftUI |

Each has its own README with build/install instructions specific to that platform.

## Features

All three apps share the same core feature set and visual theme:

| Feature | Details |
|---|---|
| Current conditions | Temperature, feels-like, humidity, wind, visibility |
| 7-day forecast | Daily cards with high/low, tap a day for hourly detail |
| Hourly breakdown | Filtered by selected day — time, icon, temp, precipitation % |
| Live radar map | RainViewer tiles on a dark map, auto-refreshes every 5 min |
| Units | Metric (°C) or Imperial (°F) |
| No API key | Powered by Open-Meteo and RainViewer |

## CI / Releases

Each platform releases independently, on its own tag prefix, so tagging one app doesn't
kick off builds for the other two. All three can also be run manually from the Actions tab.

| Workflow | Tag prefix | Produces |
|---|---|---|
| [`desktop-build.yml`](.github/workflows/desktop-build.yml) | `desktop-v*` | `.deb` / `.dmg` / `.msi` installers, attached to a GitHub Release |
| [`android-build.yml`](.github/workflows/android-build.yml) | `android-v*` | Debug APK + Release AAB |
| [`ios-build.yml`](.github/workflows/ios-build.yml) | `ios-v*` | Archives and uploads to TestFlight |

```bash
git tag android-v1.0.9 && git push origin android-v1.0.9   # Android release only
git tag ios-v1.0.0     && git push origin ios-v1.0.0       # iOS release only
git tag desktop-v1.0.0 && git push origin desktop-v1.0.0   # Desktop release only
```

## License

Licensed under the **BlueView Non-Commercial License v1.0**.

- **Free** for personal, educational, research, and non-profit use.
- **Commercial use requires a paid license.**

Contact: **frank@blueview.ai** — Subject: `Commercial License — Blue View AI Weather`

See [LICENSE](LICENSE) for full terms.

## Author

**Frank Perez** — frank@blueview.ai

| | |
|--|--|
| Weather OS | [bvos.blueview.ai](https://bvos.blueview.ai) |
| Paper Trail | [mypapertrail.co](https://mypapertrail.co) |
| Read2Me | [read2me.co](https://read2me.co) |
| BlueView | [blueview.ai](https://blueview.ai) |
