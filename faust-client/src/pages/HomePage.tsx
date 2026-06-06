import React from 'react';
import {
  Terminal, Shield, Database, Network,
  Activity, Lock, Eye, Globe, User
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';


const HomePage = () => {
  const { user } = useAuth();

  return (
    <div className="relative h-full w-full overflow-y-auto bg-brand-dark text-slate-200 flex flex-col items-center animate-in fade-in duration-1000 select-none custom-scrollbar">

      {/* CRT SCANLINE & NOISE OVERLAY */}
      <div className="fixed inset-0 pointer-events-none z-50 overflow-hidden">
        {/* Scanlines */}
        <div className="absolute inset-0 bg-[linear-gradient(rgba(18,16,16,0)_50%,rgba(0,0,0,0.1)_50%),linear-gradient(90deg,rgba(255,0,0,0.01),rgba(0,255,0,0.01),rgba(0,0,255,0.01))] bg-[length:100%_2px,3px_100%]" />

        {/* Subtle Static Noise - OPRAVENO: Použito neprůstřelné inline SVG bez síťového požadavku */}
        <div
          className="absolute inset-0 opacity-[0.02]"
          style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E")`
          }}
        />
      </div>

      <div className="relative z-10 w-full max-w-7xl px-8 lg:px-12 flex flex-col min-h-full">

        {/* TOP STATUS BAR - Brutalist style */}
        <div className="flex justify-between items-center py-4 border-b border-brand-border/30 w-full font-mono text-[10px]">
          <div className="flex items-center gap-6 tracking-[0.2em] text-slate-500">
            <span className="flex items-center gap-2">
              <div className="w-1 h-1 bg-brand-accent animate-pulse" />
              SYS_STATUS: <span className="text-brand-accent">NOMINAL</span>
            </span>
            <span className="hidden md:inline italic">LOC_NODE: PRG_BUNKER_01</span>
          </div>
          <div className="flex items-center gap-6 text-brand-accent/60 tracking-widest uppercase">
            <span>Auth_Level: {user?.clearance?.split('_')[1] || '00'}</span>
            <span className="text-slate-600">Enc: RSA_4096</span>
          </div>
        </div>

        {/* HERO SECTION - The "Agency" Core */}
        <div className="flex-1 flex flex-col items-center justify-center py-16 text-center group">
          <div className="flex items-center gap-4 mb-4">
            <div className="h-[1px] w-8 bg-brand-border" />
            <div className="inline-flex items-center gap-2 px-2 py-0.5 border border-brand-accent/20 bg-brand-accent/5">
              <Shield size={10} className="text-brand-accent" />
              <span className="font-mono text-[9px] tracking-[0.5em] text-brand-accent uppercase font-black">
                {user ? 'Authority_Verified' : 'Access_Restricted'}
              </span>
            </div>
            <div className="h-[1px] w-8 bg-brand-border" />
          </div>

          <div className="relative mb-6">
            <h1 className="text-8xl md:text-9xl font-black tracking-[-0.05em] text-white leading-none italic uppercase">
              PROJECT <span className="text-brand-accent drop-shadow-[0_0_15px_rgba(59,130,246,0.4)]">FAUST</span>
            </h1>
            <div className="absolute -bottom-2 right-0 font-mono text-[9px] text-slate-600 tracking-[0.2em] uppercase font-bold">
              Arch_Node: [DPT_77-X]
            </div>
          </div>

          <div className="max-w-2xl text-left border-l-2 border-brand-accent/40 pl-6 py-2 mb-12">
            <p className="text-sm md:text-base text-slate-400 font-mono leading-relaxed tracking-tight">
              <span className="text-white font-black">&gt; SYNOPSIS:</span> Multi-node intelligence orchestration.
              Real-time institutional mapping and personnel nexus reconstruction for <span className="text-slate-200">state security apparatus</span>.
              All interactions are logged under <span className="text-brand-accent/80 border-b border-brand-accent/20">Protocol_09</span>.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-0 w-full max-w-md border border-brand-border p-1 bg-black/20">
            <Link
              to="/terminal"
              className="flex-1 group relative flex items-center justify-center gap-3 px-8 py-5 bg-brand-accent text-brand-dark font-black transition-all hover:bg-white duration-150"
            >
              <Terminal size={18} />
              <span className="tracking-[0.2em] uppercase text-xs font-mono">Omni_Search_Terminal</span>
            </Link>
            <Link
              to="/protocols"
              className="flex-1 px-8 py-5 bg-transparent text-slate-500 hover:text-white hover:bg-white/5 border-l border-brand-border transition-all font-mono text-[10px] uppercase tracking-[0.3em] font-black flex items-center justify-center"
            >
              View_Protocols
            </Link>
          </div>
        </div>

        {/* ANALYTICAL TILES */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-px bg-brand-border/20 border border-brand-border/20 mb-16 shadow-2xl">
          <FeatureTile icon={<Database />} title="Entity_Archive" code="0x44_REG" />
          <FeatureTile icon={<Network />} title="Nexus_Link" code="0x12_NEX" />
          <FeatureTile icon={<Globe />} title="Active_Nodes" code="0x88_TRK" />
        </div>

        {/* FOOTER */}
        <div className="py-8 border-t border-brand-border/20 flex flex-col md:flex-row justify-between items-center gap-4 opacity-40 font-mono text-[8px] uppercase tracking-[0.4em]">
          <div className="flex gap-8 text-slate-400">
            <span className="flex items-center gap-2">
              <Activity size={10} className="text-brand-success animate-pulse" />
              UPLINK: ACTIVE
            </span>
            <span>DATA_INTEGRITY: 100%</span>
          </div>
          <div className="flex items-center gap-4 text-slate-500 font-bold">
            <span>DEPT_ARCHIVAL_INTEL</span>
            <span className="h-3 w-[1px] bg-slate-800" />
            <span>© 2026 // FAUST_PRJ</span>
          </div>
        </div>
      </div>
    </div>
  );
};

const FeatureTile = ({ icon, title, code }: { icon: any, title: string, code: string }) => (
  <div className="bg-brand-dark p-10 flex flex-col items-center text-center group hover:bg-brand-accent/[0.02] transition-all cursor-crosshair relative overflow-hidden">
    <div className="absolute top-2 right-2 font-mono text-[7px] text-slate-800 tracking-tighter group-hover:text-brand-accent/50 transition-colors uppercase font-black">
      Ref_{code}
    </div>
    <div className="text-slate-600 group-hover:text-brand-accent mb-5 transition-all duration-700 group-hover:rotate-[360deg] group-hover:scale-110">
      {React.cloneElement(icon, { size: 28, strokeWidth: 1.5 })}
    </div>
    <h3 className="text-[10px] font-mono font-black text-slate-500 group-hover:text-white tracking-[0.4em] uppercase transition-colors">
      {title}
    </h3>
    {/* Minimalist corner marker */}
    <div className="absolute bottom-0 right-0 w-1.5 h-1.5 bg-brand-border group-hover:bg-brand-accent transition-colors" />
  </div>
);

export default HomePage;
