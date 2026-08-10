# Blue View Weather — Privacy Policy

**Effective date: 10 August 2026**
**Applies to:** the Blue View Weather Android application (package `ai.blueview.weather`)
**Publisher / contact:** Frank Perez — <frank@blueview.ai>

Blue View Weather is a weather app. It shows the current conditions, an hourly and
7-day forecast, and a precipitation radar map for a place you choose or for where
your device currently is.

This policy describes exactly what the app does with data. Every statement below
describes behaviour that is present in the app's source code, which is public.

---

## 1. Short version

- There is **no account**. You never sign in, and the app has no user identifier.
- There is **no analytics SDK, no crash-reporting SDK, no advertising SDK, and no
  tracking or attribution SDK** of any kind in the app.
- The app **does not collect an advertising ID**, and does not build a profile of you.
- The developer operates **no server**. Nothing you do in the app is sent to Blue
  View. The app talks only to the third-party weather and map services listed in
  section 4, and it talks to them directly from your device.
- Your saved cities and settings are stored **only on your device**.
- Location is used **only while the app is open**, only to work out which city to
  show, and coordinates are **rounded to about 11 metres before they are sent
  anywhere**.

---

## 2. Information the app uses

### 2.1 Device location (optional)

If you grant the location permission, the app reads a single location fix from the
Android system while you are using it, in order to work out which city you are in
and fetch that city's forecast.

Specific, verifiable details:

- The app requests **`ACCESS_COARSE_LOCATION` only**. It does not request, and does
  not declare, `ACCESS_FINE_LOCATION` — approximate location is all that is needed to
  work out which city you are in, so the app never asks for precise location.
- The app has **no background location permission** and **no foreground service**.
  It cannot and does not read your location when it is not in the foreground.
- The app will accept a cached system fix only if it is **less than 5 minutes old**;
  otherwise it asks for one live fix, with a **10 second** budget, and then stops.
  It does not subscribe to continuous location updates.
- The fix is converted to a city name using the **Android system geocoder**, which
  is a function of your device/operating system. On many devices that geocoder is
  itself served by a Google backend; that lookup is performed by Android, not by
  this app, and is governed by your device manufacturer's and Google's own privacy
  terms.
- Before any coordinate leaves the app, it is **rounded to 4 decimal places
  (approximately 11 metres)**. The full-precision GPS fix is never transmitted and
  is never written to storage or to a log in release builds.
- Your location is **not stored** by the app as location history. Only the resulting
  city (name, country, and its rounded coordinates) can end up in your saved-city
  list, and only on your device.

**You can refuse or revoke this at any time.** The app works without location — see
section 3 for the fallback and section 8 for how to revoke.

### 2.2 IP-derived approximate location (fallback)

When device location is unavailable — permission not granted, location turned off,
no fix obtained in time, or the system geocoder unavailable — the app requests
`https://ipapi.co/json/` to obtain an approximate city.

That request necessarily exposes your device's **public IP address** to ipapi.co,
which uses it to derive an approximate city, country, latitude and longitude. The
app sends no other information in that request: no identifier, no cookie, no API
key, no name, and no device location. The app keeps only the resulting city name,
country and coordinates, in memory and — if you save that city — on your device.

### 2.3 City searches you type

When you search for a city, the text you type is sent to Open-Meteo's geocoding API
in order to resolve it to coordinates. It is not sent anywhere else, and it is not
retained by the app except as a saved city if you choose to save it.

### 2.4 Settings and saved cities

Your unit preference (metric/imperial), your saved cities, which city is pinned,
whether the app is in automatic or pinned location mode, and a flag recording that
the location permission has already been requested once, are stored in Android
DataStore in the app's own private storage on your device. **None of this is
transmitted to the developer or to any third party.**

Note that the app allows Android's standard **Auto Backup** (`allowBackup="true"`).
That means Android may include this app's settings file in the device backup that
your operating system makes to your own Google account. That backup is created and
controlled by Android, not by this app, and is governed by Google's privacy policy.
You can turn device backup off in your Android system settings.

### 2.5 What the app does **not** collect

The app does not collect, request, or transmit: your name, email address, phone
number, contacts, calendar, photos, files, microphone or camera input, installed
app list, advertising identifier, IMEI/serial number, browsing history, health data,
financial data, or any other personal identifier. There is no analytics or telemetry
event of any kind.

---

## 3. How the app decides which city to show

1. If you have pinned a city, that city is used and no location lookup happens.
2. Otherwise, if location permission is granted, the app takes one location fix and
   reverse-geocodes it (section 2.1).
3. If that fails or is not permitted, the app falls back to IP-derived location
   (section 2.2).
4. If that also fails, the app asks you to search for a city.

---

## 4. Third parties that receive requests

The app makes network requests directly from your device to the services below. Any
service your device connects to necessarily sees your device's **public IP address**
and the request itself; that is inherent to how the internet works, and is the main
privacy consideration here. None of these services receives an account, a user ID,
or an advertising ID from this app, because the app has none.

| Service | What it is used for | What it receives | Their policy |
|---|---|---|---|
| **Open-Meteo** (`api.open-meteo.com`, `geocoding-api.open-meteo.com`) | Forecast data; converting a city name you type into coordinates | Your IP address; the coordinates of the city being displayed, **rounded to ~11 m**; the city name text you search for; your chosen units | <https://open-meteo.com/en/terms> |
| **ipapi.co** (`ipapi.co`) | Fallback city lookup when device location is unavailable | Your IP address only | <https://ipapi.co/privacy/> |
| **RainViewer** (`api.rainviewer.com` and its tile hosts) | Precipitation radar overlay on the radar map | Your IP address; the map tile coordinates being viewed (i.e. roughly which part of the world is on screen) | <https://www.rainviewer.com/privacy-policy.html> |
| **CARTO** (`basemaps.cartocdn.com`) | Base map imagery under the radar overlay | Your IP address; the map tile coordinates being viewed | <https://carto.com/privacy/> |

Map tile requests identify a tile grid square, not a point. The radar map opens at a
regional zoom level, so tile requests reveal an approximate area, not a precise
position.

The mapping library (Leaflet) is **bundled inside the app**, not loaded from a CDN,
so no third-party script host is contacted to render the map.

Builds of this app distributed outside Google Play — that is, from the project's
public source repository — additionally contact GitHub's public API to check whether
a newer release exists. **The version published on Google Play does not include that
feature and does not contact GitHub.** If it applied to your build, GitHub would see
your IP address and nothing else; see <https://docs.github.com/site-policy/privacy-policies/github-privacy-statement>.

---

## 5. Retention

- Location fixes are held in memory only for as long as it takes to resolve a city,
  and are discarded when the app process ends. The app keeps no location history.
- Saved cities and settings persist on your device until you delete them in the app,
  clear the app's storage, or uninstall the app. **Uninstalling removes all of it.**
- The developer retains nothing, because the developer receives nothing.
- Retention by the third parties in section 4 — for example, IP addresses in their
  web server logs — is governed by their own policies, linked above.

---

## 6. How the data is protected

- All network requests use **HTTPS**. Cleartext traffic is disabled at the platform
  level (`android:usesCleartextTraffic="false"`), so the app cannot fall back to
  unencrypted HTTP.
- Settings and saved cities live in the app's private storage, which Android isolates
  from other apps.
- Network request logging, which would echo coordinates into the system log, is
  **compiled out of release builds** and exists only in debug builds.

---

## 7. Children's privacy

Blue View Weather is a general-audience weather app. It is not directed at children,
and it does not knowingly collect personal information from anyone, including
children under 13 (or the equivalent minimum age in your jurisdiction). There is no
account creation, no user-generated content, no messaging, no advertising, and no
profiling, so there is nothing collected that could identify a child. If you believe
a child has somehow provided personal information through this app, contact
<frank@blueview.ai>.

---

## 8. Your choices and your rights

- **Refuse location.** Decline the permission prompt. The app falls back to
  IP-derived city detection, or you can just search for a city.
- **Revoke location later.** Android **Settings → Apps → Blue View Weather →
  Permissions → Location → Don't allow**. This takes effect immediately; the app
  re-checks the permission on every lookup.
- **Avoid the IP lookup entirely.** Grant location, or pin a city. When a city is
  pinned, the app does not perform any location lookup at all.
- **Delete your data.** Remove saved cities in the app, or clear the app's storage,
  or uninstall the app. There is no server-side copy to request the deletion of.
- **Access / portability.** All data the app holds about you is on your device and
  is visible in the app's own UI.

Because the app collects no personal data on any server, requests under the GDPR,
UK GDPR, CCPA/CPRA and similar laws (access, deletion, correction, portability,
opt-out of sale or sharing) are satisfied by the on-device controls above. **The
developer does not sell or share personal information, and never has.** Where
processing of your IP address by the app's use of the third-party services in
section 4 requires a legal basis under the GDPR, that basis is legitimate interest
in delivering the forecast and map you requested; you can avoid the ipapi.co request
entirely by pinning a city, and avoid the map-tile requests by not opening the radar
section.

---

## 9. Changes to this policy

If this policy changes materially, the effective date at the top will be updated and
the revised policy will be published at the same location, alongside the app's public
source. Continued use of the app after that date is subject to the revised policy.

---

## 10. Contact

Questions, corrections, or privacy requests: **<frank@blueview.ai>**
