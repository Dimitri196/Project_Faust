import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { 
  ChevronLeft, Building2, Search, Database, 
  Navigation2, Layers, ShieldAlert, ArrowDownRight,
  ChevronRight, Target, Globe, Map as MapIcon
} from 'lucide-react';
import axios from 'axios';
import type { LocationResponse, InstitutionResponse } from '../types';

const LocationDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // 1. Fetch Current Location Detail & Ancestry Path (Upwards)
const { data: location, isLoading: isLocLoading, isError: isLocError } = useQuery({
  queryKey: ['location', id],
  queryFn: async () => {
    // 1. Získáme základní detail lokace
    const detailRes = await axios.get<LocationResponse>(`/api/v1/locations/${id}`);
    
    // 2. Získáme cestu (ancestry) přes tvůj dedikovaný endpoint
    const pathRes = await axios.get<LocationResponse[]>(`/api/v1/locations/${id}/path`);
    
    // 3. Vrátíme to v jednom objektu, aby Hierarchy_Ancestry měla data
    return {
      ...detailRes.data,
      path: pathRes.data
    };
  },
  enabled: !!id
});

  // 2. Fetch Sub-Sectors (Downwards: Kraje -> Okrsky -> Cities)
  const { data: subSectors, isLoading: isSubLoading } = useQuery<LocationResponse[]>({
    queryKey: ['location-subs', id],
    queryFn: async () => {
      const res = await axios.get(`/api/v1/locations/${id}/sub-locations`);
      return res.data;
    },
    enabled: !!id
  });

  // 3. Fetch Institutions bound to this specific node
  const { data: institutions, isLoading: isInstLoading } = useQuery<InstitutionResponse[]>({
    queryKey: ['location-institutions', id],
    queryFn: async () => {
      const res = await axios.get('/api/v1/institutions/search', {
        params: { locationId: id }
      });
      return res.data;
    },
    enabled: !!id
  });

  if (isLocLoading) return <LoadingState />;
  if (isLocError || !location) return <ErrorState message="GEOSPATIAL_COORDINATES_INVALID" />;

  return (
    <div className="h-full bg-brand-dark text-slate-200 flex flex-col animate-in fade-in duration-500">
      
      {/* HEADER: Tactical Context */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20 relative overflow-hidden">
        <div className="absolute inset-0 opacity-5 pointer-events-none">
            <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:40px_40px]" />
        </div>
        
        <div className="max-w-7xl mx-auto relative z-10">
          <button 
            onClick={() => navigate(-1)} 
            className="flex items-center gap-2 text-brand-accent/60 hover:text-brand-accent transition-colors mb-6 font-mono text-[10px] uppercase tracking-widest group"
          >
            <ChevronLeft size={14} className="group-hover:-translate-x-1 transition-transform" /> Back_to_Registry
          </button>
          
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
            <div>
              <div className="flex items-center gap-3 mb-3">
                <Target size={20} className="text-brand-accent animate-pulse" />
                <span className="px-2 py-0.5 bg-brand-accent/10 border border-brand-accent/30 text-[9px] font-mono text-brand-accent uppercase tracking-[0.2em]">
                  {location.type} // {location.isoCode || 'DOMESTIC_SECTOR'}
                </span>
              </div>
              <h1 className="text-5xl font-black text-white italic tracking-tighter uppercase leading-none">
                {location.name}
              </h1>
            </div>
            
            <div className="text-right font-mono">
              <div className="text-[10px] text-slate-500 uppercase mb-1 tracking-widest">Sector_UUID</div>
              <div className="text-xs text-brand-accent font-bold tracking-tighter uppercase">{location.externalId}</div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* LEFT COLUMN: HIERARCHY EXPLORER */}
          <div className="lg:col-span-4 space-y-6">
            
            {/* 1. Ancestry (The "Where am I?" Path) */}
            <section className="bg-brand-panel/10 border border-brand-border p-6 backdrop-blur-sm">
              <h3 className="text-[10px] font-mono font-black text-slate-500 uppercase tracking-[0.3em] mb-6 flex items-center gap-2">
                <Layers size={14} className="text-brand-accent" /> Hierarchy_Ancestry
              </h3>
              <div className="bg-black/20 p-4 border border-brand-border/30 rounded-sm">
                 <LocationHierarchy path={location.path} currentId={location.externalId} />
              </div>
            </section>

            {/* 2. Sub-Sectors (The "Drill Down" Path) */}
            <section className="bg-brand-panel/10 border border-brand-border p-6 backdrop-blur-sm">
              <h3 className="text-[10px] font-mono font-black text-brand-accent uppercase tracking-[0.3em] mb-6 flex items-center gap-2">
                <ArrowDownRight size={14} /> Sub_Sectors_Detected
              </h3>
              <div className="space-y-2 max-h-[400px] overflow-y-auto custom-scrollbar pr-2">
                {isSubLoading ? (
                   <div className="animate-pulse space-y-2">
                      {[1,2,3].map(i => <div key={i} className="h-10 bg-white/5 border border-brand-border/20" />)}
                   </div>
                ) : subSectors && subSectors.length > 0 ? (
                  subSectors.map((sub) => (
                    <Link 
                      key={sub.externalId}
                      to={`/locations/${sub.externalId}`}
                      className="flex items-center justify-between p-3 bg-black/40 border border-brand-border/50 hover:border-brand-accent/50 group transition-all"
                    >
                      <div className="flex flex-col">
                        <span className="text-[11px] font-mono text-slate-300 group-hover:text-white uppercase">{sub.name}</span>
                        <span className="text-[8px] font-mono text-slate-600 uppercase tracking-tighter">{sub.type}</span>
                      </div>
                      <ChevronRight size={12} className="text-slate-700 group-hover:text-brand-accent group-hover:translate-x-1 transition-all" />
                    </Link>
                  ))
                ) : (
                  <div className="py-8 text-center border border-dashed border-brand-border/20">
                    <span className="text-[9px] font-mono text-slate-600 uppercase tracking-widest italic">Terminal_Node // No_Subsectors</span>
                  </div>
                )}
              </div>
            </section>
          </div>

          {/* RIGHT COLUMN: INSTITUTIONAL NODES */}
          <div className="lg:col-span-8">
            <section className="bg-brand-panel/10 border border-brand-border p-6 min-h-full backdrop-blur-sm">
              <div className="flex items-center justify-between mb-8 border-b border-brand-border/30 pb-4">
                <h3 className="text-[10px] font-mono font-black text-slate-500 uppercase tracking-[0.4em] flex items-center gap-2">
                  <Database size={16} className="text-brand-accent" /> Bound_Institutional_Nodes
                </h3>
                <div className="text-[10px] font-mono text-slate-500 uppercase">
                    Detected: <span className="text-brand-accent font-bold">{institutions?.length || 0}</span>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {isInstLoading ? (
                    <div className="col-span-2 space-y-4 animate-pulse">
                        {[1, 2].map(i => <div key={i} className="h-20 bg-white/5 border border-brand-border/30" />)}
                    </div>
                ) : institutions && institutions.length > 0 ? (
                  institutions.map((inst) => (
                    <Link 
                        key={inst.publicId} 
                        to={`/institutions/${inst.publicId}`}
                        className="group p-4 bg-black/40 border border-brand-border/50 hover:border-brand-accent/50 hover:bg-brand-accent/[0.02] transition-all relative overflow-hidden"
                    >
                        <div className="flex items-start gap-4">
                            <div className="p-2 bg-brand-panel/50 border border-brand-border group-hover:border-brand-accent/30 transition-colors">
                                <Building2 className="text-slate-500 group-hover:text-brand-accent" size={20} />
                            </div>
                            <div className="flex-1 min-w-0">
                                <div className="text-sm font-black text-white uppercase group-hover:text-brand-accent transition-colors truncate">{inst.name}</div>
                                <div className="flex items-center gap-2 mt-1 font-mono text-[9px] text-slate-500 uppercase">
                                    <span>{inst.type}</span>
                                    <span className="text-brand-accent/30">//</span>
                                    <span>{inst.level}</span>
                                </div>
                            </div>
                        </div>
                    </Link>
                  ))
                ) : (
                  <div className="col-span-2 py-32 text-center border border-dashed border-brand-border/20">
                    <Search className="mx-auto text-slate-800 mb-4" size={32} />
                    <div className="text-[10px] font-mono text-slate-700 uppercase tracking-widest">
                        Zero active institutional assets found at this coordinate
                    </div>
                  </div>
                )}
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
};

// --- SUBSIDIARY COMPONENTS ---

const LocationHierarchy = ({ path, currentId }: { path?: LocationResponse[], currentId?: string }) => {
    if (!path) return null;
    return (
        <div className="flex flex-col gap-0">
            {path.map((loc, idx) => (
                <div key={loc.externalId} className="flex items-center gap-3 group/link">
                    <div className="flex flex-col items-center w-4">
                        <div className={`w-2 h-2 rounded-full z-10 border transition-all ${
                            loc.externalId === currentId 
                            ? 'bg-brand-accent border-brand-accent shadow-[0_0_10px_#38bdf8]' 
                            : 'bg-brand-dark border-slate-700'
                        }`} />
                        {idx < path.length - 1 && <div className="w-[1px] h-6 bg-brand-border/50" />}
                    </div>
                    <Link 
                        to={`/locations/${loc.externalId}`}
                        className={`text-[11px] font-mono uppercase tracking-wider py-1 hover:text-brand-accent transition-colors ${
                            loc.externalId === currentId ? 'text-white font-black' : 'text-slate-500'
                        }`}
                    >
                        {loc.name}
                        {loc.externalId === currentId && <span className="ml-2 text-[8px] text-brand-accent animate-pulse">●</span>}
                    </Link>
                </div>
            ))}
        </div>
    );
};

const ErrorState = ({ message }: { message: string }) => (
    <div className="h-full bg-brand-dark flex flex-col items-center justify-center gap-4">
      <ShieldAlert className="text-red-500 animate-pulse" size={48} />
      <div className="font-mono text-red-500 text-xs uppercase tracking-widest">{message}</div>
      <Link to="/locations" className="text-brand-accent text-[10px] font-mono border border-brand-accent/30 px-4 py-2 hover:bg-brand-accent/10 transition-colors uppercase">
        Return_to_Registry
      </Link>
    </div>
);

const LoadingState = () => (
    <div className="h-full bg-brand-dark flex flex-col items-center justify-center gap-6 font-mono">
      <div className="relative w-16 h-16">
        <div className="absolute inset-0 border-2 border-brand-accent/10 rounded-full" />
        <div className="absolute inset-0 border-t-2 border-brand-accent rounded-full animate-spin" />
      </div>
      <div className="text-[10px] text-brand-accent uppercase tracking-[0.5em] animate-pulse">Synchronizing_Geospatial_Node...</div>
    </div>
);

export default LocationDetailPage;