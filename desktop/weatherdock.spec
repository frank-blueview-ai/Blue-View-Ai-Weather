# -*- mode: python ; coding: utf-8 -*-
# ─────────────────────────────────────────────────────────────────────────────
# Blue View AI Weather — PyInstaller spec (macOS .app + Windows one-dir)
# Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.
# ─────────────────────────────────────────────────────────────────────────────
# Usage:
#   macOS:   pyinstaller weatherdock.spec
#   Windows: pyinstaller weatherdock.spec   (run build_win.bat instead)
# ─────────────────────────────────────────────────────────────────────────────

from PyInstaller.utils.hooks import collect_all, collect_submodules
import sys, os

# Collect everything PyQt6 needs (including WebEngine / Chromium)
qt_datas, qt_binaries, qt_hidden = collect_all('PyQt6')

a = Analysis(
    ['weatherdock.py'],
    pathex=[],
    binaries=qt_binaries,
    datas=qt_datas,
    hiddenimports=qt_hidden + [
        'PyQt6.QtWebEngineWidgets',
        'PyQt6.QtWebEngineCore',
        'PyQt6.QtWebChannel',
        'PyQt6.sip',
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=['tkinter', 'matplotlib', 'numpy', 'PIL'],
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='Blue View Weather',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=False,
    # macOS / Windows icon (place icon files alongside this spec)
    icon='assets/icon.icns' if sys.platform == 'darwin' else 'assets/icon.ico',
)

coll = COLLECT(
    exe,
    a.binaries,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='Blue View Weather',
)

# macOS .app bundle
if sys.platform == 'darwin':
    app = BUNDLE(
        coll,
        name='Blue View Weather.app',
        icon='assets/icon.icns',
        bundle_identifier='ai.blueview.weather',
        info_plist={
            'CFBundleDisplayName':        'Blue View Weather',
            'CFBundleName':               'Blue View Weather',
            'CFBundleShortVersionString': '1.0.0',
            'CFBundleVersion':            '1.0.0',
            'NSHighResolutionCapable':    True,
            'NSRequiresAquaSystemAppearance': False,
            'LSApplicationCategoryType':  'public.app-category.weather',
            'NSHumanReadableCopyright':   '© 2026 BlueView / Frank Perez',
        },
    )
