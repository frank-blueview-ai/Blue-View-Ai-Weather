#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# BlueView Weather — WeatherDock installer for Linux Mint
# Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.
#
# Licensed under the BlueView Non-Commercial License v1.0.
# Free for personal and non-commercial use.
# Commercial use requires a paid license — contact: frank@blueview.ai
# See the LICENSE file in the project root for full terms.
# ─────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP="$SCRIPT_DIR/weatherdock.py"
DESKTOP_DIR="$HOME/.local/share/applications"
AUTOSTART_DIR="$HOME/.config/autostart"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  BlueView Weather — WeatherDock installer for Linux Mint"
echo "  Copyright (c) 2026 BlueView / Frank Perez"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo

# ── Python dependencies ────────────────────────────────────────────────────────
echo "▶  Installing Python dependencies …"
pip3 install --user PyQt6 PyQt6-WebEngine 2>&1 | tail -5
echo

# ── .desktop entry ─────────────────────────────────────────────────────────────
echo "▶  Installing application menu entry …"
mkdir -p "$DESKTOP_DIR"
cat > "$DESKTOP_DIR/blueview-weather.desktop" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=BlueView Weather
GenericName=Weather Panel
Comment=Floating weather panel with 7-day forecast and live radar map
Exec=python3 $APP
Icon=weather-overcast
Terminal=false
Categories=Utility;Weather;
Keywords=weather;forecast;radar;dock;panel;blueview;
StartupNotify=false
X-BlueView-Copyright=Copyright (c) 2026 BlueView / Frank Perez
EOF
chmod +x "$DESKTOP_DIR/blueview-weather.desktop"
echo "   Installed: $DESKTOP_DIR/blueview-weather.desktop"
echo

# ── Autostart (optional) ──────────────────────────────────────────────────────
read -rp "  Start BlueView Weather automatically on login? [y/N] " ans
if [[ "$ans" =~ ^[Yy]$ ]]; then
    mkdir -p "$AUTOSTART_DIR"
    cp "$DESKTOP_DIR/blueview-weather.desktop" "$AUTOSTART_DIR/blueview-weather.desktop"
    echo "   Autostart enabled: $AUTOSTART_DIR/blueview-weather.desktop"
fi

# ── Done ──────────────────────────────────────────────────────────────────────
echo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Installation complete!"
echo
echo "  Run:    python3 $APP"
echo "  Or:     Search 'BlueView Weather' in the app menu"
echo
echo "  Free API key:  https://openweathermap.org/api"
echo "  Commercial licensing:  frank@blueview.ai"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
