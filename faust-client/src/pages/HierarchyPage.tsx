import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import type { OccupationTreeResponse } from '../types';
import { 
  ShieldAlert, User, UserMinus, ChevronRight, ChevronDown, 
  Activity, Target, LayoutList, Network 
} from 'lucide-react';
import { useState } from 'react';
// IMPORT NOVÉ KOMPONENTY
import { HierarchyFlow } from '../components/occupations/HierarchyFlow'; 

// --- 1. TACTICAL NODE (Původní seznamový styl) ---
const OccupationNodeTactical = ({ node, depth }: { node: OccupationTreeResponse; depth: number }) => {
  const [isOpen, setIsOpen] = useState(true);
  const hasSubordinates = node.subordinates && node.subordinates.length > 0;

  const getCategoryColor = (cat: string) => {
    switch (cat) {
      case 'GOVERNANCE': return 'text-purple-400 border-purple-900/50 bg-purple-500/5';
      case 'EXECUTIVE': return 'text-blue-400 border-blue-900/50 bg-blue-500/5';
      case 'OPERATIONAL': return 'text-emerald-400 border-emerald-900/50 bg-emerald-500/5';
      default: return 'text-slate-500 border-slate-800 bg-slate-500/5';
    }
  };

  return (
    <div className="relative">
      {depth > 0 && <div className="absolute -left-4 top-0 bottom-0 w-px bg-brand-border/20" />}
      
      <div className={`
        group flex items-center gap-4 p-3 mb-1 transition-all border-l-2
        ${node.isVacant 
          ? 'bg-red-500/5 border-red-500/50' 
          : 'bg-brand-panel/10 border-brand-accent/30 hover:bg-brand-panel/30 hover:border-brand-accent'}
      `}>
        <button 
          onClick={() => setIsOpen(!isOpen)} 
          className={`transition-transform duration-300 ${!hasSubordinates && 'opacity-0'}`}
        >
          {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </button>

        <div className={`p-2 rounded-sm ${node.isVacant ? 'bg-red-500/10' : 'bg-brand-accent/5'}`}>
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
            <span className={`${node.isVacant ? 'text-red-500/60 italic' : 'text-brand-accent/60 font-bold'} tracking-tight uppercase`}>
              {node.isVacant ? '[ UNASSIGNED_SLOT ]' : node.currentOccupantName}
            </span>
          </div>
        </div>

        <div className={`text-[8px] font-mono uppercase px-2 py-0.5 border rounded-sm tracking-widest ${getCategoryColor(node.category)}`}>
          {node.category}
        </div>
      </div>

      {isOpen && hasSubordinates && (
        <div className="ml-8 mt-1 space-y-1 animate-in slide-in-from-left-2 duration-300">
          {node.subordinates.map((sub) => (
            <OccupationNodeTactical key={sub.publicId} node={sub} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

// --- 3. MAIN PAGE COMPONENT ---
const HierarchyPage = () => {
  const [viewMode, setViewMode] = useState<'tactical' | 'visual'>('tactical');

  const { data: tree, isLoading } = useQuery<OccupationTreeResponse[]>({
    queryKey: ['occupation-tree'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/occupations/tree'); 
      return res.data;
    }
  });

  const totalSlots = tree ? countNodes(tree) : 0;
  const vacantSlots = tree ? countVacant(tree) : 0;

  return (
    <div className="h-full flex flex-col bg-brand-dark overflow-hidden font-sans">
      {/* HEADER SECTION */}
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

          {/* VIEW SWITCHER */}
          <div className="flex bg-black/40 border border-brand-border p-1 rounded-sm overflow-hidden">
            <button 
              onClick={() => setViewMode('tactical')}
              className={`flex items-center gap-2 px-4 py-2 text-[10px] font-mono transition-all ${viewMode === 'tactical' ? 'bg-brand-accent text-black' : 'text-slate-500 hover:bg-white/5'}`}
            >
              <LayoutList size={14} /> [ 01_TACTICAL ]
            </button>
            <button 
              onClick={() => setViewMode('visual')}
              className={`flex items-center gap-2 px-4 py-2 text-[10px] font-mono transition-all ${viewMode === 'visual' ? 'bg-brand-accent text-black' : 'text-slate-500 hover:bg-white/5'}`}
            >
              <Network size={14} /> [ 02_VISUAL ]
            </button>
          </div>
          
          <div className="grid grid-cols-2 gap-4 font-mono">
            <StatBox label="Total_Nodes" value={totalSlots} icon={<Activity size={12}/>} />
            <StatBox label="Vacancies" value={vacantSlots} icon={<Target size={12}/>} color="text-red-500" />
          </div>
        </div>
      </div>

      {/* MAIN VIEWPORT */}
      <div className="flex-1 relative overflow-hidden bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px]">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-full gap-4 animate-pulse">
            <div className="w-12 h-1 bg-brand-accent" />
            <div className="font-mono text-[10px] text-brand-accent uppercase tracking-[1em]">Establishing_Chain_Link...</div>
          </div>
        ) : (
          <div className="h-full">
            {viewMode === 'tactical' ? (
              <div className="overflow-auto h-full p-8 custom-scrollbar">
                <div className="max-w-5xl mx-auto space-y-6">
                  {tree?.map((root) => (
                    <div key={root.publicId} className="bg-black/20 border border-brand-border/30 p-6 backdrop-blur-sm">
                      <OccupationNodeTactical node={root} depth={0} />
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              // NOVÁ KOMPONENTA PRO VIZUÁLNÍ STROM (Coca-Cola styl)
              <div className="w-full h-full">
                <HierarchyFlow data={tree || []} />
              </div>
            )}
          </div>
        )}
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