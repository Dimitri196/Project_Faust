import React from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios';
import {
  ChevronLeft, History, User, Calendar,
  ShieldCheck, ArrowRight, Clock,
  ExternalLink, Fingerprint, Network, Info,
  ShieldAlert, Database, Users, GitMerge
} from 'lucide-react';
import type { OccupationResponse, AppointmentResponse } from '../types';

const OccupationDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // 1. Základní data o pozici
  const { data: occupation, isLoading: isOccLoading, isError: isOccError } = useQuery<OccupationResponse>({
    queryKey: ['occupation', id],
    queryFn: async () => (await api.get(`/occupations/${id}`)).data,
    enabled: !!id
  });

  // 2. Historie personálního obsazení (Active & Past Assets)
  const { data: history, isLoading: isHistoryLoading } = useQuery<AppointmentResponse[]>({
    queryKey: ['occupation-history', id],
    queryFn: async () => (await api.get(`/appointments/occupation/${id}/history`)).data,
    enabled: !!id
  });

  // 3. STRATEGICKÉ DOPLNĚNÍ: Načtení podřízených uzlů (Direct Reports)
  const { data: subordinates, isLoading: isSubLoading } = useQuery<OccupationResponse[]>({
    queryKey: ['occupation-subordinates', id],
    queryFn: async () => (await api.get(`/occupations/${id}/subordinates`)).data,
    enabled: !!id
  });

  const activeAppointments = history?.filter(app => !app.endDate) || [];
  const pastAppointments = history?.filter(app => app.endDate) || [];

  if (isOccLoading || isHistoryLoading || isSubLoading) return <LoadingState />;
  if (isOccError || !occupation) return <ErrorState message="NODE_DATA_RECOVERY_FAILED" />;

  return (
    <div className="h-full bg-[#0f172a] text-slate-200 flex flex-col overflow-hidden">

      {/* --- STRATEGIC HEADER --- */}
      <div className="px-8 py-8 border-b border-slate-800 bg-slate-900/50 relative overflow-hidden">
        <div className="absolute top-0 right-0 p-4 opacity-[0.03] pointer-events-none">
          <ShieldCheck size={200} />
        </div>

        <div className="max-w-7xl mx-auto relative z-10">
          <div className="flex items-center gap-3 mb-8">
            <button
              onClick={() => navigate('/occupations')}
              className="flex items-center gap-2 text-slate-500 hover:text-white transition-colors font-mono text-[10px] uppercase tracking-widest group"
            >
              <ChevronLeft size={14} className="group-hover:-translate-x-1 transition-transform" />
              Back_to_Hierarchy
            </button>
            <span className="text-slate-800">/</span>
            <Link
              to={`/institutions/${occupation.institutionPublicId}`}
              className="flex items-center gap-2 text-blue-400 hover:text-blue-300 transition-colors font-mono text-[10px] uppercase tracking-widest"
            >
              <ExternalLink size={10} /> {occupation.institutionName}
            </Link>
          </div>

          <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-8">
            <div className="space-y-3">
              <div className="flex items-center gap-3 font-mono text-[10px] tracking-[0.2em] uppercase">
                <span className="px-2 py-1 bg-blue-500/10 border border-blue-500/20 text-blue-400 rounded">
                  Ref: {occupation.code}
                </span>
                <span className="text-slate-600">//</span>
                <span className="text-slate-500 italic">{occupation.category}</span>
              </div>
              <h1 className="text-5xl font-bold text-white tracking-tight uppercase leading-tight">
                {occupation.title}
              </h1>
            </div>

            <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl min-w-[200px] text-right shadow-2xl relative group">
              <div className="absolute inset-x-0 -top-px h-px bg-gradient-to-r from-transparent via-blue-500/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
              <div className="text-[9px] text-slate-500 font-bold tracking-[0.3em] uppercase mb-1">Structural Rank</div>
              <div className="text-4xl font-mono font-black text-blue-500">
                LVL_{occupation.rank.toString().padStart(2, '0')}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-8 custom-scrollbar bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px]">
        <div className="max-w-5xl mx-auto space-y-12 pb-20">

          {/* --- SECTION: HIERARCHICAL CONTEXT (SUPERIOR & SUBORDINATES) --- */}
          <section className="grid grid-cols-1 lg:grid-cols-2 gap-8 animate-in slide-in-from-left duration-700">

            {/* Superior Officer */}
            <div>
              <div className="flex items-center gap-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-4">
                <Network size={14} className="text-blue-500" /> Command_Lineage
              </div>
              {occupation.reportsToPublicId ? (
                <div
                  onClick={() => navigate(`/occupations/${occupation.reportsToPublicId}`)}
                  className="group flex items-center justify-between p-5 bg-slate-900/50 border border-slate-800 rounded-xl hover:border-blue-500/30 hover:bg-slate-900 transition-all cursor-pointer"
                >
                  <div className="flex items-center gap-4">
                    <div className="p-3 bg-slate-950 border border-slate-800 rounded-lg group-hover:bg-blue-500/5 transition-colors">
                      <ShieldCheck size={20} className="text-blue-500" />
                    </div>
                    <div>
                      <div className="text-[9px] font-mono text-slate-500 uppercase">Superior Officer</div>
                      <div className="text-sm font-bold text-slate-200 group-hover:text-blue-400 uppercase tracking-tight transition-colors">
                        {occupation.supervisorTitle}
                      </div>
                    </div>
                  </div>
                  <ArrowRight size={18} className="text-slate-700 group-hover:text-blue-500 group-hover:translate-x-1 transition-all" />
                </div>
              ) : (
                <div className="p-5 border border-dashed border-slate-800 rounded-xl text-[10px] text-slate-600 uppercase font-mono italic">
                  Root_Node // No_Superior_Authority
                </div>
              )}
            </div>

            {/* Subordinate Nodes */}
            <div>
              <div className="flex items-center gap-2 text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-4">
                <GitMerge size={14} className="text-cyan-500" /> Direct_Reports
              </div>
              <div className="space-y-2">
                {subordinates && subordinates.length > 0 ? (
                  subordinates.map((sub) => (
                    <div
                      key={sub.publicId}
                      onClick={() => navigate(`/occupations/${sub.publicId}`)}
                      className="group flex items-center justify-between p-3 bg-slate-900/30 border border-slate-800/50 rounded-lg hover:border-cyan-500/30 hover:bg-slate-900 transition-all cursor-pointer"
                    >
                      <div className="flex items-center gap-3">
                        <Users size={12} className="text-slate-600 group-hover:text-cyan-500 transition-colors" />
                        <span className="text-xs font-bold text-slate-400 group-hover:text-slate-200 uppercase tracking-tight">
                          {sub.title}
                        </span>
                      </div>
                      <ArrowRight size={14} className="text-slate-800 group-hover:text-cyan-500 transition-all" />
                    </div>
                  ))
                ) : (
                  <div className="p-3 border border-dashed border-slate-800 rounded-lg text-[10px] text-slate-600 uppercase font-mono italic">
                    Terminal_Node // No_Subordinates
                  </div>
                )}
              </div>
            </div>
          </section>

          {/* --- SECTION: ACTIVE ASSETS (MULTI-SUPPORT) --- */}
          <section className="animate-in fade-in slide-in-from-bottom duration-700">
            <div className="flex items-center gap-4 mb-8">
              <h3 className="text-[11px] font-bold text-blue-500 uppercase tracking-[0.4em] flex items-center gap-3">
                <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                {activeAppointments.length > 1 ? 'Collective_Asset_Group' : 'Active_Asset_Verification'}
              </h3>
              <div className="h-px flex-1 bg-slate-800" />
            </div>

            {activeAppointments.length > 0 ? (
              <div className={`grid gap-6 ${activeAppointments.length > 1 ? 'grid-cols-1 md:grid-cols-2' : 'grid-cols-1'}`}>
                {activeAppointments.map((app) => (
                  <div
                    key={app.publicId}
                    onClick={() => navigate(`/personnel/${app.personPublicId}`)}
                    className="group bg-slate-900 border border-slate-800 rounded-2xl p-2 flex cursor-pointer transition-all hover:border-blue-500/40 shadow-xl overflow-hidden relative"
                  >
                    <div className={`${activeAppointments.length > 1 ? 'w-24 h-32' : 'w-48 h-64'} bg-slate-950 rounded-xl relative overflow-hidden flex-shrink-0 transition-all duration-500`}>
                      {app.personPhotoUrl ? (
                        <img
                          src={app.personPhotoUrl}
                          alt=""
                          className="w-full h-full object-cover grayscale group-hover:grayscale-0 brightness-75 group-hover:brightness-100 group-hover:scale-105 transition-all duration-700"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center">
                          <User size={activeAppointments.length > 1 ? 32 : 64} className="text-slate-800" />
                        </div>
                      )}
                    </div>

                    <div className="flex-1 p-6 flex flex-col justify-center relative min-w-0">
                      <Fingerprint size={80} className="absolute right-4 opacity-[0.03] group-hover:opacity-[0.08] transition-opacity pointer-events-none" />

                      <div className="space-y-3">
                        <div>
                          <div className="text-[8px] font-mono text-blue-500 uppercase tracking-widest mb-1 font-bold">Verified Holder</div>
                          <h2 className={`${activeAppointments.length > 1 ? 'text-xl' : 'text-4xl'} font-black text-white uppercase tracking-tighter group-hover:text-blue-400 transition-colors truncate`}>
                            {app.personDisplayName}
                          </h2>
                        </div>

                        <div className="flex flex-wrap items-center gap-3">
                          <div className="px-2 py-1 bg-slate-950 border border-slate-800 rounded flex items-center gap-2">
                            <Calendar size={10} className="text-blue-500" />
                            <span className="text-[9px] font-mono text-slate-400 uppercase tracking-tighter">SINCE_{app.startDate}</span>
                          </div>
                        </div>

                        {app.appointmentNote && (
                          <p className="text-[10px] text-slate-500 italic font-serif line-clamp-2 leading-tight border-l border-slate-800 pl-3">
                            {app.appointmentNote}
                          </p>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="border border-dashed border-red-500/20 rounded-2xl p-16 text-center bg-red-500/5">
                <ShieldAlert className="text-red-500 mx-auto mb-4 animate-pulse" size={40} />
                <div className="font-mono text-sm text-red-500 font-bold uppercase tracking-widest">
                  Operational_Gap_Detected
                </div>
                <p className="text-[10px] text-slate-600 mt-2 uppercase">Node is currently vacant // Personnel assignment required</p>
              </div>
            )}
          </section>

          {/* --- SECTION: MANDATE --- */}
          <section className="max-w-4xl animate-in fade-in duration-1000">
            <h3 className="text-[11px] font-bold text-slate-500 uppercase tracking-widest mb-6 flex items-center gap-2">
              <Info size={16} className="text-blue-500" /> Operational Mandate
            </h3>
            <div className="p-8 bg-slate-900/30 border border-slate-800 rounded-2xl font-serif italic text-xl text-slate-400 leading-relaxed relative">
              <div className="text-3xl text-blue-500/20 absolute top-4 left-4 font-mono">"</div>
              {occupation.description || "The official mandate for this node remains classified or undocumented."}
              <div className="text-3xl text-blue-500/20 absolute bottom-4 right-4 font-mono">"</div>
            </div>
          </section>

          {/* --- SECTION: HISTORY --- */}
          <section className="animate-in fade-in duration-1000">
            <div className="flex items-center justify-between mb-8 border-b border-slate-800 pb-4">
              <h3 className="text-[11px] font-bold text-slate-500 uppercase tracking-widest flex items-center gap-2">
                <History size={16} /> Asset Chronicle
              </h3>
              <div className="text-[9px] font-mono text-slate-600 uppercase">Records: {pastAppointments?.length || 0}</div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {pastAppointments && pastAppointments.length > 0 ? (
                pastAppointments.map((app) => (
                  <div
                    key={app.publicId}
                    onClick={() => navigate(`/personnel/${app.personPublicId}`)}
                    className="bg-slate-900/50 border border-slate-800 p-4 rounded-xl flex items-center gap-4 group hover:border-slate-600 transition-all cursor-pointer"
                  >
                    <div className="w-12 h-16 bg-slate-950 rounded-lg overflow-hidden flex-shrink-0">
                      {app.personPhotoUrl ? (
                        <img src={app.personPhotoUrl} className="w-full h-full object-cover grayscale opacity-50 group-hover:opacity-100 transition-all" alt="" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center"><User size={16} className="text-slate-700" /></div>
                      )}
                    </div>
                    <div>
                      <div className="text-sm font-bold text-slate-300 group-hover:text-blue-400 transition-colors uppercase tracking-tight">{app.personDisplayName}</div>
                      <div className="text-[9px] font-mono text-slate-500 flex items-center gap-2 mt-1">
                        <Clock size={10} /> {app.startDate} — {app.endDate}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="col-span-full py-12 text-center border border-dashed border-slate-800 rounded-2xl text-slate-700 font-mono text-[10px] uppercase tracking-widest">
                  <Database size={24} className="mx-auto mb-3 opacity-20" />
                  No archival records for this node.
                </div>
              )}
            </div>
          </section>
        </div>
      </div>

      <style>{`
        @keyframes scan {
          0% { transform: translateY(0); }
          100% { transform: translateY(256px); }
        }
        .animate-scan-line {
          animation: scan 4s linear infinite;
        }
      `}</style>
    </div>
  );
};

// --- STATES ---

const LoadingState = () => (
  <div className="h-full bg-[#0f172a] flex flex-col items-center justify-center font-mono">
    <div className="w-16 h-16 relative mb-6">
      <div className="absolute inset-0 border-t-2 border-blue-500 rounded-full animate-spin" />
      <Users className="absolute inset-0 m-auto text-blue-500 opacity-20" size={32} />
    </div>
    <div className="text-slate-500 text-[10px] tracking-[1em] uppercase animate-pulse">Initializing_Node_Sync</div>
  </div>
);

const ErrorState = ({ message }: { message: string }) => (
  <div className="h-full bg-[#0f172a] flex flex-col items-center justify-center p-12 text-center">
    <ShieldAlert size={48} className="text-red-500 mb-6 opacity-20" />
    <h2 className="text-white font-bold mb-2 uppercase tracking-widest">{message}</h2>
    <Link to="/occupations" className="mt-6 text-blue-400 font-mono text-[10px] uppercase border border-blue-500/20 px-8 py-3 rounded hover:bg-blue-500/10 transition-all">
      Return_to_Grid
    </Link>
  </div>
);

export default OccupationDetail;