# Blue View AI Weather

**Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.**

A floating, dockable weather panel with live radar, 7-day forecast, and hourly drill-down.
No API key required — powered by [Open-Meteo](https://open-meteo.com) and [RainViewer](https://rainviewer.com).

![License: Non-Commercial](https://img.shields.io/badge/license-Non--Commercial-blue)
![Python](https://img.shields.io/badge/python-3.10%2B-blue)
![Qt](https://img.shields.io/badge/Qt-PyQt6-green)
![Version](https://img.shields.io/badge/version-1.0.0-brightgreen)

---

## Install

### 🐧 Linux (Debian / Ubuntu / Linux Mint)

**Option A — Download the .deb installer** *(recommended)*

1. Go to the [Releases](https://github.com/frank-blueview-ai/Blue-View-Ai-Weather/releases/latest) page
2. Download `blue-view-ai-weather_1.0.0_all.deb`
3. Open a terminal and run:

```bash
sudo dpkg -i blue-view-ai-weather_1.0.0_all.deb
sudo apt-get install -f
```

Then launch from your app menu or run `blueview-weather` in a terminal.

**Option B — pip**

```bash
pip install PyQt6 PyQt6-WebEngine
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/desktop
python3 weatherdock.py
```

**Option C — build the .deb yourself**

```bash
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/desktop
bash build_deb.sh
sudo dpkg -i blue-view-ai-weather_1.0.0_all.deb
```

---

### 🍎 macOS (11 Big Sur or later)

**Option A — Download the .dmg installer** *(recommended)*

1. Go to the [Releases](https://github.com/frank-blueview-ai/Blue-View-Ai-Weather/releases/latest) page
2. Download `Blue-View-AI-Weather-1.0.0.dmg`
3. Open the `.dmg` file
4. Drag **Blue View Weather** into your **Applications** folder
5. Double-click to launch

> **First launch:** macOS may show a security prompt because the app is not yet notarized.
> Right-click (or Control-click) the app → **Open** → **Open** to allow it.

**Option B — run from source**

```bash
# Requires Python 3.10+ from python.org or Homebrew
pip3 install PyQt6 PyQt6-WebEngine
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/desktop
python3 weatherdock.py
```

**Option C — build the .dmg yourself**

```bash
pip3 install pyinstaller PyQt6 PyQt6-WebEngine
brew install create-dmg
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/desktop
bash build_mac.sh
```

---

### 🪟 Windows (10 / 11, 64-bit)

**Option A — Download the .msi installer** *(recommended)*

1. Go to the [Releases](https://github.com/frank-blueview-ai/Blue-View-Ai-Weather/releases/latest) page
2. Download `Blue-View-AI-Weather-1.0.0-amd64.msi`
3. Double-click the `.msi` and follow the installer
4. Launch **Blue View Weather** from the Start menu

> **Windows SmartScreen:** Click **More info → Run anyway** if prompted
> (expected for new apps without an EV code-signing certificate).

**Option B — run from source**

```powershell
# Requires Python 3.10+ from python.org
pip install PyQt6 PyQt6-WebEngine
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/desktop
python weatherdock.py
```

**Option C — build the .msi yourself**

```powershell
pip install cx_Freeze PyQt6 PyQt6-WebEngine
git clone https://github.com/frank-blueview-ai/Blue-View-Ai-Weather.git
cd Blue-View-Ai-Weather/desktop
build_win.bat
# Installer appears in dist\
```

---

## Features

| Feature | Detail |
|---|---|
| **No API key** | Powered by Open-Meteo — completely free, no account required |
| **Current conditions** | Temperature, feels like, humidity, wind, visibility |
| **7-day forecast** | Collapsible panel — click header to expand or collapse |
| **Hourly drill-down** | Click any day card → hour-by-hour scroll; click again to collapse |
| **Live radar** | RainViewer precipitation tiles on a dark CartoDB basemap |
| **Floating window** | Frameless, translucent, draggable |
| **Dock to edge** | Snap to left or right screen edge with X11 strut hints (Linux) |
| **System tray** | Minimise to tray; left-click icon to restore |
| **Always on top** | 📌 toggle, remembered across restarts |
| **Auto-refresh** | Background thread, configurable 5–60 min |
| **About dialog** | Version, contact, and BlueView product links |

---

## Usage

| Action | How |
|---|---|
| Move the panel | Drag the title bar |
| Expand / collapse forecast | Click the **▸ 7-Day Forecast** header |
| Hourly detail | Click any day card; click same card again to close |
| Expand / collapse radar | Click the **▸ Radar Map** header |
| Dock to screen edge | Click **⊟** → Dock Left / Dock Right |
| Undock | Click **⊟** → Float |
| Always on top | Click 📌 |
| Minimise to tray | Click **—** |
| Refresh now | Click **↻** |
| Settings | Click **⚙** (city, units, refresh interval, opacity) |
| About | Click **ℹ** |
| Quit | Click **✕** or tray → Quit |

---

## Building packages locally

| Platform | Script | Output |
|---|---|---|
| 🐧 Linux | `bash build_deb.sh` | `blue-view-ai-weather_1.0.0_all.deb` |
| 🍎 macOS | `bash build_mac.sh` | `Blue-View-AI-Weather-1.0.0.dmg` |
| 🪟 Windows | `build_win.bat` | `dist\Blue-View-AI-Weather-1.0.0-amd64.msi` |

GitHub Actions builds all three automatically on every release tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The Actions workflow creates a GitHub Release and attaches all three installers as downloadable assets.

---

## Requirements

| Platform | Requirements |
|---|---|
| 🐧 Linux | Python 3.10+, PyQt6 ≥ 6.4, PyQt6-WebEngine, X11 |
| 🍎 macOS | macOS 11 Big Sur or later |
| 🪟 Windows | Windows 10 or 11 (64-bit) |

---

## License

Licensed under the **BlueView Non-Commercial License v1.0**.

- **Free** for personal, educational, research, and non-profit use.
- **Commercial use requires a paid license.**

Contact: **frank@blueview.ai** — Subject: `Commercial License — Blue View AI Weather`

See [LICENSE](LICENSE) for full terms.

---

## Credits

- Weather data: [Open-Meteo](https://open-meteo.com/) — free, no API key
- Radar tiles: [RainViewer](https://rainviewer.com/)
- Map tiles: [CartoDB](https://carto.com/) via [OpenStreetMap](https://www.openstreetmap.org/)
- Map library: [Leaflet.js](https://leafletjs.com/)

---

## BlueView Products

| | |
|---|---|
| 🖥  BlueView OS | [bvos.blueview.ai](https://bvos.blueview.ai) |
| 📄  My Papertrail | [mypapertrail.co](https://mypapertrail.co) |
| 🎧  Read2Me | [read2me.co](https://read2me.co) |
| 🌐  BlueView | [blueview.ai](https://blueview.ai) |

---

*© 2026 BlueView / Frank Perez — frank@blueview.ai*
