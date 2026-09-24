import { useQuery } from '@tanstack/react-query';
import api from '../api/axios';
import type { OccupationTreeResponse } from '../types';
import {
  ShieldAlert, Activity, Target, LayoutList, Network,
  X, User, ExternalLink, ChevronRight, ChevronDown,
  ShieldCheck, Eye, EyeOff, Briefcase
} from 'lucide-react';
import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { countNodes, countVacancies } from '../utils/occupationTree';
import HierarchyFlow from '../components/occupations/HierarchyFlow';

// ── Types ────────────────────────────────────────────────────────────────────

interface SelectedNode {
  publicId: string;
  title: string;
  code: string;
  rank: string;
  vacant: boolean;
  currentOccupantName?: string | null;
  currentOccupantId?: string | null;
}

// ── Main Page ────────────────────────────────────────────────────────────────

const HierarchyPage = () => {
  const [viewMode, setViewMode]         = useState<'tactical' | 'visual'>('tactical');
  const [selectedNode, setSelectedNode] = useState<SelectedNode | null>(null);
  const [vacancyOnly, setVacancyOnly]   = useState(false);

  const { data: tree, isLoading } = useQuery<OccupationTreeResponse[]>({
    queryKey: ['occupation-tree'],
    queryFn: async () => {
      const res = await api.get('/occupations/tree');
      return res.data;
    }
  });

  const totalSlots  = tree ? countNodes(tree) : 0;
  const vacantSlots = tree ? countVacancies(tree) : 0;

  const handleNodeSelect = useCallback((node: SelectedNode) => {
    setSelectedNode(prev => prev?.publicId === node.publicId ? null : node);
  }, []);

  // Filter tree to vacancy-only nodes when toggle is active
  const filterVacant = (nodes: OccupationTreeResponse[]): OccupationTreeResponse[] => {
    if (!vacancyOnly) return nodes;
    return nodes.reduce<OccupationTreeResponse[]>((acc, node) => {
      const filteredChildren = node.subordinates
        ? filterVacant(node.subordinates)
        : [];
      if (node.vacant || filteredChildren.length > 0) {
        acc.push({ ...node, subordinates: filteredChildren });
      }
      return acc;
    }, []);
  };

  const displayTree = tree ? filterVacant(tree) : [];

  return (
    <div className="h-full flex flex-col bg-brand-dark overflow-hidden font-sans">

      {/* HEADER */}
      <div className="px-6 py-4 border-b border-brand-border bg-brand-panel/20 relative shrink-0">
        <div className="absolute top-0 right-0 w-64 h-full bg-gradient-to-l from-brand-accent/5 to-transparent pointer-events-none" />
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4 relative z-10">

          {/* Title */}
          <div>
            <div className="flex items-center gap-2 text-red-500 mb-1">
              <ShieldAlert size={12} className="animate-pulse" />
              <span className="text-[9px] font-mono tracking-[0.4em] uppercase">Security Clearance: LEVEL_05</span>
            </div>
            <h1 className="text-3xl font-black text-white italic tracking-tighter uppercase leading-none">
              Command_Hierarchy
            </h1>
            <p className="text-[9px] font-mono text-slate-600 mt-1 tracking-[0.2em] uppercase">
              Operational Real-time Structural Mapping // Project_Faust
            </p>
          </div>

          {/* Controls */}
          <div className="flex items-center gap-3 flex-wrap">

            {/* Vacancy filter toggle */}
            <button
              onClick={() => setVacancyOnly(v => !v)}
              className={`flex items-center gap-2 px-3 py-2 text-[9px] font-mono font-bold uppercase tracking-widest border transition-all ${
                vacancyOnly
                  ? 'bg-red-500/20 border-red-500/50 text-red-400 animate-pulse'
                  : 'bg-black/40 border-brand-border text-slate-500 hover:border-red-500/40 hover:text-red-400'
              }`}
            >
              {vacancyOnly ? <Eye size={12} /> : <EyeOff size={12} />}
              {vacancyOnly ? 'Vacancies_Only' : 'Show_All'}
            </button>

            {/* View switcher */}
            <div className="flex bg-black/40 border border-brand-border p-0.5">
              <button
                onClick={() => setViewMode('tactical')}
                className={`flex items-center gap-2 px-3 py-1.5 text-[9px] font-mono transition-all ${
                  viewMode === 'tactical' ? 'bg-brand-accent text-black font-bold' : 'text-slate-500 hover:bg-white/5'
                }`}
              >
                <LayoutList size={12} /> Tactical
              </button>
              <button
                onClick={() => setViewMode('visual')}
                className={`flex items-center gap-2 px-3 py-1.5 text-[9px] font-mono transition-all ${
                  viewMode === 'visual' ? 'bg-brand-accent text-black font-bold' : 'text-slate-500 hover:bg-white/5'
                }`}
              >
                <Network size={12} /> Visual
              </button>
            </div>

            {/* Stats */}
            <StatBox label="Total_Nodes" value={vacancyOnly ? displayTree.length : totalSlots} icon={<Activity size={11} />} />
            <StatBox label="Vacancies"   value={vacantSlots} icon={<Target size={11} />} color="text-red-500" />
          </div>
        </div>
      </div>

      {/* MAIN AREA */}
      <div className="flex-1 flex overflow-hidden">

        {/* TREE VIEWPORT */}
        <div className="flex-1 relative overflow-hidden bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px]">
          {isLoading ? (
            <div className="flex flex-col items-center justify-center h-full gap-4 animate-pulse">
              <div className="w-12 h-1 bg-brand-accent" />
              <div className="font-mono text-[10px] text-brand-accent uppercase tracking-[1em]">
                Establishing_Chain_Link...
              </div>
            </div>
          ) : (
            <div className="h-full">
              {viewMode === 'tactical' ? (
                <div className="overflow-auto h-full p-6 custom-scrollbar">
                  <div className="max-w-5xl mx-auto space-y-4">
                    {displayTree.length === 0 && vacancyOnly ? (
                      <div className="py-20 text-center font-mono text-slate-700 text-[10px] uppercase tracking-widest border border-dashed border-brand-border/20">
                        No_Vacancies_Detected — All_Positions_Filled
                      </div>
                    ) : (
                      displayTree.map((root) => (
                        <div key={root.publicId} className="bg-black/20 border border-brand-border/30 p-4 backdrop-blur-sm">
                          <HierarchyNode
                            node={root}
                            depth={0}
                            selectedId={selectedNode?.publicId}
                            onSelect={handleNodeSelect}
                          />
                        </div>
                      ))
                    )}
                  </div>
                </div>
              ) : (
                <div className="w-full h-full">
                  <HierarchyFlow key={viewMode} data={tree || []} />
                </div>
              )}
            </div>
          )}
        </div>

        {/* RIGHT PANEL — slides in when a node is selected */}
        {selectedNode && (
          <NodeIntelPanel node={selectedNode} onClose={() => setSelectedNode(null)} />
        )}
      </div>
    </div>
  );
};

// ── Hierarchy Node ────────────────────────────────────────────────────────────
// Replaces OccupationNodeTactical on this page — same visual language but
// calls onSelect instead of navigate() on row click, so the right panel
// opens instead of leaving the page.

interface HierarchyNodeProps {
  node: OccupationTreeResponse;
  depth: number;
  selectedId?: string;
  onSelect: (node: SelectedNode) => void;
}

const HierarchyNode: React.FC<HierarchyNodeProps> = ({
  node, depth, selectedId, onSelect
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const hasChildren = node.subordinates && node.subordinates.length > 0;
  const isSelected  = selectedId === node.publicId;

  const handleRowClick = () => {
    onSelect({
      publicId:            node.publicId,
      title:               node.title,
      code:                node.code,
      rank:                node.rank,
      vacant:              node.vacant,
      currentOccupantName: node.currentOccupantName,
      currentOccupantId:   node.currentOccupantId,
    });
  };

  return (
    <div className="font-mono">
      <div
        className={`group flex items-center p-2.5 border-l-2 transition-all cursor-pointer mb-0.5 ${
          isSelected
            ? 'border-brand-accent bg-brand-accent/10'
            : node.vacant
              ? 'border-red-500/50 bg-red-500/5 hover:bg-red-500/10'
              : 'border-brand-accent/30 bg-brand-panel/20 hover:bg-brand-accent/10'
        }`}
        style={{ marginLeft: `${depth * 24}px` }}
        onClick={handleRowClick}
      >
        {/* Toggle */}
        <div
          onClick={(e) => { e.stopPropagation(); setIsOpen(o => !o); }}
          className="p-1 hover:bg-white/10 rounded mr-2 shrink-0"
        >
          {hasChildren
            ? isOpen
              ? <ChevronDown size={13} className="text-brand-accent" />
              : <ChevronRight size={13} className="text-slate-500" />
            : <div className="w-[13px]" />}
        </div>

        {/* Status icon */}
        <div className="mr-3 shrink-0">
          {node.vacant
            ? <ShieldAlert size={14} className="text-red-500 animate-pulse" />
            : <ShieldCheck size={14} className="text-brand-accent" />}
        </div>

        {/* Content */}
        <div className="flex-1 flex items-center justify-between min-w-0">
          <div className="truncate mr-3">
            <span className="text-[9px] text-slate-600 mr-2">[{node.code}]</span>
            <span className={`text-[11px] font-bold uppercase tracking-tighter transition-colors ${
              isSelected ? 'text-brand-accent' : 'text-white group-hover:text-brand-accent'
            }`}>
              {node.title}
            </span>
          </div>

          <div className="flex items-center gap-4 shrink-0">
            <div className="hidden md:flex items-center gap-1.5 px-2 py-0.5 bg-black/40 border border-white/5">
              <User size={10} className={node.vacant ? 'text-red-500/50' : 'text-brand-accent'} />
              <span className={`text-[9px] font-bold uppercase tracking-widest ${
                node.vacant ? 'text-red-500/40 italic' : 'text-slate-300'
              }`}>
                {node.vacant ? 'VACANT' : (node.currentOccupantName || 'IN_TRANSITION')}
              </span>
            </div>
            <div className="text-[8px] bg-brand-accent/10 border border-brand-accent/20 px-1.5 py-0.5 text-brand-accent font-black">
              RANK_{node.rank}
            </div>
          </div>
        </div>
      </div>

      {isOpen && hasChildren && (
        <div className="animate-in fade-in slide-in-from-top-1 duration-200">
          {node.subordinates!.map(child => (
            <HierarchyNode
              key={child.publicId}
              node={child}
              depth={depth + 1}
              selectedId={selectedId}
              onSelect={onSelect}
            />
          ))}
        </div>
      )}
    </div>
  );
};

// ── Right Panel — Node Intel ──────────────────────────────────────────────────

const NodeIntelPanel: React.FC<{
  node: SelectedNode;
  onClose: () => void;
}> = ({ node, onClose }) => {
  const navigate = useNavigate();

  return (
    <div className="w-80 shrink-0 bg-[#020617] border-l border-brand-border flex flex-col animate-in slide-in-from-right duration-300 shadow-[-10px_0_30px_rgba(0,0,0,0.5)]">

      {/* Panel header */}
      <div className={`px-4 py-3 border-b flex items-center justify-between ${
        node.vacant ? 'border-red-500/30 bg-red-950/20' : 'border-brand-border bg-brand-panel/20'
      }`}>
        <div>
          <div className="text-[8px] font-mono text-slate-500 uppercase tracking-widest mb-0.5">
            Node_Intelligence
          </div>
          <div className={`text-[10px] font-mono font-bold uppercase tracking-tight ${
            node.vacant ? 'text-red-400' : 'text-brand-accent'
          }`}>
            [{node.code}]
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-slate-600 hover:text-red-400 transition-colors p-1 border border-slate-800 hover:border-red-500/40"
        >
          <X size={12} />
        </button>
      </div>

      {/* Position title */}
      <div className="px-4 py-4 border-b border-brand-border/30">
        <div className="text-sm font-black text-white uppercase tracking-tight leading-tight mb-2">
          {node.title}
        </div>
        <div className={`inline-flex items-center gap-1.5 px-2 py-1 text-[8px] font-mono font-bold border ${
          node.vacant
            ? 'bg-red-500/10 border-red-500/30 text-red-400'
            : 'bg-brand-accent/10 border-brand-accent/20 text-brand-accent'
        }`}>
          {node.vacant
            ? <><ShieldAlert size={9} className="animate-pulse" /> POSITION_VACANT</>
            : <><ShieldCheck size={9} /> POSITION_FILLED</>}
        </div>
      </div>

      {/* Intel sections */}
      <div className="flex-1 overflow-y-auto custom-scrollbar px-4 py-4 space-y-4">

        {/* Rank */}
        <IntelRow label="Rank_Classification" value={`RANK_${node.rank}`} />

        {/* Current occupant */}
        <div>
          <div className="text-[8px] font-mono text-slate-600 uppercase tracking-widest mb-1.5">
            Current_Occupant
          </div>
          {node.vacant ? (
            <div className="p-3 border border-red-500/20 bg-red-500/5 text-[9px] font-mono text-red-500/60 uppercase italic">
              No_Active_Assignment
            </div>
          ) : (
            <div className="p-3 border border-brand-border/40 bg-black/30 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <User size={12} className="text-brand-accent shrink-0" />
                <span className="text-[10px] font-bold text-white uppercase tracking-tight">
                  {node.currentOccupantName}
                </span>
              </div>
              {node.currentOccupantId && (
                <button
                  onClick={() => navigate(`/personnel/${node.currentOccupantId}`)}
                  className="flex items-center gap-1 text-[8px] font-mono text-brand-accent/60 hover:text-brand-accent transition-colors"
                >
                  <ExternalLink size={9} /> Dossier
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Footer — explicit full navigation */}
      <div className="px-4 py-3 border-t border-brand-border/30 space-y-2">
        <button
          onClick={() => navigate(`/occupations/${node.publicId}`)}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 text-[9px] font-mono font-bold uppercase tracking-widest border border-brand-accent/30 text-brand-accent hover:bg-brand-accent hover:text-black transition-all"
        >
          <Briefcase size={11} /> Open_Full_Dossier
        </button>
      </div>
    </div>
  );
};

// ── Helpers ───────────────────────────────────────────────────────────────────

const IntelRow = ({ label, value }: { label: string; value: string }) => (
  <div>
    <div className="text-[8px] font-mono text-slate-600 uppercase tracking-widest mb-1">{label}</div>
    <div className="text-[10px] font-mono text-slate-300 bg-black/30 border border-brand-border/30 px-2 py-1.5">
      {value}
    </div>
  </div>
);

const StatBox = ({ label, value, icon, color = "text-brand-accent" }: {
  label: string; value: number; icon: React.ReactNode; color?: string;
}) => (
  <div className="bg-black/40 border border-brand-border px-3 py-2 min-w-[100px]">
    <div className="flex items-center gap-1.5 text-[7px] text-slate-500 uppercase mb-0.5 font-bold">
      {icon} {label}
    </div>
    <div className={`text-xl font-black leading-none ${color}`}>{value}</div>
  </div>
);

export default HierarchyPage;