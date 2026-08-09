import SwiftUI
import WebKit

private func radarHtml(lat: Double, lon: Double, city: String) -> String {
    let citySafe = city.replacingOccurrences(of: "'", with: "\\'")
    return """
    <!DOCTYPE html>
    <html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>*{margin:0;padding:0}html,body,#map{width:100%;height:100%;background:#0b0e1c}
    .leaflet-control-attribution{font-size:9px!important;opacity:0.4!important}
    </style></head><body><div id="map"></div>
    <script>
    (function(){
      var map=L.map('map',{zoomControl:true}).setView([\(lat),\(lon)],7);
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
          }).catch(function(){});
      }
      load();setInterval(load,300000);
      L.circleMarker([\(lat),\(lon)],{color:'#52bee8',fillColor:'#52bee8',
        fillOpacity:0.9,radius:8,weight:2}).addTo(map)
        .bindTooltip('\(citySafe)',{direction:'top',offset:[0,-10]});
    })();
    </script></body></html>
    """
}

struct RadarWebView: UIViewRepresentable {
    let lat: Double
    let lon: Double
    let city: String

    final class Coordinator {
        var lastKey: String?
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        reload(webView, context: context)
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        reload(webView, context: context)
    }

    private func reload(_ webView: WKWebView, context: Context) {
        let key = "\(lat),\(lon),\(city)"
        guard context.coordinator.lastKey != key else { return }
        context.coordinator.lastKey = key
        webView.loadHTMLString(radarHtml(lat: lat, lon: lon, city: city), baseURL: URL(string: "https://weatherdock.local/"))
    }
}
