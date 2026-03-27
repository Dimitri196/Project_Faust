import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { 
  UserPlus, ShieldCheck, Mail, Fingerprint, 
  Terminal, AlertTriangle, Loader2, ArrowRight 
} from 'lucide-react';
import api from '../api/axios';
import type { AgentOnboardingRequest, UserProfile } from '../types';

const OnboardingPage = () => {
  const navigate = useNavigate();
  const [status, setStatus] = useState<'idle' | 'submitting' | 'success' | 'error'>('idle');
  const [createdProfile, setCreatedProfile] = useState<UserProfile | null>(null);
  
  const { register, handleSubmit, reset, formState: { errors } } = useForm<AgentOnboardingRequest>();

  const onSubmit = async (data: AgentOnboardingRequest) => {
    setStatus('submitting');
    try {
      // Volání tvého nového endpointu: @PostMapping("/onboard")
      const response = await api.post<UserProfile>('/profile/onboard', data);
      
      setCreatedProfile(response.data);
      setStatus('success');
      // Automatický reset formuláře pro případ další registrace
      reset(); 
    } catch (error) {
      console.error("ONBOARDING_FAILURE:", error);
      setStatus('error');
    }
  };

  return (
    <div className="min-h-full p-6 md:p-12 flex flex-col items-center justify-center">
      <div className="w-full max-w-2xl bg-brand-panel/40 backdrop-blur-xl border border-brand-border p-8 relative shadow-2xl">
        
        {/* Dekorativní ID tag */}
        <div className="absolute top-0 right-0 p-2 font-mono text-[8px] text-brand-accent/30 tracking-widest uppercase">
          Auth_Node: {window.location.hostname} // Admin_Verified
        </div>

        <div className="flex items-center gap-4 mb-10 border-b border-brand-border pb-6">
          <div className="p-3 bg-brand-accent/10 border border-brand-accent/50 text-brand-accent">
            <UserPlus size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-white italic tracking-tighter uppercase">
              Agent_Onboarding
            </h1>
            <p className="text-[10px] font-mono text-slate-500 uppercase tracking-widest">
              Protocol: New_Identity_Creation // Endpoint: /api/v1/profile/onboard
            </p>
          </div>
        </div>

        {status === 'success' && createdProfile ? (
          <div className="py-8 animate-in fade-in zoom-in duration-500 text-center">
            <div className="inline-flex items-center justify-center w-16 h-16 bg-emerald-500/10 border border-emerald-500/50 text-emerald-500 mb-6 rounded-sm">
              <ShieldCheck size={32} />
            </div>
            
            <h2 className="text-xl font-bold text-white uppercase mb-2 tracking-tight">
              Subject_Registered_Successfully
            </h2>
            
            <div className="bg-black/40 border border-brand-border p-6 my-6 text-left font-mono">
              <div className="flex justify-between mb-2">
                <span className="text-slate-500 text-[10px]">CODENAME:</span>
                <span className="text-brand-accent text-[10px]">{createdProfile.fullName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500 text-[10px]">ASSIGNED_ID:</span>
                <span className="text-white text-[10px]">{createdProfile.id.substring(0, 8)}...</span>
              </div>
            </div>

            <div className="flex flex-col gap-3">
              <button 
                onClick={() => navigate(`/personnel/${createdProfile.id}`)}
                className="w-full py-4 bg-emerald-600 text-white text-[11px] font-black uppercase tracking-[0.2em] hover:bg-emerald-500 transition-all flex items-center justify-center gap-3"
              >
                Open_Subject_Dossier <ArrowRight size={16} />
              </button>
              
              <button 
                onClick={() => { setStatus('idle'); setCreatedProfile(null); }}
                className="text-[9px] font-mono text-slate-500 uppercase hover:text-white transition-colors"
              >
                Register_Another_Asset
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* CODENAME */}
              <div className="space-y-2">
                <label className="text-[9px] font-mono text-slate-500 uppercase tracking-widest ml-1 italic">Agent_Codename</label>
                <div className="relative group">
                  <Fingerprint className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-brand-accent transition-colors" size={16} />
                  <input 
                    {...register('codename', { required: true })}
                    placeholder="e.g. MORPHEUS_9"
                    className="w-full bg-black/60 border border-brand-border py-3 pl-10 pr-4 text-xs font-mono text-white focus:border-brand-accent outline-none transition-all"
                  />
                </div>
              </div>

              {/* OFFICIAL EMAIL */}
              <div className="space-y-2">
                <label className="text-[9px] font-mono text-slate-500 uppercase tracking-widest ml-1 italic">Uplink_Email</label>
                <div className="relative group">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-600 group-focus-within:text-brand-accent transition-colors" size={16} />
                  <input 
                    {...register('officialEmail', { required: true, pattern: /^\S+@\S+$/i })}
                    placeholder="access@faust.sys"
                    className="w-full bg-black/60 border border-brand-border py-3 pl-10 pr-4 text-xs font-mono text-white focus:border-brand-accent outline-none transition-all"
                  />
                </div>
              </div>
            </div>

            {/* CLEARANCE SELECTION */}
            <div className="space-y-2">
              <label className="text-[9px] font-mono text-slate-500 uppercase tracking-widest ml-1 italic">Clearance_Authorization</label>
              <select 
                {...register('assignedLevel')}
                className="w-full bg-black/60 border border-brand-border py-3 px-4 text-xs font-mono text-brand-accent focus:border-brand-accent outline-none appearance-none cursor-pointer"
              >
                <option value="LEVEL_1_PUBLIC">LEVEL_1: PUBLIC</option>
                <option value="LEVEL_2_INTERNAL">LEVEL_2: INTERNAL</option>
                <option value="LEVEL_3_CONFIDENTIAL">LEVEL_3: CONFIDENTIAL</option>
                <option value="LEVEL_4_SECRET">LEVEL_4: SECRET</option>
                <option value="LEVEL_5_TOP_SECRET">LEVEL_5: TOP_SECRET</option>
              </select>
            </div>

            {/* FIELD ACCESS TOGGLE */}
            <label className="flex items-center gap-4 p-4 bg-brand-dark/60 border border-brand-border border-dashed cursor-pointer hover:bg-brand-accent/5 transition-all group">
              <input 
                type="checkbox" 
                {...register('requiresFieldAccess')} 
                className="w-4 h-4 rounded-none border-brand-border bg-black text-brand-accent focus:ring-0 focus:ring-offset-0"
              />
              <div className="flex-1">
                <span className="block text-[10px] font-black text-white uppercase italic group-hover:text-brand-accent transition-colors">Grant_Tactical_Field_Access</span>
                <span className="block text-[8px] font-mono text-slate-600 uppercase">Enables field uplink and biometric dossier sync</span>
              </div>
            </label>

            <button 
              type="submit"
              disabled={status === 'submitting'}
              className="w-full py-4 bg-brand-accent/10 border border-brand-accent text-brand-accent text-[11px] font-black uppercase tracking-[0.3em] hover:bg-brand-accent hover:text-white transition-all flex items-center justify-center gap-3 disabled:opacity-50 group"
            >
              {status === 'submitting' ? (
                <>
                  <Loader2 className="animate-spin" size={16} />
                  Transmitting_Data...
                </>
              ) : (
                <>
                  <Terminal size={16} className="group-hover:animate-pulse" />
                  Finalize_Registration
                </>
              )}
            </button>

            {status === 'error' && (
              <div className="p-4 bg-rose-500/10 border border-rose-500/50 flex items-center gap-3 text-rose-500 font-mono text-[9px] uppercase tracking-widest">
                <AlertTriangle size={18} /> 
                System_Error: Uplink rejected by ProfileController. Verify database connection.
              </div>
            )}
          </form>
        )}
      </div>
    </div>
  );
};

export default OnboardingPage;
