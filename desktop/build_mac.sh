#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# Blue View AI Weather — macOS .app + .dmg builder
# Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.
# ─────────────────────────────────────────────────────────────────────────────
# Requirements (run once):
#   pip install pyinstaller PyQt6 PyQt6-WebEngine
#   brew install create-dmg          # for the .dmg step
#
# Usage:  bash build_mac.sh
# Output: Blue-View-AI-Weather-1.0.0.dmg  (current directory)
# ─────────────────────────────────────────────────────────────────────────────
set -e

APP_NAME="Blue View Weather"
PKG_NAME="Blue-View-AI-Weather"
VERSION="1.0.0"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "→ Building $APP_NAME $VERSION …"

cd "$SCRIPT_DIR"

# ── 1. PyInstaller → .app ─────────────────────────────────────────────────────
pip install --quiet pyinstaller PyQt6 PyQt6-WebEngine 2>/dev/null || true

pyinstaller --clean --noconfirm weatherdock.spec

APP_PATH="dist/${APP_NAME}.app"

if [ ! -d "$APP_PATH" ]; then
    echo "✗ PyInstaller build failed — dist/${APP_NAME}.app not found"
    exit 1
fi

echo "✓ .app bundle created: $APP_PATH"

# ── 2. create-dmg → .dmg ──────────────────────────────────────────────────────
DMG_NAME="${PKG_NAME}-${VERSION}.dmg"

if command -v create-dmg &>/dev/null; then
    create-dmg \
        --volname "${APP_NAME}" \
        --volicon "assets/icon.icns" \
        --window-pos 200 120 \
        --window-size 600 400 \
        --icon-size 120 \
        --icon "${APP_NAME}.app" 160 185 \
        --hide-extension "${APP_NAME}.app" \
        --app-drop-link 430 185 \
        --no-internet-enable \
        "${SCRIPT_DIR}/${DMG_NAME}" \
        "dist/"
else
    # Fallback: plain hdiutil DMG (no background, no drag-to-Applications)
    echo "ℹ  create-dmg not found — building plain DMG via hdiutil"
    STAGING="$(mktemp -d)"
    cp -R "$APP_PATH" "$STAGING/"
    hdiutil create \
        -volname "${APP_NAME}" \
        -srcfolder "$STAGING" \
        -ov -format UDZO \
        "${SCRIPT_DIR}/${DMG_NAME}"
    rm -rf "$STAGING"
fi

echo ""
echo "✓ Package built: ${DMG_NAME}"
echo ""
echo "Install: open ${DMG_NAME}, drag '${APP_NAME}' to Applications"
