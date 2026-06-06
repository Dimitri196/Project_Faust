import React, { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios';
import { 
  ChevronLeft, History, User, Calendar, 
  ShieldCheck, ArrowRight, Clock, 
  ExternalLink, Fingerprint, Network, Info, 
  ShieldAlert, Database, Users, GitMerge, Activity,
  Terminal, Briefcase, Search, Globe, ChevronRight,
  Maximize2, X
} from 'lucide-react';
import type { OccupationResponse, AppointmentResponse, OccupationTreeResponse } from '../types';
import HierarchyFlow from '../components/occupations/HierarchyFlow';

const OccupationDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [isNexusOpen, setIsNexusOpen] = useState(false);

  // ZÁKLADNÍ DATA POZICE
  const { data: occupation, isLoading: isOccLoading, isError: isOccError } = useQuery<OccupationResponse>({
    queryKey: ['occupation', id],
    queryFn: async () => (await api.get(`/occupations/${id}`)).data,
    enabled: !!id
  });

  // HISTORIE JMENOVÁNÍ
  const { data: history, isLoading: isHistoryLoading } = useQuery<AppointmentResponse[]>({
    queryKey: ['occupation-history', id],
    queryFn: async () => (await api.get(`/appointments/occupation/${id}/history`)).data,
    enabled: !!id
  });

  // PŘÍMÍ PODŘÍZENÍ (PRO RYCHLÝ ASIDE PŘEHLED)
  const { data: subordinates, isLoading: isSubLoading } = useQuery<OccupationResponse[]>({
    queryKey: ['occupation-subordinates', id],
    queryFn: async () => (await api.get(`/occupations/${id}/subordinates`)).data,
    enabled: !!id
  });

  // DATA PRO VISUAL NEXUS (STROM)
  // Vyžaduje backend endpoint: GET /api/v1/occupations/{id}/tree
  const { data: treeData, isLoading: isTreeLoading } = useQuery<OccupationTreeResponse[]>({
    queryKey: ['occupation-tree', id],
    queryFn: async () => {
      const res = await api.get(`/occupations/${id}/tree`);
      // HierarchyFlow očekává pole kořenových uzlů
      return Array.isArray(res.data) ? res.data : [res.data];
    },
    enabled: !!id && isNexusOpen,
    staleTime: 300000 // Cache po dobu 5 minut
  });

  const activeAppointments = history?.filter(app => !app.endDate) || [];
  const pastAppointments = history?.filter(app => app.endDate) || [];
  const isVacant = activeAppointments.length === 0;

  if (isOccLoading || isHistoryLoading || isSubLoading) return <LoadingState />;
  if (isOccError || !occupation) return <ErrorState message="NODE_DATA_RECOVERY_FAILED" />;

  const themeColor = isVacant ? 'text-red-500' : 'text-blue-500';
  const themeBorder = isVacant ? 'border-red-500/30' : 'border-blue-500/30';
  const themeBg = isVacant ? 'bg-[#140505]' : 'bg-[#05080f]';

  return (
    <div className={`h-screen bg-[#02040a] text-slate-300 flex flex-col overflow-hidden font-sans border-4 ${isVacant ? 'border-red-900/30' : 'border-[#0a0f18]'}`}>
      
      {/* STATUS BAR */}
      <div className="h-6 bg-[#0a0f18] border-b border-white/10 flex items-center px-4 justify-between text-[9px] font-mono text-slate-500 uppercase tracking-[0.2em]">
        <div className="flex gap-6">
          <button onClick={() => navigate('/occupations')} className="flex items-center gap-2 hover:text-white transition-colors">
            <ChevronLeft size={10} /> EXIT_DOSSIER
          </button>
          <div className="h-4 w-px bg-white/10" />
          <span className="flex items-center gap-2">
            <Terminal size={10} className={themeColor}/> SYSTEM_CONNECTED: FAUST_v4.2
          </span>
        </div>
        <div className="flex gap-4">
          <span className="flex items-center gap-2 text-blue-500/70 italic uppercase text-[10px] font-black">
              RANK_LVL: {occupation.rank.toString().padStart(2, '0')}
          </span>
          {isVacant ? <span className="text-red-500 animate-pulse">● CRITICAL_VACANCY</span> : <span className="text-blue-500/70 italic animate-pulse">● NODE_OPERATIONAL</span>}
        </div>
      </div>

      {/* HEADER */}
      <header className={`h-28 ${themeBg} border-b border-white/10 flex items-center px-8 relative overflow-hidden shrink-0`}>
        <div className="flex items-center gap-8 z-10 w-full">
          <div className={`w-20 h-20 bg-white/5 border ${themeBorder} p-2 relative flex items-center justify-center`}>
            <Briefcase size={32} className={isVacant ? 'text-red-950' : 'text-blue-900'} />
            <div className={`absolute inset-0 w-full h-[1px] ${isVacant ? 'bg-red-500/40' : 'bg-blue-500/20'} animate-scan-line pointer-events-none`} />
          </div>
          
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-1">
              <Link to={`/institutions/${occupation.institutionPublicId}`} className={`${isVacant ? 'bg-red-600 text-white' : 'bg-blue-600 text-black'} text-[10px] font-black px-2 py-0.5 uppercase italic hover:opacity-80 transition-opacity`}>
                {occupation.institutionName}
              </Link>
              <div className="flex items-center gap-2 px-2 py-0.5 border border-white/10 bg-white/5 text-[9px] font-black uppercase italic tracking-widest">
                <Network size={12} className={themeColor} /> {occupation.category}
              </div>
            </div>
            <h1 className={`text-4xl font-black uppercase tracking-tighter leading-none italic ${isVacant ? 'text-red-600/80' : 'text-white'}`}>
              {occupation.title}
            </h1>
            <div className="flex items-center gap-4 mt-2 font-mono text-[9px] text-slate-600 uppercase tracking-widest italic">
               <Fingerprint size={10} className={themeColor} /> Ref_Code: {occupation.code}
            </div>
          </div>

          <div className="flex gap-4 h-20">
             {/* NEXUS TRIGGER */}
             <button 
                onClick={() => setIsNexusOpen(true)}
                className="group flex flex-col items-center justify-center px-6 bg-blue-600/5 border border-blue-500/20 hover:bg-blue-600 hover:border-blue-400 transition-all text-blue-500 hover:text-white"
             >
               <GitMerge size={20} className="mb-1 group-hover:scale-110 transition-transform" />
               <span className="text-[8px] font-black uppercase tracking-[0.2em] leading-tight text-center">Open_Visual<br/>Nexus</span>
             </button>

             <button onClick={() => navigate('/search')} className="px-6 bg-white/5 border border-white/10 hover:border-blue-500 text-slate-400 transition-colors">
               <Search size={24} />
             </button>
          </div>
        </div>
      </header>

      {/* MAIN CONTENT */}
      <main className="flex-1 grid grid-cols-12 overflow-hidden bg-[#09122a]">
        
        {/* ASIDE: STRUCTURAL CONTEXT */}
        <aside className="col-span-3 border-r border-white/5 bg-[#05080f] flex flex-col overflow-hidden p-6 space-y-8">
          
          <section className="flex flex-col">
            <div className="flex items-center gap-3 mb-4">
              <div className={`h-[1px] flex-1 bg-gradient-to-r ${isVacant ? 'from-red-500/40' : 'from-blue-500/40'} to-transparent`} />
              <h3 className={`text-[10px] font-black ${themeColor} uppercase tracking-[0.3em] flex items-center gap-2`}>
                <GitMerge size={12} /> Command_Line
              </h3>
            </div>
            
            <div className={`space-y-4 relative pl-4 border-l ${isVacant ? 'border-red-500/20' : 'border-blue-500/20'}`}>
              <div>
                <span className="text-[8px] font-mono text-slate-600 uppercase block mb-1 italic">Immediate_Superior</span>
                <div className={`p-3 bg-black border border-white/5 border-l-2 ${isVacant ? 'border-l-red-500' : 'border-l-blue-500'}`}>
                  {occupation.reportsToPublicId ? (
                    <Link to={`/occupations/${occupation.reportsToPublicId}`} className="block text-[11px] font-black text-white hover:text-blue-500 uppercase italic truncate transition-colors">
                      {occupation.supervisorTitle}
                    </Link>
                  ) : <span className="text-[11px] text-slate-600 font-mono italic uppercase">Root_Authority</span>}
                </div>
              </div>

              <div>
                <div className="flex justify-between items-end mb-2">
                  <span className="text-[8px] font-mono text-slate-600 uppercase italic">Direct_Reports</span>
                  <span className={`text-[10px] font-mono ${themeColor} px-2 bg-white/5 border border-white/10`}>
                    {subordinates?.length || 0}
                  </span>
                </div>
                <div className="space-y-1 max-h-[250px] overflow-y-auto pr-2 custom-scrollbar">
                  {subordinates && subordinates.length > 0 ? subordinates.map(sub => (
                    <Link key={sub.publicId} to={`/occupations/${sub.publicId}`} className="flex items-center justify-between p-2 bg-white/[0.02] border border-white/5 hover:border-blue-500/30 transition-all group">
                      <span className="text-[9px] text-slate-400 group-hover:text-white uppercase italic truncate pr-2">{sub.title}</span>
                      <ChevronRight size={10} className="text-slate-700 group-hover:text-blue-500 shrink-0" />
                    </Link>
                  )) : <div className="text-[9px] font-mono text-slate-700 italic px-2 uppercase">Standalone_Node</div>}
                </div>
              </div>
            </div>
          </section>

          <section className="flex-1 flex flex-col overflow-hidden">
            <div className="flex items-center gap-3 mb-4">
              <div className={`h-[1px] flex-1 bg-gradient-to-r ${isVacant ? 'from-red-500/50' : 'from-blue-500/50'} to-transparent`} />
              <h3 className={`text-[10px] font-black ${themeColor} uppercase tracking-[0.3em] flex items-center gap-2`}>
                <Info size={12} /> Unit_Transcript
              </h3>
            </div>
            <div className="relative flex-1 bg-black/40 p-5 overflow-y-auto border border-white/5">
                <p className="text-[11px] leading-relaxed text-slate-400 font-serif italic mb-8">
                  {occupation.description || "The official operational mandate for this structural unit remains classified within the active central registry."}
                </p>
                <div className="space-y-3 pt-4 border-t border-white/5">
                  <MetaField label="Deployment_Rank" value={`LVL_${occupation.rank}`} icon={<Activity size={10} />} />
                  <MetaField label="Security_Level" value="PUBLIC_RECORD" icon={<ShieldCheck size={10} />} />
                </div>
            </div>
          </section>
        </aside>

        {/* MAIN PANEL: ASSETS & CHRONICLE */}
        <section className="col-span-9 flex flex-col bg-[#02040a] relative overflow-hidden">
          <div className="absolute inset-0 bg-[radial-gradient(#1e293b_0.5px,transparent_0.5px)] [background-size:20px_20px] opacity-20 pointer-events-none" />
          
          <div className="flex-1 overflow-y-auto p-8 custom-scrollbar z-10">
            
            {/* ACTIVE HOLDERS */}
            <div className="mb-12">
              <h2 className="text-xl font-black text-white uppercase tracking-[0.4em] flex items-center gap-3 italic mb-8 text-shadow-glow">
                <Users size={22} className={themeColor} /> Active_Holders
              </h2>
              
              {activeAppointments.length > 0 ? (
                <div className={`grid gap-4 ${activeAppointments.length > 1 ? 'grid-cols-2' : 'grid-cols-1'}`}>
                  {activeAppointments.map(app => (
                    <Link 
                      key={app.publicId} 
                      to={`/personnel/${app.personPublicId}`}
                      className="group bg-black/40 border border-white/5 p-4 flex gap-6 hover:border-blue-500/50 transition-all relative overflow-hidden"
                    >
                      <div className="w-24 h-32 bg-slate-900 border border-white/10 shrink-0 overflow-hidden relative">
                         {app.personPhotoUrl ? (
                            <img src={app.personPhotoUrl} className="w-full h-full object-cover grayscale group-hover:grayscale-0 transition-all duration-500" alt="" />
                         ) : <User size={40} className="m-auto mt-8 text-slate-800" />}
                         <div className={`absolute top-0 left-0 w-full h-[1px] ${themeColor} opacity-20 animate-scan-line`} />
                      </div>
                      <div className="flex-1 flex flex-col justify-center">
                        <span className={`text-[8px] font-mono ${themeColor} uppercase tracking-widest mb-1`}>Verified_Asset_ID</span>
                        <h4 className="text-2xl font-black text-white uppercase italic tracking-tighter leading-none group-hover:text-blue-400 transition-colors">
                          {app.personDisplayName}
                        </h4>
                        <div className="mt-4 flex gap-6">
                           <div className="flex flex-col">
                             <span className="text-[7px] font-mono text-slate-600 uppercase">Commencement</span>
                             <span className="text-[10px] text-slate-300 italic font-bold tracking-widest">{app.startDate}</span>
                           </div>
                           <div className="flex flex-col">
                             <span className="text-[7px] font-mono text-slate-600 uppercase">Verification_Status</span>
                             <span className="text-[10px] text-emerald-500 italic font-bold tracking-widest">ACTIVE_HOLDER</span>
                           </div>
                        </div>
                      </div>
                      <ExternalLink size={14} className="absolute top-4 right-4 text-slate-700 group-hover:text-blue-500" />
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="bg-red-500/5 border border-dashed border-red-500/20 rounded p-12 text-center">
                  <ShieldAlert className="text-red-600 mx-auto mb-4 opacity-40 animate-pulse" size={48} strokeWidth={1} />
                  <div className="font-mono text-xs text-red-500 font-black uppercase tracking-[0.3em]">
                    OPERATIONAL_GAP_DETECTED
                  </div>
                  <p className="text-[9px] text-slate-600 mt-2 uppercase tracking-widest">No active personnel assigned to this node // Structural instability risk: Nominal</p>
                </div>
              )}
            </div>

            {/* CHRONICLE */}
            <div>
              <h2 className="text-sm font-black text-slate-500 uppercase tracking-[0.4em] flex items-center gap-3 italic mb-6 border-b border-white/5 pb-2">
                <History size={16} /> Node_Chronicle
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {pastAppointments.map(app => (
                  <button 
                    key={app.publicId}
                    onClick={() => navigate(`/personnel/${app.personPublicId}`)}
                    className="flex items-center gap-4 p-3 bg-white/[0.02] border border-white/5 rounded hover:border-white/20 transition-all group text-left"
                  >
                    <div className="w-10 h-12 bg-black border border-white/10 shrink-0 overflow-hidden">
                      {app.personPhotoUrl ? (
                        <img src={app.personPhotoUrl} className="w-full h-full object-cover grayscale opacity-40 group-hover:opacity-80" alt="" />
                      ) : <User size={16} className="m-auto mt-3 text-slate-800" />}
                    </div>
                    <div className="min-w-0">
                      <div className="text-[10px] font-black text-slate-400 group-hover:text-white truncate uppercase italic">{app.personDisplayName}</div>
                      <div className="text-[8px] font-mono text-slate-600 uppercase flex items-center gap-1 mt-1">
                        <Clock size={8} /> {app.startDate} — {app.endDate}
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* VISUAL NEXUS TERMINAL (MODAL) */}
      {isNexusOpen && (
        <div className="fixed inset-0 z-[100] bg-[#02040a]/98 backdrop-blur-3xl flex flex-col animate-in fade-in duration-300">
           {/* Modal Header */}
           <div className="h-16 border-b border-white/10 flex justify-between items-center px-8 bg-black/90">
              <div className="flex items-center gap-4">
                <div className="p-2 bg-blue-500/10 border border-blue-500/20">
                   <GitMerge className="text-blue-500" size={20} />
                </div>
                <div>
                   <h2 className="text-[11px] font-black text-white uppercase tracking-[0.4em] leading-none">Personnel_Nexus_Terminal</h2>
                   <p className="text-[8px] font-mono text-slate-500 uppercase tracking-widest mt-1">Mapping operational hierarchy for node: {occupation.code}</p>
                </div>
              </div>
              <button 
                onClick={() => setIsNexusOpen(false)}
                className="flex items-center gap-2 px-6 py-2 border border-white/10 hover:border-red-500 hover:text-red-500 transition-all text-[10px] font-black uppercase italic group"
              >
                Close_Link <X size={14} className="group-hover:rotate-90 transition-transform" />
              </button>
           </div>

           {/* Flow Component Container */}
           <div className="flex-1 relative overflow-hidden">
              {isTreeLoading ? (
                <div className="absolute inset-0 flex items-center justify-center font-mono text-[10px] text-blue-500/40 animate-pulse">
                  INITIALIZING_NEURAL_MAP...
                </div>
              ) : treeData && treeData.length > 0 ? (
                <HierarchyFlow data={treeData} />
              ) : (
                <div className="absolute inset-0 flex flex-col items-center justify-center font-mono text-red-500/60">
                   <ShieldAlert size={32} className="mb-2 opacity-40" />
                   <span className="text-[10px] uppercase tracking-widest">Nexus_Data_Unreachable</span>
                </div>
              )}
           </div>
        </div>
      )}

      <style>{`
        @keyframes scan { 0% { top: 0%; opacity: 0; } 50% { opacity: 0.5; } 100% { top: 100%; opacity: 0; } }
        .animate-scan-line { animation: scan 6s linear infinite; }
        .custom-scrollbar::-webkit-scrollbar { width: 4px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #1e293b; border-radius: 2px; }
        .text-shadow-glow { text-shadow: 0 0 10px rgba(59, 130, 246, 0.5); }
      `}</style>
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

const LoadingState = () => (
  <div className="h-screen bg-[#02040a] flex flex-col items-center justify-center font-mono">
    <Terminal className="text-blue-500 mb-4 animate-pulse" size={32} />
    <div className="text-blue-500 text-[10px] tracking-[0.8em] animate-pulse uppercase italic text-center">
       Syncing_Dossier...<br/>
       <span className="opacity-40">Connecting to FAUST v4.2 Core</span>
    </div>
  </div>
);

const ErrorState = ({ message }: { message: string }) => (
  <div className="h-screen bg-[#02040a] flex flex-col items-center justify-center font-mono p-10 text-center text-white">
    <ShieldAlert size={48} className="text-red-500/40 mb-6" />
    <div className="text-xs uppercase tracking-[0.4em] font-black mb-8 italic">{message}</div>
    <button onClick={() => window.history.back()} className="text-[10px] text-blue-500 border border-blue-500/20 px-6 py-3 hover:bg-blue-500/10 uppercase tracking-widest transition-all italic font-black">
      Return_to_Safe_Sector
    </button>
  </div>
);

export default OccupationDetail;