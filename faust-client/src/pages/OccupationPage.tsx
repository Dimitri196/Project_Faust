import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/axios';
import { GitBranch, Info, Database, Layers, AlertCircle, Search } from 'lucide-react';
import type { OccupationTreeResponse } from '../types';
import OccupationNode from '../components/occupations/OccupationNode';

const OccupationsPage = () => {
  const { data: tree, isLoading, error } = useQuery<OccupationTreeResponse[]>({
    queryKey: ['occupation-tree'],
    queryFn: async () => {
      const res = await api.get('/occupations/tree');
      return res.data;
    }
  });

  // Odvozené statistiky
  const totalNodes = tree ? countNodes(tree) : 0;
  const vacancies = tree ? countVacancies(tree) : 0;

  return (
    <div className="h-full flex flex-col bg-[#0f172a] text-slate-200">
      
      {/* ANALYTICAL HEADER */}
      <div className="px-8 py-6 border-b border-slate-800 bg-slate-900/50">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 text-slate-500 mb-1 font-mono text-[10px] uppercase tracking-[0.3em]">
              <GitBranch size={12} className="text-blue-500" />
              <span>Org_Architecture // System_View</span>
            </div>
            <h1 className="text-3xl font-bold text-white tracking-tight">
              Structural <span className="text-slate-500 font-light">Hierarchy</span>
            </h1>
          </div>
          
          <div className="flex items-center gap-4">
            <Metric 
              label="Total Positions" 
              value={totalNodes} 
              color="text-blue-400" 
            />
            <Metric 
              label="Active Vacancies" 
              value={vacancies} 
              color="text-amber-500" 
              isWarning={vacancies > 0} 
            />
          </div>
        </div>
      </div>

      {/* VIEWPORT AREA */}
      <div className="flex-1 overflow-auto p-8 custom-scrollbar relative bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:32px_32px]">
        
        <div className="max-w-5xl mx-auto">
          {/* USER GUIDANCE */}
          <div className="mb-8 flex items-center justify-between bg-blue-500/5 border border-blue-500/10 p-4 rounded-lg">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-500/10 rounded">
                <Info size={16} className="text-blue-400" />
              </div>
              <p className="text-xs text-slate-400 leading-relaxed max-w-xl font-medium">
                Exploration of the institutional command chain. Use the nodes below to drill down into 
                <span className="text-slate-200"> departmental mandates</span>, <span className="text-slate-200">legal assignments</span> and <span className="text-slate-200">personnel history</span>.
              </p>
            </div>
            <div className="hidden lg:block">
              <Search size={18} className="text-slate-600" />
            </div>
          </div>

          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-32 gap-4">
              <div className="w-48 h-1.5 bg-slate-800 rounded-full overflow-hidden relative">
                <div className="absolute inset-0 bg-blue-600 animate-[loading_1.5s_infinite]" style={{ width: '30%' }} />
              </div>
              <span className="text-[10px] font-mono text-slate-500 uppercase tracking-widest animate-pulse">
                Reconstructing Command Tree...
              </span>
            </div>
          ) : error ? (
            <div className="max-w-md mx-auto mt-20 p-6 border border-red-900/50 bg-red-950/20 rounded-lg text-center">
              <AlertCircle size={32} className="text-red-500 mx-auto mb-4" />
              <h2 className="text-red-200 font-bold mb-1">Architecture Sync Failed</h2>
              <p className="text-red-500/70 text-xs font-mono lowercase">err_ds_connection_refused</p>
            </div>
          ) : (
            <div className="space-y-2 animate-in fade-in slide-in-from-bottom-4 duration-700">
              {tree?.map((rootNode) => (
                <OccupationNode 
                  key={rootNode.publicId} 
                  node={rootNode} 
                  depth={0} 
                />
              ))}
            </div>
          )}
        </div>

        {/* Floating Decorative Element */}
        <div className="fixed bottom-8 right-8 opacity-[0.03] pointer-events-none">
          <Database size={200} />
        </div>
      </div>

      <style>{`
        @keyframes loading {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(400%); }
        }
      `}</style>
    </div>
  );
};

// --- SUBSIDIARY COMPONENTS ---

interface MetricProps {
  label: string;
  value: number;
  color: string;
  isWarning?: boolean;
  block?: boolean;
}

const Metric = ({ label, value, color, isWarning, block }: MetricProps) => (
  <div className={`px-5 py-3 rounded-lg border border-slate-800 bg-slate-900 shadow-sm min-w-[140px] ${block ? 'w-full md:w-auto' : ''}`}>
    <div className="text-[9px] text-slate-500 font-bold uppercase tracking-wider mb-1">{label}</div>
    <div className={`text-2xl font-mono font-bold ${color}`}>
      {value.toString().padStart(2, '0')}
      {isWarning && <span className="ml-2 inline-block w-2 h-2 bg-amber-500 rounded-full animate-pulse" />}
    </div>
  </div>
);

// --- UTILITIES ---

const countNodes = (nodes: OccupationTreeResponse[]): number => {
  return nodes.reduce((acc, node) => acc + 1 + (node.subordinates ? countNodes(node.subordinates) : 0), 0);
};

const countVacancies = (nodes: OccupationTreeResponse[]): number => {
  return nodes.reduce((acc, node) => acc + (node.isVacant ? 1 : 0) + (node.subordinates ? countVacancies(node.subordinates) : 0), 0);
};

export default OccupationsPage;