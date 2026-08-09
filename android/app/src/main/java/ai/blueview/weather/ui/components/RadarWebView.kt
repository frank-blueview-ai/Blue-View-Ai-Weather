package ai.blueview.weather.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private fun radarHtml(lat: Double, lon: Double, city: String): String {
    val citySafe = city.replace("'", "\\'")
    return """<!DOCTYPE html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>*{margin:0;padding:0}html,body,#map{width:100%;height:100%;background:#0b0e1c}
.leaflet-control-attribution{font-size:9px!important;opacity:0.4!important}
</style></head><body><div id="map"></div>
<script>
(function(){
  var map=L.map('map',{zoomControl:true}).setView([$lat,$lon],7);
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
    {attribution:'©OSM ©CARTO',subdomains:'abcd',maxZoom:19}).addTo(map);
  var radar=null,lastT=null;
  function load(){
    fetch('https://api.rainviewer.com/public/weather-maps.json')
      .then(r=>r.json()).then(d=>{
        if(!d.radar||!d.radar.past||!d.radar.past.length)return;
        var f=d.radar.past[d.radar.past.length-1];
        if(f.time===lastT)return;lastT=f.time;
        if(radar)map.removeLayer(radar);
        radar=L.tileLayer(d.host+f.path+'/256/{z}/{x}/{y}/4/1_1.png',
          {opacity:0.65,zIndex:10}).addTo(map);
      }).catch(()=>{});
  }
  load();setInterval(load,300000);
  L.circleMarker([$lat,$lon],{color:'#52bee8',fillColor:'#52bee8',
    fillOpacity:0.9,radius:8,weight:2}).addTo(map)
    .bindTooltip('$citySafe',{direction:'top',offset:[0,-10]});
})();
</script></body></html>""".replace("\$lat","$lat").replace("\$lon","$lon")
        .replace("\$citySafe", citySafe)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RadarWebView(lat: Double, lon: Double, city: String, modifier: Modifier = Modifier) {
    val html = remember(lat, lon) { radarHtml(lat, lon, city) }
    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled  = true
                loadDataWithBaseURL("https://weatherdock.local/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://weatherdock.local/", html, "text/html", "UTF-8", null)
        }
    )
}
