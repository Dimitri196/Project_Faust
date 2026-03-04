import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios';
import { 
  Network, 
  Database, 
  Info, 
  Search, 
  Activity, 
  ShieldAlert, 
  Cpu,
  X
} from 'lucide-react';
import type { InstitutionTreeResponse } from '../types';
import InstitutionTree from '../components/institutions/InstitutionTree';

const InstitutionsPage = () => {
  const [searchQuery, setSearchQuery] = useState('');

  const { data: tree, isLoading, error } = useQuery<InstitutionTreeResponse[]>({
    queryKey: ['institution-tree'],
    queryFn: async () => {
      const res = await api.get('/institutions/tree');
      return res.data;
    }
  });

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

  const filteredTree = useMemo(() => {
    if (!searchQuery) return tree;
    const lowerQuery = searchQuery.toLowerCase();
    return tree?.filter(node => 
      node.name.toLowerCase().includes(lowerQuery) || 
      node.publicId.toLowerCase().includes(lowerQuery)
    );
  }, [tree, searchQuery]);

  return (
    <div className="h-full flex flex-col bg-[#0f172a] text-slate-200">
      
      {/* HEADER SECTION */}
      <div className="px-8 py-6 border-b border-slate-800 bg-slate-900/50 relative overflow-hidden">
        {/* Subtle Decorative Gradient */}
        <div className="absolute top-0 right-0 w-64 h-full bg-blue-500/5 skew-x-[-20deg] translate-x-32 pointer-events-none border-l border-white/5" />
        
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-end justify-between gap-6 relative z-10">
          <div>
            <div className="flex items-center gap-2 text-blue-500 mb-1 font-mono text-[10px] uppercase tracking-[0.3em]">
              <Network size={12} className="animate-pulse" />
              <span>Structural_Nexus_Mapping</span>
            </div>
            <h1 className="text-3xl font-bold text-white tracking-tight uppercase">
              Nexus <span className="text-slate-500 font-light italic">Explorer</span>
            </h1>
          </div>
          
          <div className="flex gap-4">
            <MetricBlock label="Active Nodes" value={stats.total} color="text-blue-400" />
            <MetricBlock label="Intel Assets" value={stats.intelligence} color="text-red-500" isCritical />
            
            <div className="h-12 w-px bg-slate-800 mx-2 hidden md:block" />
            
            <div className="hidden md:flex items-center gap-3 px-4 py-2 bg-slate-900 border border-slate-800 rounded-lg self-center">
              <Activity size={14} className="text-emerald-500" />
              <div className="flex flex-col">
                <span className="text-[8px] text-slate-500 uppercase font-bold tracking-widest">Uplink Status</span>
                <span className="text-[10px] text-emerald-500 font-mono font-bold uppercase">Encrypted_Active</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* SEARCH BAR */}
      <div className="px-8 py-4 bg-slate-900/30 border-b border-slate-800/50 flex justify-center shrink-0">
        <div className="max-w-4xl w-full relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-600" size={16} />
          <input 
            type="text"
            placeholder="FILTER INSTITUTIONS BY NAME OR IDENTIFIER..."
            className="w-full bg-slate-900 border border-slate-800 py-3 pl-12 pr-12 text-xs font-mono text-blue-400 placeholder:text-slate-700 focus:outline-none focus:border-blue-500/50 focus:ring-1 focus:ring-blue-500/20 transition-all rounded-lg uppercase tracking-wider"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <button 
              onClick={() => setSearchQuery('')}
              className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-600 hover:text-white transition-colors"
            >
              <X size={14} />
            </button>
          )}
        </div>
      </div>

      {/* MAIN VIEWPORT */}
      <div className="flex-1 overflow-auto p-8 custom-scrollbar bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px]">
        <div className="max-w-4xl mx-auto">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-32">
              <Cpu className="w-10 h-10 mb-4 text-blue-500/20 animate-spin" />
              <div className="w-32 h-1 bg-slate-800 rounded-full overflow-hidden relative">
                <div className="absolute inset-0 bg-blue-500 animate-[loading_1.5s_infinite]" style={{ width: '40%' }} />
              </div>
              <span className="mt-4 text-[10px] font-mono text-slate-500 uppercase tracking-[0.2em]">Reconstructing Nexus Tree</span>
            </div>
          ) : error ? (
            <div className="p-6 border border-red-900/50 bg-red-950/20 rounded-lg flex items-center gap-4">
              <ShieldAlert size={24} className="text-red-500 shrink-0" />
              <div>
                <div className="text-red-200 font-bold uppercase text-sm mb-1">Critical Uplink Failure</div>
                <div className="text-red-500/70 text-xs font-mono lowercase">failed_to_initialize_institutional_nexus_sync</div>
              </div>
            </div>
          ) : (
            <div className="space-y-6">
              <div className="flex items-center gap-3 text-slate-500 border-b border-slate-800/50 pb-4 mb-6">
                <Info size={14} className="text-blue-500/50" />
                <span className="text-[10px] font-mono uppercase tracking-widest italic">
                  {searchQuery ? `Active filter: "${searchQuery}"` : 'Select an entity node to initialize metadata extraction'}
                </span>
              </div>
              
              <div className="space-y-3 animate-in fade-in slide-in-from-bottom-2 duration-500">
                {filteredTree?.map((rootNode) => (
                  <InstitutionTree 
                    key={rootNode.publicId} 
                    node={rootNode} 
                    depth={0} 
                    forceOpen={searchQuery.length > 0} 
                  />
                ))}

                {filteredTree?.length === 0 && (
                  <div className="py-24 text-center border border-dashed border-slate-800 rounded-xl">
                    <Database size={40} className="mx-auto text-slate-800 mb-4" />
                    <span className="font-mono text-[10px] text-slate-600 uppercase tracking-[0.3em]">
                      Query_Returned_Zero_Matches
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
      
      <style>{`
        @keyframes loading {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(300%); }
        }
      `}</style>
    </div>
  );
};

// --- HELPER COMPONENT ---

const MetricBlock = ({ label, value, color, isCritical }: { label: string, value: number, color: string, isCritical?: boolean }) => (
  <div className="text-right px-4 py-2 bg-slate-900/50 border border-slate-800 rounded-lg min-w-[100px]">
    <div className={`text-[9px] uppercase tracking-widest font-bold mb-1 ${isCritical ? 'text-red-500/70' : 'text-slate-500'}`}>
      {label.replace(' ', '_')}
    </div>
    <div className={`text-xl font-mono font-black italic ${color}`}>
      {value.toString().padStart(value > 99 ? 3 : 2, '0')}
    </div>
  </div>
);

export default InstitutionsPage;