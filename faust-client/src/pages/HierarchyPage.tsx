import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import type { OccupationTreeResponse } from '../types';
import { ShieldAlert, User, UserMinus, GitMerge, ChevronRight, ChevronDown, Activity, Target } from 'lucide-react';
import { useState } from 'react';

// REKURZIVNÍ KOMPONENTA PRO POZICE
const OccupationNode = ({ node, depth }: { node: OccupationTreeResponse; depth: number }) => {
  const [isOpen, setIsOpen] = useState(true);
  const hasSubordinates = node.subordinates && node.subordinates.length > 0;

  // Dynamické barvy podle kategorie z tvého nového typu OccupationCategory
  const getCategoryColor = (cat: string) => {
    switch (cat) {
      case 'POLITICAL': return 'text-purple-400 border-purple-900/50 bg-purple-500/5';
      case 'TECHNICAL': return 'text-blue-400 border-blue-900/50 bg-blue-500/5';
      case 'ADVISORY': return 'text-emerald-400 border-emerald-900/50 bg-emerald-500/5';
      case 'MILITARY': return 'text-orange-400 border-orange-900/50 bg-orange-500/5';
      default: return 'text-slate-500 border-slate-800 bg-slate-500/5';
    }
  };

  return (
    <div className="relative">
      {/* Vizuální linka hierarchie */}
      {depth > 0 && (
        <div className="absolute -left-4 top-0 bottom-0 w-px bg-brand-border/20" />
      )}
      
      <div className={`
        group flex items-center gap-4 p-3 mb-1 transition-all border-l-2
        ${node.isVacant 
          ? 'bg-red-500/5 border-red-500/50 animate-in fade-in' 
          : 'bg-brand-panel/10 border-brand-accent/30 hover:bg-brand-panel/30 hover:border-brand-accent'}
      `}>
        <button 
          onClick={() => setIsOpen(!isOpen)} 
          className={`transition-transform duration-300 ${!hasSubordinates && 'opacity-0'}`}
        >
          {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </button>

        <div className={`p-2 rounded-sm transition-colors ${node.isVacant ? 'bg-red-500/10' : 'bg-brand-accent/5'}`}>
          {node.isVacant ? (
            <UserMinus size={16} className="text-red-500 animate-pulse" />
          ) : (
            <User size={16} className="text-brand-accent group-hover:scale-110 transition-transform" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3">
            <span className={`text-xs font-black tracking-tighter uppercase truncate ${node.isVacant ? 'text-red-400' : 'text-white'}`}>
              {node.title}
            </span>
            <span className="text-[8px] font-mono text-slate-500 bg-black/60 px-1.5 py-0.5 border border-white/5 rounded-sm">
              {node.rank}
            </span>
          </div>
          <div className="text-[10px] font-mono flex items-center gap-2 mt-0.5">
            {node.isVacant ? (
              <span className="text-red-500/60 uppercase tracking-widest text-[8px] italic animate-pulse">
                [ UNASSIGNED_SLOT ]
              </span>
            ) : (
              <span className="text-brand-accent/60 tracking-tight uppercase font-bold truncate">
                {node.currentOccupantName}
              </span>
            )}
          </div>
        </div>

        {/* Category Badge z tvého OccupationCategory typu */}
        <div className={`text-[8px] font-mono uppercase px-2 py-0.5 border rounded-sm tracking-widest ${getCategoryColor(node.category)}`}>
          {node.category}
        </div>
      </div>

      {/* Subordinates Wrapper */}
      {isOpen && hasSubordinates && (
        <div className="ml-8 mt-1 space-y-1 animate-in slide-in-from-left-2 duration-300">
          {node.subordinates.map((sub) => (
            <OccupationNode key={sub.publicId} node={sub} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

const HierarchyPage = () => {
  const { data: tree, isLoading } = useQuery<OccupationTreeResponse[]>({
    queryKey: ['occupation-tree'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/occupations/tree'); 
      return res.data;
    }
  });

  // Pomocné statistiky pro header
  const totalSlots = tree ? countNodes(tree) : 0;
  const vacantSlots = tree ? countVacant(tree) : 0;

  return (
    <div className="h-full flex flex-col bg-brand-dark overflow-hidden font-sans">
      {/* TACTICAL HEADER */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20 relative">
        <div className="absolute top-0 right-0 w-64 h-full bg-gradient-to-l from-brand-accent/5 to-transparent" />
        
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6 relative z-10">
          <div>
            <div className="flex items-center gap-2 text-red-500 mb-2">
              <ShieldAlert size={14} className="animate-pulse" />
              <span className="text-[10px] font-mono tracking-[0.5em] uppercase">Security Clearance: LEVEL_05</span>
            </div>
            <h1 className="text-4xl font-black text-white italic tracking-tighter uppercase leading-none">
              Command_Hierarchy
            </h1>
            <p className="text-[9px] font-mono text-slate-500 mt-2 tracking-[0.2em] uppercase">
              Operational Real-time Structural Mapping // Project_Faust
            </p>
          </div>
          
          <div className="grid grid-cols-2 gap-4 font-mono">
            <StatBox label="Total_Nodes" value={totalSlots} icon={<Activity size={12}/>} />
            <StatBox label="Vacancies" value={vacantSlots} icon={<Target size={12}/>} color="text-red-500" />
          </div>
        </div>
      </div>

      {/* MAIN VIEWPORT */}
      <div className="flex-1 overflow-auto p-8 custom-scrollbar bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px]">
        <div className="max-w-5xl mx-auto">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center h-64 gap-4">
              <div className="w-12 h-1 border-2 border-brand-accent animate-pulse" />
              <div className="font-mono text-[10px] text-brand-accent animate-pulse uppercase tracking-[1em]">
                Establishing_Chain_Link...
              </div>
            </div>
          ) : (
            <div className="space-y-6">
              {tree?.map((root) => (
                <div key={root.publicId} className="bg-black/20 border border-brand-border/30 p-6 backdrop-blur-sm">
                  <OccupationNode node={root} depth={0} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// --- UTILS ---
const StatBox = ({ label, value, icon, color = "text-brand-accent" }: any) => (
  <div className="bg-black/40 border border-brand-border p-3 min-w-[120px]">
    <div className="flex items-center gap-2 text-[8px] text-slate-500 uppercase mb-1 font-black">
      {icon} {label}
    </div>
    <div className={`text-xl font-black leading-none ${color}`}>{value}</div>
  </div>
);

const countNodes = (nodes: OccupationTreeResponse[]): number => {
  return nodes.reduce((acc, node) => acc + 1 + countNodes(node.subordinates || []), 0);
};

const countVacant = (nodes: OccupationTreeResponse[]): number => {
  return nodes.reduce((acc, node) => acc + (node.isVacant ? 1 : 0) + countVacant(node.subordinates || []), 0);
};

export default HierarchyPage;