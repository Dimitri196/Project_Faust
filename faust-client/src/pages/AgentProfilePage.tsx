import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/axios';
import { 
  Shield, Terminal, Fingerprint, Activity, 
  Lock, AlertTriangle, ChevronLeft, Save, X, Edit3, Cpu
} from 'lucide-react';
import type { UserProfile, UpdateProfileRequest } from '../types';

const AgentProfilePage = () => {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState<UpdateProfileRequest>({});

  const { data: agent, isLoading } = useQuery<UserProfile>({
    queryKey: ['agent', id],
    queryFn: async () => {
      const res = await api.get(`/profile/dossier/${id}`);
      return res.data;
    }
  });

  useEffect(() => {
    if (agent) {
      setFormData({
        fullName: agent.fullName,
        role: agent.role,
        status: agent.status,
        clearance: agent.clearance,
        techStack: agent.techStack
      });
    }
  }, [agent]);

  const updateMutation = useMutation({
    mutationFn: async (data: UpdateProfileRequest) => {
      return await api.patch(`/profile/dossier/${id}`, data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agent', id] });
      setIsEditing(false);
    }
  });

  if (isLoading) return <LoadingTerminal />;

  return (
    <div className="min-h-screen bg-[#050505] text-emerald-500/90 font-mono p-4 md:p-8 selection:bg-emerald-500 selection:text-black">
      {/* SCANLINE EFFECT OVERLAY */}
      <div className="fixed inset-0 pointer-events-none z-50 opacity-[0.05] bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] bg-[length:100%_2px,3px_100%]" />

      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* TOP NAVIGATION / STATUS */}
        <div className="flex justify-between items-center border-b border-emerald-900/50 pb-4">
          <Link to="/archive" className="flex items-center gap-2 text-[10px] hover:text-emerald-300 transition-colors uppercase tracking-[0.3em]">
            <ChevronLeft size={14} /> [ RETURN_TO_ARCHIVE ]
          </Link>
          <div className="text-[10px] tracking-[0.2em] flex gap-6">
            <span className="flex items-center gap-2"><Activity size={10} className="animate-pulse" /> SYSTEM_ONLINE</span>
            <span className="text-emerald-900">|</span>
            <span>ID: {agent?.id.split('-')[0]}</span>
          </div>
        </div>

        {/* MAIN DOSSIER GRID */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          
          {/* LEFT COLUMN: IDENTITY */}
          <div className="lg:col-span-4 space-y-6">
            <div className="border border-emerald-900/50 bg-emerald-950/5 p-6 relative overflow-hidden">
              {/* Background ID Watermark */}
              <div className="absolute top-0 right-0 p-2 opacity-10">
                <Fingerprint size={120} />
              </div>

              <div className="relative z-10">
                <div className="w-full aspect-square bg-black border border-emerald-900 mb-6 flex items-center justify-center relative group">
                  <Fingerprint size={80} className="text-emerald-900 group-hover:text-emerald-500 transition-colors duration-700" />
                  <div className="absolute inset-0 bg-gradient-to-t from-emerald-900/20 to-transparent" />
                  {/* Digital Crosshair Decoration */}
                  <div className="absolute top-2 left-2 w-4 h-4 border-t border-l border-emerald-500/50" />
                  <div className="absolute bottom-2 right-2 w-4 h-4 border-b border-r border-emerald-500/50" />
                </div>

                {isEditing ? (
                  <div className="space-y-4">
                    <input 
                      className="w-full bg-black border border-emerald-500 p-2 text-emerald-400 text-sm focus:ring-1 ring-emerald-500 outline-none"
                      value={formData.fullName}
                      onChange={e => setFormData({...formData, fullName: e.target.value})}
                    />
                    <input 
                      className="w-full bg-black border border-emerald-500 p-2 text-emerald-400 text-xs outline-none"
                      value={formData.role}
                      onChange={e => setFormData({...formData, role: e.target.value as UpdateProfileRequest['role']})}
                    />
                  </div>
                ) : (
                  <div className="space-y-1">
                    <h2 className="text-2xl font-bold uppercase tracking-tighter text-emerald-400">{agent?.fullName}</h2>
                    <p className="text-[10px] text-emerald-700 font-bold uppercase tracking-widest">{agent?.role}</p>
                  </div>
                )}
              </div>
            </div>

            {/* STATUS CARDS */}
            <div className="border border-emerald-900/30 bg-emerald-950/5 p-4 space-y-4">
               <StatusRow label="Clearance" value={agent?.clearance || ''} critical={agent?.clearance.includes('LEVEL_5')} />
               <StatusRow label="Status" value={agent?.status || ''} />
               <StatusRow label="Network" value={agent?.admin ? 'ROOT_ADMIN' : 'RESRICTED'} />
            </div>
          </div>

          {/* RIGHT COLUMN: CORE DATA */}
          <div className="lg:col-span-8 space-y-6">
            
            {/* CAPABILITY SECTION */}
            <section className="border border-emerald-900/50 bg-black p-8 relative">
              <div className="flex justify-between items-center mb-8 border-b border-emerald-900/30 pb-2">
                <h3 className="text-xs font-black flex items-center gap-3 tracking-[0.4em] text-emerald-500/50">
                  <Cpu size={16} /> OPERATIVE_TECH_STACK
                </h3>
                {!isEditing ? (
                  <button onClick={() => setIsEditing(true)} className="text-[9px] border border-emerald-500/50 px-3 py-1 hover:bg-emerald-500 hover:text-black transition-all uppercase">
                    Modify_Entry
                  </button>
                ) : (
                  <div className="flex gap-2">
                    <button onClick={() => updateMutation.mutate(formData)} className="text-[9px] bg-emerald-500 text-black px-3 py-1 font-bold">COMMIT</button>
                    <button onClick={() => setIsEditing(false)} className="text-[9px] border border-red-900 text-red-500 px-3 py-1">ABORT</button>
                  </div>
                )}
              </div>

              {isEditing ? (
                <div className="space-y-4">
                  <p className="text-[9px] text-emerald-800 uppercase">[!] Input protocols separated by comma</p>
                  <textarea 
                    className="w-full bg-emerald-950/10 border border-emerald-800 p-4 text-emerald-400 text-xs outline-none focus:border-emerald-500"
                    rows={6}
                    value={formData.techStack?.join(', ')}
                    onChange={e => setFormData({...formData, techStack: e.target.value.split(',').map(s => s.trim())})}
                  />
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                  {agent?.techStack.map((tech, i) => (
                    <div key={i} className="flex items-center gap-3 border border-emerald-900/20 p-3 bg-emerald-950/5 group hover:border-emerald-500/40 transition-colors">
                      <div className="w-1.5 h-1.5 bg-emerald-500 opacity-20 group-hover:opacity-100 transition-opacity" />
                      <span className="text-[10px] font-bold uppercase tracking-widest">{tech}</span>
                    </div>
                  ))}
                </div>
              )}
            </section>

            {/* SECURITY LOGS / FOOTER */}
            <section className="border border-red-900/20 bg-red-950/5 p-6">
              <div className="flex items-start gap-4">
                <Lock className="text-red-900 mt-1" size={18} />
                <div className="text-[9px] leading-relaxed text-red-900 uppercase tracking-widest">
                  <p>WARNING: Unauthorized modification of operative records is a federal offense.</p>
                  <p className="mt-1 opacity-50">Current Access Session: [ROOT_OVERRIDE_ACTIVE]</p>
                  <p className="opacity-50">Location Log: UNKNOWN_SECTOR // IP_MASKED</p>
                </div>
              </div>
            </section>

          </div>
        </div>
      </div>
    </div>
  );
};


const StatusRow = ({ label, value, critical }: { label: string, value: string, critical?: boolean }) => (
  <div className="flex justify-between items-center text-[10px] uppercase tracking-widest border-b border-emerald-900/10 py-2">
    <span className="text-emerald-900">{label}</span>
    <span className={critical ? 'text-red-500 font-black' : 'text-emerald-400'}>{value}</span>
  </div>
);

const LoadingTerminal = () => (
  <div className="h-screen bg-black flex flex-col items-center justify-center font-mono text-emerald-500">
    <Terminal size={40} className="animate-pulse mb-4" />
    <div className="text-[10px] tracking-[0.5em] uppercase">Decrypting_Personnel_Dossier...</div>
  </div>
);

export default AgentProfilePage;
