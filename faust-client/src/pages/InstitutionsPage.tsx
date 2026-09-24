import React, { useState, useMemo, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import api from '../api/axios';
import {
  Network, Search, Cpu, X, ListTree, Zap, SearchCode
} from 'lucide-react';
import type { InstitutionTreeResponse } from '../types';
import InstitutionTree from '../components/institutions/InstitutionTree';
import { InstitutionFlow } from '../components/institutions/InstitutionFlow';

const InstitutionsPage = () => {
  const location = useLocation();
  const [searchQuery, setSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState<'tree' | 'nexus'>('tree');
  const [focusNodeId, setFocusNodeId] = useState<string | null>(null);

  // 1. ROOT NODES (top-level institutions only, for initial render performance)
  const { data: tree, isLoading: isTreeLoading } = useQuery<InstitutionTreeResponse[]>({
    queryKey: ['institution-tree'],
    queryFn: async () => {
      const res = await api.get('/institutions/tree');
      return res.data;
    }
  });

  // 2. FOCUS DATA — deep sub-tree for Nexus visualization.
  // CHANGED: removed the `if (!focusNodeId) return null` branch. The query
  // is already gated by `enabled: !!focusNodeId`, so queryFn never runs
  // with a null focusNodeId — the early return was dead code that also
  // produced a type mismatch (null vs InstitutionTreeResponse).
  const { data: subTreeData, isLoading: isSubTreeLoading } = useQuery<InstitutionTreeResponse>({
    queryKey: ['institution-subtree', focusNodeId],
    queryFn: async () => (await api.get(`/institutions/${focusNodeId}/sub-tree`)).data,
    enabled: !!focusNodeId,
    staleTime: 1000 * 60 * 5
  });

  useEffect(() => {
    const state = location.state as { focusId?: string; view?: 'tree' | 'nexus' };
    if (state?.focusId) setFocusNodeId(state.focusId);
    if (state?.view) setViewMode(state.view);
  }, [location]);

  // Stats computed from currently-loaded root nodes only. A dedicated
  // global stats endpoint would be needed for a true system-wide count.
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

  const finalFocusData = useMemo(() => {
    return subTreeData ? [subTreeData] : [];
  }, [subTreeData]);

  const filteredTree = useMemo(() => {
    if (!searchQuery) return tree;
    const lowerQuery = searchQuery.toLowerCase();
    // Lazy-loaded tree only searches root nodes currently in memory.
    // A dedicated /search endpoint would be needed for true global full-text search.
    return tree?.filter(node =>
      node.name.toLowerCase().includes(lowerQuery) ||
      node.publicId.toLowerCase().includes(lowerQuery)
    );
  }, [tree, searchQuery]);

  const EMPTY_TREE_ARRAY: InstitutionTreeResponse[] = [];

  const memoizedTreeData = useMemo(() => {
    return tree || EMPTY_TREE_ARRAY;
  }, [tree]);

  return (
    <div className="h-full flex flex-col bg-[#0f172a] text-slate-200 overflow-hidden relative">

      {/* HEADER */}
      <div className="px-8 py-6 border-b border-slate-800 bg-slate-900/50 relative z-20 shrink-0">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-2 text-blue-500 mb-1 font-mono text-[10px] uppercase tracking-[0.3em]">
                <Network size={12} className="animate-pulse" />
                <span>Structural_Nexus_Mapping</span>
              </div>
              <h1 className="text-3xl font-bold text-white tracking-tight uppercase">
                Nexus <span className="text-slate-500 font-light italic">Explorer</span>
              </h1>
            </div>

            {/* SEARCH BAR */}
            <div className="relative max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" size={16} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Query registry node..."
                className="w-full bg-black/40 border border-slate-800 rounded py-2 pl-10 pr-4 text-xs font-mono focus:border-blue-500 outline-none transition-all placeholder:text-slate-700"
              />
            </div>
          </div>

          <div className="flex gap-4 items-center">
            <MetricBlock label="Root Nodes" value={tree?.length || 0} color="text-blue-400" />
            <MetricBlock label="Intel Assets" value={stats.intelligence} color="text-red-500" isCritical />

            <div className="h-12 w-px bg-slate-800 mx-2 hidden md:block" />

            <div className="flex bg-black/40 p-1 rounded border border-slate-800 self-center">
              <button
                onClick={() => setViewMode('tree')}
                className={`px-3 py-1.5 rounded flex items-center gap-2 text-[10px] font-bold transition-all ${viewMode === 'tree' ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'}`}
              >
                <ListTree size={14} /> REGISTER
              </button>
              <button
                onClick={() => setViewMode('nexus')}
                className={`px-3 py-1.5 rounded flex items-center gap-2 text-[10px] font-bold transition-all ${viewMode === 'nexus' ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-500 hover:text-slate-300'}`}
              >
                <Network size={14} /> FULL NEXUS
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* VIEWPORT */}
      <div className="flex-1 relative overflow-hidden">
        {isTreeLoading ? (
          <div className="flex flex-col items-center justify-center h-full font-mono text-slate-500 uppercase tracking-widest text-[10px]">
            <Cpu className="animate-spin mb-4 text-blue-500/50" size={32} />
            Synchronizing structural nodes...
          </div>
        ) : (
          <>
            {/* REGISTER VIEW — supports lazy loading */}
            <div className={`h-full overflow-y-auto p-8 bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px] ${viewMode !== 'tree' ? 'hidden' : ''}`}>
              <div className="max-w-4xl mx-auto space-y-3 pb-24">
                {filteredTree?.length === 0 ? (
                  <div className="text-center py-20 border border-dashed border-slate-800 rounded">
                    <SearchCode className="mx-auto text-slate-700 mb-4" size={48} />
                    <p className="text-slate-500 font-mono text-xs">NO_NODES_MATCH_QUERY</p>
                  </div>
                ) : (
                  filteredTree?.map((rootNode) => (
                    <InstitutionTree
                      key={rootNode.publicId}
                      node={rootNode}
                      depth={0}
                      forceOpen={searchQuery.length > 2}
                      onFocusClick={(id) => setFocusNodeId(id)}
                    />
                  ))
                )}
              </div>
            </div>

            {/* FULL NEXUS VIEW — high load, shows only loaded roots */}
            {viewMode === 'nexus' && (
              <div className="h-full w-full bg-black">
                <InstitutionFlow data={memoizedTreeData} />
              </div>
            )}

            {/* FOCUS MODAL — deep structural analysis */}
            {focusNodeId && (
              <div className="absolute inset-0 z-50 bg-slate-950/90 backdrop-blur-xl animate-in fade-in zoom-in-95 duration-300 flex flex-col">
                <div className="p-4 border-b border-white/10 flex justify-between items-center bg-slate-900/80">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-blue-500/20 rounded text-blue-400">
                      <Zap size={16} />
                    </div>
                    <div>
                      <h2 className="text-[10px] font-black text-white uppercase tracking-[0.2em]">Structural_Focus_Mode</h2>
                      <p className="text-[9px] font-mono text-slate-500 uppercase">
                        {isSubTreeLoading ? 'FETCHING_DEEP_HIERARCHY...' : `Isolating_Nexus::${subTreeData?.name || focusNodeId}`}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setFocusNodeId(null)}
                    className="group p-2 hover:bg-red-500/20 rounded-full transition-all"
                  >
                    <X size={24} className="text-slate-500 group-hover:text-red-500" />
                  </button>
                </div>

                <div className="flex-1 bg-black/40">
                  {isSubTreeLoading ? (
                    <div className="h-full flex items-center justify-center font-mono text-[10px] text-blue-500 animate-pulse">
                      DECRYPTING_SUBSTRUCTURE_GRAPH...
                    </div>
                  ) : (
                    <InstitutionFlow data={finalFocusData} />
                  )}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

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