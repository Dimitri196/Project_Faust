import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  ChevronLeft, Building2, Search, Database,
  Layers, ShieldAlert, ArrowDownRight,
  ChevronRight, MapPin, Globe,
  Landmark, Scale, Zap, Map, ShieldCheck,
  Copy, Check, Users
} from 'lucide-react';
import api from '../api/axios';
import type { LocationResponse, InstitutionResponse, Page } from '../types';
import LocationMap from '../components/map/LocationMap';

interface LocationDetailWithPath extends LocationResponse {
  path: LocationResponse[];
}

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 350;

// ── Level config ──────────────────────────────────────────────────────────────
const LEVEL_CONFIG: Record<string, { color: string; bg: string; border: string; dot: string; icon: React.ReactNode; label: string }> = {
  CONTINENT:      { color: 'text-purple-400',  bg: 'bg-purple-500/10',  border: 'border-purple-500/30',  dot: 'bg-purple-500',  icon: <Globe    size={13} />, label: 'Continent'    },
  COUNTRY:        { color: 'text-cyan-400',    bg: 'bg-cyan-500/10',    border: 'border-cyan-500/30',    dot: 'bg-cyan-500',    icon: <Map      size={13} />, label: 'Country'      },
  PROVINCE:       { color: 'text-blue-400',    bg: 'bg-blue-500/10',    border: 'border-blue-500/30',    dot: 'bg-blue-500',    icon: <Layers   size={13} />, label: 'Province'     },
  DISTRICT:       { color: 'text-amber-400',   bg: 'bg-amber-500/10',   border: 'border-amber-500/30',   dot: 'bg-amber-500',   icon: <MapPin   size={13} />, label: 'District'     },
  CITY:           { color: 'text-emerald-400', bg: 'bg-emerald-500/10', border: 'border-emerald-500/30', dot: 'bg-emerald-500', icon: <Building2 size={13}/>, label: 'City'         },
  SUBDIVISION_L1: { color: 'text-emerald-400', bg: 'bg-emerald-500/10', border: 'border-emerald-500/30', dot: 'bg-emerald-500', icon: <Building2 size={13}/>, label: 'Borough'      },
  SUBDIVISION_L2: { color: 'text-emerald-400', bg: 'bg-emerald-500/10', border: 'border-emerald-500/30', dot: 'bg-emerald-500', icon: <Building2 size={13}/>, label: 'Neighbourhood' },
  FACILITY:       { color: 'text-rose-400',    bg: 'bg-rose-500/10',    border: 'border-rose-500/30',    dot: 'bg-rose-500',    icon: <Building2 size={13}/>, label: 'Facility'     },
  DEFAULT:        { color: 'text-slate-400',   bg: 'bg-slate-500/10',   border: 'border-slate-500/30',   dot: 'bg-slate-500',   icon: <MapPin   size={13} />, label: 'Location'     },
};

const getLevelCfg = (type: string) => LEVEL_CONFIG[type] ?? LEVEL_CONFIG.DEFAULT;

const getInstIcon = (type: string, size = 16) => {
  switch (type) {
    case 'INTELLIGENCE': return <ShieldAlert size={size} className="text-red-400" />;
    case 'MILITARY':     return <ShieldCheck size={size} className="text-orange-400" />;
    case 'LEGISLATIVE':  return <Landmark    size={size} className="text-purple-400" />;
    case 'REGULATORY':   return <Zap         size={size} className="text-yellow-400" />;
    case 'JUDICIAL':     return <Scale       size={size} className="text-blue-400" />;
    default:             return <Building2   size={size} className="text-slate-400" />;
  }
};

// ── Zoom level per location type ──────────────────────────────────────────────
const getZoom = (type: string): number => {
  switch (type) {
    case 'CONTINENT': return 3;
    case 'COUNTRY':   return 5;
    case 'PROVINCE':  return 7;
    case 'DISTRICT':  return 9;
    case 'CITY':      return 12;
    default:          return 10;
  }
};

// ── Main Page ─────────────────────────────────────────────────────────────────
const LocationDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [instPage, setInstPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [copiedCoords, setCopiedCoords] = useState(false);
  const [activeTab, setActiveTab] = useState<'institutions' | 'sublocations'>('institutions');

  React.useEffect(() => { setInstPage(0); }, [id]);

  React.useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchInput.trim()), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [searchInput]);

  React.useEffect(() => { setInstPage(0); }, [debouncedSearch]);

  const { data: location, isLoading: isLocLoading, isError: isLocError } = useQuery<LocationDetailWithPath>({
    queryKey: ['location', id],
    queryFn: async () => {
      const [detailRes, pathRes] = await Promise.all([
        api.get<LocationResponse>(`/locations/${id}`),
        api.get<LocationResponse[]>(`/locations/${id}/path`),
      ]);
      return { ...detailRes.data, path: pathRes.data };
    },
    enabled: !!id
  });

  const { data: subSectors, isLoading: isSubLoading } = useQuery<LocationResponse[]>({
    queryKey: ['location-subs', id],
    queryFn: async () => (await api.get(`/locations/${id}/sub-locations`)).data,
    enabled: !!id
  });

  const { data: institutionsPage, isLoading: isInstLoading } = useQuery<Page<InstitutionResponse>>({
    queryKey: ['location-institutions', id, instPage, debouncedSearch],
    queryFn: async () => (await api.get('/institutions/search', {
      params: { locationId: id, name: debouncedSearch || undefined, page: instPage, size: PAGE_SIZE, sort: 'name,asc' }
    })).data,
    enabled: !!id
  });

  const institutions = institutionsPage?.content ?? [];

  const copyCoords = () => {
    if (!location?.latitude || !location?.longitude) return;
    navigator.clipboard.writeText(`${location.latitude}, ${location.longitude}`);
    setCopiedCoords(true);
    setTimeout(() => setCopiedCoords(false), 2000);
  };

  // Sub-location markers for the map
  const subMarkers = (subSectors ?? [])
    .filter(s => s.latitude && s.longitude)
    .map(s => ({ lat: s.latitude!, lon: s.longitude!, name: s.name, type: s.type }));

  if (isLocLoading) return <LoadingState />;
  if (isLocError || !location) return <ErrorState message="Location not found" />;

  const cfg      = getLevelCfg(location.type);
  const hasCoords = location.latitude != null && location.longitude != null;

  return (
    <div className="h-full bg-[#05070a] text-slate-300 flex flex-col overflow-hidden font-sans">

      {/* ── HEADER ── */}
      <div className="shrink-0 bg-black/40 border-b border-white/8 px-6 py-4">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">

            {/* Back + Registry */}
            <div className="flex items-center gap-3 mb-3">
              <button
                onClick={() => window.history.length > 1 ? navigate(-1) : navigate('/locations')}
                className="flex items-center gap-1.5 text-slate-600 hover:text-brand-accent transition-colors text-[11px] font-mono group"
              >
                <ChevronLeft size={13} className="group-hover:-translate-x-0.5 transition-transform" /> Back
              </button>
              <span className="text-slate-800">·</span>
              <Link to="/locations" className="flex items-center gap-1.5 text-slate-700 hover:text-brand-accent transition-colors text-[11px] font-mono">
                <Globe size={11} /> Registry
              </Link>
            </div>

            {/* Ancestor breadcrumb */}
            {location.path && location.path.length > 1 && (
              <div className="flex items-center gap-1 flex-wrap mb-2">
                {location.path.slice(0, -1).map((ancestor, idx) => {
                  const aCfg = getLevelCfg(ancestor.type);
                  return (
                    <React.Fragment key={ancestor.externalId}>
                      <Link to={`/locations/${ancestor.externalId}`}
                        className={`text-[11px] font-mono ${aCfg.color} opacity-60 hover:opacity-100 transition-opacity`}>
                        {ancestor.name}
                      </Link>
                      {idx < location.path.length - 2 && <ChevronRight size={10} className="text-slate-800" />}
                    </React.Fragment>
                  );
                })}
                <ChevronRight size={10} className="text-slate-800" />
              </div>
            )}

            {/* Type badge + name */}
            <div className="flex items-center gap-3 mb-1.5 flex-wrap">
              <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold ${cfg.bg} ${cfg.color} border ${cfg.border} rounded-sm`}>
                {cfg.icon} {cfg.label}
              </div>
              {location.isoCode && (
                <span className="text-[11px] font-mono text-slate-600">{location.isoCode}</span>
              )}
              <div className={`w-1.5 h-1.5 rounded-full ${cfg.dot} animate-pulse`} />
              <span className="text-[11px] text-slate-600">{location.active ? 'Active' : 'Inactive'}</span>
            </div>

            <h1 className="text-2xl font-bold text-white leading-tight">{location.name}</h1>
            {location.localName && location.localName !== location.name && (
              <p className="text-[12px] text-slate-600 font-mono mt-0.5">{location.localName}</p>
            )}
          </div>

          {/* Coordinates */}
          {hasCoords && (
            <button onClick={copyCoords}
              className="shrink-0 flex flex-col items-end gap-1 group px-3 py-2 border border-white/8 hover:border-brand-accent/30 transition-colors"
              title="Copy coordinates">
              <div className="flex items-center gap-1.5 text-[9px] text-slate-600 uppercase tracking-wide font-mono">
                <MapPin size={10} /> Coordinates
                {copiedCoords
                  ? <Check size={10} className="text-emerald-400" />
                  : <Copy size={10} className="opacity-0 group-hover:opacity-100 transition-opacity" />}
              </div>
              <div className="font-mono text-[12px] text-brand-accent font-bold">
                {location.latitude!.toFixed(4)}, {location.longitude!.toFixed(4)}
              </div>
            </button>
          )}
        </div>
      </div>

      {/* ── BODY — split layout ── */}
      <div className="flex-1 flex overflow-hidden min-h-0">

        {/* LEFT PANEL — hierarchy, details, tabs */}
        <div className="w-[420px] shrink-0 flex flex-col border-r border-white/8 overflow-hidden">

          {/* Scrollable top section */}
          <div className="flex-1 overflow-y-auto custom-scrollbar p-5 space-y-4 min-h-0">

            {/* Hierarchy — only when ancestors exist */}
            {location.path && location.path.length > 1 && (
              <section className="bg-black/30 border border-white/8 p-4">
                <h3 className="text-[10px] font-semibold text-slate-600 uppercase tracking-widest mb-3 flex items-center gap-2">
                  <Layers size={12} className="text-brand-accent" /> Hierarchy
                </h3>
                <LocationHierarchy path={location.path} currentId={location.externalId} />
              </section>
            )}

            {/* Details */}
            <section className="bg-black/30 border border-white/8 p-4">
              <h3 className="text-[10px] font-semibold text-slate-600 uppercase tracking-widest mb-3">Details</h3>
              <div className="space-y-2.5">
                {[
                  { label: 'Verification', value: location.verificationStatus?.replace(/_/g, ' ') },
                  { label: 'Source',       value: location.sourceType?.replace(/_/g, ' ') },
                  { label: 'Clearance',    value: location.clearanceLevel?.replace(/_/g, ' ') },
                  { label: 'Added',        value: location.createdAt ? new Date(location.createdAt).toLocaleDateString('en-GB') : null },
                ].filter(f => f.value).map(({ label, value }) => (
                  <div key={label} className="flex items-center justify-between text-[11px] border-b border-white/[0.04] pb-2">
                    <span className="text-slate-600">{label}</span>
                    <span className="text-slate-300 font-mono">{value}</span>
                  </div>
                ))}
              </div>
            </section>
          </div>

          {/* Tab switcher */}
          <div className="shrink-0 border-t border-white/8 flex">
            <button
              onClick={() => setActiveTab('institutions')}
              className={`flex-1 flex items-center justify-center gap-2 py-3 text-[11px] font-semibold transition-all border-b-2 ${
                activeTab === 'institutions'
                  ? 'border-brand-accent text-brand-accent bg-brand-accent/5'
                  : 'border-transparent text-slate-600 hover:text-slate-300'
              }`}
            >
              <Building2 size={12} />
              Institutions
              {(institutionsPage?.totalElements ?? 0) > 0 && (
                <span className="text-[9px] bg-brand-accent/20 text-brand-accent px-1.5 py-0.5 rounded-sm font-bold">
                  {institutionsPage!.totalElements}
                </span>
              )}
            </button>
            <button
              onClick={() => setActiveTab('sublocations')}
              className={`flex-1 flex items-center justify-center gap-2 py-3 text-[11px] font-semibold transition-all border-b-2 ${
                activeTab === 'sublocations'
                  ? 'border-brand-accent text-brand-accent bg-brand-accent/5'
                  : 'border-transparent text-slate-600 hover:text-slate-300'
              }`}
            >
              <Users size={12} />
              Sub-locations
              {(subSectors?.length ?? 0) > 0 && (
                <span className={`text-[9px] px-1.5 py-0.5 rounded-sm font-bold ${
                  activeTab === 'sublocations' ? `${cfg.bg} ${cfg.color}` : 'bg-white/5 text-slate-600'
                }`}>
                  {subSectors!.length}
                </span>
              )}
            </button>
          </div>

          {/* Tab content */}
          <div className="h-64 shrink-0 overflow-y-auto custom-scrollbar border-t border-white/5">

            {/* Institutions tab */}
            {activeTab === 'institutions' && (
              <div className="p-3 space-y-2">
                <div className="relative">
                  <Search size={11} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-700 pointer-events-none" />
                  <input
                    type="text"
                    value={searchInput}
                    onChange={(e) => setSearchInput(e.target.value)}
                    placeholder="Search institutions..."
                    className="w-full bg-black/40 border border-white/8 focus:border-brand-accent/40 outline-none pl-8 pr-3 py-2 text-[11px] text-slate-300 placeholder:text-slate-700 transition-colors"
                  />
                </div>

                {isInstLoading ? (
                  <div className="space-y-2">
                    {[1,2,3].map(i => <div key={i} className="h-12 bg-white/5 animate-pulse" />)}
                  </div>
                ) : institutions.length > 0 ? (
                  <>
                    {institutions.map((inst) => (
                      <Link key={inst.publicId} to={`/institutions/${inst.publicId}`}
                        className="group flex items-center gap-3 p-2.5 bg-black/20 border border-white/5 hover:border-white/20 transition-all">
                        <div className="shrink-0">{getInstIcon(inst.type, 14)}</div>
                        <div className="flex-1 min-w-0">
                          <div className="text-[12px] font-medium text-slate-300 group-hover:text-white transition-colors truncate">{inst.name}</div>
                          <div className="text-[9px] text-slate-700">{inst.type} · {inst.level}</div>
                        </div>
                        <ChevronRight size={10} className="text-slate-800 group-hover:text-brand-accent shrink-0 transition-colors" />
                      </Link>
                    ))}
                    {institutionsPage && institutionsPage.totalPages > 1 && (
                      <div className="flex justify-between items-center pt-1 text-[10px] text-slate-700 font-mono">
                        <button disabled={institutionsPage.first} onClick={() => setInstPage(p => p - 1)}
                          className="disabled:opacity-30 hover:text-brand-accent transition-colors">← Prev</button>
                        <span>{instPage + 1} / {institutionsPage.totalPages}</span>
                        <button disabled={institutionsPage.last} onClick={() => setInstPage(p => p + 1)}
                          className="disabled:opacity-30 hover:text-brand-accent transition-colors">Next →</button>
                      </div>
                    )}
                  </>
                ) : (
                  <div className="py-8 text-center">
                    <p className="text-[11px] text-slate-700">
                      {debouncedSearch ? `No matches for "${debouncedSearch}"` : 'No institutions here'}
                    </p>
                  </div>
                )}
              </div>
            )}

            {/* Sub-locations tab */}
            {activeTab === 'sublocations' && (
              <div className="p-3 space-y-1">
                {isSubLoading ? (
                  <div className="space-y-1">
                    {[1,2,3].map(i => <div key={i} className="h-9 bg-white/5 animate-pulse" />)}
                  </div>
                ) : subSectors && subSectors.length > 0 ? (
                  subSectors.map((sub) => {
                    const subCfg = getLevelCfg(sub.type);
                    return (
                      <Link key={sub.externalId} to={`/locations/${sub.externalId}`}
                        className="flex items-center justify-between px-3 py-2 bg-black/20 border border-white/5 hover:border-white/20 group transition-all">
                        <div className="flex items-center gap-2.5 min-w-0">
                          <div className={`w-1.5 h-1.5 rounded-full shrink-0 ${subCfg.dot}`} />
                          <span className="text-[12px] text-slate-400 group-hover:text-white transition-colors truncate">{sub.name}</span>
                        </div>
                        <ChevronRight size={10} className="text-slate-800 group-hover:text-brand-accent shrink-0 transition-colors" />
                      </Link>
                    );
                  })
                ) : (
                  <div className="py-8 text-center">
                    <p className="text-[11px] text-slate-700">No sub-locations</p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* RIGHT PANEL — map */}
        <div className="flex-1 relative overflow-hidden">
          {hasCoords ? (
            <LocationMap
              lat={location.latitude!}
              lon={location.longitude!}
              name={location.name}
              zoom={getZoom(location.type)}
              markers={subMarkers}
              className="w-full h-full"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center gap-3 bg-[#0d1117]">
              <MapPin size={32} className="text-slate-800" />
              <p className="text-[12px] text-slate-700 font-mono">No coordinates available</p>
              <p className="text-[10px] text-slate-800">Add GPS coordinates to show the map</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ── LocationHierarchy ─────────────────────────────────────────────────────────
const LocationHierarchy = ({ path, currentId }: { path?: LocationResponse[]; currentId?: string }) => {
  if (!path || path.length === 0) return null;
  return (
    <div className="flex flex-col">
      {path.map((loc, idx) => {
        const cfg       = getLevelCfg(loc.type);
        const isCurrent = loc.externalId === currentId;
        return (
          <div key={loc.externalId} className="flex items-stretch gap-3">
            <div className="flex flex-col items-center w-4 shrink-0">
              <div className={`w-2 h-2 rounded-full mt-2 shrink-0 ${isCurrent ? cfg.dot : 'bg-slate-800 border border-slate-700'}`} />
              {idx < path.length - 1 && <div className="w-px flex-1 bg-white/8 mt-1" />}
            </div>
            <div className="flex-1 pb-2">
              <Link to={`/locations/${loc.externalId}`}
                className={`flex flex-col group ${isCurrent ? 'pointer-events-none' : ''}`}>
                <span className={`text-[9px] font-mono uppercase ${cfg.color}`}>{cfg.label}</span>
                <span className={`text-[12px] leading-snug transition-colors ${
                  isCurrent ? 'text-white font-semibold' : 'text-slate-500 group-hover:text-slate-300'
                }`}>{loc.name}</span>
              </Link>
            </div>
          </div>
        );
      })}
    </div>
  );
};

// ── States ────────────────────────────────────────────────────────────────────
const LoadingState = () => (
  <div className="h-full bg-[#05070a] flex flex-col items-center justify-center gap-5 font-mono">
    <div className="relative w-12 h-12">
      <div className="absolute inset-0 border border-brand-accent/10 rounded-full" />
      <div className="absolute inset-0 border-t border-brand-accent rounded-full animate-spin" />
    </div>
    <span className="text-[10px] text-brand-accent uppercase tracking-[0.5em] animate-pulse">Loading...</span>
  </div>
);

const ErrorState = ({ message }: { message: string }) => (
  <div className="h-full bg-[#05070a] flex flex-col items-center justify-center gap-4">
    <ShieldAlert className="text-red-500" size={36} />
    <p className="text-[12px] text-red-500/80 font-mono">{message}</p>
    <Link to="/locations" className="text-[11px] text-brand-accent border border-brand-accent/30 px-4 py-2 hover:bg-brand-accent/10 transition-colors">
      Back to registry
    </Link>
  </div>
);

export default LocationDetailPage;