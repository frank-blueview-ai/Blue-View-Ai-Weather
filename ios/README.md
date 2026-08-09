# Blue View Weather — iOS

Native iOS app for [Blue View Weather](https://blueview.ai) — live radar, 7-day forecast, and hourly drill-down. No API key required.

## Features

| Feature | Details |
|---------|---------|
| Current conditions | Temperature, feels-like, humidity, wind, visibility |
| 7-day forecast | Daily cards with high/low, tap a day for hourly detail |
| Hourly breakdown | Filtered by selected day — time, icon, temp, precipitation % |
| Live radar map | RainViewer tiles on a dark Leaflet map, auto-refreshes every 5 min |
| Units | Metric (°C) or Imperial (°F) |
| No API key | Powered by [Open-Meteo](https://open-meteo.com) and [RainViewer](https://rainviewer.com) |

## Requirements

- iOS 16.0+
- Xcode 15+ to build
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) to generate the `.xcodeproj`

## Build from source

```bash
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/ios
xcodegen generate
open BlueViewWeather.xcodeproj
```

`BlueViewWeather.xcodeproj` is generated from `project.yml` — don't edit it directly, edit
`project.yml` and re-run `xcodegen generate`.

## Tech stack

- Swift + SwiftUI
- URLSession (async/await) for API calls
- UserDefaults for preferences
- WKWebView + Leaflet for radar

## Release builds / TestFlight

Release archiving requires Xcode 26 (the current App Store minimum SDK requirement), so it
runs in CI rather than locally — see
[`.github/workflows/ios-build.yml`](../.github/workflows/ios-build.yml). Triggered by pushing
a `v*` tag or manually from the Actions tab. Needs these repo secrets configured:

| Secret | Description |
|---|---|
| `APPLE_API_KEY_ID` | App Store Connect API key ID |
| `APPLE_API_ISSUER_ID` | App Store Connect API issuer ID |
| `APPLE_API_KEY_P8` | Base64-encoded contents of the API key's `.p8` file |
| `APPLE_TEAM_ID` | Apple Developer Team ID |

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
