import { useState, useEffect } from 'react';
import { 
  LayoutDashboard, Activity, Fingerprint, 
  GitGraph, Briefcase, Database, LogOut,
  ChevronRight, ShieldAlert, Globe, Building2,
  Network, Search, UserPlus, Users, MapPin, 
  Zap, BrainCircuit // Přidány ikony pro Intelligence
} from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Sidebar = () => {
  const location = useLocation();
  const { user: authUser, logout } = useAuth();
  const [time, setTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div className="w-64 bg-brand-panel border-r border-brand-border flex flex-col h-full backdrop-blur-xl z-30 shadow-[4px_0_24px_rgba(0,0,0,0.5)] font-sans select-none">

      {/* HEADER: Classification & Branding */}
      <div className="p-6 border-b border-brand-border bg-brand-dark/40 relative overflow-hidden group">
        <Fingerprint size={80} className="absolute -right-4 -top-4 opacity-[0.03] text-brand-accent rotate-12 group-hover:opacity-[0.07] transition-opacity duration-700 pointer-events-none" />
        <div className="flex items-center gap-2 mb-2 text-brand-accent relative z-10 font-mono animate-pulse-glow">
          <ShieldAlert size={14} />
          <span className="text-[9px] uppercase tracking-[0.3em] font-bold">
            Clearance: {authUser?.clearance?.split('_')[1] || '00'}
          </span>
        </div>
        <h1 className="text-2xl font-black text-white tracking-tighter italic leading-none relative z-10">
          FAUST_<span className="text-brand-accent font-light uppercase tracking-widest">Sys</span>
        </h1>
        <p className="text-[8px] text-slate-500 font-mono mt-1 uppercase tracking-[0.2em] relative z-10">Global Influence Tracker</p>
      </div>

      {/* NAVIGATION CONTENT */}
      <nav className="flex-1 p-4 space-y-7 overflow-y-auto custom-scrollbar">
        
        {/* SECTION 1: GLOBAL OVERVIEW */}
        <section>
          <p className="text-[9px] font-mono text-slate-600 mb-3 px-4 tracking-[0.3em] uppercase font-bold flex items-center gap-2">
            <Globe size={10} className="text-brand-accent" /> Strategic_Overview
          </p>
          <div className="space-y-1">
            <SidebarLink 
              name="DASHBOARD" 
              path="/" 
              icon={LayoutDashboard} 
              isActive={location.pathname === '/'} 
            />
            
            {/* INTELLIGENCE HUD LINK - Nově přidaný pro globální přístup */}
            <SidebarLink 
              name="INTELLIGENCE_HUD" 
              path="/terminal" // Směřuje na váš GlobalSearchTerminal nebo prázdný terminál
              icon={BrainCircuit} 
              isActive={location.pathname.startsWith('/terminal') || location.pathname.startsWith('/intelligence')} 
              accentColor="cyan"
            />

            <SidebarLink 
              name="GLOBAL_HIERARCHY" 
              path="/hierarchy" 
              icon={GitGraph} 
              isActive={location.pathname.startsWith('/hierarchy')} 
            />
            <SidebarLink 
              name="GEOSPATIAL_SECTORS" 
              path="/locations" 
              icon={MapPin} 
              isActive={location.pathname.startsWith('/locations')} 
            />
          </div>
        </section>

        {/* SECTION 2: ASSETS DATABASE */}
        <section>
          <p className="text-[9px] font-mono text-slate-600 mb-3 px-4 tracking-[0.3em] uppercase font-bold flex items-center gap-2">
            <Database size={10} /> Intelligence_Assets
          </p>
          <div className="space-y-1">
            <SidebarLink 
              name="AGENT_ARCHIVE" 
              path="/archive" 
              icon={ShieldAlert} 
              isActive={location.pathname === '/archive'} 
            />
            <SidebarLink 
              name="PERSONNEL_REGISTRY" 
              path="/personnel" 
              icon={Search} 
              isActive={location.pathname === '/personnel' || location.pathname.startsWith('/personnel/')} 
            />
            <SidebarLink 
              name="INSTITUTIONS" 
              path="/institutions" 
              icon={Building2} 
              isActive={location.pathname.startsWith('/institutions')} 
            />
            <SidebarLink 
              name="OCCUPATIONS_MAP" 
              path="/occupations" 
              icon={Briefcase} 
              isActive={location.pathname.startsWith('/occupations')} 
            />
          </div>
        </section>

        {/* SECTION 3: OPERATIONS */}
        <section>
          <p className="text-[9px] font-mono text-orange-500/50 mb-3 px-4 tracking-[0.3em] uppercase font-bold flex items-center gap-2">
            <Activity size={10} /> Field_Operations
          </p>
          <div className="space-y-1">
            <SidebarLink 
              name="AGENT_ONBOARDING" 
              path="/onboard" 
              icon={UserPlus} 
              isActive={location.pathname.startsWith('/onboard')}
              accentColor="orange" 
            />
          </div>
        </section>
      </nav>

      {/* USER PROFILE & LOGOUT */}
      <div className="px-4 py-4 border-t border-brand-border bg-brand-dark/20">
        <div className="bg-brand-dark border border-brand-border p-3 rounded-sm relative group hover:border-brand-accent/50 transition-all">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-brand-panel border border-brand-border flex items-center justify-center rounded-sm group-hover:bg-brand-accent/5">
              <Users size={18} className="text-slate-500 group-hover:text-brand-accent transition-colors" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[10px] font-black text-white truncate uppercase italic">{authUser?.fullName || 'IDENT_PENDING'}</p>
              <p className="text-[8px] font-mono text-brand-accent truncate tracking-widest uppercase opacity-70">Operator</p>
            </div>
            <button 
              onClick={logout} 
              className="p-1.5 text-slate-700 hover:text-rose-500 transition-all"
            >
              <LogOut size={14} />
            </button>
          </div>
          <Link to="/profile" className="mt-3 flex items-center justify-between text-[8px] font-mono text-slate-500 hover:text-white transition-colors uppercase tracking-[0.2em] pt-2 border-t border-brand-border/30">
            <span>Access_My_Dossier</span>
            <ChevronRight size={10} />
          </Link>
        </div>
      </div>

      {/* TELEMETRY FOOTER */}
      <div className="p-6 border-t border-brand-border bg-brand-dark/40 font-mono">
        <div className="text-sm font-black text-white tracking-widest tabular-nums italic mb-4">
          {time.toLocaleTimeString('en-GB', { hour12: false })}
        </div>
        <div className="flex items-center justify-between text-[8px] text-slate-600 uppercase mb-2">
          <span>Data_Link:</span>
          <span className="text-emerald-500 animate-pulse">ESTABLISHED</span>
        </div>
        <div className="h-[1px] w-full bg-slate-900 overflow-hidden">
          <div className="h-full bg-brand-accent w-1/2 animate-shimmer" />
        </div>
      </div>
    </div>
  );
};

// --- SUB-COMPONENT ---

const SidebarLink = ({ name, path, icon: Icon, isActive, accentColor = "blue" }: any) => {
  const isOrange = accentColor === "orange";
  const isCyan = accentColor === "cyan";
  
  return (
    <Link
      to={path}
      className={`flex items-center gap-3 px-4 py-2.5 rounded-sm transition-all duration-300 group relative overflow-hidden ${
        isActive
          ? isOrange ? 'bg-orange-500/10 text-orange-500' : isCyan ? 'bg-cyan-500/10 text-cyan-400' : 'bg-brand-accent/10 text-brand-accent'
          : 'text-slate-500 hover:text-slate-200 hover:bg-white/5'
      }`}
    >
      {isActive && (
        <div className={`absolute left-0 top-0 bottom-0 w-[2px] ${
          isOrange ? 'bg-orange-500 shadow-[0_0_8px_rgba(249,115,22,0.8)]' : 
          isCyan ? 'bg-cyan-400 shadow-[0_0_8px_rgba(34,211,238,0.8)]' :
          'bg-brand-accent shadow-[0_0_8px_rgba(59,130,246,0.8)]'
        }`} />
      )}
      <Icon size={16} className={`transition-all duration-300 ${isActive ? 'scale-110' : 'group-hover:scale-110 group-hover:text-slate-300'}`} />
      <span className={`font-mono text-[10px] font-bold tracking-[0.2em] ${isActive ? 'text-white' : ''}`}>
        {name}
      </span>
    </Link>
  );
};

export default Sidebar;