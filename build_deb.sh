#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# BlueView Weather — Debian package builder
# Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.
# Licensed under the BlueView Non-Commercial License v1.0.
# ─────────────────────────────────────────────────────────────────────────────
# Usage:  bash build_deb.sh
# Output: blueview-weather_2.1.0_all.deb  (current directory)
# Install: sudo dpkg -i blueview-weather_2.1.0_all.deb
#          sudo apt-get install -f   (resolves any missing deps)
# ─────────────────────────────────────────────────────────────────────────────
set -e

PKG="blueview-weather"
VER="1.0.0"
ARCH="all"
MAINTAINER="Frank Perez <frank@blueview.ai>"
DESCRIPTION="Floating/dockable weather panel for Linux Mint with live radar.
 Uses Open-Meteo (free, no API key). Includes 7-day forecast,
 hourly drill-down, dock-to-edge, and RainViewer radar overlay."

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$(mktemp -d)"
DEB_ROOT="$BUILD_DIR/${PKG}_${VER}_${ARCH}"

echo "→ Building $PKG $VER in $BUILD_DIR …"

# ── Directory structure ────────────────────────────────────────────────────────
install -d "$DEB_ROOT/DEBIAN"
install -d "$DEB_ROOT/usr/lib/blueview-weather"
install -d "$DEB_ROOT/usr/bin"
install -d "$DEB_ROOT/usr/share/applications"
install -d "$DEB_ROOT/usr/share/icons/hicolor/scalable/apps"
install -d "$DEB_ROOT/usr/share/doc/$PKG"

# ── App files ─────────────────────────────────────────────────────────────────
install -m 644 "$SCRIPT_DIR/weatherdock.py"   "$DEB_ROOT/usr/lib/blueview-weather/"
install -m 644 "$SCRIPT_DIR/LICENSE"          "$DEB_ROOT/usr/share/doc/$PKG/copyright"
install -m 644 "$SCRIPT_DIR/README.md"        "$DEB_ROOT/usr/share/doc/$PKG/"

# Launcher wrapper
cat > "$DEB_ROOT/usr/bin/blueview-weather" << 'LAUNCHER'
#!/usr/bin/env bash
# BlueView Weather launcher
if [ ! -f /usr/lib/x86_64-linux-gnu/libxcb-cursor.so.0 ] && [ -f /tmp/xcb-cursor/usr/lib/x86_64-linux-gnu/libxcb-cursor.so.0 ]; then
    export LD_LIBRARY_PATH="/tmp/xcb-cursor/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH"
fi
exec python3 /usr/lib/blueview-weather/weatherdock.py "$@"
LAUNCHER
chmod 755 "$DEB_ROOT/usr/bin/blueview-weather"

# ── Desktop file ───────────────────────────────────────────────────────────────
cat > "$DEB_ROOT/usr/share/applications/blueview-weather.desktop" << 'DESKTOP'
[Desktop Entry]
Version=1.0
Type=Application
Name=BlueView Weather
GenericName=Weather Dock
Comment=Floating weather panel with live radar
Exec=blueview-weather
Icon=blueview-weather
Terminal=false
Categories=Utility;X-Weather;
Keywords=weather;forecast;radar;dock;
StartupNotify=false
DESKTOP

# ── Icon ──────────────────────────────────────────────────────────────────────
if [ -f "$SCRIPT_DIR/assets/blueview-weather.svg" ]; then
    install -m 644 "$SCRIPT_DIR/assets/blueview-weather.svg" \
        "$DEB_ROOT/usr/share/icons/hicolor/scalable/apps/"
elif [ -f "$HOME/.local/share/icons/hicolor/scalable/apps/blueview-weather.svg" ]; then
    install -m 644 \
        "$HOME/.local/share/icons/hicolor/scalable/apps/blueview-weather.svg" \
        "$DEB_ROOT/usr/share/icons/hicolor/scalable/apps/"
fi

# ── DEBIAN/control ────────────────────────────────────────────────────────────
cat > "$DEB_ROOT/DEBIAN/control" << CTRL
Package: $PKG
Version: $VER
Architecture: $ARCH
Maintainer: $MAINTAINER
Depends: python3 (>= 3.10), python3-pyqt6 | python3-pyqt6-qt6,
 python3-pyqt6.qtwebengine | python3-pyqt6-webengine
Recommends: libxcb-cursor0
Section: utils
Priority: optional
Homepage: https://github.com/frank-blueview-ai/BlueView-Weather
Description: $DESCRIPTION
CTRL

# ── DEBIAN/postinst ───────────────────────────────────────────────────────────
cat > "$DEB_ROOT/DEBIAN/postinst" << 'POST'
#!/bin/sh
set -e
update-desktop-database /usr/share/applications 2>/dev/null || true
gtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true
POST
chmod 755 "$DEB_ROOT/DEBIAN/postinst"

# ── Build ─────────────────────────────────────────────────────────────────────
dpkg-deb --build --root-owner-group "$DEB_ROOT" \
    "$SCRIPT_DIR/${PKG}_${VER}_${ARCH}.deb"

rm -rf "$BUILD_DIR"
echo ""
echo "✓ Package built: ${PKG}_${VER}_${ARCH}.deb"
echo ""
echo "Install with:"
echo "  sudo dpkg -i ${PKG}_${VER}_${ARCH}.deb"
echo "  sudo apt-get install -f    # fix any missing deps"
echo ""
echo "Or via apt if PyQt6 isn't in apt:"
echo "  pip install PyQt6 PyQt6-WebEngine --user --break-system-packages"
echo "  sudo dpkg -i ${PKG}_${VER}_${ARCH}.deb"
