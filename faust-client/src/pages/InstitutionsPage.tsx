import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Network, Database, Info } from 'lucide-react';
import type { InstitutionTreeResponse } from '../types';
// IMPORT TVÝCH NOVÝCH KOMPONENT
import InstitutionTree from '../components/institutions/InstitutionTree';

const InstitutionsPage = () => {
  const { data: tree, isLoading, error } = useQuery<InstitutionTreeResponse[]>({
    queryKey: ['institution-tree'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/institutions/tree');
      return res.data;
    }
  });

  return (
    <div className="h-full flex flex-col bg-brand-dark animate-in fade-in duration-500">
      {/* HEADER SECTION */}
      <div className="p-8 border-b border-brand-border bg-brand-panel/20">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 text-brand-accent mb-1">
              <Network size={14} />
              <span className="text-[10px] font-mono tracking-[0.3em] uppercase">Structural Mapping</span>
            </div>
            <h1 className="text-3xl font-black text-white italic tracking-tighter uppercase">Nexus_Explorer</h1>
          </div>
          
          <div className="flex gap-4">
             <div className="px-4 py-2 border border-brand-border bg-black/20 font-mono text-[10px] text-slate-500">
                SYSTEM_STATUS: <span className="text-brand-success">ONLINE</span>
             </div>
          </div>
        </div>
      </div>

      {/* MAIN CONTENT AREA */}
      <div className="flex-1 overflow-auto p-8 custom-scrollbar">
        <div className="max-w-4xl mx-auto">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center h-64 font-mono text-xs text-brand-accent">
              <div className="w-12 h-1 bg-brand-accent/20 mb-4 overflow-hidden relative">
                <div className="absolute inset-0 bg-brand-accent animate-progress-flow" />
              </div>
              &gt; SYNCHRONIZING_HIERARCHY_DATA...
            </div>
          ) : error ? (
            <div className="p-4 border border-red-900/50 bg-red-900/10 text-red-500 font-mono text-xs">
              &gt; ERROR: FAILED_TO_ESTABLISH_UPLINK_TO_DATABASE
            </div>
          ) : (
            <div className="bg-brand-panel/10 border border-brand-border/30 p-6 backdrop-blur-sm">
              <div className="mb-6 flex items-center gap-2 text-slate-500">
                 <Info size={14} />
                 <span className="text-[9px] font-mono uppercase tracking-widest italic">Select a node to inspect institutional metadata</span>
              </div>
              
              {/* RENDER STROMU */}
              <div className="space-y-1">
                {tree?.map((rootNode) => (
                  <InstitutionTree key={rootNode.publicId} node={rootNode} depth={0} />
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default InstitutionsPage;