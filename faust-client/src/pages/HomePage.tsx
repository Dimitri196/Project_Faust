import React from 'react';
import { 
  Terminal, Shield, Database, Network, ChevronRight, 
  Activity, Zap, Lock, Eye, Globe, User 
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const HomePage = () => {
  const { user } = useAuth();

  return (
    <div className="relative h-full w-full overflow-y-auto bg-brand-dark text-slate-200 flex flex-col items-center animate-in fade-in duration-1000">
      
      {/* BACKGROUND SCANNERS */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.1)_50%),linear-gradient(90deg,rgba(255,0,0,0.02),rgba(0,255,0,0.01),rgba(0,0,255,0.02))] z-50 bg-[length:100%_2px,3px_100%] pointer-events-none" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-brand-accent/5 blur-[120px] rounded-full" />
      </div>

      <div className="relative z-10 w-full max-w-7xl px-8 lg:px-12 flex flex-col min-h-full">
        
        {/* TOP STATUS BAR */}
        <div className="flex justify-between items-center py-6 border-b border-brand-border/50 w-full">
          <div className="flex items-center gap-4 font-mono text-[10px] tracking-widest text-slate-500">
            <span className="flex items-center gap-2">
              <div className="w-1 h-1 bg-brand-accent animate-ping" /> 
              UPLINK_STABLE
            </span>
            <span className="hidden md:inline">IP: 142.251.36.46</span>
          </div>
          <div className="flex items-center gap-6 font-mono text-[10px] text-brand-accent/60">
            <span>SEC_LEVEL: {user?.clearance.split('_')[1] || '00'}</span>
            <span className="text-slate-600">v1.0.4-STABLE</span>
          </div>
        </div>

        {/* IDENTITY CARD SECTION */}
        <div className="mt-8 flex justify-end w-full">
          {user ? (
            <div className="relative flex items-center gap-5 p-4 border border-brand-accent/30 bg-brand-accent/5 backdrop-blur-md overflow-hidden min-w-[320px]">
              {/* Scanline Animation Effect */}
              <div className="absolute inset-0 bg-gradient-to-b from-transparent via-brand-accent/10 to-transparent h-1/2 w-full animate-pulse pointer-events-none" />
              
              {/* Avatar Slot */}
              <div className="relative shrink-0">
                <div className="w-14 h-14 border border-brand-accent/50 bg-brand-dark flex items-center justify-center">
                  <User size={28} className="text-brand-accent/70" />
                </div>
                <div className="absolute -top-1 -left-1 w-2 h-2 border-t border-l border-brand-accent" />
                <div className="absolute -bottom-1 -right-1 w-2 h-2 border-b border-r border-brand-accent" />
              </div>

              {/* Identity Info */}
              <div className="flex flex-col">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-[8px] text-brand-accent tracking-widest uppercase">Operator_Verified</span>
                  <div className="h-px flex-1 bg-brand-accent/20 min-w-[40px]" />
                </div>
                <h2 className="text-lg font-bold text-white tracking-tight uppercase leading-tight">
                  {user.fullName}
                </h2>
                <div className="flex items-center gap-3 mt-1">
                  <span className="font-mono text-[9px] font-black text-brand-dark bg-brand-accent px-1.5 py-0.5 rounded-sm italic">
                    {user.clearance}
                  </span>
                  <span className="font-mono text-[9px] text-slate-500 uppercase">
                    {user.role}
                  </span>
                </div>
              </div>
            </div>
          ) : (
            <div className="font-mono text-[10px] text-red-500/50 flex items-center gap-2 border border-red-500/20 px-4 py-2 bg-red-500/5">
              <Lock size={12} /> SESSION_UNVERIFIED // ACCESS_RESTRICTED
            </div>
          )}
        </div>

        {/* HERO SECTION */}
        <div className="flex-1 flex flex-col items-center justify-center py-16 text-center">
          <div className="inline-flex items-center gap-3 px-3 py-1 bg-brand-accent/10 border border-brand-accent/20 rounded-sm mb-6">
            <Shield size={12} className="text-brand-accent" />
            <span className="font-mono text-[10px] tracking-[0.4em] text-brand-accent uppercase">
              {user ? 'Authority Active' : 'System Locked'}
            </span>
          </div>
          
          <h1 className="text-7xl md:text-9xl font-black tracking-tighter text-white mb-6 leading-none italic">
            PROJECT <span className="text-brand-accent drop-shadow-[0_0_30px_rgba(59,130,246,0.3)]">FAUST</span>
          </h1>
          
          <p className="text-xl md:text-2xl text-slate-400 font-light leading-relaxed max-w-3xl mb-12">
            Intelligence orchestration platform for <span className="text-white font-medium">state security apparatus</span>. 
            Real-time tracking of institutional power nodes and personnel nexus.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 w-full max-w-md">
            <Link 
              to="/people" 
              className="flex-1 group relative flex items-center justify-center gap-3 px-8 py-4 bg-brand-accent text-white font-bold rounded-sm transition-all hover:bg-blue-600 active:scale-95 shadow-[0_0_20px_rgba(59,130,246,0.3)]"
            >
              <Eye size={18} />
              <span className="tracking-widest uppercase text-xs">Enter Database</span>
            </Link>
            <button className="flex-1 px-8 py-4 border border-brand-border hover:border-brand-accent/50 text-slate-400 hover:text-white transition-all font-mono text-xs uppercase tracking-widest bg-brand-panel/20 backdrop-blur-sm">
              View Protocols
            </button>
          </div>
        </div>

        {/* ANALYTICAL TILES */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-px bg-brand-border/30 border border-brand-border/30 mb-20 shadow-2xl">
          <FeatureTile icon={<Database />} title="Entities" code="REG_ENTRY" />
          <FeatureTile icon={<Network />} title="Nexus" code="RECURSION" />
          <FeatureTile icon={<Globe />} title="Active" code="TRK_LIVE" />
        </div>

        {/* FOOTER */}
        <div className="py-8 border-t border-brand-border/30 flex flex-col md:flex-row justify-between items-center gap-4 opacity-40 grayscale hover:grayscale-0 transition-all duration-500">
          <div className="flex gap-8 font-mono text-[9px] uppercase tracking-widest">
            <span>MI5_CORE_SYNC: {user ? 'OK' : 'WAIT'}</span>
            <span>ENCRYPTION: AES_256_GCM</span>
          </div>
          <span className="font-mono text-[9px]">DEPT_ARCHIVAL_INTEL © 2026</span>
        </div>
      </div>
    </div>
  );
};

// Sub-component pro dlaždice (Tiles)
const FeatureTile = ({ icon, title, code }: { icon: any, title: string, code: string }) => (
  <div className="bg-brand-dark p-8 flex flex-col items-center text-center group hover:bg-brand-panel/30 transition-all cursor-crosshair relative overflow-hidden">
    <div className="absolute top-2 right-2 font-mono text-[7px] text-slate-700 tracking-tighter">{code}</div>
    <div className="text-brand-accent/50 group-hover:text-brand-accent mb-4 transition-colors duration-500 group-hover:scale-110">
      {icon}
    </div>
    <h3 className="text-xs font-mono font-bold text-slate-400 group-hover:text-white tracking-[0.3em] uppercase transition-colors">
      {title}
    </h3>
  </div>
);

export default HomePage;