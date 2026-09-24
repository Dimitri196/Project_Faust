import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import type { LocationResponse } from '../../types';

delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// Color per location type — matches LocationPage hierarchy colors
const TYPE_COLORS: Record<string, string> = {
  CONTINENT:      '#a855f7',  // purple
  COUNTRY:        '#22d3ee',  // cyan
  PROVINCE:       '#60a5fa',  // blue
  DISTRICT:       '#fbbf24',  // amber
  CITY:           '#34d399',  // emerald
  SUBDIVISION_L1: '#34d399',
  SUBDIVISION_L2: '#34d399',
  FACILITY:       '#f87171',  // rose
};

interface SearchResultsMapProps {
  locations: LocationResponse[];
  onSelect?: (location: LocationResponse) => void;
  className?: string;
}

/**
 * Map showing all search results as colored pins.
 * Used in LocationPage map toggle view.
 * Pins are colored by location type matching the hierarchy color system.
 */
const SearchResultsMap: React.FC<SearchResultsMapProps> = ({
  locations, onSelect, className = ''
}) => {
  const mapRef      = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const markersRef  = useRef<L.Marker[]>([]);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const map = L.map(containerRef.current, {
      center:   [50.0, 15.0], // Default: Central Europe
      zoom:     5,
      zoomControl: true,
    });

    L.tileLayer(
      'https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png',
      {
        attribution: '© <a href="https://stadiamaps.com/">Stadia Maps</a> © <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
        maxZoom: 20,
      }
    ).addTo(map);

    mapRef.current = map;
    return () => { map.remove(); mapRef.current = null; };
  }, []);

  // Update markers when locations change
  useEffect(() => {
    if (!mapRef.current) return;
    const map = mapRef.current;

    // Clear existing markers
    markersRef.current.forEach(m => m.remove());
    markersRef.current = [];

    const validLocations = locations.filter(l => l.latitude && l.longitude);
    if (validLocations.length === 0) return;

    const bounds: [number, number][] = [];

    validLocations.forEach(loc => {
      const color = TYPE_COLORS[loc.type] ?? '#94a3b8';

      const icon = L.divIcon({
        className: '',
        html: `<div style="
          width: 10px; height: 10px;
          background: ${color};
          border: 1.5px solid ${color}66;
          border-radius: 50%;
          box-shadow: 0 0 8px ${color}88;
          cursor: pointer;
        "></div>`,
        iconSize:   [10, 10],
        iconAnchor: [5, 5],
      });

      const marker = L.marker([loc.latitude!, loc.longitude!], { icon })
        .addTo(map)
        .bindPopup(
          `<div style="min-width:120px">
            <div style="color:${color}; font-size:10px; text-transform:uppercase; margin-bottom:2px">${loc.type}</div>
            <b style="color:#fff; font-size:13px">${loc.name}</b>
            ${loc.localName && loc.localName !== loc.name
              ? `<div style="color:#64748b; font-size:11px">${loc.localName}</div>`
              : ''}
          </div>`,
          { className: 'faust-popup' }
        );

      if (onSelect) {
        marker.on('click', () => onSelect(loc));
      }

      markersRef.current.push(marker);
      bounds.push([loc.latitude!, loc.longitude!]);
    });

    // Fit map to show all markers
    if (bounds.length > 0) {
      if (bounds.length === 1) {
        map.setView(bounds[0], 10);
      } else {
        map.fitBounds(bounds as L.LatLngBoundsExpression, { padding: [40, 40] });
      }
    }
  }, [locations, onSelect]);

  return (
    <div
      ref={containerRef}
      className={className}
      style={{ background: '#0d1117' }}
    />
  );
};

export default SearchResultsMap;