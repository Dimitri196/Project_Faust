import React, { useState, useEffect } from 'react';
import {
  ChevronRight,
  ChevronDown,
  Building2,
  Landmark,
  ShieldAlert,
  Zap,
  SearchCode,
  Scale,
  ShieldCheck,
  Network,
  Loader2
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import type { InstitutionTreeResponse, HierarchicalLevel, InstitutionType } from '../../types';

interface NodeProps {
  node: InstitutionTreeResponse;
  depth: number;
  forceOpen?: boolean;
  onFocusClick: (id: string) => void;
}

const getLevelStyle = (level: HierarchicalLevel): string => {
  const styles: Record<HierarchicalLevel, string> = {
    INTERNATIONAL: 'text-purple-400 border-purple-400/30 bg-purple-400/5',
    NATIONAL: 'text-brand-accent border-brand-accent/30 bg-brand-accent/5',
    REGIONAL: 'text-blue-400 border-blue-400/30 bg-blue-400/5',
    LOCAL: 'text-emerald-400 border-emerald-400/30 bg-emerald-400/5',
    SUB_LOCAL: 'text-slate-500 border-slate-500/30 bg-slate-500/5'
  };
  return styles[level] || styles.NATIONAL;
};

const getInstitutionIcon = (type: InstitutionType) => {
  switch (type) {
    case 'INTELLIGENCE':
      return <ShieldAlert size={16} className="text-red-500 animate-pulse" />;
    case 'MILITARY':
      return <ShieldCheck size={16} className="text-orange-500" />;
    case 'LEGISLATIVE':
      return <Landmark size={16} className="text-purple-400" />;
    case 'REGULATORY':
      return <Zap size={16} className="text-yellow-400" />;
    case 'JUDICIAL':
      return <Scale size={16} className="text-blue-400" />;
    case 'EXECUTIVE':
      return <Building2 size={16} className="text-brand-accent" />;
    default:
      return <Building2 size={16} className="text-slate-500" />;
  }
};

const InstitutionTree: React.FC<NodeProps> = ({ node, depth, forceOpen = false, onFocusClick }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [childNodes, setChildNodes] = useState<InstitutionTreeResponse[]>(node.children || []);
  const [isLoading, setIsLoading] = useState(false);
  // NEW: tracks fetch failures so the user sees an explicit error state
  // instead of a row that silently never expands (previously, a failed
  // fetchChildren only logged to console — the UI gave no feedback at all).
  const [hasError, setHasError] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (forceOpen) {
      setIsExpanded(true);
      if (childNodes.length === 0 && node.hasChildren) {
        fetchChildren();
      }
    }
  }, [forceOpen]);

  useEffect(() => {
    if (node.children && node.children.length > 0) {
      setChildNodes(node.children);
    }
  }, [node.children]);

  const fetchChildren = async () => {
    if (isLoading) return;
    setIsLoading(true);
    setHasError(false);
    try {
      // CHANGED: api.get already targets the configured base URL (with
      // the JWT interceptor attached) — no other change needed here since
      // this call never had the raw-axios bug other pages had. Confirmed
      // path matches institutionService's getChildren for consistency.
      const res = await api.get(`/institutions/parent/${node.publicId}`);
      if (Array.isArray(res.data)) {
        setChildNodes(res.data);
      }
    } catch (err) {
      console.error(`FAUST_CORE_ERR: Failed to fetch subordinates for ${node.publicId}`, err);
      setHasError(true);
    } finally {
      setIsLoading(false);
    }
  };

  const toggleExpand = async (e: React.MouseEvent) => {
    e.stopPropagation();
    const nextState = !isExpanded;

    if (nextState && childNodes.length === 0 && node.hasChildren) {
      await fetchChildren();
    }

    setIsExpanded(nextState);
  };

  const handleNavigate = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/institutions/${node.publicId}`);
  };

  const handleFocusMode = (e: React.MouseEvent) => {
    e.stopPropagation();
    onFocusClick(node.publicId);
  };

  const hasLoadedChildren = childNodes && childNodes.length > 0;

  return (
    <div className="relative">
      {/* MAIN NODE ROW */}
      <div
        className={`group flex items-center gap-4 p-3 mb-1 border border-transparent transition-all duration-200
          ${isExpanded && hasLoadedChildren ? 'bg-brand-panel/40 border-brand-border/30 shadow-lg' : 'hover:bg-white/5 hover:border-brand-border/20'}`}
      >
        <div className="flex items-center gap-2 z-10">
          <div
            onClick={toggleExpand}
            className={`flex items-center justify-center w-6 h-6 rounded transition-colors cursor-pointer
              ${node.hasChildren ? 'hover:bg-brand-accent/20' : 'opacity-20 pointer-events-none'}`}
          >
            {isLoading ? (
              <Loader2 size={12} className="text-brand-accent animate-spin" />
            ) : isExpanded ? (
              <ChevronDown size={14} className="text-brand-accent" />
            ) : (
              <ChevronRight size={14} className="text-slate-500" />
            )}
          </div>

          <div className="flex items-center justify-center w-8 h-8 bg-black/20 border border-brand-border/20 rounded-sm">
            {getInstitutionIcon(node.type)}
          </div>
        </div>

        {/* INSTITUTION IDENTITY */}
        <div className="flex-1 min-w-0 cursor-pointer group/label" onClick={handleNavigate}>
          <div className="flex flex-col">
            <div className="flex items-center gap-3">
              <span className="text-xs font-black tracking-widest text-slate-200 uppercase truncate group-hover/label:text-brand-accent transition-colors">
                {node.name}
              </span>
              <span className={`text-[8px] px-1.5 py-0.5 border font-mono font-bold whitespace-nowrap hidden sm:block ${getLevelStyle(node.level)}`}>
                {node.level}
              </span>
            </div>
            <div className="flex items-center gap-2 mt-0.5">
               <span className="text-[9px] font-mono text-slate-600 uppercase tracking-tighter">
                 Type::{node.type}
               </span>
               {node.stateOwned && (
                <div className="flex items-center gap-1 text-yellow-500/40 text-[8px] font-mono uppercase">
                  <span className="w-1 h-1 bg-yellow-500 rounded-full animate-pulse" />
                  State_Prop
                </div>
               )}
               {/* NEW: inline retry affordance when a child fetch fails.
                   Previously a failed fetchChildren left the row stuck —
                   no children appeared and no error was shown, so the
                   only recourse was a full page refresh. */}
               {hasError && (
                 <button
                   onClick={(e) => { e.stopPropagation(); fetchChildren(); }}
                   className="text-[8px] font-mono uppercase text-red-400 hover:text-red-300 underline"
                 >
                   Fetch_Failed // Retry
                 </button>
               )}
            </div>
          </div>
        </div>

        {/* SYSTEM ACTIONS */}
        <div className="flex items-center gap-2 font-mono text-[9px]">
          <button
            onClick={handleFocusMode}
            className="hidden group-hover:flex items-center gap-2 px-3 py-1.5 bg-blue-600/20 text-blue-400 border border-blue-500/30 font-black tracking-tighter uppercase hover:bg-blue-600 hover:text-white transition-all"
            title="Visualize Structural Nexus"
          >
            <Network size={12} />
            Nexus_Focus
          </button>

          <button
            onClick={handleNavigate}
            className="hidden group-hover:flex items-center gap-2 px-3 py-1.5 bg-brand-accent text-brand-dark font-black tracking-tighter uppercase hover:bg-white transition-all"
          >
            <SearchCode size={12} />
            Inspect_Node
          </button>

          <span className="text-slate-800 hidden lg:block tracking-widest font-mono text-[8px] ml-2">
            ID::{node.publicId.split('-')[0]}
          </span>
        </div>
      </div>

      {/* RECURSIVE BRANCHES (CHILDREN) */}
      {isExpanded && (
        <div className="ml-7 relative animate-in fade-in slide-in-from-left-2 duration-300">
          <div className="absolute left-[-15px] top-0 bottom-6 w-[1px] bg-brand-border/30" />

          <div className="space-y-1 pt-1">
            {hasLoadedChildren ? (
              childNodes.map((child: InstitutionTreeResponse) => (
                <div key={child.publicId} className="relative">
                  <div className="absolute left-[-15px] top-[22px] w-3.5 h-[1px] bg-brand-border/30" />
                  <InstitutionTree
                    node={child}
                    depth={depth + 1}
                    forceOpen={forceOpen}
                    onFocusClick={onFocusClick}
                  />
                </div>
              ))
            ) : (
              isLoading && (
                <div className="flex items-center gap-3 p-2 ml-4 opacity-40">
                  <Loader2 size={12} className="animate-spin text-brand-accent" />
                  <span className="text-[10px] font-mono text-slate-500 uppercase tracking-tighter">Syncing_Sub_Structures...</span>
                </div>
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default InstitutionTree;