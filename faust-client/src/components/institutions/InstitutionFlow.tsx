import React, { useMemo, useCallback, useRef, useState, useEffect } from 'react';
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
  getNodesBounds,
  type Edge,
  type Node,
  type NodeProps,
} from 'reactflow';
import dagre from 'dagre';
import { useNavigate } from 'react-router-dom';
import { toPng } from 'html-to-image';
import 'reactflow/dist/style.css';
import {
  Building2, Landmark, ShieldAlert, Zap, Scale, ShieldCheck, ChevronRight, ChevronDown, ExternalLink
} from 'lucide-react';
import type { InstitutionTreeResponse, InstitutionType, HierarchicalLevel } from '../../types';

const getInstitutionIcon = (type: string) => {
  switch (type) {
    case 'INTELLIGENCE': return <ShieldAlert size={20} className="text-red-500 animate-pulse" />;
    case 'MILITARY': return <ShieldCheck size={20} className="text-orange-500" />;
    case 'LEGISLATIVE': return <Landmark size={20} className="text-purple-400" />;
    case 'REGULATORY': return <Zap size={20} className="text-yellow-400" />;
    case 'JUDICIAL': return <Scale size={20} className="text-blue-400" />;
    case 'EXECUTIVE': return <Building2 size={20} className="text-brand-accent" />;
    default: return <Building2 size={20} className="text-slate-500" />;
  }
};

const getLevelColor = (level: string) => {
  const colors: Record<string, string> = {
    INTERNATIONAL: '#c084fc',
    NATIONAL: '#14b8a6',
    REGIONAL: '#60a5fa',
    LOCAL: '#34d399',
    SUB_LOCAL: '#94a3b8'
  };
  return colors[level] || colors.NATIONAL;
};

// CHANGED: explicit shape for the data this custom node actually receives,
// replacing `data: any`. Mirrors InstitutionTreeResponse plus the extra
// fields injected by processedElements below. NEW: added onSelect/
// isSelected for the click-to-highlight behavior — clicking a node now
// selects/centers it within the SAME loaded tree instead of navigating
// to InstitutionDetail, which previously discarded the entire
// surrounding tree (parents, siblings) every time you clicked into a
// child node. Same fix as applied to HierarchyFlow.tsx (occupations).
interface FlowNodeData extends InstitutionTreeResponse {
  onToggle: (id: string) => void;
  isExpanded: boolean;
  hasChildren: boolean;
  onSelect: (id: string) => void;
  isSelected: boolean;
}

// CHANGED: renamed from `InstitutionNode` to `InstitutionFlowGraphNode`.
// The previous name collided with the (now-deleted) standalone
// InstitutionNode.tsx component — a completely different, unrelated
// list-style tree node. That collision was the direct cause of confusion
// when figuring out which "InstitutionNode" was actually dead code.
// Renaming removes the ambiguity for anyone reading this file going forward.
const InstitutionFlowGraphNode = ({ data }: NodeProps<FlowNodeData>) => {
  const levelColor = getLevelColor(data.level);
  const [imgError, setImgError] = useState(false);
  const navigate = useNavigate();

  return (
    <div
      onClick={(e) => { e.stopPropagation(); data.onSelect(data.publicId); }}
      className={`group relative bg-slate-900/95 backdrop-blur-xl border ${
        data.isSelected
          ? 'border-brand-accent shadow-[0_0_24px_rgba(20,184,166,0.35)] ring-1 ring-brand-accent/50'
          : data.isExpanded ? 'border-slate-700' : 'border-slate-800'
      } p-0 shadow-2xl w-[320px] min-h-[95px] rounded-sm transition-all hover:border-brand-accent/50 flex flex-col cursor-pointer`}
    >
      <div className="h-1.5 w-full shrink-0" style={{ backgroundColor: levelColor }} />
      <Handle type="target" position={Position.Left} className="!bg-slate-500 !w-2 !h-2 !border-none" />

      <div className="p-4 flex-1">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 flex items-center justify-center bg-black/40 border border-slate-800 rounded-sm overflow-hidden shrink-0">
            {data.logoUrl && !imgError ? (
              <img
                src={data.logoUrl}
                alt=""
                className="w-full h-full object-contain p-1 grayscale group-hover:grayscale-0 transition-all duration-500"
                onError={() => setImgError(true)}
              />
            ) : (
              getInstitutionIcon(data.type)
            )}
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-1.5 group/title">
              <span className="text-[11px] font-black text-white uppercase tracking-wider leading-snug break-words">
                {data.name}
              </span>
              {/* NEW: explicit "open full dossier" action, separate from
                  the select/highlight click on the rest of the card —
                  same pattern added to HierarchyFlow.tsx's node. */}
              <button
                onClick={(e) => { e.stopPropagation(); navigate(`/institutions/${data.publicId}`); }}
                title="Open full dossier"
                className="opacity-0 group-hover/title:opacity-60 hover:!opacity-100 transition-opacity shrink-0"
              >
                <ExternalLink size={11} className="text-brand-accent" />
              </button>
            </div>
            <div className="flex items-center gap-2 mt-2">
              <span className="text-[8px] font-mono py-0.5 px-1.5 bg-white/5 text-slate-400 border border-white/5 uppercase">
                {data.level}
              </span>
            </div>
          </div>
        </div>
      </div>

      {data.hasChildren && (
        <button
          onClick={(e) => { e.stopPropagation(); data.onToggle(data.publicId); }}
          className="absolute -right-3 top-1/2 -translate-y-1/2 w-6 h-6 bg-slate-800 border border-slate-700 rounded-full flex items-center justify-center text-white hover:bg-brand-accent transition-all z-10 shadow-xl"
        >
          {data.isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </button>
      )}
      <Handle type="source" position={Position.Right} className="!bg-brand-accent !w-2 !h-2 !border-none" />
    </div>
  );
};

const InstitutionFlowInternal: React.FC<{ data: InstitutionTreeResponse[] }> = ({ data }) => {
  const flowRef = useRef<HTMLDivElement>(null);
  const { fitView, getNodes, setCenter, getNode } = useReactFlow();
  const [collapsedIds, setCollapsedIds] = useState<Set<string>>(new Set());
  // NEW: tracks which node is currently selected/highlighted within the
  // already-loaded tree. Replaces the previous behavior where ANY click
  // on ANY node (via onNodeClick on <ReactFlow>) immediately navigated
  // to InstitutionDetail, discarding the surrounding tree (parents,
  // siblings) every time. Same fix as HierarchyFlow.tsx (occupations).
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
      const x = node.position.x + (node.width ?? 320) / 2;
      const y = node.position.y + (node.height ?? 95) / 2;
      setCenter(x, y, { zoom: undefined, duration: 500 });
    }
  }, [getNode, setCenter]);

  const processedElements = useMemo(() => {
    const nodes: Node<FlowNodeData>[] = [];
    const edges: Edge[] = [];

    const traverse = (items: InstitutionTreeResponse[], parentId: string | null = null, isVisible = true) => {
      items.forEach((item) => {
        const isCollapsed = collapsedIds.has(item.publicId);
        if (isVisible) {
          nodes.push({
            id: item.publicId,
            type: 'instNode',
            data: {
              ...item,
              onToggle: toggleNode,
              isExpanded: !isCollapsed,
              hasChildren: !!(item.children && item.children.length > 0),
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
              style: { stroke: '#334155', strokeWidth: 2 },
              markerEnd: { type: 'arrowclosed' as any, color: '#334155' }
            });
          }
        }
        if (item.children && item.children.length > 0) {
          traverse(item.children, item.publicId, isVisible && !isCollapsed);
        }
      });
    };
    traverse(data);

    const g = new dagre.graphlib.Graph();
    g.setGraph({ rankdir: 'LR', nodesep: 80, ranksep: 300 });
    g.setDefaultEdgeLabel(() => ({}));
    nodes.forEach((n) => g.setNode(n.id, { width: 320, height: 100 }));
    edges.forEach((e) => g.setEdge(e.source, e.target));
    dagre.layout(g);

    return {
      nodes: nodes.map((n) => {
        const pos = g.node(n.id);
        return { ...n, position: { x: pos.x - 160, y: pos.y - 50 } };
      }),
      edges
    };
  }, [data, collapsedIds, toggleNode, selectedId, selectNode]);

  // CHANGED: dependency was processedElements.nodes.length (a count),
  // not the dataset itself. Two different institution sub-trees (e.g.
  // clicking "Nexus_Focus" on different ministries from InstitutionsPage)
  // can easily have the SAME node count, in which case this dependency
  // never changes and fitView() never re-fires — leaving the viewport
  // wherever it was for the previous tree. This is the actual reason
  // institution Nexus could appear off-center while occupation's
  // HierarchyFlow (which depends on `data` directly) always recentered
  // correctly. Depending on `data` itself fixes this for any new dataset,
  // regardless of whether it happens to have the same number of nodes as
  // whatever was shown before.
  // CHANGED: previously deferred only one requestAnimationFrame, which
  // fixed the "zero-size on first mount" problem but NOT a second,
  // separate issue — InstitutionDetail.tsx wraps this entire component in
  // a 300ms CSS `animate-in fade-in zoom-in-95` transform on the modal
  // container. A single rAF fires on the very next paint, almost
  // certainly while that scale transform is still mid-animation (95% ->
  // 100%). fitView() measures the container's bounding box at whatever
  // moment it runs — if that's mid-transform, the computed fit is wrong
  // even though the deferred-by-one-frame timing looked correct in
  // isolation. Waiting out the full 300ms animation before calling
  // fitView() ensures the container has reached its final, settled size
  // first. 350ms gives a small safety margin past the animation's exact
  // duration.
  useEffect(() => {
    if (processedElements.nodes.length > 0) {
      const timer = setTimeout(() => {
        fitView({ duration: 0 });
      }, 350);
      return () => clearTimeout(timer);
    }
  }, [data, fitView]);

  const onExportPng = useCallback(async () => {
    if (flowRef.current === null) return;
    const elementToCapture = flowRef.current.querySelector('.react-flow__viewport') as HTMLElement;

    if (elementToCapture) {
      const nodesRect = getNodesBounds(getNodes());
      const padding = 150;
      const width = nodesRect.width + padding * 2;
      const height = nodesRect.height + padding * 2;

      try {
        const dataUrl = await toPng(elementToCapture, {
          backgroundColor: '#020617',
          width: width,
          height: height,
          style: {
            width: `${width}px`,
            height: `${height}px`,
            transform: `translate(${-nodesRect.x + padding}px, ${-nodesRect.y + padding}px) scale(1)`,
          },
          // Skip images that would taint the canvas due to missing CORS headers.
          // These are images from servers that don't send Access-Control-Allow-Origin
          // (e.g. mo.gov.cz military sites) — they render fine in the browser
          // via <img> but can't be drawn to canvas without crossOrigin="anonymous".
          // Skipping them lets the export succeed with institution icons as fallback
          // rather than failing entirely.
          filter: (node) => {
            if (node instanceof HTMLImageElement) {
              // Only include images that loaded successfully from CORS-permissive sources
              try {
                const canvas = document.createElement('canvas');
                const ctx = canvas.getContext('2d');
                if (ctx) {
                  ctx.drawImage(node, 0, 0);
                  canvas.toDataURL();
                }
                return true;
              } catch {
                return false; // tainted canvas — skip this image
              }
            }
            return true;
          }
        });

        const link = document.createElement('a');
        link.download = `nexus-intel-snapshot-${Date.now()}.png`;
        link.href = dataUrl;
        link.click();
      } catch (err) {
        console.error("PNG Snapshot failed:", err);
      }
    }
  }, [getNodes]);

  return (
    <div ref={flowRef} className="w-full h-full relative bg-[#020617]">
      <ReactFlow
        nodes={processedElements.nodes}
        edges={processedElements.edges}
        nodeTypes={{ instNode: InstitutionFlowGraphNode }}
        minZoom={0.02}
        maxZoom={2}
      >
        <Background variant={BackgroundVariant.Lines} color="#0f172a" gap={50} />

        <Panel position="top-right" className="flex gap-2 p-2 bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded shadow-2xl">
          <button
            onClick={() => fitView({ duration: 0 })}
            className="px-4 py-2 text-[10px] font-bold text-blue-400 border border-blue-500/20 uppercase hover:bg-blue-500/10"
          >
            Recenter_View
          </button>
          <button
            onClick={onExportPng}
            className="px-4 py-2 text-[10px] font-bold text-emerald-400 border border-emerald-500/20 uppercase hover:bg-emerald-500/10"
          >
            Intelligence_Snapshot
          </button>
        </Panel>

        <Controls />
      </ReactFlow>
    </div>
  );
};

export const InstitutionFlow: React.FC<{ data: InstitutionTreeResponse[] }> = (props) => (
  <ReactFlowProvider>
    <InstitutionFlowInternal {...props} />
  </ReactFlowProvider>
);