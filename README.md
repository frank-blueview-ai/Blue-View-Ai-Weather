# BlueView Weather — WeatherDock

**Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.**

A floating, dockable weather panel for Linux Mint with live radar map, foldable 7-day forecast, and hourly drill-down.

![License: Non-Commercial](https://img.shields.io/badge/license-Non--Commercial-blue)
![Python](https://img.shields.io/badge/python-3.10%2B-blue)
![Qt](https://img.shields.io/badge/Qt-PyQt6-green)
![Platform](https://img.shields.io/badge/platform-Linux%20Mint-orange)

---

## Features

| Feature | Detail |
|---|---|
| **Floating window** | Frameless, translucent, draggable — sits on top of your desktop |
| **Dock to edge** | ⊟ button → Dock Left / Dock Right / Float; sets X11 strut hints so maximised windows avoid it |
| **Edge snap** | Drag near any screen edge and release — window snaps flush automatically |
| **Pin / always-on-top** | 📌 toggle in the title bar, remembered across restarts |
| **System tray** | `—` button hides to tray; left-click tray icon to show again; right-click for menu |
| **7-day forecast** | Collapsible panel with animated expand/collapse |
| **Hourly drill-down** | Click any day card → 24-hour scroll appears below with temp + precip |
| **Live radar map** | RainViewer precipitation tiles on a CartoDB dark basemap (Leaflet) |
| **Settings** | City, units (°C / °F), refresh interval, opacity |
| **Auto-refresh** | Background thread, configurable 5 – 60 min interval |
| **No API key** | Powered by Open-Meteo — completely free, no account required |

---

## Installation

### Option 1 — pip (any machine with Python 3.10+)

```bash
pip install git+https://github.com/frank-blueview-ai/BlueView-Weather.git
blueview-weather
```

Or install locally from a clone:

```bash
git clone https://github.com/frank-blueview-ai/BlueView-Weather.git
cd BlueView-Weather
pip install .
blueview-weather
```

### Option 2 — .deb package (Debian / Ubuntu / Linux Mint)

```bash
git clone https://github.com/frank-blueview-ai/BlueView-Weather.git
cd BlueView-Weather
bash build_deb.sh
sudo dpkg -i blueview-weather_2.1.0_all.deb
sudo apt-get install -f          # resolves any missing apt dependencies
```

Then launch from your app menu or run `blueview-weather`.

> **If PyQt6 is not in apt** (older Linux Mint / Ubuntu):
> ```bash
> pip install PyQt6 PyQt6-WebEngine --user --break-system-packages
> sudo dpkg -i blueview-weather_2.1.0_all.deb
> ```

### Option 3 — run directly (development / no install)

```bash
git clone https://github.com/frank-blueview-ai/BlueView-Weather.git
cd BlueView-Weather
pip install PyQt6 PyQt6-WebEngine
python3 weatherdock.py
```

### Add to app menu (optional)

```bash
bash install.sh
```

---

## Usage

| Action | How |
|---|---|
| Move the panel | Drag the title bar |
| Snap to screen edge | Drag near any edge and release |
| Dock / undock | Click ⊟ → choose Left, Right, or Float |
| Toggle always-on-top | Click 📌 |
| Fold/unfold forecast | Click the **7-Day Forecast** section header |
| Hourly detail | Click any day card in the 7-day row |
| Fold/unfold radar | Click the **Radar Map** section header |
| Resize | Drag the bottom-right grip |
| Minimise to tray | Click `—`; left-click tray icon to restore |
| Quit | Click `✕` or tray → Quit |

---

## Requirements

- Linux with X11 (tested on Linux Mint 21/22 Cinnamon)
- Python 3.10+
- PyQt6 ≥ 6.4
- PyQt6-WebEngine ≥ 6.4 (for the radar map)

---

## License

This software is licensed under the **BlueView Non-Commercial License v1.0**.

- **Free** for personal, educational, research, and non-profit use.
- **Commercial use requires a paid license.**

Contact: **frank@blueview.ai** — Subject: `Commercial License Inquiry — BlueView Weather`

See [LICENSE](LICENSE) for full terms.

---

## Credits

- Weather data: [Open-Meteo](https://open-meteo.com/) — free, no API key
- Radar tiles: [RainViewer](https://rainviewer.com/)
- Map tiles: [CartoDB](https://carto.com/) via [OpenStreetMap](https://www.openstreetmap.org/)
- Map library: [Leaflet.js](https://leafletjs.com/)

---

*© 2026 BlueView / Frank Perez — frank@blueview.ai*
