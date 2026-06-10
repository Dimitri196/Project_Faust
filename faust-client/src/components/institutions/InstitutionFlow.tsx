import React, { useMemo, useCallback, useRef, useState } from 'react';
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
} from 'reactflow';
import dagre from 'dagre';
import { useNavigate } from 'react-router-dom';
import { toPng } from 'html-to-image';
import 'reactflow/dist/style.css';
import { 
  Building2, Landmark, ShieldAlert, Zap, Scale, ShieldCheck, ChevronRight, ChevronDown
} from 'lucide-react';
import type { InstitutionTreeResponse } from '../../types';

// --- POMOCNÉ FUNKCE PRO IKONY ---
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

// --- CUSTOM NODE KOMPONENTA ---
const InstitutionNode = ({ data }: { data: any }) => {
  const levelColor = getLevelColor(data.level);
  const [imgError, setImgError] = useState(false);

  return (
    <div className={`group relative bg-slate-900/95 backdrop-blur-xl border ${data.isExpanded ? 'border-slate-700' : 'border-slate-800'} p-0 shadow-2xl w-[320px] min-h-[95px] rounded-sm transition-all hover:border-brand-accent/50 flex flex-col`}>
      <div className="h-1.5 w-full shrink-0" style={{ backgroundColor: levelColor }} />
      <Handle type="target" position={Position.Left} className="!bg-slate-500 !w-2 !h-2 !border-none" />
      
      <div className="p-4 flex-1">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 flex items-center justify-center bg-black/40 border border-slate-800 rounded-sm overflow-hidden shrink-0">
            {data.logoUrl && !imgError ? (
              <img 
                src={data.logoUrl} 
                alt="" 
                crossOrigin="anonymous"
                className="w-full h-full object-contain p-1 grayscale group-hover:grayscale-0 transition-all duration-500"
                onError={() => setImgError(true)}
              />
            ) : (
              getInstitutionIcon(data.type)
            )}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[11px] font-black text-white uppercase tracking-wider leading-snug break-words">
              {data.name}
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

// --- INTERNÍ FLOW KOMPONENTA ---
const InstitutionFlowInternal: React.FC<{ data: InstitutionTreeResponse[] }> = ({ data }) => {
  const navigate = useNavigate();
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

  const processedElements = useMemo(() => {
    const nodes: Node[] = [];
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
              hasChildren: item.children && item.children.length > 0 
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
  }, [data, collapsedIds, toggleNode]);

  // --- BEZPEČNÝ EXPORT DO PNG S OCHRANOU PROTI CORS CHYBÁM ---
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
          // Ignoruje obrázky, které nejsou plně načtené nebo selhaly na CORS politice,
          // takže generování plátna nezkolabuje na "tainted canvas" chybě.
          filter: (node) => {
            if (node instanceof HTMLImageElement) {
              return node.complete && node.naturalWidth > 0;
            }
            return true;
          }
        });
        
        const link = document.createElement('a');
        link.download = `nexus-intel-snapshot-${Date.now()}.png`;
        link.href = dataUrl;
        link.click();
      } catch (err) {
        console.error("PNG Snapshot selhal:", err);
      }
    }
  }, [getNodes]);

  return (
    <div ref={flowRef} className="w-full h-full relative bg-[#020617]">
      <ReactFlow 
        nodes={processedElements.nodes} 
        edges={processedElements.edges} 
        nodeTypes={{ instNode: InstitutionNode }} 
        onNodeClick={(_, node) => navigate(`/institutions/${node.id}`)} 
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

// --- EXPORTOVANÝ PROVIDER ---
export const InstitutionFlow: React.FC<{ data: InstitutionTreeResponse[] }> = (props) => (
  <ReactFlowProvider>
    <InstitutionFlowInternal {...props} />
  </ReactFlowProvider>
);