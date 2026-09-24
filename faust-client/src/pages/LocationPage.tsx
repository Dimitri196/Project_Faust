import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Search, ChevronRight, Loader2,
  Activity, ArrowLeft, X,
  Home, Globe, Map, Building, Layers, MapPin, LayoutGrid,
  Terminal,
  Cpu,
  ExternalLink
} from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../api/axios';
import type { LocationResponse, LocationType, Page } from '../types';
import SearchResultsMap from '../components/map/SearchResultsMap';

interface LocationFilter {
  query: string;
  type: LocationType | '';
  parentId: string;
  rootOnly: boolean;
}

// ── Hierarchy color system ────────────────────────────────────────────────────
// Each level has a distinct color — type badge, card border accent, and dot
const LEVEL_CONFIG: Record<string, {
  color: string;
  bg: string;
  border: string;
  dot: string;
  icon: React.ReactNode;
  label: string;
}> = {
  CONTINENT:      { color: 'text-purple-400',  bg: 'bg-purple-500/10',  border: 'border-l-purple-500',  dot: 'bg-purple-500',  icon: <Globe   size={12} />, label: 'Continent'      },
  COUNTRY:        { color: 'text-cyan-400',    bg: 'bg-cyan-500/10',    border: 'border-l-cyan-500',    dot: 'bg-cyan-500',    icon: <Map     size={12} />, label: 'Country'        },
  PROVINCE:       { color: 'text-blue-400',    bg: 'bg-blue-500/10',    border: 'border-l-blue-500',    dot: 'bg-blue-500',    icon: <Layers  size={12} />, label: 'Province'       },
  DISTRICT:       { color: 'text-amber-400',   bg: 'bg-amber-500/10',   border: 'border-l-amber-500',   dot: 'bg-amber-500',   icon: <MapPin  size={12} />, label: 'District'       },
  CITY:           { color: 'text-emerald-400', bg: 'bg-emerald-500/10', border: 'border-l-emerald-500', dot: 'bg-emerald-500', icon: <Building size={12}/>, label: 'City'           },
  SUBDIVISION_L1: { color: 'text-emerald-400', bg: 'bg-emerald-500/10', border: 'border-l-emerald-500', dot: 'bg-emerald-500', icon: <Building size={12}/>, label: 'Borough'        },
  SUBDIVISION_L2: { color: 'text-emerald-400', bg: 'bg-emerald-500/10', border: 'border-l-emerald-500', dot: 'bg-emerald-500', icon: <Building size={12}/>, label: 'Neighbourhood'  },
  FACILITY:       { color: 'text-rose-400',    bg: 'bg-rose-500/10',    border: 'border-l-rose-500',    dot: 'bg-rose-500',    icon: <Building size={12}/>, label: 'Facility'       },
  DEFAULT:        { color: 'text-slate-400',   bg: 'bg-slate-500/10',   border: 'border-l-slate-500',   dot: 'bg-slate-500',   icon: <MapPin  size={12} />, label: 'Location'       },
};

const getLevel = (type: string) => LEVEL_CONFIG[type] ?? LEVEL_CONFIG.DEFAULT;

// ── Main Page ─────────────────────────────────────────────────────────────────

const LocationPage = () => {
  const navigate = useNavigate();
  const [hasSearched, setHasSearched]   = useState(false);
  const [isTooShort, setIsTooShort]     = useState(false);
  const [filters, setFilters]           = useState<LocationFilter>({
    query: '', type: '', parentId: '', rootOnly: true,
  });
  const [drillStack, setDrillStack] = useState<{ id: string; name: string; type: string }[]>([]);
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // Debounce query for live search
  React.useEffect(() => {
    const timer = setTimeout(() => {
      const q = filters.query.trim();
      if (q.length === 0 || q.length >= 3) {
        setDebouncedQuery(q);
        if (q.length >= 3) setHasSearched(true);
        if (q.length === 0) {
          setHasSearched(false);
          setFilters(prev => ({ ...prev, rootOnly: true, parentId: '' }));
          setDrillStack([]);
        }
      }
      setIsTooShort(q.length > 0 && q.length < 3);
    }, 350);
    return () => clearTimeout(timer);
  }, [filters.query]);

  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');

  const clearAll = () => {
    setFilters({ query: '', type: '', parentId: '', rootOnly: true });
    setHasSearched(false);
    setIsTooShort(false);
    setDebouncedQuery('');
    setDrillStack([]);
  };

  const goBack = () => {
    if (drillStack.length === 0) { navigate('/'); return; }
    const newStack = drillStack.slice(0, -1);
    const parentId = newStack.length > 0 ? newStack[newStack.length - 1].id : '';
    setDrillStack(newStack);
    setFilters(prev => ({ ...prev, parentId, rootOnly: !parentId, query: '' }));
    setHasSearched(newStack.length > 0);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const q = filters.query.trim();
    if (q.length > 0 && q.length < 3) {
      setIsTooShort(true);
      setTimeout(() => setIsTooShort(false), 3000);
      return;
    }
    setHasSearched(true);
    setDebouncedQuery(q);
  };

  const { data, isLoading, isError, refetch } = useQuery<Page<LocationResponse>>({
    queryKey: ['locations-search', filters.parentId, debouncedQuery, filters.type, hasSearched],
    queryFn: async () => {
      const res = await api.get('/locations/search', {
        params: {
          query:    debouncedQuery   || undefined,
          type:     filters.type     || undefined,
          parentId: filters.parentId || undefined,
          rootOnly: filters.parentId ? false : (debouncedQuery ? false : filters.rootOnly),
          page: 0,
          size: 100
        }
      });
      return res.data;
    },
    enabled: true,
  });

  const locations = data?.content || [];

  const enterSector = (loc: LocationResponse) => {
    if (loc.hasChildren) {
      setDrillStack(prev => [...prev, { id: loc.externalId, name: loc.name, type: loc.type }]);
      setFilters({ query: '', type: '', parentId: loc.externalId, rootOnly: false });
      setHasSearched(true);
    } else {
      navigate(`/locations/${loc.externalId}`);
    }
  };

  const currentLevel = drillStack.length > 0
    ? getLevel(drillStack[drillStack.length - 1].type)
    : null;

  return (
    <div className="h-full bg-[#05070a] flex flex-col font-sans text-slate-300 selection:bg-brand-accent selection:text-black">

      {/* ── STATUS BAR ── */}
      <div className="w-full border-b border-white/5 bg-black/60 px-5 py-2 flex justify-between items-center backdrop-blur-md sticky top-0 z-50 shrink-0">
        <div className="flex items-center gap-5">
          <div className="flex items-center gap-2 border-r border-white/10 pr-5">
            <Terminal size={13} className="text-brand-accent" />
            <span className="text-[10px] tracking-widest font-bold text-slate-200 uppercase font-mono">
              Module::Geo_Registry
            </span>
          </div>
          <div className="flex items-center gap-2 text-[9px] uppercase tracking-wide opacity-50 font-mono">
            <Cpu size={11} />
            <span>Node_Status: <span className="text-green-500">Optimal</span></span>
          </div>
        </div>
        <div className="flex items-center gap-3 text-[9px] font-mono">
          <button
            onClick={goBack}
            className="flex items-center gap-1.5 text-slate-500 hover:text-brand-accent transition-colors uppercase tracking-wide"
          >
            {drillStack.length === 0
              ? <><Home size={10} /> Home</>
              : <><ArrowLeft size={10} /> Back</>
            }
          </button>
          {(filters.parentId || hasSearched) && (
            <>
              <span className="opacity-20">·</span>
              <button onClick={clearAll} className="text-slate-600 hover:text-slate-300 transition-colors uppercase tracking-wide flex items-center gap-1">
                <X size={9} /> Reset
              </button>
            </>
          )}
        </div>
      </div>

      {/* ── BREADCRUMB TRAIL ── */}
      <div className={`w-full bg-black/40 border-b border-white/5 px-5 py-2.5 flex items-center gap-0 overflow-x-auto shrink-0 transition-all ${drillStack.length === 0 ? 'opacity-60' : ''}`}>
        {/* Root pill */}
        <button
          onClick={clearAll}
          className={`flex items-center gap-1.5 px-3 py-1 rounded-sm text-[11px] font-semibold transition-all shrink-0 ${
            drillStack.length === 0
              ? 'bg-brand-accent/20 text-brand-accent border border-brand-accent/30'
              : 'text-slate-500 hover:text-slate-300 hover:bg-white/5 border border-transparent'
          }`}
        >
          <Globe size={11} /> Root
        </button>

        {drillStack.map((entry, idx) => {
          const cfg = getLevel(entry.type);
          const isLast = idx === drillStack.length - 1;
          return (
            <React.Fragment key={entry.id}>
              <ChevronRight size={12} className="text-slate-700 mx-1 shrink-0" />
              <button
                onClick={() => {
                  const newStack = drillStack.slice(0, idx + 1);
                  setDrillStack(newStack);
                  setFilters(prev => ({ ...prev, parentId: entry.id, rootOnly: false, query: '' }));
                  setHasSearched(true);
                }}
                className={`flex items-center gap-1.5 px-3 py-1 rounded-sm text-[11px] font-semibold transition-all shrink-0 ${
                  isLast
                    ? `${cfg.bg} ${cfg.color} border ${cfg.border.replace('border-l-', 'border-')}`
                    : 'text-slate-500 hover:text-slate-300 hover:bg-white/5 border border-transparent'
                }`}
              >
                {cfg.icon}
                {entry.name}
              </button>
            </React.Fragment>
          );
        })}
      </div>

      <div className="flex-1 flex flex-col min-h-0 overflow-hidden">

        {/* ── SEARCH BAR ── */}
        <div className="px-6 pt-5 pb-4 shrink-0">
          <form onSubmit={handleSearch}>
            <div className="flex items-center gap-3 bg-black/50 border border-white/8 hover:border-white/15 focus-within:border-brand-accent/40 transition-colors px-4 py-3 rounded-sm">
              <Search size={16} className={`shrink-0 transition-colors ${isLoading ? 'text-brand-accent animate-pulse' : filters.query ? 'text-brand-accent' : 'text-slate-700'}`} />
              <input
                type="text"
                autoComplete="off"
                spellCheck="false"
                placeholder="Search locations..."
                className="flex-1 bg-transparent text-[13px] text-white focus:outline-none placeholder:text-slate-700"
                value={filters.query}
                onKeyDown={(e) => e.key === 'Escape' && clearAll()}
                onChange={(e) => {
                  const val = e.target.value;
                  setFilters(prev => ({ ...prev, query: val }));
                  if (val === '') {
                    setHasSearched(false);
                    setFilters(prev => ({ ...prev, rootOnly: true, parentId: '', query: '' }));
                  }
                }}
              />

              {/* Type filter */}
              <div className="flex items-center gap-2 border-l border-white/10 pl-3 shrink-0">
                <select
                  className="bg-transparent text-[11px] text-slate-400 outline-none cursor-pointer hover:text-slate-200 transition-colors"
                  value={filters.type}
                  onChange={(e) => {
                    setFilters(f => ({ ...f, type: e.target.value as any }));
                    setHasSearched(true);
                  }}
                >
                  <option value="">All types</option>
                  <option value="CONTINENT">Continent</option>
                  <option value="COUNTRY">Country</option>
                  <option value="PROVINCE">Province</option>
                  <option value="DISTRICT">District</option>
                  <option value="CITY">City</option>
                </select>
              </div>

              {filters.query && (
                <button type="button" onClick={clearAll}
                  className="text-slate-600 hover:text-slate-300 transition-colors shrink-0">
                  <X size={14} />
                </button>
              )}
            </div>

            {isTooShort && (
              <p className="mt-2 text-[10px] text-red-500/70 font-mono">
                ⚠ Minimum 3 characters required
              </p>
            )}
          </form>
        </div>

        <div className="px-6 pb-3 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2">
            <Activity size={11} className="text-brand-accent animate-pulse" />
            <span className="text-[11px] text-slate-500">
              <span className="text-white font-semibold">{locations.length}</span>
              {' '}location{locations.length !== 1 ? 's' : ''} found
              {drillStack.length > 0 && (
                <span className="text-slate-700"> · inside <span className="text-slate-400">{drillStack[drillStack.length - 1].name}</span></span>
              )}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setViewMode('list')}
              className={`p-1.5 border transition-all ${viewMode === 'list' ? 'border-brand-accent/50 text-brand-accent' : 'border-white/8 text-slate-600 hover:text-slate-300'}`}
              title="List view"
            >
              <LayoutGrid size={13} />
            </button>
            <button
              onClick={() => setViewMode('map')}
              className={`p-1.5 border transition-all ${viewMode === 'map' ? 'border-brand-accent/50 text-brand-accent' : 'border-white/8 text-slate-600 hover:text-slate-300'}`}
              title="Map view"
            >
              <Map size={13} />
            </button>
          </div>
        </div>

        {/* ── GRID or MAP ── */}
        <div className="flex-1 overflow-hidden min-h-0">
          {viewMode === 'map' ? (
            <SearchResultsMap
              locations={locations}
              onSelect={(loc) => {
                if (loc.hasChildren) enterSector(loc);
                else navigate(`/locations/${loc.externalId}`);
              }}
              className="w-full h-full"
            />
          ) : (
          <div className="overflow-y-auto custom-scrollbar px-6 pb-8 h-full">
          {isLoading ? (
            <div className="h-64 flex flex-col items-center justify-center gap-4">
              <Loader2 className="animate-spin text-brand-accent/40" size={28} />
              <span className="text-[11px] text-slate-700 tracking-widest">Loading locations...</span>
            </div>
          ) : isError ? (
            <div className="h-64 flex items-center justify-center">
              <span className="text-[11px] text-red-500/70 font-mono uppercase tracking-widest">Connection failed — retry</span>
            </div>
          ) : locations.length === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center gap-3 border border-dashed border-white/5">
              <MapPin size={28} className="text-slate-800" />
              <p className="text-[11px] text-slate-700 uppercase tracking-widest">No locations found</p>
              {filters.query && (
                <button onClick={clearAll}
                  className="text-[10px] text-brand-accent/70 hover:text-brand-accent border border-brand-accent/20 px-3 py-1.5 transition-colors">
                  Clear search
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
              {locations.map(loc => (
                <LocationCard key={loc.externalId} location={loc} onEnter={() => enterSector(loc)} />
              ))}
            </div>
          )}
          </div>
          )}
        </div>
      </div>
    </div>
  );
};

// ── Location Card ─────────────────────────────────────────────────────────────

const LocationCard = ({ location, onEnter }: {
  location: LocationResponse;
  onEnter: () => void;
}) => {
  const cfg = getLevel(location.type);

  return (
    <div className={`group flex flex-col bg-black/40 border border-white/8 border-l-2 ${cfg.border} hover:border-white/20 hover:bg-white/[0.03] transition-all overflow-hidden`}>

      {/* Card body */}
      <div className="p-4 flex-1 cursor-pointer" onClick={onEnter}>
        {/* Type badge */}
        <div className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-sm text-[9px] font-bold uppercase tracking-widest mb-3 ${cfg.bg} ${cfg.color}`}>
          {cfg.icon}
          {cfg.label}
        </div>

        {/* Name */}
        <h3 className="text-[13px] font-bold text-slate-200 group-hover:text-white transition-colors leading-snug mb-2 line-clamp-2">
          {location.name}
        </h3>

        {/* Parent context — critical when multiple locations share the same name */}
        {location.parentName && (
          <div className="flex items-center gap-1 mb-2">
            <ChevronRight size={9} className="text-slate-700 shrink-0" />
            <span className="text-[10px] text-slate-600 truncate">{location.parentName}</span>
          </div>
        )}

        {/* ISO code + status */}
        <div className="flex items-center justify-between">
          {location.isoCode ? (
            <span className="text-[9px] font-mono text-slate-700">{location.isoCode}</span>
          ) : (
            <span />
          )}
          <div className="flex items-center gap-1.5">
            <div className={`w-1.5 h-1.5 rounded-full ${location.hasChildren ? `${cfg.dot} animate-pulse` : 'bg-slate-800'}`} />
            <span className="text-[9px] text-slate-600">
              {location.hasChildren ? 'Has children' : 'Leaf node'}
            </span>
          </div>
        </div>
      </div>

      {/* Card footer */}
      <div className="border-t border-white/5 px-4 py-2 flex items-center justify-between bg-black/20">
        <Link
          to={`/locations/${location.externalId}`}
          onClick={(e) => e.stopPropagation()}
          className="flex items-center gap-1 text-[9px] text-slate-600 hover:text-brand-accent transition-colors"
        >
          <ExternalLink size={9} /> Open details
        </Link>
        {location.hasChildren && (
          <button
            onClick={(e) => { e.stopPropagation(); onEnter(); }}
            className={`flex items-center gap-1 text-[9px] font-semibold transition-colors ${cfg.color} opacity-60 hover:opacity-100`}
          >
            Drill down <ChevronRight size={9} />
          </button>
        )}
      </div>
    </div>
  );
};

export default LocationPage;