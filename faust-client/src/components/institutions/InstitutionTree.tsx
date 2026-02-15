import React, { useState, useEffect } from 'react';
import {
  ChevronRight,
  ChevronDown,
  Building2,
  Landmark,
  ShieldAlert,
  Zap,
  SearchCode,
  Gavel,
  Scale,
  ShieldCheck,
  Globe
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { InstitutionTreeResponse, HierarchicalLevel, InstitutionType } from '../../types';

interface NodeProps {
  node: InstitutionTreeResponse;
  depth: number;
  forceOpen?: boolean;
}

/**
 * Returns a CSS theme based on the hierarchical level of the institution.
 */
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

/**
 * Maps InstitutionType to specific tactical icons and colors.
 */
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
    case 'INTERNATIONAL' as any: // Fallback pro starší data
      return <Globe size={16} className="text-purple-400" />;
    default:
      return <Building2 size={16} className="text-slate-500" />;
  }
};

const InstitutionTree: React.FC<NodeProps> = ({ node, depth, forceOpen = false }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const navigate = useNavigate();

  // Sync state if forceOpen changes (e.g., during search)
  useEffect(() => {
    if (forceOpen) setIsExpanded(true);
  }, [forceOpen]);

  const handleNavigate = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/institutions/${node.publicId}`);
  };

  const hasChildren = node.children && node.children.length > 0;

  return (
    <div className="relative">
      {/* MAIN NODE ROW 
      */}
      <div
        className={`group flex items-center gap-4 p-3 mb-1 border border-transparent transition-all duration-200
          ${isExpanded && hasChildren ? 'bg-brand-panel/40 border-brand-border/30 shadow-lg' : 'hover:bg-white/5 hover:border-brand-border/20'}`}
      >
        {/* EXPAND ICON & TYPE ICON */}
        <div className="flex items-center gap-2 z-10">
          <div 
            onClick={(e) => { e.stopPropagation(); setIsExpanded(!isExpanded); }}
            className={`flex items-center justify-center w-6 h-6 rounded transition-colors cursor-pointer
              ${hasChildren ? 'hover:bg-brand-accent/20' : 'opacity-0 pointer-events-none'}`}
          >
            {isExpanded ? (
              <ChevronDown size={14} className="text-brand-accent" />
            ) : (
              <ChevronRight size={14} className="text-slate-500" />
            )}
          </div>

          <div className="flex items-center justify-center w-8 h-8 bg-black/20 border border-brand-border/20 rounded-sm">
            {getInstitutionIcon(node.type)}
          </div>
        </div>

        {/* IDENTITY SECTION */}
        <div
          className="flex-1 min-w-0 cursor-pointer group/label"
          onClick={handleNavigate}
        >
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
               {node.isStateOwned && (
                <div className="flex items-center gap-1 text-yellow-500/40 text-[8px] font-mono uppercase">
                  <span className="w-1 h-1 bg-yellow-500 rounded-full animate-pulse" />
                  State_Prop
                </div>
               )}
            </div>
          </div>
        </div>

        {/* SYSTEM ACTIONS Area */}
        <div className="flex items-center gap-4 font-mono text-[9px]">
          <button
            onClick={handleNavigate}
            className="hidden group-hover:flex items-center gap-2 px-3 py-1.5 bg-brand-accent text-brand-dark font-black tracking-tighter uppercase animate-in fade-in slide-in-from-right-2 duration-200 hover:bg-white transition-colors"
          >
            <SearchCode size={12} />
            Inspect_Node
          </button>

          <span className="text-slate-800 hidden lg:block tracking-widest font-mono text-[8px]">
            ID::{node.publicId.split('-')[0]}
          </span>
        </div>
      </div>

      {/* RECURSIVE BRANCHES (With Connector Lines) 
      */}
      {isExpanded && hasChildren && (
        <div className="ml-7 relative animate-in fade-in slide-in-from-left-2 duration-300">
          {/* Vertical Backbone Line */}
          <div className="absolute left-[-15px] top-0 bottom-6 w-[1px] bg-brand-border/30" />
          
          <div className="space-y-1 pt-1">
            {node.children.map((child: InstitutionTreeResponse) => (
              <div key={child.publicId} className="relative">
                {/* Horizontal Connector Line */}
                <div className="absolute left-[-15px] top-[22px] w-3.5 h-[1px] bg-brand-border/30" />
                
                <InstitutionTree 
                  node={child} 
                  depth={depth + 1} 
                  forceOpen={forceOpen}
                />
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default InstitutionTree;