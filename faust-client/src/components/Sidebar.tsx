import { useState, useEffect } from 'react';
import { Users, Building2, Shield, LayoutDashboard, Terminal, Activity, Globe, Clock } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';

const Sidebar = () => {
  const location = useLocation();
  const [time, setTime] = useState(new Date());

  // Real-time clock update
  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const menuItems = [
    { name: 'DASHBOARD', path: '/', icon: LayoutDashboard },
    { name: 'PERSONNEL', path: '/people', icon: Users },
    { name: 'INSTITUTIONS', path: '/institutions', icon: Building2 },
    { name: 'HIERARCHY', path: '/hierarchy', icon: Shield },
  ];

  return (
    <div className="w-64 bg-brand-panel/80 border-r border-brand-border flex flex-col h-full backdrop-blur-xl z-30">
      {/* HEADER: Classification & Branding */}
      <div className="p-6 border-b border-brand-border bg-brand-dark/40">
        <div className="flex items-center gap-2 mb-2 text-brand-accent">
          <Terminal size={14} className="animate-pulse" />
          <span className="text-[9px] font-mono uppercase tracking-[0.3em]">Clearance: Lvl_04</span>
        </div>
        <h1 className="text-2xl font-black text-white tracking-tighter italic leading-none">
          FAUST_<span className="text-brand-accent">PRJ</span>
        </h1>
        <p className="text-[9px] text-slate-500 font-mono mt-2 uppercase tracking-widest leading-tight">
          Intelligence Archive Deployment
        </p>
      </div>
      
      {/* NAVIGATION: Primary Systems */}
      <nav className="flex-1 p-4 space-y-1 mt-4">
        <p className="text-[10px] font-mono text-slate-600 mb-4 px-4 tracking-[0.2em]">MAIN_MODULES</p>
        {menuItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center gap-3 px-4 py-3 rounded-sm transition-all duration-300 group relative ${
                isActive 
                  ? 'bg-brand-accent/5 text-brand-accent border-l-2 border-brand-accent' 
                  : 'text-slate-500 hover:text-slate-200 hover:bg-white/5'
              }`}
            >
              <item.icon 
                size={18} 
                className={`transition-transform duration-500 ${isActive ? 'rotate-[360deg] scale-110' : 'group-hover:scale-110'}`} 
              />
              <span className="font-mono text-xs font-bold tracking-widest">
                {item.name}
              </span>
              {isActive && (
                <div className="absolute right-4 w-1 h-1 bg-brand-accent rounded-full animate-ping" />
              )}
            </Link>
          );
        })}
      </nav>

      {/* FOOTER: System Telemetry (Live Data) */}
      <div className="p-6 border-t border-brand-border bg-brand-dark/40 font-mono space-y-4">
        {/* System Time & Date */}
        <div className="space-y-1">
          <div className="flex items-center justify-between text-[10px] text-slate-400">
            <span className="flex items-center gap-2">
              <Clock size={10} className="text-brand-accent" />
              TIMESTAMP
            </span>
          </div>
          <div className="text-sm font-black text-white tracking-widest">
            {time.toLocaleTimeString('en-GB', { hour12: false })}
          </div>
          <div className="text-[9px] text-slate-600 uppercase tracking-tighter">
            {time.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}
          </div>
        </div>

        {/* Location Data */}
        <div className="space-y-1">
          <div className="flex items-center justify-between text-[10px] text-slate-400">
            <span className="flex items-center gap-2">
              <Globe size={10} className="text-brand-accent" />
              LOCATION
            </span>
          </div>
          <div className="text-[10px] font-bold text-slate-300 tracking-wider">
            PRAGUE // CZECHIA
          </div>
        </div>

        {/* Uplink Status */}
        <div className="pt-2 border-t border-brand-border/30">
          <div className="flex items-center justify-between text-[9px] text-slate-500 uppercase tracking-tighter mb-2">
            <span className="flex items-center gap-2">
              <Activity size={10} className="text-brand-success" />
              Uplink: Active
            </span>
            <span className="text-brand-accent/40">v1.0.4</span>
          </div>
          <div className="h-1 w-full bg-slate-900 rounded-full overflow-hidden">
            <div className="h-full bg-brand-accent/30 w-3/4 animate-pulse shadow-[0_0_8px_rgba(59,130,246,0.5)]" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Sidebar;