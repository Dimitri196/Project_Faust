import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { 
  Search, ChevronRight, Loader2, Shield, 
  Activity, Crosshair, MapPin, Globe, 
  Layers, Target, Landmark, Home, AlertTriangle,
  RefreshCcw, ArrowLeft, Terminal, Cpu, X
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import type { LocationResponse, LocationType, Page } from '../types';

interface LocationFilter {
  query: string;
  type: LocationType | '';
  parentId: string;
  rootOnly: boolean;
}

const LocationPage = () => {
  const navigate = useNavigate();
  const [hasSearched, setHasSearched] = useState(false);
  const [isTooShort, setIsTooShort] = useState(false);
  
  const [filters, setFilters] = useState<LocationFilter>({
    query: '',
    type: '',
    parentId: '',
    rootOnly: true,
  });

  const { data, isLoading, isError, refetch } = useQuery<Page<LocationResponse>>({
    queryKey: ['locations-search', filters.parentId, hasSearched, filters.query === ''],
    queryFn: async () => {
      const res = await axios.get('/api/v1/locations/search', {
        params: { 
          query: filters.query || undefined, 
          type: filters.type || undefined,
          parentId: filters.parentId || undefined,
          rootOnly: filters.parentId ? false : filters.rootOnly,
          page: 0,
          size: 100 
        }
      });
      return res.data;
    },
    enabled: true, 
  });

  const locations = data?.content || [];

  // --- MOSSAD RESET LOGIC ---
  const clearAll = () => {
    setFilters({ query: '', type: '', parentId: '', rootOnly: true });
    setHasSearched(false);
    setIsTooShort(false);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (filters.query.trim().length > 0 && filters.query.trim().length < 3) {
      setIsTooShort(true);
      setTimeout(() => setIsTooShort(false), 3000);
      return;
    }
    setHasSearched(true);
    setFilters(prev => ({ ...prev, rootOnly: !prev.query }));
    refetch();
  };

  const enterSector = (loc: LocationResponse) => {
    if (loc.hasChildren) {
      setFilters({ query: '', type: '', parentId: loc.externalId, rootOnly: false });
      setHasSearched(true);
    } else {
      navigate(`/locations/${loc.externalId}`);
    }
  };

  return (
    <div className="h-full bg-[#05070a] flex flex-col font-mono text-slate-400 selection:bg-brand-accent selection:text-black">
      
      {/* 1. TOP MINIMALIST STATUS BAR */}
      <div className="w-full border-b border-white/5 bg-black/40 px-4 py-2 flex justify-between items-center backdrop-blur-md sticky top-0 z-50">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2 border-r border-white/10 pr-6">
            <Terminal size={14} className="text-brand-accent" />
            <span className="text-[10px] tracking-widest font-bold text-slate-200 uppercase">Module::Geo_Registry</span>
          </div>
          <div className="flex items-center gap-2 text-[9px] uppercase tracking-tighter opacity-50">
            <Cpu size={12} />
            <span>Node_Status: <span className="text-green-500">Optimal</span></span>
          </div>
        </div>
        <div className="flex items-center gap-4 text-[9px]">
          {(filters.parentId || hasSearched) && (
            <button onClick={clearAll} className="text-brand-accent flex items-center gap-1 hover:text-white transition-colors uppercase tracking-tighter">
              <ArrowLeft size={10} /> [ESC]_RESET_TO_ROOT
            </button>
          )}
          <span className="opacity-20">|</span>
          <span className="text-slate-500 italic uppercase">Sigint_Encrypted</span>
        </div>
      </div>

      <div className="max-w-[1600px] mx-auto w-full px-6 pt-6 flex-1 flex flex-col">
        
        {/* 2. COMPACT SEARCH COMMAND CENTER */}
        <div className="mb-6 bg-white/[0.02] border border-white/5 p-4 rounded-sm shadow-2xl">
          <form onSubmit={handleSearch} className="flex flex-col gap-3">
            <div className="flex items-center gap-4 relative">
              <Search size={18} className={`${filters.query ? 'text-brand-accent' : 'text-slate-700'} transition-colors opacity-50`} />
              <input 
                type="text"
                autoComplete="off"
                spellCheck="false"
                placeholder="INITIATE_DEEP_SCAN_BY_QUERY_OR_COORDINATES..."
                className="flex-1 bg-transparent border-none py-2 text-sm font-bold text-white focus:outline-none placeholder:text-slate-900 uppercase tracking-wider"
                value={filters.query}
                onKeyDown={(e) => e.key === 'Escape' && clearAll()}
                onChange={(e) => {
                  const val = e.target.value;
                  setFilters(prev => ({ ...prev, query: val }));
                  if (val === '') {
                    setHasSearched(false);
                    setFilters(prev => ({ ...prev, rootOnly: true, parentId: '' }));
                  }
                }}
              />
              
              <div className="flex items-center gap-3">
                {filters.query && (
                  <button 
                    type="button" 
                    onClick={clearAll}
                    className="flex items-center gap-1 text-[9px] text-slate-600 hover:text-red-500 transition-colors uppercase font-bold border-r border-white/10 pr-3"
                  >
                    <X size={12} /> Clear_Buffer
                  </button>
                )}
                <span className="text-[9px] text-slate-700 mr-2 font-mono">SYS_VER::4.8.2</span>
                <button 
                  type="submit"
                  className="bg-brand-accent/10 border border-brand-accent/30 text-brand-accent px-4 py-1.5 text-[10px] font-black uppercase tracking-widest hover:bg-brand-accent hover:text-black transition-all active:scale-95"
                >
                  {isLoading ? "Executing..." : "Execute_Scan"}
                </button>
              </div>
            </div>

            {/* LOWER HUD FILTERS */}
            <div className="flex items-center gap-2 border-t border-white/5 pt-3">
              <div className="flex items-center bg-black/40 border border-white/10 px-2 py-1">
                <span className="text-[8px] text-slate-600 uppercase mr-2 font-bold tracking-tighter">Classification:</span>
                <select 
                  className="bg-transparent text-[9px] text-brand-accent uppercase outline-none font-bold cursor-pointer"
                  value={filters.type}
                  onChange={(e) => setFilters(f => ({ ...f, type: e.target.value as any }))}
                >
                  <option value="">ALL_LEVELS</option>
                  <option value="CONTINENT">CONTINENT</option>
                  <option value="COUNTRY">COUNTRY</option>
                  <option value="PROVINCE">PROVINCE</option>
                  <option value="CITY">CITY</option>
                </select>
              </div>
              <div className="flex-1 flex items-center bg-black/40 border border-white/10 px-3 py-1 gap-3">
                <Crosshair size={12} className="text-slate-700" />
                <span className="text-[8px] text-slate-600 uppercase font-bold tracking-tighter">NexusID:</span>
                <input 
                  type="text"
                  placeholder="AUTO_DETECTED_ROOT"
                  className="bg-transparent flex-1 text-[9px] text-slate-500 outline-none uppercase font-mono tracking-tighter"
                  value={filters.parentId}
                  onChange={(e) => setFilters(prev => ({ ...prev, parentId: e.target.value }))}
                />
              </div>
            </div>
          </form>
        </div>

        {/* 3. DATA GRID FEED */}
        <div className="flex-1 overflow-y-auto custom-scrollbar pr-2">
          <div className="flex items-center justify-between mb-4 border-b border-white/5 pb-2">
            <div className="flex items-center gap-2">
              <Activity size={12} className="text-brand-accent animate-pulse" />
              <span className="text-[9px] uppercase font-bold tracking-[0.2em] text-slate-500">
                Data_Feed: <span className="text-white">{locations.length}</span> Records_Identified
              </span>
            </div>
            <div className="text-[7px] text-slate-700 font-mono">
              {filters.parentId ? `PARENT_LINK_ACTIVE::${filters.parentId}` : "MODE::GLOBAL_SURVEILLANCE"}
            </div>
          </div>

          {isLoading ? (
            <div className="h-64 flex flex-col items-center justify-center gap-4 border border-white/5 bg-white/[0.01]">
              <Loader2 className="animate-spin text-brand-accent/40" size={32} />
              <span className="text-[8px] tracking-[0.5em] uppercase opacity-40 italic">Decrypting_Registry_Sub_Nodes...</span>
            </div>
          ) : isError ? (
            <div className="h-64 flex flex-col items-center justify-center gap-4 border border-red-900/20 bg-red-950/5">
              <AlertTriangle className="text-red-500" size={32} />
              <span className="text-[8px] tracking-[0.3em] uppercase text-red-500 font-black">Critical_Uplink_Failure: Server_Timeout</span>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3 pb-10">
                {locations.map(loc => (
                  <TerminalCard 
                    key={loc.externalId} 
                    location={loc} 
                    onEnter={() => enterSector(loc)}
                  />
                ))}
              </div>
              
              {locations.length === 0 && (
                <div className="py-20 text-center border border-dashed border-white/5 rounded-sm">
                  <p className="text-[10px] text-slate-700 uppercase tracking-[0.5em]">Zero_Matches: No_Coordinates_In_Buffer</p>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

// 4. AGENT_STYLE COMPACT CARD
const TerminalCard = ({ location, onEnter }: { location: LocationResponse, onEnter: () => void }) => (
  <div 
    onClick={onEnter}
    className="group bg-white/[0.02] border border-white/10 p-3 hover:border-brand-accent/50 hover:bg-brand-accent/[0.02] transition-all cursor-pointer relative overflow-hidden"
  >
    {/* Background scanline effect */}
    <div className="absolute top-0 left-0 w-full h-[1px] bg-brand-accent/5 translate-y-[-100%] group-hover:translate-y-[3000%] transition-all duration-[2s] pointer-events-none" />
    
    <div className="flex justify-between items-start mb-2">
      <div className="bg-brand-accent/10 text-brand-accent text-[7px] px-1 font-black uppercase tracking-tighter border border-brand-accent/20">
        {location.type.replace('_', ' ')}
      </div>
      <div className="text-[7px] text-slate-700 font-mono tracking-tighter">0x{location.externalId.substring(0, 6)}</div>
    </div>
    
    <h3 className="text-xs font-bold text-slate-200 uppercase group-hover:text-brand-accent transition-colors truncate mb-3 tracking-wide">
      {location.name}
    </h3>

    <div className="flex items-center justify-between border-t border-white/5 pt-2 mt-auto">
      <div className="flex items-center gap-1.5">
        <div className={`w-1 h-1 rounded-full ${location.hasChildren ? 'bg-brand-accent animate-pulse shadow-[0_0_5px_rgba(var(--brand-accent-rgb),0.5)]' : 'bg-slate-800'}`} />
        <span className="text-[7px] uppercase text-slate-600 font-black tracking-widest">
          {location.hasChildren ? "Downlink" : "Leaf"}
        </span>
      </div>
      <ChevronRight size={10} className="text-slate-800 group-hover:text-brand-accent group-hover:translate-x-0.5 transition-all" />
    </div>
  </div>
);

export default LocationPage;