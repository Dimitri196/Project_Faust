import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios';
import type { UserProfile, ClearanceLevel } from '../types';
import { 
  Users, Search, ChevronRight, 
  Shield, Activity, Fingerprint, RefreshCw
} from 'lucide-react';

const ArchivePage = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterLevel, setFilterLevel] = useState<ClearanceLevel | 'ALL'>('ALL');

  const { data: archive, isLoading } = useQuery<UserProfile[]>({
    queryKey: ['archive'],
    queryFn: async () => {
      // Endpoint v Java ProfileController: @GetMapping("/archive")
      const res = await api.get('/profile/archive');
      return res.data;
    }
  });

  const getLevelColor = (level: ClearanceLevel) => {
    const colors: Record<ClearanceLevel, string> = {
      LEVEL_1_PUBLIC: 'text-slate-500 border-slate-800',
      LEVEL_2_INTERNAL: 'text-blue-400 border-blue-900/50',
      LEVEL_3_CONFIDENTIAL: 'text-cyan-400 border-cyan-900/50',
      LEVEL_4_SECRET: 'text-purple-400 border-purple-900/50',
      LEVEL_5_TOP_SECRET: 'text-red-500 border-red-900/50 shadow-[0_0_10px_rgba(239,68,68,0.1)]',
    };
    return colors[level] || colors.LEVEL_1_PUBLIC;
  };

  const filteredArchive = archive?.filter(user => {
    const matchesSearch = user.fullName.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          user.role.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterLevel === 'ALL' || user.clearance === filterLevel;
    return matchesSearch && matchesFilter;
  });

  if (isLoading) return (
    <div className="h-full flex flex-col items-center justify-center bg-brand-dark font-mono text-[11px] text-brand-accent/50 tracking-[0.3em]">
      <RefreshCw size={32} className="mb-4 animate-spin text-brand-accent/30" />
      SYNCHRONIZING_ARCHIVE_NODE...
    </div>
  );

  return (
    <div className="min-h-full bg-brand-dark p-6 md:p-10 relative overflow-hidden">
      <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')] opacity-[0.03] pointer-events-none" />

      <div className="max-w-6xl mx-auto relative z-10">
        
        {/* HEADER SECTION */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-12 border-b border-brand-border pb-8">
          <div>
            <div className="flex items-center gap-2 text-brand-accent mb-2 font-mono text-[10px] tracking-[0.4em] uppercase">
              <Shield size={14} /> Faust_Asset_Archive
            </div>
            <h1 className="text-5xl font-black text-white tracking-tighter uppercase italic">
              Registry <span className="text-slate-700">Archive</span>
            </h1>
          </div>

          <div className="flex flex-wrap gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-600" size={16} />
              <input 
                type="text"
                placeholder="SEARCH_BY_AGENT_NAME..."
                className="bg-black/40 border border-brand-border py-2 pl-10 pr-4 rounded-sm text-xs font-mono text-brand-accent focus:border-brand-accent/50 outline-none w-64 uppercase tracking-widest"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <select 
              className="bg-black/40 border border-brand-border py-2 px-4 rounded-sm text-[10px] font-mono uppercase tracking-widest outline-none text-slate-400 focus:border-brand-accent/50"
              value={filterLevel}
              onChange={(e) => setFilterLevel(e.target.value as any)}
            >
              <option value="ALL">All_Clearance</option>
              <option value="LEVEL_1_PUBLIC">Level_1</option>
              <option value="LEVEL_2_INTERNAL">Level_2</option>
              <option value="LEVEL_3_CONFIDENTIAL">Level_3</option>
              <option value="LEVEL_4_SECRET">Level_4</option>
              <option value="LEVEL_5_TOP_SECRET">Level_5</option>
            </select>
          </div>
        </div>

        {/* TABLE SECTION */}
        <div className="bg-brand-panel/20 backdrop-blur-sm border border-brand-border rounded-sm overflow-hidden shadow-2xl">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-black/40 border-b border-brand-border">
                <th className="p-4 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Operative</th>
                <th className="p-4 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Security_Level</th>
                <th className="p-4 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500">Current_Status</th>
                <th className="p-4 font-mono text-[9px] uppercase tracking-[0.2em] text-slate-500 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-brand-border/30">
              {filteredArchive?.map((agent) => (
                <tr 
                  key={agent.id}
                  /* KLÍČOVÁ ZMĚNA: Navigujeme na /agents/ místo /personnel/ */
                  onClick={() => navigate(`/agents/${agent.id}`)}
                  className="group hover:bg-brand-accent/5 transition-all cursor-pointer"
                >
                  <td className="p-4">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 bg-brand-dark border border-brand-border flex items-center justify-center rounded-sm text-slate-700 group-hover:text-brand-accent group-hover:border-brand-accent/30 transition-all">
                        <Fingerprint size={16} />
                      </div>
                      <div>
                        <div className="text-xs font-black text-slate-200 uppercase tracking-tight group-hover:text-white">{agent.fullName}</div>
                        <div className="text-[10px] text-slate-600 font-mono italic">{agent.role}</div>
                      </div>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`px-3 py-1 border text-[9px] font-black font-mono tracking-widest rounded-sm ${getLevelColor(agent.clearance)}`}>
                      {agent.clearance.replace('LEVEL_', 'L_')}
                    </span>
                  </td>
                  <td className="p-4">
                    <div className="flex items-center gap-2 text-[10px] font-mono">
                      <div className={`w-1.5 h-1.5 rounded-full animate-pulse ${agent.status === 'OPERATIONAL' ? 'bg-emerald-500' : 'bg-slate-700'}`} />
                      <span className={agent.status === 'OPERATIONAL' ? 'text-emerald-500/80' : 'text-slate-600'}>{agent.status}</span>
                    </div>
                  </td>
                  <td className="p-4 text-right text-slate-700 group-hover:text-brand-accent transition-all">
                    <ChevronRight size={18} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          
          {filteredArchive?.length === 0 && (
            <div className="p-20 text-center font-mono text-[10px] text-slate-600 uppercase tracking-[0.3em]">
              No_Results_Found_In_Archive
            </div>
          )}
        </div>

        {/* FOOTER STATS */}
        <div className="mt-8 grid grid-cols-1 sm:grid-cols-3 gap-4">
           <StatMini label="Total_Assets" value={archive?.length || 0} icon={<Users size={12}/>} />
           <StatMini label="Active_Ops" value={archive?.filter(a => a.status === 'OPERATIONAL').length || 0} icon={<Activity size={12}/>} />
           <StatMini label="Admin_Nodes" value={archive?.filter(a => a.isAdmin).length || 0} icon={<Shield size={12}/>} />
        </div>
      </div>
    </div>
  );
};

const StatMini = ({ label, value, icon }: { label: string, value: number, icon: React.ReactNode }) => (
  <div className="bg-brand-panel/20 border border-brand-border p-4 rounded-sm flex items-center justify-between">
    <div className="flex items-center gap-3 text-slate-600">
      {icon}
      <span className="text-[9px] font-mono uppercase tracking-widest">{label}</span>
    </div>
    <span className="text-lg font-black text-white font-mono">{value}</span>
  </div>
);

export default ArchivePage;