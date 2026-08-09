package ai.blueview.weather.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private fun radarHtml(lat: Double, lon: Double, city: String, tileUrl: String): String {
    val citySafe = city.replace("'", "\\'").replace("\"", "\\\"")
    return """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="leaflet.css"/>
<script src="leaflet.js"></script>
<style>
* { margin: 0; padding: 0; }
html, body { width: 100%; height: 100%; background: #0b0e1c; }
#map { width: 100%; height: 100%; }
.leaflet-control-attribution { font-size: 9px !important; opacity: 0.4 !important; }
</style>
</head>
<body>
<div id="map"></div>
<script>
var map = L.map('map', { zoomControl: true }).setView([$lat, $lon], 7);

L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
  attribution: '&copy; OSM &copy; CARTO',
  subdomains: 'abcd',
  maxZoom: 19
}).addTo(map);

L.tileLayer('$tileUrl', {
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
</script>
</body>
</html>"""
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RadarWebView(
    lat: Double,
    lon: Double,
    city: String,
    tileUrl: String,
    modifier: Modifier = Modifier
) {
    val html = remember(lat, lon, city, tileUrl) { radarHtml(lat, lon, city, tileUrl) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView, request: WebResourceRequest
                    ) = false
                }
                with(settings) {
                    javaScriptEnabled    = true
                    domStorageEnabled    = true
                    useWideViewPort      = true
                    loadWithOverviewMode = true
                    allowFileAccess      = true   // required to load assets
                    setSupportZoom(false)
                }
                tag = ""
            }
        },
        update = { webView ->
            if (webView.tag as? String != html) {
                webView.tag = html
                // Base URL points to assets folder — leaflet.css and leaflet.js
                // are resolved relative to this, no CDN needed
                webView.loadDataWithBaseURL(
                    "file:///android_asset/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}
