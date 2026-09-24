import React, { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Search, Shield, Fingerprint, Building2,
  X, Activity, Command, Zap, Cpu, Briefcase, Share2, MapPin, AlertTriangle
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
// CHANGED: was `import axios from 'axios'`. Same root cause as the Location
// pages — raw axios has no Authorization interceptor. Now that
// /api/v1/search/global requires hasRole('VIEWER'), every search would
// return 403 without this.
import api from '../api/axios';
import type { GlobalSearchResponse, SearchCategory } from '../types';

const GlobalSearchTerminal = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [isHudActive, setIsHudActive] = useState(false);
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedQuery(searchTerm), 400);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  useEffect(() => {
    if (debouncedQuery.length >= 3) {
      setIsHudActive(true);
    } else if (debouncedQuery.length === 0) {
      setIsHudActive(false);
    }
  }, [debouncedQuery]);

  // CHANGED: useQuery<SearchResultDTO[]> -> useQuery<GlobalSearchResponse[]>.
  // GlobalSearchResponse now lives in types/index.ts, matching the backend
  // record exactly (id, displayName, category, subLabel, rankScore) rather
  // than a locally-duplicated, narrower-typed interface.
  const { data: results, isFetching, isError } = useQuery<GlobalSearchResponse[]>({
    queryKey: ['global-search', debouncedQuery],
    queryFn: async () => {
      if (!debouncedQuery || debouncedQuery.length < 3) return [];
      // CHANGED: axios -> api, '/api/v1/search/global' -> '/search/global'
      const response = await api.get('/search/global', {
        params: { query: debouncedQuery, vector: 'ALL' }
      });
      return response.data;
    },
    enabled: debouncedQuery.length >= 3,
  });

  const handleClose = () => {
    setSearchTerm('');
    setIsHudActive(false);
  };

  // CHANGED: added LOCATION case. Previously, clicking a LOCATION result
  // (which global_search_view almost certainly returns, since locations
  // have a search_vector like every other entity) fell through this
  // if/else chain silently — no navigation occurred at all.
  const handleNavigate = (res: GlobalSearchResponse) => {
    switch (res.category as SearchCategory) {
      case 'PERSON':
        navigate(`/personnel/${res.id}`);
        break;
      case 'INSTITUTION':
        navigate(`/institutions/${res.id}`);
        break;
      case 'OCCUPATION':
        navigate(`/occupations/${res.id}`);
        break;
      case 'LOCATION':
        navigate(`/locations/${res.id}`);
        break;
      default:
        // Unknown category from backend — log for diagnostics rather than
        // silently doing nothing.
        console.warn(`FAUST_SEARCH: Unhandled result category "${res.category}" for id ${res.id}`);
    }
  };

  // CHANGED: added LOCATION icon (MapPin), matching the new category.
  const getCategoryIcon = (category: string) => {
    switch (category) {
      case 'PERSON': return <Fingerprint size={18} />;
      case 'INSTITUTION': return <Building2 size={18} />;
      case 'OCCUPATION': return <Briefcase size={18} />;
      case 'LOCATION': return <MapPin size={18} />;
      default: return <Zap size={18} />;
    }
  };

  return (
    <div className="h-full bg-[#020305] text-cyan-500 font-mono flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute inset-0 pointer-events-none z-[70] opacity-[0.03] bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] bg-[length:100%_2px,3px_100%]" />

      {/* GHOST TERMINAL */}
      <div className={`w-full max-w-xl transition-all duration-1000 transform ${isHudActive ? 'opacity-0 scale-150 blur-3xl pointer-events-none' : 'opacity-100 scale-100'}`}>
        <div className="flex flex-col items-center text-center space-y-6">
          <div className="p-6 border border-cyan-500/20 bg-cyan-500/5 rounded-full animate-pulse">
            <Shield size={64} strokeWidth={1} />
          </div>
          <div className="space-y-2">
            <h1 className="text-4xl font-black uppercase tracking-[0.6em] text-white">MEPHISTO_OS</h1>
            <p className="text-[10px] text-cyan-500/40 tracking-[0.4em]">CENTRAL_INTELLIGENCE_UPLINK</p>
          </div>
          <div className="w-full relative group max-w-md">
            <input
              autoFocus
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="IDENTIFY_SUBJECT..."
              className="relative w-full bg-black border border-cyan-500/40 py-6 px-8 text-xl text-white outline-none uppercase tracking-widest text-center placeholder:transition-opacity placeholder:duration-300 focus:placeholder:opacity-0"
            />
          </div>
        </div>
      </div>

      {/* HUD OVERLAY */}
      {isHudActive && (
        <div className="absolute inset-0 z-50 flex items-center justify-center p-2 md:p-8 animate-in fade-in zoom-in duration-500">
          <div className="absolute inset-0 bg-black/95 backdrop-blur-3xl" />
          <div className="relative w-full max-w-7xl h-[90vh] flex flex-col border border-cyan-500/40 bg-[#050608] shadow-[0_0_100px_rgba(6,182,212,0.1)]">

            <div className="flex-1 overflow-y-auto custom-scrollbar relative">
              <table className="w-full text-left border-collapse">
                <thead className="sticky top-0 bg-[#050608] z-20 border-b border-cyan-500/30">
                  <tr className="text-[9px] text-cyan-500/60 uppercase tracking-[0.3em] font-black bg-cyan-500/5">
                    <th className="px-8 py-4 w-16">Class</th>
                    <th className="px-8 py-4">Designation</th>
                    <th className="px-8 py-4">Sector</th>
                    <th className="px-8 py-4 text-right pr-20">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-cyan-500/10">
                  {results?.map((res) => (
                    <tr key={res.id} className="group hover:bg-cyan-500/5 transition-all cursor-pointer" onClick={() => handleNavigate(res)}>
                      <td className="px-8 py-5 text-cyan-500/30 group-hover:text-cyan-500">{getCategoryIcon(res.category)}</td>
                      <td className="px-8 py-5">
                        <div className="text-lg font-black text-white group-hover:text-cyan-400 transition-colors uppercase italic tracking-tighter">{res.displayName}</div>
                        <div className="text-[9px] text-slate-600 font-mono tracking-widest italic uppercase">{res.id}</div>
                      </td>
                      <td className="px-8 py-5 text-[10px] uppercase tracking-widest text-cyan-500/50">{res.category} // {res.subLabel || 'ROOT'}</td>
                      <td className="px-8 py-5 text-right pr-8">
                        <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button onClick={(e) => { e.stopPropagation(); handleNavigate(res); }} className="px-3 py-1 border border-cyan-500/30 text-[10px] text-cyan-500 hover:bg-cyan-500 hover:text-black transition-all uppercase">
                            Dossier
                          </button>
                          {res.category === 'PERSON' && (
                            <button onClick={(e) => { e.stopPropagation(); navigate(`/intelligence/${res.id}`); }} className="px-3 py-1 border border-blue-500 bg-blue-500/10 text-blue-400 text-[10px] hover:bg-blue-500 hover:text-white transition-all font-black flex items-center gap-2">
                              <Share2 size={10} /> NETWORK_SCAN
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* NEW: feedback states. Previously, an error response or a
                  query with zero matches both rendered as a table with
                  only headers — indistinguishable from a broken page. */}
              {isFetching && (
                <div className="flex flex-col items-center justify-center py-24 gap-4 text-cyan-500/40">
                  <Cpu className="animate-spin" size={32} />
                  <span className="text-[10px] uppercase tracking-[0.4em] animate-pulse">Querying_Central_Registry...</span>
                </div>
              )}

              {!isFetching && isError && (
                <div className="flex flex-col items-center justify-center py-24 gap-4 text-red-500/70">
                  <AlertTriangle size={32} />
                  <span className="text-[10px] uppercase tracking-[0.4em]">Search_Uplink_Failed // Insufficient_Clearance_Or_Server_Error</span>
                </div>
              )}

              {!isFetching && !isError && results && results.length === 0 && (
                <div className="flex flex-col items-center justify-center py-24 gap-4 text-cyan-500/30">
                  <Search size={32} />
                  <span className="text-[10px] uppercase tracking-[0.4em]">Zero_Matches // No_Records_Identified</span>
                </div>
              )}
            </div>

            {/* BOTTOM CONTROL PANEL */}
            <div className="bg-cyan-500/5 border-t border-cyan-500/40 p-6 flex flex-col gap-4">
              <div className="flex items-center gap-6">
                <Command size={24} className="text-cyan-500 animate-pulse" />
                <input
                  autoFocus
                  ref={inputRef}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="flex-1 bg-transparent border-b-2 border-cyan-500/20 focus:border-cyan-500 py-3 text-2xl text-white outline-none uppercase tracking-[0.3em]"
                  placeholder="REIDENTIFY_SUBJECT..."
                />
                <button onClick={handleClose} className="px-6 py-3 border border-red-500/50 text-red-500 hover:bg-red-500 hover:text-white transition-all text-[10px] font-black tracking-widest">TERMINATE_SESSION</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default GlobalSearchTerminal;