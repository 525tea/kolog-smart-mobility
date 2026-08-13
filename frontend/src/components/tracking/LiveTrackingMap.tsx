import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import type { TrackingResponse } from "../../types";

export function LiveTrackingMap({ tracking }: { tracking: TrackingResponse }) {
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const route = tracking.route.length > 1
      ? tracking.route.map((point) => [point.latitude, point.longitude] as L.LatLngTuple)
      : [
          [tracking.originLatitude, tracking.originLongitude] as L.LatLngTuple,
          [tracking.destinationLatitude, tracking.destinationLongitude] as L.LatLngTuple,
        ];
    const map = L.map(containerRef.current, {
      zoomControl: false,
      attributionControl: true,
      dragging: true,
      scrollWheelZoom: false,
    });

    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(map);
    L.control.zoom({ position: "bottomright" }).addTo(map);
    L.polyline(route, { color: "#244bb3", weight: 5, opacity: 0.8 }).addTo(map);

    const stationIcon = (label: string) => L.divIcon({
      className: "",
      html: `<div style="white-space:nowrap;transform:translate(-40%,-100%);background:#fff;border:1px solid #dbe3f5;border-radius:10px;padding:5px 8px;box-shadow:0 2px 8px rgba(15,23,42,.16);font-size:11px;font-weight:700;color:#172033">${label}</div>`,
      iconSize: [1, 1],
    });
    L.marker(route[0], { icon: stationIcon(`출발 · ${tracking.originStation}`) }).addTo(map);
    L.marker(route[route.length - 1], { icon: stationIcon(`도착 · ${tracking.destinationStation}`) }).addTo(map);

    const trainIcon = L.divIcon({
      className: "",
      html: '<div style="display:flex;width:42px;height:42px;align-items:center;justify-content:center;border:4px solid white;border-radius:999px;background:#244bb3;box-shadow:0 4px 14px rgba(36,75,179,.45);font-size:22px">🚂</div>',
      iconSize: [42, 42],
      iconAnchor: [21, 21],
    });
    L.marker([tracking.currentLatitude, tracking.currentLongitude], { icon: trainIcon, zIndexOffset: 1000 })
      .bindTooltip(tracking.currentSegment, { permanent: false, direction: "top", offset: [0, -20] })
      .addTo(map);

    map.fitBounds(L.latLngBounds(route), { padding: [35, 35], maxZoom: 9 });
    window.setTimeout(() => map.invalidateSize(), 0);
    return () => {
      map.remove();
    };
  }, [tracking]);

  return <div ref={containerRef} className="h-64 w-full overflow-hidden rounded-2xl bg-brand-50" aria-label="실시간 열차 위치 지도" />;
}
