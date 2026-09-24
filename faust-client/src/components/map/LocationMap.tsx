import React, { useEffect, useRef } from 'react';
import L from 'leaflet';

// Fix Leaflet default marker icon broken by webpack/vite bundling
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

interface LocationMapProps {
  lat: number;
  lon: number;
  name: string;
  /** Zoom level — defaults to 10 for cities, lower for larger areas */
  zoom?: number;
  /** Additional markers to show (e.g. sub-locations) */
  markers?: { lat: number; lon: number; name: string; type: string }[];
  className?: string;
}

/**
 * Dark-themed Leaflet map for LocationDetailPage.
 * Uses CartoDB Dark Matter tiles — free, no API key required.
 * Renders a marker at the primary location and optional secondary markers.
 */
const LocationMap: React.FC<LocationMapProps> = ({
  lat, lon, name, zoom = 10, markers = [], className = ''
}) => {
  const mapRef     = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    // Initialize map
    const map = L.map(containerRef.current, {
      center:          [lat, lon],
      zoom,
      zoomControl:     true,
      attributionControl: true,
    });

    // Stadia Maps — dark tiles, free tier, no API key required for low usage
    L.tileLayer(
      'https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png',
      {
        attribution: '© <a href="https://stadiamaps.com/">Stadia Maps</a> © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 20,
      }
    ).addTo(map);

    // Custom accent-colored marker for primary location
    const primaryIcon = L.divIcon({
      className: '',
      html: `<div style="
        width: 14px; height: 14px;
        background: #38bdf8;
        border: 2px solid rgba(56,189,248,0.4);
        border-radius: 50%;
        box-shadow: 0 0 12px rgba(56,189,248,0.8), 0 0 4px rgba(56,189,248,1);
      "></div>`,
      iconSize:   [14, 14],
      iconAnchor: [7, 7],
    });

    // Secondary marker style for sub-locations
    const secondaryIcon = L.divIcon({
      className: '',
      html: `<div style="
        width: 8px; height: 8px;
        background: #94a3b8;
        border: 1px solid rgba(148,163,184,0.4);
        border-radius: 50%;
        box-shadow: 0 0 6px rgba(148,163,184,0.4);
      "></div>`,
      iconSize:   [8, 8],
      iconAnchor: [4, 4],
    });

    // Primary marker
    L.marker([lat, lon], { icon: primaryIcon })
      .addTo(map)
      .bindPopup(`<b style="color:#38bdf8">${name}</b>`, { className: 'faust-popup' });

    // Secondary markers
    markers.forEach(m => {
      if (m.lat && m.lon) {
        L.marker([m.lat, m.lon], { icon: secondaryIcon })
          .addTo(map)
          .bindPopup(`<span style="color:#94a3b8">${m.name}</span><br/><small>${m.type}</small>`);
      }
    });

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Re-center when location changes
  useEffect(() => {
    if (mapRef.current) {
      mapRef.current.setView([lat, lon], zoom);
    }
  }, [lat, lon, zoom]);

  return (
    <div
      ref={containerRef}
      className={className}
      style={{ background: '#0d1117' }}
    />
  );
};

export default LocationMap;