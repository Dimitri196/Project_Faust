import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  Search, ChevronRight, Loader2, Shield, Zap, 
  Activity, Crosshair, MapPin, Globe, Filter,
  Layers, Target, Landmark, Home, AlertTriangle
} from 'lucide-react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import type { LocationResponse, LocationType, Page } from '../types';

const LocationPage = () => {
  const [hasSearched, setHasSearched] = useState(false);
  const [isTooShort, setIsTooShort] = useState(false);
  const [filters, setFilters] = useState({
    query: '',
    type: '' as LocationType | '',
    parentId: '',
  });

  const { data, isLoading, isError, refetch } = useQuery<Page<LocationResponse>>({
    queryKey: ['locations-search'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/locations/search', {
        params: { 
          query: filters.query || undefined, 
          type: filters.type || undefined,
          parentId: filters.parentId || undefined,
          page: 0,
          size: 100 
        }
      });
      return res.data;
    },
    enabled: false,
  });

  const locations = data?.content || [];

  // --- VALIDATION LOGIC ---
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    
    // SECURITY PROTOCOL: Prevent global database dump
    if (filters.query.trim().length < 3) {
      setIsTooShort(true);
      setTimeout(() => setIsTooShort(false), 3000);
      return;
    }

    setIsTooShort(false);
    setHasSearched(true);
    refetch();
  };

  // --- SMART GROUPING (SYNCED WITH JAVA ENUM GRANULARITY) ---
  const grouped = locations.reduce((acc, loc) => {
    const type = loc.type as LocationType;
    if (['CONTINENT', 'COUNTRY', 'PROVINCE'].includes(type)) acc.macro.push(loc);
    else if (['DISTRICT', 'CITY'].includes(type)) acc.urban.push(loc);
    else if (['SUBDIVISION_L1', 'SUBDIVISION_L2'].includes(type)) acc.neighborhoods.push(loc);
    else acc.tactical.push(loc); // FACILITY, ZONE, SUBLOCATION
    return acc;
  }, { macro: [], urban: [], neighborhoods: [], tactical: [] } as Record<string, LocationResponse[]>);

  const renderSection = (title: string, items: LocationResponse[], icon: React.ReactNode, color: string, subtitle: string) => {
    if (items.length === 0) return null;
    return (
      <div className="mb-10 animate-in fade-in slide-in-from-left-4 duration-500">
        <div className="flex flex-col mb-4 px-4 border-l-2" style={{ borderColor: color }}>
          <div className="flex items-center gap-3">
            <div style={{ color: color }}>{icon}</div>
            <h2 className="text-sm font-black uppercase tracking-[0.2em] text-white italic">{title}</h2>
            <span className="text-[10px] text-slate-600 font-mono">COUNT::{items.length}</span>
          </div>
          <p className="text-[8px] text-slate-500 uppercase tracking-widest mt-1 italic">{subtitle}</p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {items.map(loc => <IntelligenceCard key={loc.externalId} location={loc} color={color} />)}
        </div>
      </div>
    );
  };

  return (
    <div className="h-full bg-brand-dark flex flex-col overflow-y-auto custom-scrollbar font-mono text-slate-300">
      
      {/* TOP STATUS BAR */}
      <div className="w-full border-b border-brand-border/30 bg-black/60 p-4 flex justify-between items-center backdrop-blur-md sticky top-0 z-50">
        <div className="flex items-center gap-4">
          <Shield className="text-brand-accent animate-pulse" size={18} />
          <span className="text-[10px] tracking-[0.4em] font-black uppercase">Vanguard_Geo_Intelligence</span>
        </div>
        <div className="flex items-center gap-4 text-[9px] text-slate-500">
          <span className="text-green-500 italic font-bold">● SYSTEM_UPLINK_STABLE</span>
          <span className="opacity-20">|</span>
          <span className="uppercase tracking-widest text-brand-accent/60">Encrypted_Session</span>
        </div>
      </div>

      <div className={`max-w-7xl mx-auto w-full px-8 transition-all duration-700 ${hasSearched ? 'pt-8' : 'pt-32'}`}>
        
        {/* TERMINAL SEARCH HEADER */}
        <div className="mb-12">
          {!hasSearched && (
            <div className="mb-12 animate-in fade-in zoom-in duration-1000">
              <h1 className="text-8xl font-black text-white italic tracking-tighter uppercase leading-[0.8]">
                Sector_<br/><span className="text-brand-accent font-light">Surveillance</span>
              </h1>
              <p className="mt-4 text-slate-600 text-[10px] tracking-[0.8em] uppercase italic opacity-50">Automated_Geospatial_Registry // Core_Access_Only</p>
            </div>
          )}

          <form onSubmit={handleSearch} className="space-y-4">
            <div className="relative group">
              <div className={`absolute left-6 top-1/2 -translate-y-1/2 transition-colors duration-300 ${isTooShort ? 'text-red-500' : 'text-brand-accent'} opacity-50`}>
                <Search size={32} />
              </div>
              <input 
                type="text"
                placeholder="ENTER_LOCATION_NAME_OR_CODE_FOR_DEEP_SCAN..."
                className={`w-full bg-brand-panel/5 border transition-all duration-300 p-8 pl-24 text-3xl font-black italic text-white focus:outline-none placeholder:text-slate-900 uppercase
                  ${isTooShort ? 'border-red-600 shadow-[0_0_20px_rgba(220,38,38,0.15)]' : 'border-brand-border focus:border-brand-accent'}`}
                value={filters.query}
                onChange={(e) => setFilters(prev => ({ ...prev, query: e.target.value }))}
              />
              <button 
                type="submit"
                disabled={isLoading}
                className={`absolute right-4 top-1/2 -translate-y-1/2 px-10 py-5 font-black italic uppercase transition-all active:scale-95 shadow-lg
                  ${isTooShort ? 'bg-red-600 text-white' : 'bg-brand-accent text-black hover:bg-white'}`}
              >
                {isTooShort ? 'INSUFFICIENT_DATA' : 'Execute_Scan'}
              </button>
            </div>

            {/* ERROR NOTIFICATION */}
            {isTooShort && (
              <div className="flex items-center gap-2 text-red-500 animate-in slide-in-from-top-2 ml-4">
                <AlertTriangle size={14} className="animate-pulse" />
                <span className="text-[10px] font-black uppercase tracking-[0.2em]">
                  Validation_Error: Search_Query_Must_Exceed_2_Characters
                </span>
              </div>
            )}

            {/* FILTERS HUD */}
            <div className="grid grid-cols-2 md:grid-cols-5 gap-2 pt-2">
              <div className="border border-brand-border bg-black/40 group focus-within:border-brand-accent">
                <div className="px-3 pt-2 text-[8px] text-slate-600 uppercase font-black italic">Classification</div>
                <select 
                  className="w-full bg-transparent p-3 pt-1 text-[11px] text-white uppercase focus:outline-none cursor-pointer font-black italic"
                  value={filters.type}
                  onChange={(e) => setFilters(f => ({ ...f, type: e.target.value as any }))}
                >
                  <option value="" className="bg-brand-dark">ALL_LEVELS [1-10]</option>
                  <optgroup label="MACRO" className="bg-brand-dark">
                    <option value="CONTINENT">CONTINENT</option>
                    <option value="COUNTRY">COUNTRY</option>
                    <option value="PROVINCE">PROVINCE</option>
                  </optgroup>
                  <optgroup label="URBAN" className="bg-brand-dark">
                    <option value="CITY">CITY</option>
                    <option value="SUBDIVISION_L1">BOROUGH</option>
                  </optgroup>
                  <optgroup label="TACTICAL" className="bg-brand-dark">
                    <option value="FACILITY">FACILITY</option>
                    <option value="ZONE">ZONE</option>
                  </optgroup>
                </select>
              </div>

              <div className="md:col-span-3 h-full border border-brand-border/20 bg-black/20 flex items-center px-4 gap-4">
                <Crosshair size={14} className="text-slate-700" />
                <input 
                  type="text"
                  placeholder="FILTER_BY_PARENT_ID (UUID_NEXUS)"
                  className="bg-transparent w-full text-[10px] text-brand-accent focus:outline-none placeholder:text-slate-800 uppercase"
                  value={filters.parentId}
                  onChange={(e) => setFilters(prev => ({ ...prev, parentId: e.target.value }))}
                />
              </div>

              {hasSearched && (
                <button 
                  type="button"
                  onClick={() => { setHasSearched(false); setFilters({ query: '', type: '', parentId: '' }); }}
                  className="bg-red-950/20 border border-red-900/40 text-red-500 text-[10px] uppercase font-black tracking-widest hover:bg-red-500 hover:text-black transition-all"
                >
                  Clear_Buffer
                </button>
              )}
            </div>
          </form>
        </div>

        {/* DATA FEED RESULTS */}
        {hasSearched && (
          <div className="pb-20">
            <div className="flex items-center justify-between border-b border-brand-border/20 pb-4 mb-10">
              <div className="flex items-center gap-3">
                <Activity size={18} className="text-brand-accent animate-pulse" />
                <span className="text-[10px] uppercase font-black italic tracking-[0.3em] text-white">
                  Identified_Nodes: <span className="text-brand-accent">{locations.length}</span>
                </span>
              </div>
              <div className="text-[8px] text-slate-700 font-mono tracking-widest uppercase">
                Filter_Applied: {filters.type || "NONE"}
              </div>
            </div>

            {isLoading ? (
              <div className="py-24 flex flex-col items-center gap-6">
                <Loader2 className="animate-spin text-brand-accent" size={48} />
                <span className="text-[10px] tracking-[1em] text-slate-500 uppercase animate-pulse">Scanning_Registry...</span>
              </div>
            ) : isError ? (
              <div className="py-20 text-center border border-red-900/20 bg-red-950/5 text-red-500">
                <p className="text-xs font-black uppercase tracking-[0.5em]">Critical_Uplink_Failure: Server_Unresponsive</p>
              </div>
            ) : (
              <>
                {renderSection("Geopolitical_Hierarchy", grouped.macro, <Globe size={16} />, "#3b82f6", "Levels_1-3: Global and State Governance")}
                {renderSection("Municipal_Sectors", grouped.urban, <Landmark size={16} />, "#fbbf24", "Levels_4-5: Administrative Units and Cities")}
                {renderSection("Urban_Subdivisions", grouped.neighborhoods, <Home size={16} />, "#a855f7", "Levels_6-7: Boroughs and Neighborhoods")}
                {renderSection("Tactical_Infiltration_Points", grouped.tactical, <Target size={16} />, "#ef4444", "Levels_8-10: Facilities, Zones and Sublocations")}

                {locations.length === 0 && (
                  <div className="py-32 text-center border border-dashed border-brand-border/10">
                    <p className="text-slate-700 text-xs uppercase tracking-[0.6em] italic italic">Zero_Matches: No_Coordinates_Found</p>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

const IntelligenceCard = ({ location, color }: { location: LocationResponse, color: string }) => (
  <Link 
    to={`/locations/${location.externalId}`}
    className="group relative bg-brand-panel/5 border border-brand-border/30 p-5 hover:bg-brand-accent/[0.03] transition-all overflow-hidden"
    style={{ borderLeft: `2px solid ${color}66` }}
  >
    {/* Background Level Glow */}
    <div className="absolute top-0 right-0 p-1 opacity-5 group-hover:opacity-20 transition-opacity pointer-events-none">
       <MapPin size={60} style={{ color }} />
    </div>

    <div className="mb-4 relative z-10">
      <div className="flex justify-between items-start mb-1">
        <span className="text-[8px] font-black uppercase tracking-widest" style={{ color }}>
          {location.type.replace('_', ' ')}
        </span>
        <span className="text-[8px] text-slate-700 font-mono">
          ID::{location.externalId.substring(0, 8)}
        </span>
      </div>
      <h3 className="text-xl font-black text-white italic uppercase group-hover:text-brand-accent transition-colors truncate tracking-tighter">
        {location.name}
      </h3>
    </div>

    <div className="space-y-2 pt-3 border-t border-brand-border/10 relative z-10">
      <div className="flex items-center justify-between text-[9px] uppercase tracking-tighter">
        <span className="text-slate-600 italic">Parent_Nexus:</span>
        <span className="text-slate-400 font-bold truncate max-w-[150px]">{location.parentName || 'DATABASE_ROOT'}</span>
      </div>
      <div className="flex items-center justify-between text-[9px] uppercase tracking-tighter">
        <span className="text-slate-600 italic">Iso_Ref:</span>
        <span className="text-slate-500">{location.isoCode || '---'}</span>
      </div>
    </div>

    <div className="mt-4 flex items-center justify-between relative z-10">
      <div className="flex items-center gap-1.5">
        <div className={`w-1 h-1 rounded-full ${location.hasChildren ? 'animate-ping' : ''}`} style={{ backgroundColor: color }} />
        <span className="text-[7px] font-black uppercase tracking-[0.2em] text-slate-500">
          {location.hasChildren ? 'Downlink_Available' : 'Leaf_Terminal'}
        </span>
      </div>
      <ChevronRight size={14} className="text-slate-800 group-hover:text-brand-accent group-hover:translate-x-1 transition-all" />
    </div>
  </Link>
);

export default LocationPage;