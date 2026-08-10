# Google Play submission pack — Blue View Weather

Everything needed to complete the Play Console listing for `ai.blueview.weather`,
version 1.2.0 (versionCode 12).

Every factual claim below was checked against the app source. Where an answer is
easy to get wrong, the reasoning is annotated so it can be re-verified later.

---

## 1. Data safety

Play Console → **App content → Data safety**.

### 1.1 Declared data types

| Data type (Play's exact category) | Collected? | Shared? | Processed ephemerally? | Required or optional | Purpose(s) |
|---|---|---|---|---|---|
| **Location → Approximate location** | **Yes** | **Yes** | **No** — see note E | **Optional** (the user can decline the permission and the app still works via the IP fallback) | App functionality |
| **Location → Precise location** | **No** — see note A | — | — | — | — |
| **Personal info → Name / Email / User IDs / Address / Phone** | No | No | — | — | — |
| **Financial info** (any) | No | No | — | — | — |
| **Messages, Photos and videos, Audio, Files and docs, Calendar, Contacts, Health and fitness** | No | No | — | — | — |
| **App activity → App interactions, In-app search history, Installed apps, Other user-generated content** | No | No | — | — | — |
| **App info and performance → Crash logs, Diagnostics, Other app performance data** | No | No | — | — | — |
| **Device or other IDs → Device or other IDs** | No | No | — | — | — |
| **Web browsing history** | No | No | — | — | — |

### 1.2 Why each answer is what it is

**A. Location is APPROXIMATE, not precise — because the app ships COARSE only.**
This is the answer most likely to cause a suspension if it is wrong, so the reasoning
is explicit. Re-check it against the manifest before every submission:

- `AndroidManifest.xml` declares **`ACCESS_COARSE_LOCATION` only**.
  `ACCESS_FINE_LOCATION` is deliberately NOT declared, and `HomeScreen.kt` requests
  COARSE only.
- Google maps the permissions directly onto the two Data Safety categories:
  `ACCESS_COARSE_LOCATION` -> approximate location, `ACCESS_FINE_LOCATION` -> precise
  location. With FINE absent, the platform itself coarsens every fix it returns, so
  the app cannot access precise location even though `LocationProvider` asks the
  fused/network providers for one.
- `LocationProvider.usableProviders()` only adds `GPS_PROVIDER` when `hasFine()` is
  true. FINE is undeclared, so that check is always false and GPS is never used.
- The 4-decimal rounding before transmission is a defence-in-depth measure, NOT the
  basis for this declaration. Do not cite it as the reason for answering "approximate".

**IF ACCESS_FINE_LOCATION IS EVER RE-ADDED**, this answer must flip to *precise
location* in the same release. Declaring approximate while shipping FINE is a direct
mismatch and is exactly the kind of misdeclaration that gets an app suspended.

**B. "Collected" means transmitted off the device.** The rounded coordinates are sent
as URL query parameters to `api.open-meteo.com` (`WeatherService.forecast`), and to
RainViewer/CARTO indirectly as map tile coordinates. That is collection under Play's
definition even though the developer runs no server and receives nothing.

**C. Approximate location is also collected, via IP.** When device location is
unavailable, `IpGeolocation.locate()` calls `https://ipapi.co/json/`; ipapi.co reads
the device's public IP and returns a city plus coordinates. Play requires IP-derived
location to be declared as approximate location. It is marked **Required** because
the user cannot opt out of it — it is the fallback path that runs precisely when the
location permission was declined.

**D. What "Shared" means here — declare YES for both location types.** Play defines
sharing as transferring the data to a **third party**. The service-provider exemption
does not apply: Open-Meteo, ipapi.co, RainViewer and CARTO are independent public
APIs, not processors the developer has contracted to handle data on its behalf. The
transfer is real (coordinates in a URL; IP address to ipapi.co). Answering "not
shared" here because "we don't have a server and we don't sell anything" is the
classic mis-declaration. Answer **Yes, shared**, and in the free-text purpose note
list the recipients: Open-Meteo (forecast + geocoding), ipapi.co (IP→city fallback),
RainViewer (radar tiles), CARTO (base map tiles).

**E. Ephemeral processing.** Play's "processed ephemerally" means the data is only
held in memory and retained no longer than needed to service the request.

- **Approximate (IP) location: Yes, ephemeral.** Nothing derived from the IP lookup
  is persisted unless the user explicitly saves that city.
- **Precise location: answer No.** The resolved city — including its rounded
  coordinates — can be written to on-device DataStore as a saved city
  (`UserPreferencesRepository.addCity` / `addAndPinCity`). Even though that storage is
  local and never leaves the device, "No" is the defensible answer; claiming
  ephemeral-only while a coordinate can be persisted is a contradiction reviewers can
  see in the app. Answering No costs nothing — it does not add a disclosure the app
  does not already carry.

**F. Everything else is genuinely absent — verified, not assumed.** The dependency
list (`app/build.gradle.kts`, `gradle/libs.versions.toml`) contains no Firebase, no
Google Analytics, no Crashlytics, no AdMob, no Play Services, and no attribution or
telemetry SDK. There is no `AD_ID` permission, no account, no login, no
user-generated content, no crash reporting. So: no advertising ID, no diagnostics,
no app-activity collection.

**G. Saved cities are NOT "collected".** Units, saved cities, pinned city and location
mode live in `androidx.datastore` in the app's private storage. On-device-only storage
is explicitly out of scope for Play's data safety collection question.

### 1.3 Remaining Data safety questions

| Question | Answer | Note |
|---|---|---|
| Is all user data encrypted in transit? | **Yes** | Every endpoint is HTTPS and `android:usesCleartextTraffic="false"` makes HTTP impossible. |
| Do you provide a way for users to request data deletion? | **Yes** — users can delete data in-app | Cities are removable in-app; uninstalling erases everything. There is no server copy. |
| Does your app have an independent security review? | No | Optional badge; leave unchecked. |
| Privacy policy URL | Public URL of `PRIVACY.md` | Must be a live, publicly reachable URL that does not require login, and must actually describe **this** app. See §5. |

### 1.4 Permission declarations

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: foreground only. No
  `ACCESS_BACKGROUND_LOCATION` is declared, so **no background-location declaration
  form is required**. Do not add one.
- No `REQUEST_INSTALL_PACKAGES`, no `QUERY_ALL_PACKAGES`, no
  `MANAGE_EXTERNAL_STORAGE`, no SMS/Call Log permissions — none of the sensitive
  permission declaration forms apply.
- **Before uploading, confirm the Play build's merged manifest contains no
  `REQUEST_INSTALL_PACKAGES` and no self-update/sideload path.** An app that
  downloads and installs an APK is a Device and Network Abuse violation.

---

## 2. Store listing copy

### App name (max 30 characters)

```
Blue View Weather
```
*(17 characters.)*

### Short description (max 80 characters)

```
Current conditions, a 7-day forecast and live rain radar. No ads, no tracking.
```
*(78 characters.)*

### Full description (max 4000 characters)

```
Blue View Weather is a clean, fast weather app that tells you what you actually
need to know: what it is doing outside right now, what it will do for the rest of
the day, and what the week ahead looks like.

CURRENT CONDITIONS
See temperature and what it feels like, humidity, wind speed and direction, and
visibility at a glance, with a clear day and night treatment so the screen matches
the sky.

HOURLY AND 7-DAY FORECAST
Scroll the next hours for temperature, chance of precipitation, wind and
conditions. Open the 7-day view for daily highs and lows and the odds of rain, so
you can plan the weekend without guessing.

LIVE PRECIPITATION RADAR
A radar map shows recent precipitation over your area, so you can see whether the
shower is arriving or already moving away. Pan and zoom the map to follow the
weather across the region.

YOUR CITIES
Save the places you care about — home, work, family, wherever you are heading — and
switch between them in a tap. Pin one city to keep the app on it, or let the app
follow wherever your device is.

AUTOMATIC LOCATION, WITH A CHOICE
Allow location and the app finds your city for you. Decline it and the app still
works: it can estimate your city, or you can simply search for one. Location is
used only while the app is open, only to work out which city to show, and your
coordinates are rounded before any forecast is requested.

METRIC OR IMPERIAL
Choose Celsius and km/h or Fahrenheit and mph. The setting sticks.

BUILT TO BE UNOBTRUSIVE
No account. No sign-up. No advertising. No analytics, no crash reporting and no
tracking of any kind. There is no Blue View server, so nothing you do in the app is
sent to us — the app talks directly to the weather services that provide the data.
Your saved cities and settings stay on your device and are deleted when you
uninstall.

Forecast data is provided by Open-Meteo. Radar imagery is provided by RainViewer.
Map tiles are provided by CARTO, based on OpenStreetMap data. Weather forecasts are
predictions and can be wrong; do not rely on this app for decisions where safety
depends on the weather. Always follow official warnings from your national weather
service.

Questions or feedback: frank@blueview.ai
```
*(Approximately 1,950 characters — comfortably inside the 4,000 limit.)*

**Copy rules that were deliberately followed — keep them if you edit the text:**

- No keyword stuffing, no repeated "weather app weather forecast weather radar",
  no competitor names, no "#1 / best / top-rated" claims.
- No feature the app does not have: **no severe-weather alerts or push
  notifications, no widgets, no wear/tablet claims, no air quality, no pollen, no
  offline mode** — none of these exist in the code, and claiming them is a
  Deceptive Behaviour violation.
- **No mention of GitHub, the source repository, "download the APK", or any other
  way to obtain the app.** Directing Play users to an alternative distribution
  channel for the same app is independently actionable under Play policy, quite
  apart from the payments rules. If a link to source is wanted at all, it belongs
  in the privacy policy page, not in the listing.
- No email-address harvesting or promises of support SLAs; a contact address is
  fine and is required elsewhere anyway.

### Other listing fields

| Field | Value |
|---|---|
| App or game | **App** |
| Category | **Weather** |
| Tags | Weather, Forecast (choose from Play's fixed tag list; max 5) |
| Contact email | frank@blueview.ai *(shown publicly on the store page)* |
| Contact website | Optional — the project or privacy-policy page |
| Contact phone | Optional — omit |
| Privacy policy URL | Required (see §1.3) |
| External marketing / promotional content | Declare "does not contain ads" |

---

## 3. Graphic asset inventory

| Asset | Exact spec | Count | Mandatory? |
|---|---|---|---|
| **App icon** | 512 × 512 px, 32-bit PNG with alpha, ≤ 1 MB. No drop shadow or rounded-corner mask baked in — Play applies its own shape. | 1 | **Mandatory** |
| **Feature graphic** | 1024 × 500 px, PNG or JPEG, ≤ 15 MB, no alpha/transparency. Keep text away from the edges; it gets cropped in some placements. | 1 | **Mandatory** |
| **Phone screenshots** | PNG or JPEG, ≤ 8 MB each. 16:9 or 9:16 aspect ratio. Each side between 320 px and 3840 px, and the longest side no more than twice the shortest. Practical target: **1080 × 1920** portrait. | **Minimum 2, maximum 8.** Supply **at least 4 at ≥ 1080 px** — below 4, the app is not eligible for promotion on certain Play surfaces. | **Mandatory** (min 2) |
| **7-inch tablet screenshots** | Same format rules; practical target 1200 × 1920. | Up to 8 | Optional — but without tablet screenshots Play may show "not designed for your device" on tablets |
| **10-inch tablet screenshots** | Same format rules; practical target 1600 × 2560. | Up to 8 | Optional, same caveat |
| **Chromebook / Android TV / Wear screenshots** | — | — | Not applicable; the app targets phones |
| **Promo video** | A YouTube URL (not an upload) | 1 | Optional |

Suggested screenshot set (4–6, in this order): current conditions card; hourly
strip expanded; 7-day forecast expanded; radar map; saved-cities list; settings.
Use real app output — mocked-up or heavily embellished screenshots that do not
reflect the app are a listing violation.

---

## 4. App content declarations

Play Console → **App content**. Every item must be green before a production
release can be rolled out.

| Section | Answer | Reasoning |
|---|---|---|
| **Privacy policy** | Public URL | Required because the app accesses location. |
| **Ads** | **No, my app does not contain ads** | No ad SDK, no `AD_ID` permission, no promotional interstitials. |
| **App access** | **All functionality is available without special access** | No login, no account, no gated region. Do not fill in test credentials. |
| **Content rating** | Complete the IARC questionnaire — see below | Rating is generated automatically from the answers. |
| **Target audience and content** | Target age groups: **18 and over** (or 13+). Answer **No** to "is your app designed for children". Do not opt into Teacher Approved. | The app is general-audience utility software. Selecting any age band under 13 pulls the app into the **Families policy**, which brings extra ad-SDK, content and design requirements for no benefit here. |
| **News app** | **No** | It provides forecast data from a weather API; it does not publish news or editorial content. Answering yes would trigger a publisher-credentials review it cannot pass. |
| **COVID-19 contact tracing / status apps** | **No** | |
| **Data safety** | Per §1 | |
| **Government apps** | **No** | The publisher is an individual developer, not acting on behalf of any government. |
| **Financial features** | **None of these** | No loans, no crypto, no payments. |
| **Health apps** | **No** | Weather is not health data. |
| **Advertising ID** | **Not used** — the app declares no `AD_ID` permission | Must match the Data safety "no Device or other IDs" answer. |

### Content rating questionnaire (IARC) — answers for a weather app

Choose category **Utility, Productivity, Communication or Other**, then:

| Question | Answer |
|---|---|
| Violence (realistic, fantasy, sexual, or otherwise) | No |
| Sexuality / nudity | No |
| Profanity or crude humour | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Gambling, simulated gambling, or contests | No |
| Horror or fear-inducing content | No |
| Bodily functions / crude content | No |
| User-generated content or user-to-user communication | No |
| Does the app share the user's **location** with other users? | **No** — location is used only to fetch the user's own forecast; it is never shown to or shared with other users. Answer carefully: "yes" here jumps the rating. |
| Does the app allow purchases of digital goods? | No |
| Does the app contain ads? | No |
| Does the app allow users to share personal information with third parties? | No |
| Does the app provide an unrestricted internet browser? | **No** — the WebView is used solely to render a bundled Leaflet radar map; there is no address bar and no general browsing. |
| Miscellaneous — is the app a store front / social network / dating app? | No |

Expected outcome: **Everyone / PEGI 3 / USK 0 / ESRB Everyone**.

---

## 5. Checklist — only the user can do these, in this order

Legend: 🔒 = **irreversible or effectively permanent**.

### Phase 0 — account

1. 🔒 Create the Play Console developer account and pay the one-time USD 25 fee.
   **Choose the account type (personal vs organisation) carefully — changing it
   later requires support intervention and, for organisations, a D-U-N-S number and
   verification.** The account's legal name and address will be shown publicly on
   the store listing.
2. Complete identity verification (government ID / D-U-N-S) and add the required
   public developer contact details. Nothing can be published until this clears —
   allow days, not hours.
3. Set up the Payments profile only if a paid app or IAP is ever planned. Not
   needed for a free app.

### Phase 1 — create the app

4. **Create app** → name, default language, **App**, and 🔒 **Free**. A free app can
   never be converted to paid. (Paid → free is allowed; free → paid is not.)
5. 🔒 The **package name `ai.blueview.weather` is permanent.** It cannot be changed or
   reused, even if the app is later deleted. Verify it once more before the first
   upload.
6. Accept the Developer Programme Policies and the US export declaration.

### Phase 2 — the signing decision (do this deliberately)

7. 🔒 **Play App Signing.** New apps are enrolled automatically and **cannot opt
   out.** Decide *how* the app signing key is created, because this is the part that
   is genuinely one-way:
   - **Let Google generate the app signing key** (simplest, recommended), or
   - **Upload your own key** — do this only if the app must keep an existing
     signature for installs outside Play.
   Once the first bundle is uploaded, the **app signing key is fixed**; changing it
   later requires a Play-approved key upgrade, which does not update already-installed
   copies of the app.
8. 🔒 Register an **upload key** and back up the keystore, its password and the alias
   **off-machine**. The existing `keystore/blueview-release.jks` is never committed
   (CI materialises it from `KEYSTORE_BASE64`); confirm that secret and the
   `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` values are stored somewhere
   recoverable. Losing the upload key is survivable (Google can reset it); losing
   the *app signing key* when you supplied it yourself is not.
9. Note that the AAB uploaded to Play will be re-signed by Google, so its signature
   **will differ from the GitHub-distributed APK**. Users cannot update from one to
   the other in place; they are separate installs of the same package name and only
   one can exist on a device.

### Phase 3 — content and listing

10. Complete **App content** in full (§4) and **Data safety** (§1). Publish the
    privacy policy at a stable public URL *before* filling in the field.
11. Fill in the **Main store listing** (§2) and upload the graphic assets (§3).
12. Select countries/regions for distribution.

### Phase 4 — testing (this is the long pole)

13. Upload the release AAB to **Internal testing** first and install it from Play on
    a real device. This is the only way to catch signing, minification and
    `usesCleartextTraffic` problems as users will experience them.
14. ⚠️ **Closed testing requirement for new personal developer accounts.** Individual
    (personal) developer accounts created after **13 November 2023** must run a
    **closed test with at least 12 testers who opted in and remained opted in
    continuously for 14 days** before they can apply for production access.
    - Recruit the 12 testers before starting the clock; a tester who opts out resets
      that person's contribution and can push the eligibility date out.
    - The 14 days are continuous and start when the closed test starts, so **begin
      this as early as possible** — it gates the launch date more than anything else
      on this list.
    - Internal testing does **not** satisfy this requirement. It must be a closed
      track.
15. After the 14 days, **apply for production access** and answer the questions about
    the app's testing and readiness. This is a human review; allow additional time.

### Phase 5 — release

16. Create the **Production** release, upload the AAB, write the release notes, and
    set the rollout percentage. A **staged rollout can be halted but not un-shipped**
    for users who already received it.
17. Submit for review. First submissions from new accounts routinely take several
    days.
18. 🔒 Once a **versionCode** is published on a track it can never be reused. Bump
    `versionCode` for every upload, including rejected ones.

---

## 6. Pre-submission verification the developer should re-run

- Confirm the Play variant contains **no update checker, no `REQUEST_INSTALL_PACKAGES`,
  and no "download the latest APK" UI**. This is now enforced in two places, but
  re-verify against the **built AAB**, not the source: the real updater lives only in
  `src/github/`, `src/play/` holds an inert stub, and `AboutScreen` gates the UI on
  `BuildConfig.UPDATE_CHECK_ENABLED` so R8 strips it. The CI job fails if the string
  `api.github.com/repos` appears in the Play AAB's dex. To check by hand:

      unzip -p app/build/outputs/bundle/playRelease/app-play-release.aab base/dex/*.dex \
        | strings | grep -c "api.github.com/repos"     # must print 0
- Confirm the listing text shipped to Play contains no GitHub link.
- Confirm the privacy policy URL resolves publicly and matches §1's declarations.
