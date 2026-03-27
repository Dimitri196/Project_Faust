import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import type { UserProfile, ClearanceLevel } from '../types';
import { 
  Shield, Activity, Terminal, Lock, 
  Database, Cpu, User, ChevronLeft, 
  Fingerprint, Briefcase, ScanLine,
  Mail, Edit3, Save, X, Loader2
} from 'lucide-react';

const UserProfilePage = () => {
  const { id } = useParams(); 
  const { user: authUser, logout } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // --- STATE PRO EDITACI ---
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<Partial<UserProfile>>({});

  const { data: profile, isLoading, error } = useQuery<UserProfile>({
    queryKey: ['profile', id || authUser?.email],
    queryFn: async () => {
      const res = await api.get(id ? `/profile/dossier/${id}` : `/profile/me?email=${authUser?.email}`);
      return res.data;
    },
    enabled: !!(id || authUser?.email)
  });

  // --- MUTACE PRO BACKEND (PATCH) ---
  const updateMutation = useMutation({
    mutationFn: async (updatedData: Partial<UserProfile>) => {
      // Používáme tvůj @PatchMapping("/dossier/{id}")
      return api.patch(`/profile/dossier/${profile?.id}`, updatedData);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setIsEditing(false);
    }
  });

  const handleStartEdit = () => {
    if (profile) {
      setEditForm({
        fullName: profile.fullName,
        role: profile.role,
        techStack: profile.techStack
      });
      setIsEditing(true);
    }
  };

  const handleSave = () => {
    updateMutation.mutate(editForm);
  };

  if (isLoading) return (
    <div className="h-full flex flex-col items-center justify-center bg-[#0a0c10] font-mono text-[11px] text-blue-500/50 tracking-[0.3em]">
      <div className="relative mb-6">
        <Fingerprint size={40} className="animate-pulse" />
        <div className="absolute inset-0 bg-blue-500 blur-xl opacity-20 animate-pulse" />
      </div>
      INITIALIZING_SECURE_UPLINK...
    </div>
  );

  if (error || !profile) return (/* ... tvůj error state ... */ null);

  return (
    <div className="min-h-full bg-[#0a0c10] p-6 md:p-10 font-sans text-slate-300 relative overflow-hidden">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#161b22_1px,transparent_1px),linear-gradient(to_bottom,#161b22_1px,transparent_1px)] bg-[size:40px_40px] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)]" />

      <div className="max-w-5xl mx-auto relative z-10">
        
        {/* HEADER */}
        <div className="flex justify-between items-center mb-8 border-b border-slate-800/50 pb-4">
          <button onClick={() => navigate(-1)} className="group flex items-center gap-2 text-slate-600 hover:text-blue-400 transition-all text-[10px] font-mono uppercase tracking-[0.2em]">
            <ChevronLeft size={14} className="group-hover:-translate-x-1 transition-transform" /> 
            Back_To_Personnel_Index
          </button>
          <div className="flex items-center gap-6 font-mono text-[9px] text-slate-600 uppercase tracking-widest">
             <span className="flex items-center gap-2 italic">
               Mode: <span className={isEditing ? "text-orange-500" : "text-blue-500"}>
                 {isEditing ? 'EXTERNAL_OVERRIDE' : 'READ_ONLY'}
               </span>
             </span>
          </div>
        </div>

        {/* IDENTITY DOSSIER CARD */}
        <div className="bg-[#0f1117]/80 backdrop-blur-md border border-slate-800 shadow-2xl mb-8 relative rounded-sm overflow-hidden">
          <div className="p-8 flex flex-col md:flex-row items-center md:items-start gap-10">
            {/* Foto Area */}
            <div className="relative group">
              <div className="w-44 h-52 bg-[#05070a] border border-slate-800 flex items-center justify-center relative overflow-hidden rounded-sm">
                <User size={100} className="relative z-10 opacity-20" />
                <div className="absolute top-0 left-0 w-full h-0.5 bg-blue-500/40 animate-scan-line-slow pointer-events-none" />
              </div>
            </div>

            {/* Core Info */}
            <div className="flex-1 text-center md:text-left pt-2 w-full">
              {isEditing ? (
                <input 
                  className="bg-black/50 border-b-2 border-orange-500/50 text-5xl font-black text-white uppercase italic outline-none w-full mb-4 focus:border-orange-500 transition-all"
                  value={editForm.fullName}
                  onChange={e => setEditForm({...editForm, fullName: e.target.value})}
                />
              ) : (
                <h1 className="text-5xl font-black text-white tracking-tighter uppercase italic leading-none mb-4">
                  {profile.fullName}
                </h1>
              )}
              
              <div className="flex flex-wrap justify-center md:justify-start gap-6 text-slate-500 text-[10px] font-mono uppercase mb-8">
                <span className="flex items-center gap-2">
                   <Briefcase size={12} className="text-blue-500/50" />
                   {isEditing ? (
                     <input 
                       className="bg-transparent border-b border-slate-700 outline-none text-blue-400"
                       value={editForm.role}
                       onChange={e => setEditForm({...editForm, role: e.target.value})}
                     />
                   ) : profile.role}
                </span>
                <span className="flex items-center gap-2 text-slate-400 font-bold uppercase tracking-widest">
                  &gt; STATUS: <span className="text-emerald-500">{profile.status}</span>
                </span>
              </div>

              <div className={`inline-flex px-5 py-3 border-l-4 font-mono text-[11px] font-black tracking-widest bg-slate-900/40 border-slate-700 text-slate-400`}>
                SEC_LEVEL: {profile.clearance.replace(/_/g, ' ')}
              </div>
            </div>
          </div>
        </div>

        {/* LOGISTICAL MATRIX GRID */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-[#0f1117]/80 border border-slate-800 p-8 rounded-sm">
              <h3 className="text-[10px] font-black text-slate-600 mb-8 uppercase tracking-[0.5em] flex items-center gap-2 border-b border-slate-800/50 pb-3 italic">
                <Database size={14} className="text-blue-500/50" /> Identity_Parameters
              </h3>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-y-10 gap-x-12">
                <InfoRow label="Network_Alias" value={profile.email} icon={<Mail size={14}/>} readOnly />
                <InfoRow label="Internal_ID" value={profile.id} icon={<Fingerprint size={14}/>} readOnly />
              </div>
            </div>

            {/* TECH STACK */}
            <div className="bg-[#0f1117]/80 border border-slate-800 p-8 rounded-sm">
               <h3 className="text-[10px] font-black text-slate-600 mb-6 uppercase tracking-[0.5em] border-b border-slate-800/50 pb-3 italic">
                 Capabilities_Manifest
               </h3>
               <div className="flex flex-wrap gap-2">
                 {(isEditing ? editForm.techStack : profile.techStack)?.map((tech, idx) => (
                   <span key={idx} className="px-4 py-2 bg-slate-900/50 text-slate-400 font-mono text-[10px] border border-slate-800 uppercase italic">
                     {tech}
                   </span>
                 ))}
               </div>
            </div>
          </div>

          {/* COMMAND PANEL */}
          <div className="space-y-6">
            <div className="bg-slate-900/40 border border-slate-800 p-6 rounded-sm">
                <h4 className="text-[9px] font-black text-slate-600 uppercase mb-6 tracking-[0.3em]">Operational_Commands</h4>
                <div className="space-y-3">
                  {!isEditing ? (
                    <button 
                      onClick={handleStartEdit}
                      className="w-full py-4 bg-blue-600/10 border border-blue-600/50 text-blue-400 font-black text-[9px] uppercase tracking-[0.2em] hover:bg-blue-600 hover:text-white transition-all flex items-center justify-center gap-2 group"
                    >
                      <Edit3 size={14} className="group-hover:rotate-12 transition-transform" /> Modify_Dossier
                    </button>
                  ) : (
                    <>
                      <button 
                        onClick={handleSave}
                        disabled={updateMutation.isPending}
                        className="w-full py-4 bg-emerald-600/20 border border-emerald-500 text-emerald-500 font-black text-[9px] uppercase tracking-[0.2em] hover:bg-emerald-600 hover:text-white transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                      >
                        {updateMutation.isPending ? <Loader2 className="animate-spin" size={14} /> : <Save size={14} />} Commit_Changes
                      </button>
                      <button 
                        onClick={() => setIsEditing(false)}
                        className="w-full py-4 bg-slate-800/50 border border-slate-700 text-slate-500 font-black text-[9px] uppercase tracking-[0.2em] hover:bg-red-900/20 hover:text-red-500 transition-all flex items-center justify-center gap-2"
                      >
                        <X size={14} /> Discard_Override
                      </button>
                    </>
                  )}
                  <button onClick={logout} className="w-full py-4 border border-red-900/30 text-red-900/50 font-black text-[9px] uppercase hover:text-red-500 transition-all">
                    Terminate_Session
                  </button>
                </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const InfoRow = ({ label, value, icon, readOnly }: any) => (
  <div className="flex items-start gap-4">
    <div className="mt-1 text-slate-700">{icon}</div>
    <div className="overflow-hidden">
      <div className="text-[8px] text-slate-600 uppercase font-black tracking-[0.3em] mb-2">{label}</div>
      <div className={`text-[11px] font-mono tracking-tighter uppercase italic ${readOnly ? 'text-slate-500' : 'text-slate-200 font-bold'}`}>
        {value}
      </div>
    </div>
  </div>
);

export default UserProfilePage;
