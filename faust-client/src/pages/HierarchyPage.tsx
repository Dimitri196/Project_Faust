import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import type { OccupationTreeResponse } from '../types';
import { ShieldAlert, User, UserMinus, GitMerge, ChevronRight, ChevronDown } from 'lucide-react';
import { useState } from 'react';

// REKURZIVNÍ KOMPONENTA PRO POZICE
const OccupationNode = ({ node, depth }: { node: OccupationTreeResponse; depth: number }) => {
  const [isOpen, setIsOpen] = useState(true);
  const hasSubordinates = node.subordinates && node.subordinates.length > 0;

  return (
    <div className="ml-6 border-l border-brand-border/30 relative">
      <div className="absolute left-0 top-5 w-4 h-px bg-brand-border/30" />
      
      <div className={`flex items-center gap-4 p-3 mb-1 transition-all ${node.isVacant ? 'bg-red-500/5 border border-red-500/20' : 'hover:bg-white/5'}`}>
        <button onClick={() => setIsOpen(!isOpen)} className="text-slate-500">
          {hasSubordinates ? (isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />) : <div className="w-3.5" />}
        </button>

        <div className={`p-2 rounded-sm ${node.isVacant ? 'bg-red-500/20 animate-pulse' : 'bg-brand-accent/10'}`}>
          {node.isVacant ? <UserMinus size={16} className="text-red-500" /> : <User size={16} className="text-brand-accent" />}
        </div>

        <div className="flex-1">
          <div className="flex items-center gap-2">
            <span className={`text-xs font-bold tracking-widest uppercase ${node.isVacant ? 'text-red-400' : 'text-slate-200'}`}>
              {node.title}
            </span>
            <span className="text-[9px] font-mono text-slate-600 bg-black/40 px-1 italic">
              {node.rank}
            </span>
          </div>
          <div className="text-[10px] font-mono text-slate-500">
            {node.isVacant ? (
              <span className="text-red-500/60 uppercase tracking-tighter animate-pulse">&gt; VACANT_SLOT // PERSONNEL_REQUIRED</span>
            ) : (
              <span className="text-brand-accent/70 tracking-tighter uppercase italic">{node.currentOccupantName}</span>
            )}
          </div>
        </div>

        <div className="text-[9px] font-mono text-slate-700 uppercase px-2 border border-slate-800">
          {node.category}
        </div>
      </div>

      {isOpen && hasSubordinates && (
        <div className="animate-in slide-in-from-left-2 duration-300">
          {node.subordinates.map((sub) => (
            <OccupationNode key={sub.publicId} node={sub} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

// HLAVNÍ STRÁNKA
const HierarchyPage = () => {
  const { data: tree, isLoading } = useQuery<OccupationTreeResponse[]>({
    queryKey: ['occupation-tree'],
    queryFn: async () => {
      // Předpokládáme, že tvůj backend vrací strom pozic pro hlavní HQ
      const res = await axios.get('/api/v1/occupations/tree'); 
      return res.data;
    }
  });

  return (
    <div className="h-full flex flex-col bg-brand-dark overflow-hidden">
      {/* HEADER */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/10 backdrop-blur-md">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 text-red-500 mb-1">
              <ShieldAlert size={14} className="animate-pulse" />
              <span className="text-[10px] font-mono tracking-[0.4em] uppercase">Security Clearance: Level_05</span>
            </div>
            <h1 className="text-3xl font-black text-white italic tracking-tighter uppercase underline decoration-red-500/50">Command_Hierarchy</h1>
          </div>
          
          <div className="flex gap-4 font-mono">
            <div className="flex items-center gap-2 px-3 py-1 bg-brand-accent/10 border border-brand-accent/20 text-[10px] text-brand-accent">
              <GitMerge size={12} />
              <span>CHAIN_OF_COMMAND</span>
            </div>
          </div>
        </div>
      </div>

      {/* CONTENT */}
      <div className="flex-1 overflow-auto p-8 custom-scrollbar">
        <div className="max-w-4xl mx-auto">
          {isLoading ? (
            <div className="font-mono text-xs text-brand-accent animate-pulse uppercase">&gt; Mapping_Deployment_Structure...</div>
          ) : (
            <div className="bg-brand-panel/5 border border-brand-border/20 p-6 rounded-sm shadow-2xl">
              {tree?.map((root) => (
                <OccupationNode key={root.publicId} node={root} depth={0} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default HierarchyPage;