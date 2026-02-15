import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { 
  Network, 
  Database, 
  Info, 
  Search, 
  Activity, 
  ShieldAlert, 
  Cpu 
} from 'lucide-react';
import type { InstitutionTreeResponse } from '../types';
import InstitutionTree from '../components/institutions/InstitutionTree';

const InstitutionsPage = () => {
  const [searchQuery, setSearchQuery] = useState('');

  const { data: tree, isLoading, error } = useQuery<InstitutionTreeResponse[]>({
    queryKey: ['institution-tree'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/institutions/tree');
      return res.data;
    }
  });

  // Rekurzivní výpočet statistik pro dashboard efekt
  const stats = useMemo(() => {
    if (!tree) return { total: 0, intelligence: 0 };
    
    let total = 0;
    let intelligence = 0;

    const traverse = (nodes: InstitutionTreeResponse[]) => {
      nodes.forEach(node => {
        total++;
        if (node.type === 'INTELLIGENCE') intelligence++;
        if (node.children) traverse(node.children);
      });
    };

    traverse(tree);
    return { total, intelligence };
  }, [tree]);

  // Filtrování kořenových uzlů (pokud hledáme konkrétní větev)
  const filteredTree = useMemo(() => {
    if (!searchQuery) return tree;
    const lowerQuery = searchQuery.toLowerCase();
    
    // Filtrujeme pouze top-level, vnitřní rekurzi řeší forceOpen ve stromu
    return tree?.filter(node => 
      node.name.toLowerCase().includes(lowerQuery) || 
      node.publicId.toLowerCase().includes(lowerQuery)
    );
  }, [tree, searchQuery]);

  return (
    <div className="h-full flex flex-col bg-brand-dark animate-in fade-in duration-500 overflow-hidden">
      
      {/* HEADER SECTION - TACTICAL OVERLAY */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20 relative">
        <div className="absolute top-0 right-0 w-1/3 h-full bg-brand-accent/5 skew-x-[-20deg] translate-x-20 pointer-events-none border-l border-brand-accent/10" />
        
        <div className="max-w-7xl mx-auto flex items-end justify-between relative z-10">
          <div>
            <div className="flex items-center gap-2 text-brand-accent mb-1">
              <Network size={14} className="animate-pulse" />
              <span className="text-[10px] font-mono tracking-[0.3em] uppercase text-brand-accent/70">Structural_Nexus_Mapping</span>
            </div>
            <h1 className="text-4xl font-black text-white italic tracking-tighter uppercase leading-none">
              Nexus_Explorer<span className="text-brand-accent animate-pulse">_</span>
            </h1>
          </div>
          
          <div className="flex gap-8 items-center font-mono">
            <div className="text-right">
              <div className="text-[9px] text-slate-500 uppercase tracking-widest">Active_Nodes</div>
              <div className="text-xl font-black text-brand-accent italic">{stats.total.toString().padStart(3, '0')}</div>
            </div>
            <div className="text-right">
              <div className="text-[9px] text-red-500/70 uppercase tracking-widest">Intel_Assets</div>
              <div className="text-xl font-black text-red-500 italic">{stats.intelligence.toString().padStart(2, '0')}</div>
            </div>
            <div className="h-10 w-[1px] bg-brand-border/50" />
            <div className="px-4 py-2 border border-brand-border bg-black/40 rounded-sm">
               <div className="flex items-center gap-3 text-[10px]">
                 <Activity size={12} className="text-brand-success" />
                 <span className="text-slate-400 uppercase tracking-tighter">Status:</span>
                 <span className="text-brand-success font-bold uppercase">Uplink_Active</span>
               </div>
            </div>
          </div>
        </div>
      </div>

      {/* SEARCH BAR - TACTICAL INPUT */}
      <div className="px-8 py-4 bg-black/20 border-b border-brand-border flex justify-center shrink-0">
        <div className="max-w-4xl w-full relative group">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-brand-accent/40 group-focus-within:text-brand-accent transition-colors" size={16} />
          <input 
            type="text"
            placeholder="SCAN_HIERARCHY_BY_NAME_OR_PUBLIC_ID..."
            className="w-full bg-brand-panel/10 border border-brand-border/50 py-3 pl-12 pr-4 text-xs font-mono text-brand-accent placeholder:text-slate-700 focus:outline-none focus:border-brand-accent/50 focus:bg-brand-accent/5 transition-all outline-none"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* MAIN CONTENT AREA */}
      <div className="flex-1 overflow-auto p-8 custom-scrollbar">
        <div className="max-w-4xl mx-auto">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center h-64 font-mono text-[10px] text-brand-accent tracking-[0.3em]">
              <Cpu className="w-8 h-8 mb-4 animate-spin text-brand-accent/20" />
              <div className="w-24 h-1 bg-brand-accent/10 mb-4 overflow-hidden relative">
                <div className="absolute inset-0 bg-brand-accent animate-progress-flow" />
              </div>
              &gt; RECONSTRUCTING_HIERARCHY_TREE...
            </div>
          ) : error ? (
            <div className="p-6 border border-red-900/50 bg-red-900/10 text-red-500 font-mono text-xs flex items-center gap-4">
              <ShieldAlert size={20} className="animate-pulse" />
              <div>
                <div className="font-bold uppercase mb-1">Critical_Uplink_Failure</div>
                <div className="opacity-70">Failed to establish connection to the central institutional database.</div>
              </div>
            </div>
          ) : (
            <div className="space-y-6">
              <div className="flex items-center gap-2 text-slate-500 border-b border-brand-border/30 pb-4 mb-4">
                 <Info size={14} />
                 <span className="text-[9px] font-mono uppercase tracking-widest italic">
                   {searchQuery ? `Search results for: "${searchQuery}"` : 'Select an entity node to initialize metadata extraction'}
                 </span>
              </div>
              
              <div className="space-y-2">
                {filteredTree?.map((rootNode) => (
                  <div key={rootNode.publicId} className="animate-in slide-in-from-bottom-2 duration-300">
                    <InstitutionTree 
                      node={rootNode} 
                      depth={0} 
                      forceOpen={searchQuery.length > 0} 
                    />
                  </div>
                ))}

                {filteredTree?.length === 0 && (
                  <div className="py-20 text-center border border-dashed border-brand-border/30 rounded-sm">
                    <Database size={32} className="mx-auto text-slate-800 mb-4" />
                    <span className="font-mono text-xs text-slate-600 uppercase tracking-widest">
                      Query_Returned_Zero_Results
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default InstitutionsPage;