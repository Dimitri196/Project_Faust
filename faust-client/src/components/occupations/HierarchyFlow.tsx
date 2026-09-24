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
  type Edge,
  type Node,
  type NodeProps,
} from 'reactflow';
import dagre from 'dagre';
import { useNavigate } from 'react-router-dom';
import 'reactflow/dist/style.css';
import {
  UserMinus, Maximize,
  ChevronRight, ChevronDown, Briefcase, ExternalLink
} from 'lucide-react';
import type { OccupationTreeResponse, ClearanceLevel } from '../../types';

// CHANGED: was getRankColor(rank: number) keyed off OccupationTreeResponse.rank,
// but rank is actually a String on the backend (a military/police rank
// label like "COLONEL", not a numeric scale) — calling this with
// data.rank produced a real type error (string not assignable to
// number) once data.rank was properly typed instead of `any`.
// requiredClearanceLevel is a real ordered enum and a more meaningful
// "severity" signal for visual hierarchy anyway, so the color stripe now
// reflects clearance level instead of rank.
const getClearanceColor = (level: ClearanceLevel): string => {
  const colors: Record<ClearanceLevel, string> = {
    LEVEL_1_PUBLIC: '#94a3b8',
    LEVEL_2_INTERNAL: '#3b82f6',
    LEVEL_3_CONFIDENTIAL: '#14b8a6',
    LEVEL_4_SECRET: '#f59e0b',
    LEVEL_5_TOP_SECRET: '#f43f5e',
  };
  return colors[level] ?? '#94a3b8';
};

// CHANGED: explicit shape for this custom node's data, replacing `data: any`
// — same fix applied to InstitutionFlow.tsx's custom node earlier this
// session. Without it, a typo on any of these fields (e.g. `isVacant`
// vs the now-renamed `vacant`) compiles silently and only fails at
// runtime. NEW: added onSelect/isSelected for the click-to-highlight
// behavior — clicking a node now selects/centers it within the SAME
// loaded tree instead of navigating to OccupationDetail, which
// previously discarded the entire surrounding tree (parents, siblings)
// every time you clicked into a child node.
interface OccupationFlowNodeData extends OccupationTreeResponse {
  onToggle: (id: string) => void;
  isExpanded: boolean;
  hasChildren: boolean;
  onSelect: (id: string) => void;
  isSelected: boolean;
}

// CHANGED: renamed from `OccupationNode` to `OccupationFlowGraphNode`.
// Same naming-collision risk identified and fixed for InstitutionFlow's
// internal node component — this name otherwise collides with the
// separate, unrelated OccupationNodeTactical/OccupationNodeVisual list
// components living in the same components/occupations folder.
// CHANGED: clicking the node body now selects/highlights it within the
// already-loaded tree (data.onSelect) instead of navigating away via
// handleNav. Opening the full OccupationDetail dossier is now a
// deliberate, separate action via the small ExternalLink icon next to
// the title — so the two interactions ("look at this node in context"
// vs "open its full record") are no longer the same click target.
const OccupationFlowGraphNode = ({ data }: NodeProps<OccupationFlowNodeData>) => {
  const navigate = useNavigate();
  const rankColor = getClearanceColor(data.requiredClearanceLevel);

  const handleNav = (e: React.MouseEvent, path: string) => {
    e.stopPropagation();
    e.preventDefault();
    navigate(path);
  };

  const handleSelect = (e: React.MouseEvent) => {
    e.stopPropagation();
    data.onSelect(data.publicId);
  };

  return (
    <div className={`
      group relative bg-slate-900/95 backdrop-blur-xl border 
      ${data.isSelected
        ? 'border-brand-accent shadow-[0_0_24px_rgba(20,184,166,0.35)] ring-1 ring-brand-accent/50'
        : data.vacant ? 'border-red-900/50 shadow-[0_0_15px_rgba(239,68,68,0.1)]' : 'border-slate-800 shadow-2xl'} 
      p-0 w-[280px] min-h-[85px] rounded-sm transition-all hover:border-brand-accent/50 flex flex-col
    `}>
      <div className="h-1 w-full shrink-0" style={{ backgroundColor: rankColor }} />
      <Handle type="target" position={Position.Left} className="!bg-slate-600 !w-2 !h-2 !border-none" />

      <div className="p-3 flex-1 flex items-center gap-3" onClick={handleSelect}>
        <div
          className={`nodrag cursor-pointer w-10 h-10 flex items-center justify-center rounded-sm shrink-0 border transition-all
            ${data.vacant ? 'bg-red-500/10 border-red-500/20 hover:bg-red-500/20' : 'bg-brand-accent/5 border-brand-accent/20 hover:bg-brand-accent/10'}`}
        >
          {data.vacant ? <UserMinus size={18} className="text-red-500" /> : <Briefcase size={18} className="text-brand-accent" />}
        </div>

        <div className="flex-1 min-w-0">
          <div className="nodrag cursor-pointer flex items-center gap-1.5 group/title">
            <span className="text-[10px] font-black text-white uppercase tracking-wider leading-tight break-words hover:text-brand-accent transition-colors">
              {data.title}
            </span>
            {/* NEW: explicit "open full dossier" action, separate from
                the select/highlight click on the rest of the card. */}
            <button
              onClick={(e) => handleNav(e, `/occupations/${data.publicId}`)}
              title="Open full dossier"
              className="nodrag opacity-0 group-hover/title:opacity-60 hover:!opacity-100 transition-opacity shrink-0"
            >
              <ExternalLink size={10} className="text-brand-accent" />
            </button>
          </div>

          <div className="flex flex-col gap-0.5 mt-1">
            <span
              onClick={(e) => {
                if (!data.vacant && data.personPublicId) {
                  handleNav(e, `/personnel/${data.personPublicId}`);
                }
              }}
              className={`nodrag text-[9px] font-mono uppercase tracking-tighter transition-all flex items-center gap-1
                ${data.vacant ? 'text-red-400 italic' : 'text-slate-400 cursor-pointer hover:text-emerald-400'}`}
            >
              {data.vacant ? '[ VACANT_SLOT ]' : (
                <>
                  {data.currentOccupantName}
                  <ExternalLink size={8} className="opacity-0 group-hover:opacity-40" />
                </>
              )}
            </span>
            <span className="text-[7px] text-slate-600 font-mono tracking-widest">CODE_{data.code}{data.rank ? ` // ${data.rank}` : ''}</span>
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

const NODE_TYPES = { occNode: OccupationFlowGraphNode };

const OccupationFlowInternal: React.FC<{ data: OccupationTreeResponse[] }> = ({ data }) => {
  const flowRef = useRef<HTMLDivElement>(null);
  const { fitView, setCenter, getNode } = useReactFlow();
  const [collapsedIds, setCollapsedIds] = useState<Set<string>>(new Set());
  // NEW: tracks which node is currently selected/highlighted within the
  // already-loaded tree. Replaces the previous click-to-navigate behavior,
  // which discarded the surrounding tree (parents, siblings) every time a
  // child node was clicked, since OccupationDetail's Nexus always loads a
  // fresh subtree rooted at whichever node you navigated to.
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const toggleNode = useCallback((id: string) => {
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  // NEW: selecting a node centers the viewport on it (without changing
  // zoom or reloading any data) in addition to marking it highlighted.
  const selectNode = useCallback((id: string) => {
    setSelectedId(id);
    const node = getNode(id);
    if (node) {
      const x = node.position.x + (node.width ?? 280) / 2;
      const y = node.position.y + (node.height ?? 85) / 2;
      setCenter(x, y, { zoom: undefined, duration: 500 });
    }
  }, [getNode, setCenter]);

  const { nodes, edges } = useMemo(() => {
    const nodes: Node<OccupationFlowNodeData>[] = [];
    const edges: Edge[] = [];

    const traverse = (items: OccupationTreeResponse[], parentId: string | null = null, isVisible = true) => {
      items.forEach((item) => {
        const isCollapsed = collapsedIds.has(item.publicId);

        if (isVisible) {
          nodes.push({
            id: item.publicId,
            type: 'occNode',
            data: {
              ...item,
              onToggle: toggleNode,
              isExpanded: !isCollapsed,
              hasChildren: !!(item.subordinates && item.subordinates.length > 0),
              personPublicId: item.personPublicId,
              onSelect: selectNode,
              isSelected: item.publicId === selectedId,
            },
            position: { x: 0, y: 0 },
          });

          if (parentId) {
            edges.push({
              id: `e-${parentId}-${item.publicId}`,
              source: parentId,
              target: item.publicId,
              type: ConnectionLineType.SmoothStep,
              // CHANGED: item.isVacant -> item.vacant. Same renamed-field
              // fix as above — edge color (red for vacant downstream
              // positions, teal otherwise) was always teal regardless of
              // actual vacancy.
              style: { stroke: item.vacant ? '#ef4444' : '#14b8a6', strokeWidth: 1.5, opacity: 0.4 },
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
  }, [data, collapsedIds, toggleNode, selectedId, selectNode]);

  // CHANGED: added `data` to the dependency array (was only
  // [collapsedIds, fitView]). This is the actual root cause of the
  // centering bug. In OccupationDetail.tsx's modal, HierarchyFlow mounts
  // fresh each time (data fetch gated on isNexusOpen, component unmounts
  // on modal close) — so this effect "happened" to run on mount anyway,
  // masking the missing `data` dependency. In HierarchyPage.tsx, where
  // the component could stay mounted/revealed via conditional rendering
  // rather than a true remount, the effect never re-ran when a NEW
  // dataset arrived, since `data` wasn't a tracked dependency — only
  // expanding/collapsing a node (changing collapsedIds) re-centered it.
  // Including `data` makes the centering behavior correct regardless of
  // how the parent component mounts/remounts this one.
  useEffect(() => {
    const timer = setTimeout(() => fitView({ padding: 0.2, duration: 800 }), 50);
    return () => clearTimeout(timer);
  }, [data, collapsedIds, fitView]);

  return (
    <div ref={flowRef} className="w-full h-full relative bg-[#020617]">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={NODE_TYPES}
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