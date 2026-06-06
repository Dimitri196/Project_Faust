import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  Building2, Users, MapPin, Briefcase, Info, ShieldAlert, ShieldCheck, 
  Landmark, Zap, Scale, Network, Globe, ChevronRight, ListTree, 
  Fingerprint, X, Terminal, Target, Cpu, Search, History, AlertTriangle,
  Activity
} from 'lucide-react';
import api from '../api/axios';
import type {
  InstitutionResponse,
  OccupationResponse,
  InstitutionTreeResponse,
  LocationResponse // PŘIDÁNO: pro čisté typování cesty
} from '../types';
import { InstitutionFlow } from '../components/institutions/InstitutionFlow';

const InstitutionDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isNexusOpen, setIsNexusOpen] = useState(false);

  // --- DATA FETCHING ---
  const { data: institution, isLoading: isInstLoading, isError: isInstError } = useQuery<InstitutionResponse>({
    queryKey: ['institution', id],
    queryFn: async () => (await api.get(`/institutions/${id}`)).data
  });

  // ZMĚNA: deepTree se teď načítá vždy (odstraněno isNexusOpen z enabled), 
  // abychom z něj mohli brát zaniklé instituce pro boční panel
  const { data: deepTree } = useQuery<InstitutionTreeResponse>({
    queryKey: ['institution-deep-tree', id],
    queryFn: async () => (await api.get(`/institutions/${id}/sub-tree`)).data,
    enabled: !!id
  });

  const { data: immediateChildren } = useQuery<InstitutionResponse[]>({
    queryKey: ['institution-children', id],
    queryFn: async () => (await api.get(`/institutions/parent/${id}`)).data,
    enabled: !!id
  });

  const { data: occupations, isLoading: isOccLoading } = useQuery<OccupationResponse[]>({
    queryKey: ['institution-occupations', id],
    queryFn: async () => (await api.get(`/occupations/institution/${id}`)).data,
    enabled: !!id
  });

  const { data: ascendedPath } = useQuery<any>({
    queryKey: ['institution-path', id],
    queryFn: async () => (await api.get(`/institutions/${id}/path-to-root`)).data,
    enabled: !!id
  });

  // --- LOGIC: MERGE ACTIVE AND DISSOLVED ---
  const subordinates = useMemo(() => {
    const activeList = immediateChildren || [];
    
    // Pokud API nevrací zaniklou instituci, vytáhneme ji z deepTree.children
    const allFromTree = deepTree?.children?.map(c => c as unknown as InstitutionResponse) || [];
    
    // Sjednotíme seznamy, aby tam bylo všech 5 (aktivní i zaniklé)
    const combined = [...activeList];
    allFromTree.forEach(treeChild => {
      if (!combined.find(c => c.publicId === treeChild.publicId)) {
        combined.push(treeChild);
      }
    });

    return combined.sort((a, b) => a.name.localeCompare(b.name));
  }, [immediateChildren, deepTree]);

  const nexusData = useMemo(() => (deepTree ? [deepTree] : []), [deepTree]);

  if (isInstLoading) return <LoadingState />;
  if (isInstError || !institution) return <ErrorState message="CRITICAL_FAILURE: NODE_UNREACHABLE" />;

  const isActive = institution.active;
  const themeColor = isActive ? 'text-brand-accent' : 'text-orange-500';
  const themeBorder = isActive ? 'border-brand-accent/30' : 'border-orange-500/50';
  const themeBg = isActive ? 'bg-[#05080f]' : 'bg-[#140b05]';

  return (
    <div className={`h-screen bg-[#02040a] text-slate-300 flex flex-col overflow-hidden font-sans border-4 ${isActive ? 'border-[#0a0f18]' : 'border-orange-900/30'}`}>
      
      {/* STATUS BAR */}
      <div className="h-6 bg-[#0a0f18] border-b border-white/10 flex items-center px-4 justify-between text-[9px] font-mono text-slate-500 uppercase tracking-[0.2em]">
        <div className="flex gap-6">
          <span className="flex items-center gap-2">
            <Terminal size={10} className={themeColor}/> SYSTEM_CONNECTED: FAUST_v4.2
          </span>
          <span>CLEARANCE_LVL: 05</span>
        </div>
        <div className="flex gap-4">
          {isActive ? <span className="text-emerald-500/50 italic animate-pulse">● DATA_LINK_STABLE</span> : <span className="text-orange-500/70 italic animate-pulse">▲ ARCHIVAL_RECORD_ACCESS</span>}
          <span>{new Date().toISOString()}</span>
        </div>
      </div>

      {/* HEADER */}
      <header className={`h-28 ${themeBg} border-b border-white/10 flex items-center px-8 relative overflow-hidden shrink-0`}>
        <div className="flex items-center gap-8 z-10 w-full">
          <div className={`w-20 h-20 bg-white/5 border ${themeBorder} p-2 relative group overflow-hidden ${!isActive && 'grayscale opacity-60'}`}>
            {institution.logoUrl ? <img src={institution.logoUrl} alt="logo" className="w-full h-full object-contain" /> : getInstitutionIcon(institution.type, 32)}
          </div>
          
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-1">
              <span className={`${isActive ? 'bg-brand-accent text-black' : 'bg-orange-600 text-white'} text-[10px] font-black px-2 py-0.5 uppercase italic`}>
                {isActive ? institution.level : 'DISSOLVED_UNIT'}
              </span>
              <div className="flex items-center gap-2 px-2 py-0.5 border border-white/10 bg-white/5">
                {getInstitutionIcon(institution.type, 12)}
                <span className="text-white text-[9px] font-black uppercase italic tracking-widest">{institution.type}</span>
              </div>
            </div>
            <h1 className={`text-4xl font-black uppercase tracking-tighter leading-none italic ${isActive ? 'text-white' : 'text-slate-400'}`}>
              {institution.name}
            </h1>
            <div className="flex items-center gap-4 mt-2">
              <LocationPathDisplay path={institution.fullLocationPath} isLoading={isInstLoading} />
            </div>
          </div>

          <div className="flex gap-4">
            <button onClick={() => setIsNexusOpen(true)} className={`flex flex-col items-center justify-center w-28 h-20 transition-all border ${isActive ? 'bg-blue-600/5 border-blue-500/20 hover:bg-blue-600' : 'bg-slate-800/10 border-white/5 hover:bg-slate-700 text-slate-400'}`}>
              <Network size={20} className="mb-1" />
              <span className="text-[8px] font-black uppercase tracking-[0.2em] text-center">Open<br/>Visual_Nexus</span>
            </button>
            <button onClick={() => navigate('/search')} className="p-4 bg-white/5 border border-white/10 hover:border-brand-accent text-slate-400">
              <Search size={24} />
            </button>
          </div>
        </div>
      </header>

      {/* MAIN CONTENT */}
      <main className="flex-1 grid grid-cols-12 overflow-hidden bg-[#09122a]">
        <aside className="col-span-3 border-r border-white/5 bg-[#05080f] flex flex-col overflow-hidden p-6 space-y-8">
          
          <section className="flex flex-col">
            <div className="flex items-center gap-3 mb-4">
              <div className={`h-[1px] flex-1 bg-gradient-to-r ${isActive ? 'from-brand-accent/40' : 'from-orange-500/40'} to-transparent`} />
              <h3 className={`text-[10px] font-black ${themeColor} uppercase tracking-[0.3em] flex items-center gap-2`}>
                <ListTree size={12} /> Structural_Chain
              </h3>
            </div>
            
            <div className={`space-y-4 relative pl-4 border-l ${isActive ? 'border-brand-accent/20' : 'border-orange-500/20'}`}>
              <div className="relative">
                <span className="text-[8px] font-mono text-slate-600 uppercase block mb-1 italic">Immediate_Superior</span>
                <div className={`p-3 bg-black border border-white/5 border-l-2 ${isActive ? 'border-l-brand-accent' : 'border-l-orange-500'}`}>
                  {institution.parentId ? (
                    <Link to={`/institutions/${institution.parentId}`} className="block text-[11px] font-black text-white hover:text-brand-accent uppercase italic truncate transition-colors">
                      {ascendedPath?.parent?.name || 'SYNCING_NODE...'}
                    </Link>
                  ) : <span className="text-[11px] text-slate-600 font-mono italic">ROOT_NODE_AUTHORITY</span>}
                </div>
              </div>

              <div className="relative">
                <div className="flex justify-between items-end mb-2">
                  <span className="text-[8px] font-mono text-slate-600 uppercase italic">Subordinate_Units</span>
                  <span className={`text-[10px] font-mono ${themeColor} px-2 bg-white/5 border border-white/10`}>
                    {subordinates.length}
                  </span>
                </div>
                <div className="space-y-1 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                  {subordinates.map((child) => (
                    <Link 
                      key={child.publicId} 
                      to={`/institutions/${child.publicId}`} 
                      className={`flex items-center justify-between p-2 border transition-all group
                        ${child.active 
                          ? 'bg-white/[0.02] border-white/5 hover:border-brand-accent/30' 
                          : 'bg-orange-500/5 border-orange-500/20 hover:border-orange-500/50'
                        }`}
                    >
                      <div className="flex flex-col truncate pr-4">
                        <span className={`text-[9px] uppercase italic transition-colors truncate
                          ${child.active ? 'text-slate-400 group-hover:text-white' : 'text-orange-400 group-hover:text-orange-300'}`}>
                          {child.name}
                        </span>
                        {!child.active && (
                          <span className="text-[7px] font-mono text-orange-600 uppercase italic flex items-center gap-1">
                            <History size={8} /> DISSOLVED_UNIT
                          </span>
                        )}
                      </div>
                      <ChevronRight size={10} className={child.active ? 'text-slate-700 group-hover:text-brand-accent' : 'text-orange-900 group-hover:text-orange-500'} />
                    </Link>
                  ))}
                </div>
              </div>
            </div>
          </section>

          <section className="flex-1 flex flex-col overflow-hidden">
            <div className="flex items-center gap-3 mb-4">
              <div className={`h-[1px] flex-1 bg-gradient-to-r ${isActive ? 'from-brand-accent/50' : 'from-orange-500/50'} to-transparent`} />
              <h3 className={`text-[10px] font-black ${themeColor} uppercase tracking-[0.3em] flex items-center gap-2`}>
                <Info size={12} /> Tactical_Briefing
              </h3>
            </div>
            <div className="relative flex-1 bg-black/40 p-5 overflow-y-auto border border-white/5">
               <p className="text-[11px] leading-relaxed text-slate-400 font-serif italic mb-8">
                 {institution.description || "REPORT_SECURE: No brief provided."}
               </p>
               <div className="space-y-3 pt-4 border-t border-white/5">
                 <MetaField label="Deployment_Sector" value={institution.locationName} icon={<MapPin size={10} />} />
                 <MetaField label="Node_Status" value={isActive ? 'OPERATIONAL' : 'ARCHIVED'} icon={<Activity size={10} />} />
               </div>
            </div>
          </section>
        </aside>

        <section className="col-span-9 flex flex-col bg-[#02040a] relative">
          <div className="p-8 pb-4">
            <h2 className="text-xl font-black text-white uppercase tracking-[0.4em] flex items-center gap-3 italic">
              <Users size={22} className={themeColor} /> Personnel_Registry
            </h2>
          </div>
          <div className="flex-1 overflow-y-auto p-8 pt-4 custom-scrollbar">
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
              {occupations?.map(occ => (
                <OccupationCard key={occ.publicId} occupation={occ} isActiveNode={isActive} onClick={() => navigate(`/occupations/${occ.publicId}`)} />
              ))}
            </div>
          </div>
        </section>
      </main>

      {/* NEXUS MODAL */}
      {isNexusOpen && (
        <div className="fixed inset-0 z-[100] bg-[#02040a]/98 backdrop-blur-3xl flex flex-col">
          <div className="h-16 border-b border-white/10 flex justify-between items-center px-8 bg-black/90">
            <div className="flex items-center gap-4">
              <Network className={themeColor} size={24} />
              <h2 className="text-[11px] font-black text-white uppercase tracking-[0.4em]">Visual_Nexus_Terminal</h2>
            </div>
            <button onClick={() => setIsNexusOpen(false)} className="px-6 py-2 border border-white/10 hover:text-red-500 transition-all">
              <X size={20} />
            </button>
          </div>
          <div className="flex-1">
            <InstitutionFlow data={nexusData} />
          </div>
        </div>
      )}
    </div>
  );
};

// --- HELPERS ---
const MetaField = ({ label, value, icon }: any) => (
  <div className="flex items-center justify-between text-[10px] border-b border-white/[0.03] pb-2">
    <div className="flex items-center gap-2 text-slate-500 uppercase font-mono italic">{icon} <span>{label}</span></div>
    <div className="text-slate-200 font-black uppercase italic truncate ml-4">{value || 'N/A'}</div>
  </div>
);

const OccupationCard = ({ occupation, onClick, isActiveNode }: any) => (
  <div onClick={onClick} className={`group bg-black border border-white/5 ${isActiveNode ? 'hover:border-brand-accent/50' : 'hover:border-orange-500/50'} p-4 cursor-crosshair transition-all`}>
    <div className="flex items-center gap-4">
      <div className="w-12 h-12 flex items-center justify-center border border-white/10 bg-[#05080f]">
        <Briefcase size={20} className="text-slate-600" />
      </div>
      <div className="min-w-0">
        <div className="text-[11px] font-black text-white uppercase italic truncate">{occupation.title}</div>
      </div>
    </div>
  </div>
);

// OPRAVENO: Směrování změněno z nefunkční vyhledávací routy na přímou routu detailu lokace
const LocationPathDisplay = ({ path, isLoading }: { path: LocationResponse[] | null | undefined, isLoading: boolean }) => {
  if (isLoading) return <div className="h-2 w-48 bg-white/5 animate-pulse" />;
  
  if (!path || path.length === 0) {
    return <span className="text-[9px] font-mono text-slate-600 italic">UNKNOWN_LOCATION</span>;
  }

  return (
    <div className="flex items-center gap-1.5 font-mono text-[9px] uppercase italic text-slate-500 flex-wrap">
      <Globe size={10} className="text-brand-accent shrink-0" />
      {path.map((loc, i) => (
        <React.Fragment key={loc.externalId || i}>
          <Link
            to={`/locations/${loc.externalId}`}
            className="hover:text-brand-accent transition-colors duration-150 cursor-pointer text-slate-400 font-bold"
            title={`Type: ${loc.type} ${loc.isoCode ? `(${loc.isoCode})` : ''}`}
          >
            {loc.name}
          </Link>
          {i < path.length - 1 && <span className="text-slate-700 mx-0.5">/</span>}
        </React.Fragment>
      ))}
    </div>
  );
};

const LoadingState = () => <div className="h-screen bg-[#02040a] flex items-center justify-center text-brand-accent font-mono">LOADING...</div>;
const ErrorState = ({ message }: any) => <div className="h-screen bg-[#02040a] flex items-center justify-center text-red-500 font-mono">{message}</div>;
const getInstitutionIcon = (type: string, size: number) => <Building2 size={size} className="text-slate-400" />;

export default InstitutionDetail;