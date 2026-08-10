package ai.blueview.weather.ui.components

import ai.blueview.weather.BuildConfig
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

private const val TAG = "RadarWeb"

/**
 * Assets are served over a real https origin instead of file:///android_asset/.
 * A file:// base URL produces an opaque origin on modern WebView, which silently
 * drops relative subresource loads (leaflet.css / leaflet.js) and would also make
 * the https tile requests mixed content.
 */
private const val ASSET_BASE_URL = "https://appassets.androidplatform.net/"

/**
 * Escapes a value for embedding inside a single-quoted JavaScript string literal.
 *
 * Both values interpolated into the script below — the city name and the RainViewer tile
 * URL — originate in remote API responses, so neither can be assumed quote-free.
 *
 * Order matters: the backslash pass must run first or it would double-escape the
 * backslashes the later passes insert. The "</" pass runs last for the same reason,
 * since the backslash it adds is intentional.
 *
 * The "</" case is the one a plain quote-escape misses: the HTML parser terminates the
 * <script> block on "</script" before JavaScript ever sees the string, so a city named
 * "</script><img onerror=...>" would escape the script entirely. "<\/" is an identical
 * string to JavaScript but is invisible to the HTML tokenizer.
 *
 * Line terminators are escaped because a raw newline inside a string literal is a
 * syntax error, and JavaScript additionally treats U+2028/U+2029 as line breaks.
 */
private fun jsEscape(value: String): String = value
    .replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\u2028", "\\u2028")
    .replace("\u2029", "\\u2029")
    .replace("</", "<\\/")

private fun radarHtml(lat: Double, lon: Double, city: String, tileUrl: String, heightDp: Int): String {
    val citySafe = jsEscape(city)
    val tileSafe = jsEscape(tileUrl)
    return """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="/assets/leaflet.css"/>
<script src="/assets/leaflet.js"></script>
<style>
* { margin: 0; padding: 0; }
/* An explicit pixel height, not height:100%. Inside this WebView the percentage
   chain resolves against a zero-height viewport, so #map measured 384x0 and Leaflet
   loaded no tiles at all. CSS px maps 1:1 to dp at initial-scale=1. */
html, body { width: 100%; height: ${heightDp}px; background: #0b0e1c; }
#map { width: 100%; height: ${heightDp}px; }
.leaflet-control-attribution { font-size: 9px !important; opacity: 0.4 !important; }
</style>
</head>
<body>
<div id="map"></div>
<script>
window.onerror = function (msg, src, line, col) {
  console.error('RADAR_FAIL onerror ' + msg + ' @ ' + src + ':' + line + ':' + col);
  return false;
};

try {
  if (typeof L === 'undefined') {
    throw new Error('Leaflet global L is undefined - /assets/leaflet.js did not execute');
  }

  var map = L.map('map', { zoomControl: true }).setView([$lat, $lon], 7);

  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OSM &copy; CARTO',
    subdomains: 'abcd',
    maxZoom: 19
  }).addTo(map);

  L.tileLayer('$tileSafe', {
    opacity: 0.65,
    zIndex: 10
  }).addTo(map);

  L.circleMarker([$lat, $lon], {
    color: '#52bee8',
    fillColor: '#52bee8',
    fillOpacity: 0.9,
    radius: 8,
    weight: 2
  }).addTo(map).bindTooltip('$citySafe', { direction: 'top', offset: [0, -10] });

  var el = document.getElementById('map');
  console.log('RADAR_OK map built at $lat,$lon size=' + el.clientWidth + 'x' + el.clientHeight +
              ' tiles=$tileSafe');

  // The page is loaded before the WebView has been laid out to its final height, so
  // html/body height:100% resolves against a zero-height viewport. Leaflet caches that
  // size at construction, concludes no tiles are needed, and paints nothing — the map
  // object builds fine, which is why RADAR_OK alone was not proof of a visible map.
  // Re-measure whenever the container actually gets a size.
  var fix = function (why) {
    map.invalidateSize();
    console.log('RADAR_RESIZE ' + why + ' size=' + el.clientWidth + 'x' + el.clientHeight);
  };
  if (typeof ResizeObserver !== 'undefined') {
    new ResizeObserver(function () { fix('observer'); }).observe(el);
  }
  window.addEventListener('resize', function () { fix('window'); });
  // Belt and braces for WebViews that fire neither during the initial layout pass.
  setTimeout(function () { fix('t100'); }, 100);
  setTimeout(function () { fix('t600'); }, 600);
} catch (e) {
  console.error('RADAR_FAIL ' + (e && e.message ? e.message : e));
}
</script>
</body>
</html>"""
}

@SuppressLint("SetJavaScriptEnabled")
// setSafeBrowsingEnabled is deprecated from API 35 (Safe Browsing became mandatory) but
// still meaningful on the API 26-34 devices this app supports, so keep calling it.
@Suppress("DEPRECATION")
@Composable
fun RadarWebView(
    lat: Double,
    lon: Double,
    city: String,
    tileUrl: String,
    heightDp: Int = 280,
    modifier: Modifier = Modifier
) {
    val html = remember(lat, lon, city, tileUrl, heightDp) {
        radarHtml(lat, lon, city, tileUrl, heightDp)
    }

    AndroidView(
        // The composable owns its height so the CSS pixel height above and the actual
        // view height can never disagree.
        modifier = modifier.height(heightDp.dp),
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView, request: WebResourceRequest
                    ) = false

                    override fun shouldInterceptRequest(
                        view: WebView, request: WebResourceRequest
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                    // Legacy overload — some WebView paths still route through it.
                    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
                    override fun shouldInterceptRequest(
                        view: WebView, url: String
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(Uri.parse(url))

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        // Request URLs can carry the tile coordinates derived from the
                        // device fix, so this never reaches a release logcat.
                        if (BuildConfig.DEBUG) {
                            Log.e(
                                TAG,
                                "onReceivedError url=${request.url} " +
                                    "code=${error.errorCode} desc=${error.description}"
                            )
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse
                    ) {
                        if (BuildConfig.DEBUG) {
                            Log.e(
                                TAG,
                                "onReceivedHttpError url=${request.url} " +
                                    "status=${errorResponse.statusCode} " +
                                    "reason=${errorResponse.reasonPhrase}"
                            )
                        }
                    }
                }

                // The page's RADAR_OK line contains the raw GPS fix in AUTOMATIC mode, and
                // R8 does not strip Log calls, so the console bridge exists only in debug
                // builds — where the on-device radar check greps logcat for those markers.
                if (BuildConfig.DEBUG) {
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            Log.i(
                                TAG,
                                "console[${message.messageLevel()}] ${message.message()} " +
                                    "(${message.sourceId()}:${message.lineNumber()})"
                            )
                            return true
                        }
                    }
                }

                with(settings) {
                    // Leaflet needs script and its own tile cache; everything below this
                    // pair is switched off because the radar page never uses it, and an
                    // enabled-by-default capability is reachable by any script the page
                    // ends up running.
                    javaScriptEnabled    = true
                    domStorageEnabled    = true
                    useWideViewPort      = true
                    loadWithOverviewMode = true

                    // Assets now arrive through the asset loader over the https origin, so
                    // no file:// or content:// reach is needed. The two *FromFileURLs flags
                    // default to true below API 30 and are the classic path for a local
                    // page to read arbitrary app files, so they are pinned off explicitly.
                    allowFileAccess                  = false
                    allowContentAccess               = false
                    allowFileAccessFromFileURLs      = false
                    allowUniversalAccessFromFileURLs = false

                    // The map centres on a fix the app already resolved and passes in as
                    // lat/lon, so the page has no reason to ask the platform for a position
                    // — and the app no longer holds a location permission a prompt could use.
                    setGeolocationEnabled(false)

                    // No media on this page, and no popups: a tile response cannot turn
                    // into an autoplaying element or a new window.
                    mediaPlaybackRequiresUserGesture      = true
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)

                    // Default-on since API 26 and mandatory (setter is a no-op) from API 35,
                    // stated anyway so the check on the remote tile hosts is not left to a
                    // default that a WebView update could flip.
                    safeBrowsingEnabled = true

                    setSupportZoom(false)
                }
                tag = ""
            }
        },
        update = { webView ->
            // Tag holds the last-loaded HTML so recomposition does not reload the map.
            if (webView.tag as? String != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(
                    ASSET_BASE_URL,
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        onRelease = { webView ->
            // The WebView holds the Activity context, and the page keeps a ResizeObserver,
            // two timers and a live tile-fetching Leaflet map alive. Without this teardown
            // the Radar section collapsing leaks MainActivity and keeps fetching tiles.
            webView.stopLoading()
            // Replaces the document so its timers and observers are torn down before the
            // native object goes away.
            webView.loadUrl("about:blank")
            webView.removeAllViews()
            (webView.parent as? ViewGroup)?.removeView(webView)
            // destroy() must be last and only once detached, otherwise WebView logs
            // "Error: WebView.destroy() called while still attached".
            webView.destroy()
        }
    )
}
