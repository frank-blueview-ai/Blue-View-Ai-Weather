# ─────────────────────────────────────────────────────────────────────────────
# Blue View AI Weather — Windows MSI builder (cx_Freeze)
# Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.
#
# Usage (on Windows, inside a venv with cx_Freeze installed):
#   pip install cx_Freeze PyQt6 PyQt6-WebEngine
#   python setup_win.py bdist_msi
#
# Output: dist/Blue-View-AI-Weather-1.0.0-amd64.msi
# ─────────────────────────────────────────────────────────────────────────────

import sys
from pathlib import Path
from cx_Freeze import setup, Executable

VERSION = "1.0.0"

build_options = {
    "packages": [
        "PyQt6",
        "PyQt6.QtWidgets",
        "PyQt6.QtCore",
        "PyQt6.QtGui",
        "PyQt6.QtWebEngineWidgets",
        "PyQt6.QtWebEngineCore",
        "PyQt6.QtWebChannel",
        "PyQt6.sip",
    ],
    "excludes": ["tkinter", "unittest", "email", "html", "http", "xml",
                 "pydoc", "doctest", "argparse", "difflib"],
    "include_files": [],
    "optimize": 2,
}

msi_options = {
    "upgrade_code":     "{A7B3C4D5-E6F7-4891-A234-B5C6D7E8F901}",
    "add_to_path":      False,
    "initial_target_dir": r"[ProgramFilesFolder]\BlueView\Blue View Weather",
    "install_icon":     "assets\\icon.ico",
    "summary_data": {
        "author":   "Frank Perez — BlueView",
        "comments": "Floating weather panel with live radar. No API key required.",
        "keywords": "weather forecast radar dock",
    },
}

executables = [
    Executable(
        "weatherdock.py",
        base="Win32GUI",           # no console window
        target_name="Blue View Weather.exe",
        icon="assets\\icon.ico",
        shortcut_name="Blue View Weather",
        shortcut_dir="ProgramMenuFolder",
        copyright="© 2026 BlueView / Frank Perez",
    )
]

setup(
    name="Blue View AI Weather",
    version=VERSION,
    description="Floating weather panel with live radar — no API key needed",
    author="Frank Perez",
    author_email="frank@blueview.ai",
    url="https://github.com/frank-blueview-ai/Blue-View-Ai-Weather",
    options={
        "build_exe": build_options,
        "bdist_msi": msi_options,
    },
    executables=executables,
)
