#!/usr/bin/env python3
# ─────────────────────────────────────────────────────────────────────────────
# BlueView Weather — WeatherDock
# Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.
#
# Licensed under the BlueView Non-Commercial License v1.0.
# Free for personal and non-commercial use.
# Commercial use requires a paid license — contact: frank@blueview.ai
# See the LICENSE file in the project root for full terms.
# ─────────────────────────────────────────────────────────────────────────────
"""
WeatherDock — Floating/dockable weather panel for Linux Mint
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Weather:  Open-Meteo (https://open-meteo.com) — free, no API key
Radar:    RainViewer  (https://rainviewer.com)  — free
Install:  pip install PyQt6 PyQt6-WebEngine
Run:      python3 weatherdock.py
"""

from __future__ import annotations

import subprocess
import sys
import json
import urllib.request
import urllib.parse
import urllib.error
from datetime import datetime
from typing import Optional

from PyQt6.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QPushButton, QLineEdit, QSystemTrayIcon, QMenu, QFrame,
    QSizeGrip, QScrollArea, QDialog, QFormLayout, QDialogButtonBox,
    QCheckBox, QComboBox, QSlider, QToolButton,
)
from PyQt6.QtCore import (
    Qt, QTimer, QPoint, QRect, QUrl, QSettings,
    pyqtSignal, QObject, QThread,
)
from PyQt6.QtGui import (
    QIcon, QFont, QColor, QPainter, QLinearGradient,
    QBrush, QPen, QAction, QPixmap, QPainterPath, QCursor,
)

try:
    from PyQt6.QtWebEngineWidgets import QWebEngineView
    from PyQt6.QtWebEngineCore import QWebEngineSettings
    HAS_WEBENGINE = True
except ImportError:
    HAS_WEBENGINE = False

# ═══════════════════════════════════════════════════════════════════════════════
# Constants
# ═══════════════════════════════════════════════════════════════════════════════

APP_NAME      = "WeatherDock"
APP_DISPLAY   = "Blue View Weather"
APP_ORG       = "BlueView"
VERSION       = "1.0.0"
GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
FORECAST_URL  = "https://api.open-meteo.com/v1/forecast"

WIND_DIRS = ["N","NE","E","SE","S","SW","W","NW"]
DAY_NAMES = ["Mon","Tue","Wed","Thu","Fri","Sat","Sun"]

WMO: dict[int, tuple[str, str, str]] = {
    0:  ("☀",  "☾",  "Clear Sky"),       1:  ("☀",  "☾",  "Mainly Clear"),
    2:  ("⛅",  "🌤", "Partly Cloudy"),   3:  ("☁",  "☁",  "Overcast"),
    45: ("🌫", "🌫", "Fog"),             48: ("🌫", "🌫", "Freezing Fog"),
    51: ("🌦", "🌧", "Light Drizzle"),   53: ("🌦", "🌧", "Drizzle"),
    55: ("🌧", "🌧", "Heavy Drizzle"),   61: ("🌧", "🌧", "Light Rain"),
    63: ("🌧", "🌧", "Rain"),            65: ("🌧", "🌧", "Heavy Rain"),
    66: ("🌧", "🌧", "Light Freezing Rain"), 67: ("🌧", "🌧", "Freezing Rain"),
    71: ("❄",  "❄",  "Light Snow"),      73: ("❄",  "❄",  "Snow"),
    75: ("❄",  "❄",  "Heavy Snow"),      77: ("❄",  "❄",  "Snow Grains"),
    80: ("🌦", "🌧", "Light Showers"),   81: ("🌦", "🌧", "Showers"),
    82: ("🌧", "🌧", "Heavy Showers"),   85: ("❄",  "❄",  "Snow Showers"),
    86: ("❄",  "❄",  "Heavy Snow Showers"),
    95: ("⛈",  "⛈",  "Thunderstorm"),
    96: ("⛈",  "⛈",  "Thunderstorm + Hail"),
    99: ("⛈",  "⛈",  "Thunderstorm + Heavy Hail"),
}

def wmo_icon(code: int, is_day: bool = True) -> str:
    e = WMO.get(code, WMO[3]); return e[0] if is_day else e[1]

def wmo_desc(code: int) -> str:
    return WMO.get(code, WMO[3])[2]

ACNT    = QColor(82, 190, 232)
BORD    = QColor(255, 255, 255, 22)
MENU_SS = (
    "QMenu{background:#0b0e1c;color:#dae2f8;"
    "border:1px solid rgba(255,255,255,0.13);border-radius:6px;padding:4px;}"
    "QMenu::item{padding:5px 18px;border-radius:4px;}"
    "QMenu::item:selected{background:rgba(82,190,232,0.18);}"
    "QMenu::separator{height:1px;background:rgba(255,255,255,0.08);margin:3px 0;}"
)

# ═══════════════════════════════════════════════════════════════════════════════
# Stylesheet
# ═══════════════════════════════════════════════════════════════════════════════

QSS = """
* { font-family: 'Ubuntu', 'Noto Sans', 'DejaVu Sans', sans-serif; }
QWidget { background: transparent; color: #dae2f8; }
QLabel  { background: transparent; }
QPushButton, QToolButton {
    background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.13);
    border-radius: 5px; color: #dae2f8; padding: 3px 9px; font-size: 12px;
}
QPushButton:hover, QToolButton:hover {
    background: rgba(82,190,232,0.18); border-color: rgba(82,190,232,0.45); color: #fff;
}
QPushButton:pressed, QToolButton:pressed { background: rgba(82,190,232,0.10); }
QPushButton:checked, QToolButton:checked {
    background: rgba(82,190,232,0.22); border-color: #52bee8; color: #52bee8;
}
QToolButton { padding: 2px; border-radius: 4px; }
QToolButton::menu-indicator { image: none; }
QLineEdit {
    background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.16);
    border-radius: 6px; color: #dae2f8; padding: 5px 10px;
    selection-background-color: rgba(82,190,232,0.38);
}
QLineEdit:focus { border-color: rgba(82,190,232,0.55); }
QComboBox {
    background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.16);
    border-radius: 6px; color: #dae2f8; padding: 4px 8px;
}
QComboBox::drop-down { border: none; width: 20px; }
QComboBox::down-arrow { width: 10px; }
QComboBox QAbstractItemView {
    background: #12162a; border: 1px solid rgba(255,255,255,0.18); color: #dae2f8;
    selection-background-color: rgba(82,190,232,0.28); outline: none;
}
QSlider::groove:horizontal { height:4px;background:rgba(255,255,255,0.10);border-radius:2px; }
QSlider::handle:horizontal { background:#52bee8;border-radius:7px;width:14px;height:14px;margin:-5px 0; }
QSlider::sub-page:horizontal { background:#52bee8;border-radius:2px; }
QCheckBox { color:#dae2f8;spacing:7px; }
QCheckBox::indicator {
    width:16px;height:16px;border-radius:3px;
    background:rgba(255,255,255,0.07);border:1px solid rgba(255,255,255,0.22);
}
QCheckBox::indicator:checked { background:#52bee8;border-color:#52bee8; }
QScrollArea, QScrollBar { background: transparent; border: none; }
QScrollBar:vertical { width:5px;background:transparent;margin:0; }
QScrollBar:horizontal { height:5px;background:transparent;margin:0; }
QScrollBar::handle:vertical, QScrollBar::handle:horizontal {
    background:rgba(255,255,255,0.15);border-radius:2px;min-height:24px;min-width:24px;
}
QScrollBar::add-line, QScrollBar::sub-line { height:0;width:0; }
QDialog { background:#0b0e1c;border:1px solid rgba(255,255,255,0.14);border-radius:12px; }
QFormLayout QLabel { color:#7887aa;font-size:12px; }
QDialogButtonBox QPushButton { min-width:80px;padding:6px 14px; }
"""


# ═══════════════════════════════════════════════════════════════════════════════
# Radar HTML
# ═══════════════════════════════════════════════════════════════════════════════

def make_radar_html(lat: float, lon: float, city: str) -> str:
    city_safe = city.replace("'", "\\'")
    return f"""<!DOCTYPE html>
<html><head><meta charset="utf-8">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  *{{margin:0;padding:0}}html,body{{width:100%;height:100%;overflow:hidden;background:#0b0e1c}}
  #map{{width:100%;height:100%}}
  .leaflet-control-attribution{{font-size:9px;opacity:0.35!important;background:rgba(0,0,0,0.4)!important;color:#aaa!important}}
  .leaflet-control-attribution a{{color:#52bee8!important}}
  .leaflet-control-zoom a{{background:#12162a!important;color:#dae2f8!important;border-color:rgba(255,255,255,0.12)!important;font-size:16px!important;line-height:26px!important}}
  .leaflet-control-zoom a:hover{{background:rgba(82,190,232,0.2)!important}}
  .leaflet-tooltip{{background:rgba(11,14,28,0.88);border:1px solid rgba(82,190,232,0.4);color:#dae2f8;border-radius:5px;font-size:12px;padding:3px 8px}}
  .leaflet-tooltip-top::before{{border-top-color:rgba(82,190,232,0.35)}}
</style></head><body><div id="map"></div>
<script>
(function(){{
  var map=L.map('map',{{zoomControl:true,preferCanvas:true}}).setView([{lat},{lon}],7);
  L.tileLayer('https://{{s}}.basemaps.cartocdn.com/dark_all/{{z}}/{{x}}/{{y}}{{r}}.png',{{
    attribution:'©<a href="https://www.openstreetmap.org/copyright">OSM</a> ©<a href="https://carto.com/">CARTO</a>',
    subdomains:'abcd',maxZoom:19
  }}).addTo(map);
  var radarLayer=null,lastTime=null;
  function loadRadar(){{
    fetch('https://api.rainviewer.com/public/weather-maps.json')
      .then(r=>r.json()).then(d=>{{
        if(!d.radar||!d.radar.past||!d.radar.past.length)return;
        var f=d.radar.past[d.radar.past.length-1];
        if(f.time===lastTime)return; lastTime=f.time;
        if(radarLayer)map.removeLayer(radarLayer);
        radarLayer=L.tileLayer(d.host+f.path+'/256/{{z}}/{{x}}/{{y}}/4/1_1.png',
          {{opacity:0.62,zIndex:10}}).addTo(map);
      }}).catch(()=>{{}});
  }}
  loadRadar();setInterval(loadRadar,300000);
  L.circleMarker([{lat},{lon}],{{color:'#52bee8',fillColor:'#52bee8',fillOpacity:0.9,radius:7,weight:2}})
   .addTo(map).bindTooltip('{city_safe}',{{direction:'top',offset:[0,-10]}});
  if(window.ResizeObserver)new ResizeObserver(()=>map.invalidateSize()).observe(document.body);
}})();
</script></body></html>"""


# ═══════════════════════════════════════════════════════════════════════════════
# Weather worker
# ═══════════════════════════════════════════════════════════════════════════════

class WeatherWorker(QObject):
    data_ready = pyqtSignal(dict)
    error      = pyqtSignal(str)
    finished   = pyqtSignal()

    def __init__(self, city: str, units: str):
        super().__init__()
        self.city  = city
        self.units = units

    def run(self):
        try:
            hdrs = {"User-Agent": f"{APP_NAME}/{VERSION}"}

            geo_params = urllib.parse.urlencode(
                {"name": self.city, "count": 1, "language": "en", "format": "json"})
            with urllib.request.urlopen(
                    urllib.request.Request(f"{GEOCODING_URL}?{geo_params}", headers=hdrs),
                    timeout=14) as r:
                geo = json.loads(r.read())

            results = geo.get("results")
            if not results:
                self.error.emit(f"City not found: {self.city}")
                return

            hit        = results[0]
            lat        = hit["latitude"]
            lon        = hit["longitude"]
            city_label = f"{hit.get('name', self.city)}, {hit.get('country_code','')}"

            temp_unit  = "fahrenheit" if self.units == "imperial" else "celsius"
            speed_unit = "mph"        if self.units == "imperial" else "kmh"

            fc_params = urllib.parse.urlencode({
                "latitude":         lat, "longitude":        lon,
                "current":          "temperature_2m,relative_humidity_2m,"
                                    "apparent_temperature,weather_code,"
                                    "wind_speed_10m,wind_direction_10m,visibility,is_day",
                "hourly":           "temperature_2m,precipitation_probability,"
                                    "weather_code,wind_speed_10m,is_day",
                "daily":            "weather_code,temperature_2m_max,"
                                    "temperature_2m_min,precipitation_probability_max",
                "temperature_unit": temp_unit, "wind_speed_unit": speed_unit,
                "timezone":         "auto",    "forecast_days":   7,
            })
            with urllib.request.urlopen(
                    urllib.request.Request(f"{FORECAST_URL}?{fc_params}", headers=hdrs),
                    timeout=14) as r2:
                fc = json.loads(r2.read())

            self.data_ready.emit({
                "current": fc["current"], "hourly": fc["hourly"],
                "daily":   fc["daily"],   "city":   city_label,
                "lat":     lat,           "lon":    lon,
            })
        except urllib.error.URLError as e:
            self.error.emit(f"Network: {e.reason}")
        except Exception as e:
            self.error.emit(str(e))
        finally:
            self.finished.emit()


# ═══════════════════════════════════════════════════════════════════════════════
# Collapsible section
#
# Root issue with the previous implementation: the section body was inside a
# QScrollArea whose content widget always stretches to fill the viewport, so
# hiding/animating the body never made the window shrink.
#
# Fix: CollapsibleSection calls self.window().adjustSize() after each toggle.
# The main window has no QScrollArea stretch — its layout is a plain VBox so
# adjustSize() computes the exact natural height of visible widgets.
# ═══════════════════════════════════════════════════════════════════════════════

class CollapsibleSection(QWidget):
    def __init__(self, title: str, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self._expanded = True
        self._title    = title

        root = QVBoxLayout(self)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)

        self.btn = QPushButton()
        self.btn.setFlat(True)
        self.btn.setStyleSheet("""
            QPushButton {
                text-align: left; padding: 7px 14px;
                background: rgba(255,255,255,0.04); border: none;
                border-top: 1px solid rgba(255,255,255,0.07);
                border-radius: 0; font-size: 10px; font-weight: 700;
                color: #7887aa; letter-spacing: 1.5px;
            }
            QPushButton:hover { background: rgba(255,255,255,0.07); color: #dae2f8; }
        """)
        self.btn.clicked.connect(self._toggle)
        root.addWidget(self.btn)

        self.body = QWidget()
        root.addWidget(self.body)
        self.body_layout = QVBoxLayout(self.body)
        self.body_layout.setContentsMargins(0, 0, 0, 0)
        self.body_layout.setSpacing(0)
        self._update_label()

    def set_title(self, title: str):
        self._title = title
        self._update_label()

    def add_widget(self, w: QWidget):
        self.body_layout.addWidget(w)

    def expand(self):
        if not self._expanded:
            self._expanded = True
            self._update_label()
            self.body.setVisible(True)
            QTimer.singleShot(0, self._refit)

    def collapse(self):
        if self._expanded:
            self._expanded = False
            self._update_label()
            self.body.setVisible(False)
            QTimer.singleShot(0, self._refit)

    def _update_label(self):
        self.btn.setText(f"  {'▾' if self._expanded else '▸'}   {self._title}")

    def _toggle(self):
        if self._expanded:
            self.collapse()
        else:
            self.expand()

    def _refit(self):
        # Walk up to the top-level window and resize it to fit the new layout.
        w = self.window()
        if w and w is not self:
            w.adjustSize()


# ═══════════════════════════════════════════════════════════════════════════════
# Hourly card
# ═══════════════════════════════════════════════════════════════════════════════

class HourlyCard(QFrame):
    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self.setFixedWidth(54)
        self.setFixedHeight(96)
        lay = QVBoxLayout(self)
        lay.setContentsMargins(4, 6, 4, 6)
        lay.setSpacing(1)
        lay.setAlignment(Qt.AlignmentFlag.AlignCenter)

        def lbl(txt: str, ss: str) -> QLabel:
            l = QLabel(txt); l.setAlignment(Qt.AlignmentFlag.AlignCenter)
            l.setStyleSheet(ss); return l

        self.time_lbl = lbl("—", "color:#7887aa;font-size:9px;font-weight:700;")
        self.icon_lbl = lbl("☁", "font-size:20px;")
        self.temp_lbl = lbl("—", "color:#dae2f8;font-size:12px;font-weight:600;")
        self.pop_lbl  = lbl("",  "color:#52bee8;font-size:9px;")
        for w in (self.time_lbl, self.icon_lbl, self.temp_lbl, self.pop_lbl):
            lay.addWidget(w)

    def paintEvent(self, ev):
        p = QPainter(self)
        p.setRenderHint(QPainter.RenderHint.Antialiasing)
        path = QPainterPath()
        path.addRoundedRect(0, 0, self.width(), self.height(), 7, 7)
        p.fillPath(path, QBrush(QColor(255, 255, 255, 8)))
        p.setPen(QPen(BORD, 1))
        p.drawPath(path)

    def update_data(self, hour_str: str, code: int, temp: float,
                    sym: str, pop: int, is_day: bool):
        try:
            dt  = datetime.strptime(hour_str, "%Y-%m-%dT%H:%M")
            lbl = dt.strftime("%-I %p").replace(" AM","a").replace(" PM","p")
        except Exception:
            lbl = hour_str[-5:]
        self.time_lbl.setText(lbl)
        self.icon_lbl.setText(wmo_icon(code, is_day))
        self.temp_lbl.setText(f"{temp:.0f}{sym}")
        self.pop_lbl.setText(f"💧{pop}%" if pop >= 10 else "")


# ═══════════════════════════════════════════════════════════════════════════════
# Hourly panel — horizontal scroll of 24 cards for one day
# ═══════════════════════════════════════════════════════════════════════════════

class HourlyPanel(QWidget):
    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        lay = QVBoxLayout(self)
        lay.setContentsMargins(10, 4, 10, 8)
        lay.setSpacing(0)

        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAsNeeded)
        scroll.setFixedHeight(106)
        scroll.setStyleSheet("background:transparent;border:none;")
        lay.addWidget(scroll)

        self._row = QWidget()
        self._row.setStyleSheet("background:transparent;")
        self._hlay = QHBoxLayout(self._row)
        self._hlay.setContentsMargins(0, 0, 0, 0)
        self._hlay.setSpacing(5)
        self._hlay.addStretch()
        scroll.setWidget(self._row)
        self._cards: list[HourlyCard] = []

    def update_data(self, date: str, hourly: dict, units: str):
        sym   = "°F" if units == "imperial" else "°C"
        times = hourly.get("time", [])
        slots = [i for i, t in enumerate(times) if t.startswith(date)]

        while self._hlay.count() > 1:
            item = self._hlay.takeAt(0)
            if item.widget():
                item.widget().deleteLater()
        self._cards.clear()

        for i in slots:
            c = HourlyCard()
            c.update_data(
                times[i],
                hourly["weather_code"][i],
                hourly["temperature_2m"][i],
                sym,
                int(hourly.get("precipitation_probability", [0]*len(times))[i] or 0),
                bool(hourly.get("is_day", [1]*len(times))[i]),
            )
            self._hlay.insertWidget(self._hlay.count() - 1, c)
            self._cards.append(c)


# ═══════════════════════════════════════════════════════════════════════════════
# 7-day forecast card
# ═══════════════════════════════════════════════════════════════════════════════

class ForecastCard(QFrame):
    day_clicked = pyqtSignal(str)

    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self._date     = ""
        self._selected = False
        self.setMinimumWidth(44)
        self.setMinimumHeight(108)
        self.setCursor(QCursor(Qt.CursorShape.PointingHandCursor))

        lay = QVBoxLayout(self)
        lay.setContentsMargins(5, 8, 5, 10)
        lay.setSpacing(2)
        lay.setAlignment(Qt.AlignmentFlag.AlignCenter)

        def lbl(txt: str, ss: str) -> QLabel:
            l = QLabel(txt); l.setAlignment(Qt.AlignmentFlag.AlignCenter)
            l.setStyleSheet(ss); return l

        self.day_lbl  = lbl("—", "color:#7887aa;font-size:10px;font-weight:700;letter-spacing:0.5px;")
        self.icon_lbl = lbl("☁", "font-size:24px;")
        self.hi_lbl   = lbl("—", "color:#ffb74d;font-size:13px;font-weight:700;")
        self.lo_lbl   = lbl("—", "color:#4a5878;font-size:11px;font-weight:500;")
        self.pop_lbl  = lbl("",  "color:#52bee8;font-size:9px;")
        for w in (self.day_lbl, self.icon_lbl, self.hi_lbl, self.lo_lbl, self.pop_lbl):
            lay.addWidget(w)

    def paintEvent(self, ev):
        p = QPainter(self)
        p.setRenderHint(QPainter.RenderHint.Antialiasing)
        path = QPainterPath()
        path.addRoundedRect(0, 0, self.width(), self.height(), 8, 8)
        fill   = QColor(82, 190, 232, 22) if self._selected else QColor(255, 255, 255, 9)
        border = QColor(82, 190, 232, 90) if self._selected else BORD
        p.fillPath(path, QBrush(fill))
        p.setPen(QPen(border, 1)); p.drawPath(path)

    def set_selected(self, on: bool):
        self._selected = on; self.update()

    def mousePressEvent(self, ev):
        if ev.button() == Qt.MouseButton.LeftButton and self._date:
            self.day_clicked.emit(self._date)
        super().mousePressEvent(ev)

    def update_data(self, day: str, date: str, code: int,
                    hi: float, lo: float, sym: str, pop: int):
        self._date = date
        self._selected = False
        self.day_lbl.setText(day)
        self.icon_lbl.setText(wmo_icon(code, is_day=True))
        self.hi_lbl.setText(f"{hi:.0f}{sym}")
        self.lo_lbl.setText(f"{lo:.0f}{sym}")
        self.pop_lbl.setText(f"💧{pop}%" if pop >= 10 else "")
        self.update()


# ═══════════════════════════════════════════════════════════════════════════════
# 7-day forecast row
# ═══════════════════════════════════════════════════════════════════════════════

class ForecastWidget(QWidget):
    day_selected = pyqtSignal(str)

    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        lay = QHBoxLayout(self)
        lay.setContentsMargins(10, 8, 10, 10)
        lay.setSpacing(5)
        self.cards = [ForecastCard() for _ in range(7)]
        for c in self.cards:
            c.day_clicked.connect(self._on_card_click)
            lay.addWidget(c, 1)

    def clear_selection(self):
        for c in self.cards:
            c.set_selected(False)

    def _on_card_click(self, date: str):
        for c in self.cards:
            c.set_selected(c._date == date)
        self.day_selected.emit(date)

    def update_data(self, daily: dict, units: str):
        sym   = "°F" if units == "imperial" else "°C"
        today = datetime.now().strftime("%Y-%m-%d")
        times = daily.get("time", [])
        for i, card in enumerate(self.cards):
            if i < len(times):
                date  = times[i]
                label = "Today" if date == today else DAY_NAMES[datetime.strptime(date, "%Y-%m-%d").weekday()]
                card.update_data(
                    label, date,
                    daily["weather_code"][i],
                    daily["temperature_2m_max"][i],
                    daily["temperature_2m_min"][i],
                    sym,
                    int(daily.get("precipitation_probability_max", [0]*7)[i] or 0))
                card.setVisible(True)
            else:
                card.setVisible(False)


# ═══════════════════════════════════════════════════════════════════════════════
# Current conditions
# ═══════════════════════════════════════════════════════════════════════════════

class CurrentWeatherWidget(QWidget):
    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        root = QVBoxLayout(self)
        root.setContentsMargins(16, 16, 16, 8)
        root.setSpacing(6)

        self.city_lbl = QLabel("—")
        self.city_lbl.setStyleSheet(
            "color:#52bee8;font-size:13px;font-weight:700;letter-spacing:0.5px;")

        main_row = QHBoxLayout()
        main_row.setSpacing(10)
        main_row.setAlignment(Qt.AlignmentFlag.AlignLeft | Qt.AlignmentFlag.AlignVCenter)

        self.icon_lbl = QLabel("☁")
        self.icon_lbl.setStyleSheet("font-size:54px;")
        self.icon_lbl.setFixedWidth(72)
        self.icon_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)

        rhs = QVBoxLayout()
        rhs.setSpacing(0)
        self.temp_lbl = QLabel("—")
        self.temp_lbl.setStyleSheet(
            "color:#dae2f8;font-size:44px;font-weight:200;letter-spacing:-2px;")
        self.desc_lbl = QLabel("—")
        self.desc_lbl.setStyleSheet("color:#7887aa;font-size:13px;")
        self.hilo_lbl = QLabel("")
        self.hilo_lbl.setStyleSheet("color:#ffb74d;font-size:11px;font-weight:600;")
        rhs.addWidget(self.temp_lbl); rhs.addWidget(self.desc_lbl)
        rhs.addWidget(self.hilo_lbl); rhs.addStretch()
        main_row.addWidget(self.icon_lbl); main_row.addLayout(rhs); main_row.addStretch()

        det_row = QHBoxLayout(); det_row.setSpacing(14)
        for attr, txt in [("feels_lbl","Feels —"),("humid_lbl","💧 —%"),
                           ("wind_lbl","💨 —"),   ("vis_lbl",  "👁 —")]:
            l = QLabel(txt); l.setStyleSheet("color:#7887aa;font-size:11px;")
            setattr(self, attr, l); det_row.addWidget(l)
        det_row.addStretch()

        self.updated_lbl = QLabel("")
        self.updated_lbl.setStyleSheet("color:#2e3650;font-size:10px;")

        root.addWidget(self.city_lbl); root.addLayout(main_row)
        root.addLayout(det_row); root.addWidget(self.updated_lbl)

    def update_data(self, data: dict, units: str):
        cur    = data["current"]; daily = data["daily"]; city = data["city"]
        sym    = "°F" if units == "imperial" else "°C"
        spd    = "mph" if units == "imperial" else "km/h"
        code   = cur["weather_code"]; is_day = bool(cur.get("is_day", 1))
        t      = cur["temperature_2m"]; feels = cur["apparent_temperature"]
        humid  = cur["relative_humidity_2m"]
        wsp    = cur["wind_speed_10m"]; wdir = cur.get("wind_direction_10m", 0)
        vis_km = cur.get("visibility", 0) / 1000
        t_hi   = daily["temperature_2m_max"][0]; t_lo = daily["temperature_2m_min"][0]
        dir_lbl = WIND_DIRS[int((wdir + 22.5) / 45) % 8]
        self.city_lbl.setText(f"  {city}")
        self.icon_lbl.setText(wmo_icon(code, is_day))
        self.temp_lbl.setText(f"{t:.0f}{sym}"); self.desc_lbl.setText(wmo_desc(code))
        self.hilo_lbl.setText(f"↑ {t_hi:.0f}  ↓ {t_lo:.0f}  {sym}")
        self.feels_lbl.setText(f"Feels {feels:.0f}{sym}")
        self.humid_lbl.setText(f"💧 {humid}%")
        self.wind_lbl.setText(f"💨 {wsp:.0f} {spd} {dir_lbl}")
        self.vis_lbl.setText(f"👁 {vis_km:.0f} km")
        self.updated_lbl.setText(f"  Updated {datetime.now().strftime('%H:%M')}  •  Open-Meteo")


# ═══════════════════════════════════════════════════════════════════════════════
# Settings dialog
# ═══════════════════════════════════════════════════════════════════════════════

class SettingsDialog(QDialog):
    def __init__(self, settings: QSettings, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self.settings = settings
        self.setWindowTitle(f"{APP_NAME} — Settings")
        self.setMinimumWidth(360)
        self.setStyleSheet(QSS)

        root = QVBoxLayout(self); root.setContentsMargins(22, 22, 22, 18); root.setSpacing(16)
        hdr = QLabel("⚙   Settings")
        hdr.setStyleSheet("font-size:16px;font-weight:700;color:#dae2f8;")
        root.addWidget(hdr)

        form = QFormLayout(); form.setSpacing(10)
        form.setLabelAlignment(Qt.AlignmentFlag.AlignRight)

        self.city_edit = QLineEdit(settings.value("city", ""))
        self.city_edit.setPlaceholderText("London, New York, Tokyo …")

        self.units_box = QComboBox()
        self.units_box.addItems(["metric  (°C, km/h)", "imperial  (°F, mph)"])
        self.units_box.setCurrentIndex(1 if settings.value("units","metric")=="imperial" else 0)

        self.interval_box = QComboBox()
        self._intervals = [5, 10, 15, 30, 60]
        for l in ["5 min","10 min","15 min","30 min","1 hour"]:
            self.interval_box.addItem(l)
        iv = settings.value("refresh_min", 10, type=int)
        self.interval_box.setCurrentIndex(
            self._intervals.index(iv) if iv in self._intervals else 1)

        self.ontop_chk   = QCheckBox("Always on top")
        self.ontop_chk.setChecked(settings.value("always_on_top", True, type=bool))
        self.opacity_sld = QSlider(Qt.Orientation.Horizontal)
        self.opacity_sld.setRange(30, 100)
        self.opacity_sld.setValue(settings.value("opacity", 93, type=int))

        form.addRow("City:",    self.city_edit)
        form.addRow("Units:",   self.units_box)
        form.addRow("Refresh:", self.interval_box)
        form.addRow("Opacity:", self.opacity_sld)
        form.addRow("",         self.ontop_chk)
        root.addLayout(form)

        note = QLabel("Weather: Open-Meteo · Radar: RainViewer — no API key needed.")
        note.setStyleSheet("font-size:11px;color:#4a5878;")
        root.addWidget(note)

        btns = QDialogButtonBox(QDialogButtonBox.StandardButton.Ok |
                                QDialogButtonBox.StandardButton.Cancel)
        btns.accepted.connect(self.accept); btns.rejected.connect(self.reject)
        root.addWidget(btns)

    def save(self):
        self.settings.setValue("city", self.city_edit.text().strip())
        self.settings.setValue("units", "imperial" if self.units_box.currentIndex() else "metric")
        self.settings.setValue("refresh_min", self._intervals[self.interval_box.currentIndex()])
        self.settings.setValue("always_on_top", self.ontop_chk.isChecked())
        self.settings.setValue("opacity", self.opacity_sld.value())


# ═══════════════════════════════════════════════════════════════════════════════
# About dialog
# ═══════════════════════════════════════════════════════════════════════════════

class AboutDialog(QDialog):
    def __init__(self, parent: Optional[QWidget] = None):
        super().__init__(parent)
        self.setWindowTitle(f"About {APP_DISPLAY}")
        self.setMinimumWidth(380)
        self.setStyleSheet(QSS)

        root = QVBoxLayout(self)
        root.setContentsMargins(28, 28, 28, 22)
        root.setSpacing(0)

        # App icon + name
        icon_lbl = QLabel("🌤")
        icon_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
        icon_lbl.setStyleSheet("font-size:48px;background:transparent;")
        root.addWidget(icon_lbl)
        root.addSpacing(8)

        name_lbl = QLabel(APP_DISPLAY)
        name_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
        name_lbl.setStyleSheet(
            "font-size:20px;font-weight:700;color:#dae2f8;letter-spacing:0.5px;")
        root.addWidget(name_lbl)
        root.addSpacing(4)

        ver_lbl = QLabel(f"Version {VERSION}")
        ver_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
        ver_lbl.setStyleSheet("font-size:12px;color:#7887aa;")
        root.addWidget(ver_lbl)
        root.addSpacing(6)

        tagline = QLabel("Floating weather panel for Linux\n7-day forecast · Hourly drill-down · Live radar")
        tagline.setAlignment(Qt.AlignmentFlag.AlignCenter)
        tagline.setStyleSheet("font-size:12px;color:#52bee8;line-height:1.6;")
        root.addWidget(tagline)
        root.addSpacing(18)

        # Divider
        line = QFrame(); line.setFrameShape(QFrame.Shape.HLine)
        line.setStyleSheet("border:none;border-top:1px solid rgba(255,255,255,0.08);")
        root.addWidget(line)
        root.addSpacing(14)

        # Details grid
        def row(label: str, value: str, link: bool = False):
            w = QWidget(); w.setStyleSheet("background:transparent;")
            h = QHBoxLayout(w); h.setContentsMargins(0, 2, 0, 2); h.setSpacing(10)
            lbl_w = QLabel(label)
            lbl_w.setFixedWidth(80)
            lbl_w.setStyleSheet("color:#4a5878;font-size:11px;font-weight:700;"
                                "letter-spacing:0.5px;background:transparent;")
            lbl_w.setAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
            val_w = QLabel(f'<a href="{value}" style="color:#52bee8;text-decoration:none;">'
                           f'{value}</a>' if link else value)
            val_w.setStyleSheet("color:#dae2f8;font-size:11px;background:transparent;")
            if link:
                val_w.setOpenExternalLinks(True)
            h.addWidget(lbl_w); h.addWidget(val_w); h.addStretch()
            return w

        root.addWidget(row("Author",  "Frank Perez"))
        root.addWidget(row("Email",   "frank@blueview.ai",      link=True))
        root.addWidget(row("OS",      "bvos.blueview.ai",       link=True))
        root.addWidget(row("Paper",   "mypapertrail.co",        link=True))
        root.addWidget(row("Read2Me", "read2me.co",             link=True))
        root.addWidget(row("Web",     "blueview.ai",            link=True))
        root.addSpacing(14)

        # Powered-by
        pw = QLabel("Powered by  <a href='https://open-meteo.com' "
                    "style='color:#52bee8;text-decoration:none;'>Open-Meteo</a>  ·  "
                    "<a href='https://rainviewer.com' "
                    "style='color:#52bee8;text-decoration:none;'>RainViewer</a>")
        pw.setAlignment(Qt.AlignmentFlag.AlignCenter)
        pw.setStyleSheet("font-size:11px;color:#4a5878;background:transparent;")
        pw.setOpenExternalLinks(True)
        root.addWidget(pw)
        root.addSpacing(4)

        copy_lbl = QLabel(f"© 2026 BlueView / Frank Perez. All rights reserved.")
        copy_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
        copy_lbl.setStyleSheet("font-size:10px;color:#2e3650;background:transparent;")
        root.addWidget(copy_lbl)
        root.addSpacing(16)

        # Close button
        line2 = QFrame(); line2.setFrameShape(QFrame.Shape.HLine)
        line2.setStyleSheet("border:none;border-top:1px solid rgba(255,255,255,0.08);")
        root.addWidget(line2)
        root.addSpacing(12)

        close_btn = QPushButton("Close")
        close_btn.setFixedWidth(100)
        close_btn.clicked.connect(self.accept)
        btn_row = QHBoxLayout(); btn_row.addStretch()
        btn_row.addWidget(close_btn); btn_row.addStretch()
        root.addLayout(btn_row)


# ═══════════════════════════════════════════════════════════════════════════════
# Main window
# ═══════════════════════════════════════════════════════════════════════════════

class WeatherDock(QWidget):
    SNAP_THRESHOLD = 24

    def __init__(self):
        super().__init__()
        self.settings       = QSettings(APP_ORG, APP_NAME)
        self._drag_pos: Optional[QPoint]    = None
        self._thread: Optional[QThread]     = None
        self._worker: Optional[WeatherWorker] = None
        self._lat           = 42.47
        self._lon           = -70.95
        self._city          = "—"
        self._hourly_data: dict             = {}
        self._selected_date: str            = ""
        self._is_docked     = False
        self._pre_dock_geo: Optional[QRect] = None

        self._build_ui()
        self._build_tray()
        self._apply_window_prefs()
        self._start_timer()

        if self.settings.value("city", "").strip():
            QTimer.singleShot(400, self._refresh)
        else:
            self._show_setup(True)

    # ── UI ───────────────────────────────────────────────────────────────────

    def _build_ui(self):
        self.setWindowTitle(APP_NAME)
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)
        self.setWindowFlags(Qt.WindowType.FramelessWindowHint | Qt.WindowType.Tool)
        self.setMinimumWidth(330)
        self.setMaximumWidth(500)
        self.setStyleSheet(QSS)

        outer = QVBoxLayout(self)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.setSpacing(0)

        # ── title bar ──────────────────────────────────────────────────────────
        tbar = QWidget(); tbar.setFixedHeight(38); tbar.setObjectName("tbar")
        tbar.setStyleSheet("""QWidget#tbar {
            background:rgba(8,11,22,0.85);border-radius:12px 12px 0 0;
            border-bottom:1px solid rgba(255,255,255,0.07);}""")
        tb = QHBoxLayout(tbar); tb.setContentsMargins(12,0,8,0); tb.setSpacing(4)
        brand = QLabel("🌤  Blue View Weather")
        brand.setStyleSheet("font-size:12px;font-weight:700;color:#52bee8;"
                            "letter-spacing:0.8px;background:transparent;")
        tb.addWidget(brand); tb.addStretch()
        self._status_lbl = QLabel("")
        self._status_lbl.setStyleSheet("font-size:11px;color:#52bee8;background:transparent;")
        tb.addWidget(self._status_lbl)

        def tb_btn(text, tip, checkable=False):
            b = QToolButton(); b.setText(text); b.setFixedSize(26,26)
            b.setToolTip(tip)
            if checkable: b.setCheckable(True)
            return b

        self._refresh_btn = tb_btn("↻","Refresh now")
        self._refresh_btn.clicked.connect(self._refresh)

        self._pin_btn = tb_btn("📌","Always on top",checkable=True)
        self._pin_btn.setChecked(self.settings.value("always_on_top",True,type=bool))
        self._pin_btn.toggled.connect(self._toggle_pin)

        self._dock_btn = tb_btn("⊟","Dock to screen edge")
        dock_menu = QMenu(self); dock_menu.setStyleSheet(MENU_SS)
        dock_menu.addAction("◧  Dock Left").triggered.connect(lambda: self._dock_to_edge("left"))
        dock_menu.addAction("◨  Dock Right").triggered.connect(lambda: self._dock_to_edge("right"))
        dock_menu.addSeparator()
        dock_menu.addAction("◻  Float (restore)").triggered.connect(self._undock)
        self._dock_btn.setMenu(dock_menu)
        self._dock_btn.setPopupMode(QToolButton.ToolButtonPopupMode.InstantPopup)

        about_btn = tb_btn("ℹ","About Blue View Weather")
        about_btn.clicked.connect(self._open_about)
        settings_btn = tb_btn("⚙","Settings")
        settings_btn.clicked.connect(self._open_settings)
        min_btn = tb_btn("—","Minimise to tray")
        min_btn.clicked.connect(self._hide_to_tray)
        close_btn = tb_btn("✕","Quit")
        close_btn.setStyleSheet("QToolButton{border-radius:4px;}"
                                "QToolButton:hover{background:rgba(220,60,60,0.35);color:#ff7070;}")
        close_btn.clicked.connect(QApplication.quit)

        for b in (self._refresh_btn,self._pin_btn,self._dock_btn,
                  about_btn,settings_btn,min_btn,close_btn):
            tb.addWidget(b)
        outer.addWidget(tbar)

        # ── content (NO QScrollArea — layout drives window height) ─────────────
        content = QWidget(); content.setStyleSheet("background:transparent;")
        outer.addWidget(content)
        clay = QVBoxLayout(content); clay.setContentsMargins(0,0,0,0); clay.setSpacing(0)

        self._current_w = CurrentWeatherWidget()
        clay.addWidget(self._current_w)

        # Forecast section contains: 7-day row + hourly sub-section
        self._fc_sec = CollapsibleSection("7-Day Forecast")
        fc_inner = QWidget(); fc_inner.setStyleSheet("background:transparent;")
        fc_inner_lay = QVBoxLayout(fc_inner)
        fc_inner_lay.setContentsMargins(0,0,0,0); fc_inner_lay.setSpacing(0)

        self._fc_w = ForecastWidget()
        self._fc_w.day_selected.connect(self._on_day_selected)
        fc_inner_lay.addWidget(self._fc_w)

        # Hourly sub-section (collapsed by default)
        self._hr_sec = CollapsibleSection("Hourly — tap a day above")
        self._hr_w   = HourlyPanel()
        self._hr_sec.add_widget(self._hr_w)
        self._hr_sec.collapse()          # start collapsed
        fc_inner_lay.addWidget(self._hr_sec)

        self._fc_sec.add_widget(fc_inner)
        clay.addWidget(self._fc_sec)

        # Radar section
        self._radar_sec = CollapsibleSection("Radar Map")
        if HAS_WEBENGINE:
            self._radar_view = QWebEngineView()
            self._radar_view.setFixedHeight(240)
            self._radar_view.page().settings().setAttribute(
                QWebEngineSettings.WebAttribute.LocalContentCanAccessRemoteUrls, True)
            self._radar_sec.add_widget(self._radar_view)
        else:
            no_radar = QLabel("⚠  pip install PyQt6-WebEngine")
            no_radar.setAlignment(Qt.AlignmentFlag.AlignCenter)
            no_radar.setStyleSheet("color:#7887aa;font-size:12px;padding:24px;")
            self._radar_view = None
            self._radar_sec.add_widget(no_radar)
        clay.addWidget(self._radar_sec)

        self._setup_panel = self._make_setup_panel()
        clay.addWidget(self._setup_panel)
        self._setup_panel.setVisible(False)

        # ── bottom grip ────────────────────────────────────────────────────────
        btm = QWidget(); btm.setFixedHeight(18)
        btm.setStyleSheet("background:rgba(8,11,22,0.7);border-radius:0 0 12px 12px;"
                          "border-top:1px solid rgba(255,255,255,0.06);")
        bbl = QHBoxLayout(btm); bbl.setContentsMargins(0,2,6,2); bbl.addStretch()
        grip = QSizeGrip(self); grip.setFixedSize(14,14); bbl.addWidget(grip)
        outer.addWidget(btm)

    def _make_setup_panel(self) -> QWidget:
        w = QWidget(); lay = QVBoxLayout(w)
        lay.setContentsMargins(20,18,20,20); lay.setSpacing(10)
        intro = QLabel("Welcome to WeatherDock\n\nEnter your city to get started.\nNo API key needed.")
        intro.setAlignment(Qt.AlignmentFlag.AlignCenter)
        intro.setStyleSheet("color:#7887aa;font-size:12px;")
        lay.addWidget(intro)
        self._setup_city = QLineEdit()
        self._setup_city.setPlaceholderText("City, e.g.  Miami, US  or  London")
        lay.addWidget(self._setup_city)
        go_btn = QPushButton("  Get Weather  ")
        go_btn.setStyleSheet("""QPushButton{background:rgba(82,190,232,0.18);
            border:1px solid rgba(82,190,232,0.40);border-radius:6px;color:#52bee8;
            font-size:12px;font-weight:700;padding:7px 14px;}
            QPushButton:hover{background:rgba(82,190,232,0.28);}""")
        go_btn.clicked.connect(self._quick_setup); lay.addWidget(go_btn)
        note = QLabel("Powered by Open-Meteo — free, no account required.")
        note.setAlignment(Qt.AlignmentFlag.AlignCenter)
        note.setStyleSheet("font-size:10px;color:#3a4870;"); lay.addWidget(note)
        return w

    # ── Tray ─────────────────────────────────────────────────────────────────

    def _build_tray(self):
        pix = QPixmap(22,22); pix.fill(Qt.GlobalColor.transparent)
        p = QPainter(pix); p.setRenderHint(QPainter.RenderHint.Antialiasing)
        p.setBrush(QBrush(ACNT)); p.setPen(Qt.PenStyle.NoPen)
        p.drawEllipse(2,2,18,18)
        p.setPen(QPen(QColor(255,255,255,220)))
        f = QFont("sans-serif",9); f.setBold(True); p.setFont(f)
        p.drawText(pix.rect(), Qt.AlignmentFlag.AlignCenter, "W"); p.end()

        self._tray = QSystemTrayIcon(QIcon(pix), self)
        self._tray.setToolTip(APP_NAME)
        menu = QMenu(); menu.setStyleSheet(MENU_SS)
        show_act = QAction("⊞  Show / Hide",self); refresh_act = QAction("↻  Refresh",self)
        cfg_act  = QAction("⚙  Settings",  self);  quit_act    = QAction("✕  Quit",   self)
        show_act.triggered.connect(self._toggle_visible)
        refresh_act.triggered.connect(self._refresh)
        cfg_act.triggered.connect(self._open_settings)
        quit_act.triggered.connect(QApplication.quit)
        menu.addAction(show_act); menu.addAction(refresh_act)
        menu.addSeparator(); menu.addAction(cfg_act)
        menu.addSeparator(); menu.addAction(quit_act)
        self._tray.setContextMenu(menu)
        self._tray.activated.connect(
            lambda r: self._toggle_visible()
            if r == QSystemTrayIcon.ActivationReason.Trigger else None)
        self._tray.show()

    def _toggle_visible(self):
        if self.isVisible(): self.hide()
        else: self.show(); self.raise_(); self.activateWindow()

    def _hide_to_tray(self):
        self.hide()
        if not self.settings.value("tray_hint_shown", False, type=bool):
            self._tray.showMessage(APP_NAME,
                "WeatherDock is running in the system tray.\n"
                "Left-click the tray icon to restore it.",
                QSystemTrayIcon.MessageIcon.Information, 4000)
            self.settings.setValue("tray_hint_shown", True)

    # ── Dock ─────────────────────────────────────────────────────────────────

    def _dock_to_edge(self, edge: str):
        if not self._is_docked:
            self._pre_dock_geo = self.geometry()
        self._is_docked = True
        self.settings.setValue("dock_edge", edge)
        scr = QApplication.primaryScreen().availableGeometry()
        w   = max(self.width(), 340)
        self.setMinimumWidth(w); self.setMaximumWidth(w)
        self.resize(w, scr.height())
        self.move(scr.right() - w if edge == "right" else scr.left(), scr.top())
        self._dock_btn.setChecked(True)
        self._try_set_strut(edge, w, scr)

    def _undock(self):
        self._is_docked = False
        self.settings.remove("dock_edge")
        self.setMinimumWidth(330); self.setMaximumWidth(500)
        if self._pre_dock_geo:
            self.setGeometry(self._pre_dock_geo)
        self._dock_btn.setChecked(False)
        self._try_clear_strut()

    def _try_set_strut(self, edge, width, scr):
        try:
            wid  = str(int(self.winId()))
            full = QApplication.primaryScreen().geometry()
            if edge == "right":
                vals = (f"0,{full.width()-scr.right()+width},0,0,"
                        f"0,0,{scr.top()},{scr.bottom()},0,0,0,0")
            else:
                vals = (f"{width},0,0,0,"
                        f"{scr.top()},{scr.bottom()},0,0,0,0,0,0")
            subprocess.run(["xprop","-id",wid,"-f","_NET_WM_STRUT_PARTIAL","32c",
                            "-set","_NET_WM_STRUT_PARTIAL",vals],
                           capture_output=True,timeout=2)
        except Exception:
            pass

    def _try_clear_strut(self):
        try:
            subprocess.run(["xprop","-id",str(int(self.winId())),
                            "-f","_NET_WM_STRUT_PARTIAL","32c",
                            "-set","_NET_WM_STRUT_PARTIAL",
                            "0,0,0,0,0,0,0,0,0,0,0,0"],
                           capture_output=True,timeout=2)
        except Exception:
            pass

    # ── Window prefs ─────────────────────────────────────────────────────────

    def _apply_window_prefs(self):
        self._set_on_top(self.settings.value("always_on_top", True, type=bool))
        self.setWindowOpacity(self.settings.value("opacity", 93, type=int) / 100.0)
        edge = self.settings.value("dock_edge", "")
        if edge in ("left","right"):
            QTimer.singleShot(200, lambda: self._dock_to_edge(edge))
            return
        pos  = self.settings.value("window_pos")
        size = self.settings.value("window_size")
        if pos:  self.move(pos)
        else:
            scr = QApplication.primaryScreen().availableGeometry()
            self.adjustSize()
            self.move(scr.right()-self.width()-20, scr.top()+40)
        if size: self.resize(size)

    def _set_on_top(self, on: bool):
        flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.Tool
        if on: flags |= Qt.WindowType.WindowStaysOnTopHint
        self.setWindowFlags(flags); self.show()

    def _toggle_pin(self, checked: bool):
        self.settings.setValue("always_on_top", checked)
        self._set_on_top(checked)

    # ── Drag + edge snap ─────────────────────────────────────────────────────

    def mousePressEvent(self, event):
        if (event.button() == Qt.MouseButton.LeftButton
                and event.position().y() < 38 and not self._is_docked):
            self._drag_pos = event.globalPosition().toPoint() - self.frameGeometry().topLeft()
        super().mousePressEvent(event)

    def mouseMoveEvent(self, event):
        if self._drag_pos and (event.buttons() & Qt.MouseButton.LeftButton):
            self.move(event.globalPosition().toPoint() - self._drag_pos)
        super().mouseMoveEvent(event)

    def mouseReleaseEvent(self, event):
        if self._drag_pos:
            self._drag_pos = None
            self._snap_to_edge_if_close()
        super().mouseReleaseEvent(event)

    def _snap_to_edge_if_close(self):
        scr = QApplication.primaryScreen().availableGeometry()
        x, y, t = self.x(), self.y(), self.SNAP_THRESHOLD
        if   abs(x - scr.left()) < t:               self.move(scr.left(),              max(y,scr.top()))
        elif abs((x+self.width()) - scr.right()) < t: self.move(scr.right()-self.width(), max(y,scr.top()))
        if abs(y - scr.top()) < t:                   self.move(self.x(), scr.top())

    # ── Painting ─────────────────────────────────────────────────────────────

    def paintEvent(self, event):
        p = QPainter(self); p.setRenderHint(QPainter.RenderHint.Antialiasing)
        rect = self.rect(); path = QPainterPath()
        path.addRoundedRect(0, 0, rect.width(), rect.height(), 12, 12)
        p.setClipPath(path)
        grad = QLinearGradient(0, 0, 0, rect.height())
        grad.setColorAt(0.0, QColor(14,18,36,222)); grad.setColorAt(1.0, QColor(8,11,24,228))
        p.fillPath(path, QBrush(grad))
        p.setPen(QPen(QColor(255,255,255,24),1)); p.drawPath(path)

    # ── Weather ───────────────────────────────────────────────────────────────

    def _start_timer(self):
        self._timer = QTimer(self); self._timer.timeout.connect(self._refresh)
        self._timer.start(self.settings.value("refresh_min",10,type=int)*60*1000)

    def _refresh(self):
        city = self.settings.value("city","").strip()
        if not city: self._show_setup(True); return
        if self._thread and self._thread.isRunning(): return
        self._status_lbl.setText("⟳"); self._refresh_btn.setEnabled(False)
        self._thread = QThread()
        self._worker = WeatherWorker(city, self.settings.value("units","metric"))
        self._worker.moveToThread(self._thread)
        self._thread.started.connect(self._worker.run)
        self._worker.data_ready.connect(self._on_data)
        self._worker.error.connect(self._on_error)
        self._worker.finished.connect(self._thread.quit)
        self._worker.finished.connect(self._on_done)
        self._thread.start()

    def _on_done(self):
        self._status_lbl.setText(""); self._refresh_btn.setEnabled(True)

    def _on_data(self, data: dict):
        units = self.settings.value("units","metric")
        self._lat = data["lat"]; self._lon = data["lon"]
        self._city = data["city"]; self._hourly_data = data["hourly"]
        self._show_setup(False)
        self._current_w.update_data(data, units)
        self._fc_w.update_data(data["daily"], units)
        if self._radar_view:
            self._radar_view.setHtml(
                make_radar_html(self._lat, self._lon, self._city),
                QUrl("https://weatherdock.local/"))
        cur = data["current"]; sym = "°F" if units=="imperial" else "°C"
        self._tray.setToolTip(
            f"{self._city}: {cur['temperature_2m']:.0f}{sym} — {wmo_desc(cur['weather_code'])}")

    def _on_day_selected(self, date: str):
        """Toggle hourly panel: same day again = collapse, new day = expand/update."""
        units = self.settings.value("units","metric")
        if date == self._selected_date and self._hr_sec._expanded:
            # same day clicked again → collapse hourly and deselect
            self._selected_date = ""
            self._fc_w.clear_selection()
            self._hr_sec.collapse()
            return

        self._selected_date = date
        if not self._hourly_data:
            return
        try:
            dt    = datetime.strptime(date, "%Y-%m-%d")
            today = datetime.now().strftime("%Y-%m-%d")
            label = "Today" if date == today else dt.strftime("%A, %b %-d")
            self._hr_sec.set_title(f"Hourly — {label}")
            self._hr_w.update_data(date, self._hourly_data, units)
            self._hr_sec.expand()
        except Exception:
            pass

    def _on_error(self, msg: str):
        self._status_lbl.setText("!")
        self._tray.showMessage(APP_NAME, f"Error: {msg}",
                               QSystemTrayIcon.MessageIcon.Warning, 5000)
        if "not found" in msg.lower(): self._show_setup(True)

    def _show_setup(self, visible: bool):
        self._setup_panel.setVisible(visible)
        if visible:
            c = self.settings.value("city","")
            if c: self._setup_city.setText(c)

    def _quick_setup(self):
        city = self._setup_city.text().strip()
        if city:
            self.settings.setValue("city", city)
            self._show_setup(False); self._refresh()

    def _open_about(self):
        AboutDialog(self).exec()

    def _open_settings(self):
        dlg = SettingsDialog(self.settings, self)
        if dlg.exec() == QDialog.DialogCode.Accepted:
            dlg.save(); self._apply_window_prefs()
            self._pin_btn.setChecked(self.settings.value("always_on_top",True,type=bool))
            self._timer.setInterval(self.settings.value("refresh_min",10,type=int)*60*1000)
            self._refresh()

    def closeEvent(self, event):
        if not self._is_docked:
            self.settings.setValue("window_pos", self.pos())
            self.settings.setValue("window_size", self.size())
        if self._tray.isVisible():
            self._hide_to_tray(); event.ignore()
        else:
            event.accept()


# ═══════════════════════════════════════════════════════════════════════════════
# Entry point
# ═══════════════════════════════════════════════════════════════════════════════

def main():
    QApplication.setHighDpiScaleFactorRoundingPolicy(
        Qt.HighDpiScaleFactorRoundingPolicy.PassThrough)
    app = QApplication(sys.argv)
    app.setApplicationName(APP_NAME); app.setOrganizationName(APP_ORG)
    app.setQuitOnLastWindowClosed(False)
    if not QSystemTrayIcon.isSystemTrayAvailable():
        print("Warning: system tray unavailable.", file=sys.stderr)
    dock = WeatherDock(); dock.show()
    sys.exit(app.exec())

if __name__ == "__main__":
    main()
