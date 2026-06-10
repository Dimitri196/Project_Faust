import React, { useMemo, useCallback, useRef, useEffect, useState } from 'react';
import ReactFlow, { 
  Background, 
  Controls, 
  ConnectionLineType,
  Handle,
  Position,
  BackgroundVariant,
  Panel,
  ReactFlowProvider,
  useReactFlow,
  getRectOfNodes,
  type Edge, 
  type Node, 
} from 'reactflow';
import dagre from 'dagre';
import { useNavigate } from 'react-router-dom';
import { toPng } from 'html-to-image';
import 'reactflow/dist/style.css';
import { 
  User, UserMinus, Maximize, Camera,
  ChevronRight, ChevronDown, Briefcase, ExternalLink
} from 'lucide-react';
import type { OccupationTreeResponse } from '../../types';

// --- POMOCNÉ FUNKCE PRO RANKY ---
const getRankColor = (rank: number) => {
  if (rank <= 2) return '#f43f5e'; 
  if (rank <= 5) return '#14b8a6'; 
  if (rank <= 8) return '#3b82f6'; 
  return '#94a3b8'; 
};

// --- CUSTOM NODE KOMPONENTA ---
const OccupationNode = ({ data }: { data: any }) => {
  const navigate = useNavigate();
  const rankColor = getRankColor(data.rank);

  const handleNav = (e: React.MouseEvent, path: string) => {
    e.stopPropagation();
    e.preventDefault();
    navigate(path);
  };

  return (
    <div className={`
      group relative bg-slate-900/95 backdrop-blur-xl border 
      ${data.isVacant ? 'border-red-900/50 shadow-[0_0_15px_rgba(239,68,68,0.1)]' : 'border-slate-800 shadow-2xl'} 
      p-0 w-[280px] min-h-[85px] rounded-sm transition-all hover:border-brand-accent/50 flex flex-col
    `}>
      <div className="h-1 w-full shrink-0" style={{ backgroundColor: rankColor }} />
      <Handle type="target" position={Position.Left} className="!bg-slate-600 !w-2 !h-2 !border-none" />
      
      <div className="p-3 flex-1 flex items-center gap-3">
        <div 
          onClick={(e) => handleNav(e, `/occupations/${data.publicId}`)}
          className={`nodrag cursor-pointer w-10 h-10 flex items-center justify-center rounded-sm shrink-0 border transition-all
            ${data.isVacant ? 'bg-red-500/10 border-red-500/20 hover:bg-red-500/20' : 'bg-brand-accent/5 border-brand-accent/20 hover:bg-brand-accent/10'}`}
        >
          {data.isVacant ? <UserMinus size={18} className="text-red-500" /> : <Briefcase size={18} className="text-brand-accent" />}
        </div>

        <div className="flex-1 min-w-0">
          <div 
            onClick={(e) => handleNav(e, `/occupations/${data.publicId}`)}
            className="nodrag cursor-pointer text-[10px] font-black text-white uppercase tracking-wider leading-tight break-words hover:text-brand-accent transition-colors"
          >
            {data.title}
          </div>

          <div className="flex flex-col gap-0.5 mt-1">
            <span 
              onClick={(e) => {
                if (!data.isVacant && data.personPublicId) {
                  handleNav(e, `/personnel/${data.personPublicId}`);
                }
              }}
              className={`nodrag text-[9px] font-mono uppercase tracking-tighter transition-all flex items-center gap-1
                ${data.isVacant ? 'text-red-400 italic' : 'text-slate-400 cursor-pointer hover:text-emerald-400'}`}
            >
              {data.isVacant ? '[ VACANT_SLOT ]' : (
                <>
                  {data.currentOccupantName}
                  <ExternalLink size={8} className="opacity-0 group-hover:opacity-40" />
                </>
              )}
            </span>
            <span className="text-[7px] text-slate-600 font-mono tracking-widest">CODE_{data.code}</span>
          </div>
        </div>
      </div>

      {data.hasChildren && (
        <button 
          onClick={(e) => { e.stopPropagation(); data.onToggle(data.publicId); }}
          className="nodrag absolute -right-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-slate-800 border border-slate-700 rounded-full flex items-center justify-center text-white hover:bg-brand-accent transition-all z-10 shadow-xl"
        >
          {data.isExpanded ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
        </button>
      )}
      
      <Handle type="source" position={Position.Right} className="!bg-brand-accent !w-2 !h-2 !border-none" />
    </div>
  );
};

const NODE_TYPES = { occNode: OccupationNode };

// --- INTERNÍ KOMPONENTA FLOW ---
const OccupationFlowInternal: React.FC<{ data: OccupationTreeResponse[] }> = ({ data }) => {
  const flowRef = useRef<HTMLDivElement>(null);
  const { fitView, getNodes } = useReactFlow();
  const [collapsedIds, setCollapsedIds] = useState<Set<string>>(new Set());

  const toggleNode = useCallback((id: string) => {
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  const { nodes, edges } = useMemo(() => {
    const nodes: Node[] = [];
    const edges: Edge[] = [];

    const traverse = (items: OccupationTreeResponse[], parentId: string | null = null, isVisible = true) => {
      items.forEach((item) => {
        const isCollapsed = collapsedIds.has(item.publicId);
        
        if (isVisible) {
          nodes.push({
            id: item.publicId,
            type: 'occNode',
            data: { 
              // Šíříme všechna data z itemu (včetně personPublicId) do node.data
              ...item, 
              onToggle: toggleNode, 
              isExpanded: !isCollapsed,
              hasChildren: item.subordinates && item.subordinates.length > 0,
              // Jistota pro mapování ID osoby
              personPublicId: item.personPublicId
            },
            position: { x: 0, y: 0 },
          });

          if (parentId) {
            edges.push({
              id: `e-${parentId}-${item.publicId}`,
              source: parentId,
              target: item.publicId,
              type: ConnectionLineType.SmoothStep,
              style: { stroke: item.isVacant ? '#ef4444' : '#14b8a6', strokeWidth: 1.5, opacity: 0.4 },
              markerEnd: { type: 'arrowclosed' as any, color: '#14b8a6' },
            });
          }
        }

        if (item.subordinates && item.subordinates.length > 0) {
          traverse(item.subordinates, item.publicId, isVisible && !isCollapsed);
        }
      });
    };

    traverse(data);

    const g = new dagre.graphlib.Graph();
    g.setGraph({ rankdir: 'LR', nodesep: 60, ranksep: 200 });
    g.setDefaultEdgeLabel(() => ({}));
    nodes.forEach((n) => g.setNode(n.id, { width: 280, height: 90 }));
    edges.forEach((e) => g.setEdge(e.source, e.target));
    dagre.layout(g);

    return {
      nodes: nodes.map((n) => {
        const pos = g.node(n.id);
        return { ...n, position: { x: pos.x - 140, y: pos.y - 45 } };
      }),
      edges
    };
  }, [data, collapsedIds, toggleNode]);

  useEffect(() => {
    const timer = setTimeout(() => fitView({ padding: 0.2, duration: 800 }), 50);
    return () => clearTimeout(timer);
  }, [collapsedIds, fitView]);

  return (
    <div ref={flowRef} className="w-full h-full relative bg-[#020617]">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={NODE_TYPES}
        onNodeClick={undefined}
        minZoom={0.02}
        maxZoom={2}
      >
        <Background variant={BackgroundVariant.Dots} color="#1e293b" gap={25} size={1} />
        <Panel position="top-right" className="flex gap-2 p-2 bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded">
          <button onClick={() => fitView({ duration: 800 })} className="px-4 py-2 text-[10px] font-bold text-blue-400 border border-blue-500/20 uppercase hover:bg-blue-500/10">
            <Maximize size={14} className="inline mr-2" /> Recenter
          </button>
        </Panel>
        <Controls />
      </ReactFlow>
    </div>
  );
};

export const HierarchyFlow: React.FC<{ data: OccupationTreeResponse[] }> = (props) => (
  <ReactFlowProvider>
    <OccupationFlowInternal {...props} />
  </ReactFlowProvider>
);

export default HierarchyFlow;