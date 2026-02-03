import React, { useState } from 'react';
import { 
  ChevronRight, ChevronDown, Building2, Landmark, 
  ShieldAlert, Zap, SearchCode 
} from 'lucide-react';
import { useNavigate } from 'react-router-dom'; // Přidán import
import type { InstitutionTreeResponse, HierarchicalLevel } from '../types';

interface NodeProps {
  node: InstitutionTreeResponse;
  depth: number;
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

const InstitutionTree: React.FC<NodeProps> = ({ node, depth }) => {
  const [isExpanded, setIsExpanded] = useState(depth < 1);
  const navigate = useNavigate(); // Hook pro navigaci

  const handleNavigate = (e: React.MouseEvent) => {
    e.stopPropagation(); // Zabráníme rozbalení stromu při kliku na detail
    navigate(`/institutions/${node.publicId}`);
  };

  return (
    <div className="relative">
      {/* Connector Line */}
      {depth > 0 && (
        <div className="absolute -left-4 top-0 bottom-0 w-px bg-brand-border/20" />
      )}

      <div 
        className={`group flex items-center gap-4 p-3 mb-1 border border-transparent transition-all duration-200
          ${isExpanded ? 'bg-brand-panel/40 border-brand-border/50 shadow-lg' : 'hover:bg-white/5 hover:border-brand-border/30'}`}
      >
        {/* EXPAND ICON - Samostatná zóna pro kliknutí na rozbalení */}
        <div 
          onClick={() => setIsExpanded(!isExpanded)}
          className="flex items-center gap-2 cursor-pointer z-10 p-1 hover:bg-brand-accent/10 rounded"
        >
          {node.children && node.children.length > 0 ? (
            isExpanded ? <ChevronDown size={14} className="text-brand-accent" /> : <ChevronRight size={14} className="text-slate-500" />
          ) : (
            <div className="w-3.5" />
          )}
          
          {node.type === 'INTELLIGENCE' ? (
            <ShieldAlert size={16} className="text-red-500 animate-pulse" />
          ) : node.level === 'NATIONAL' ? (
            <Landmark size={16} className="text-brand-accent" />
          ) : (
            <Building2 size={16} className="text-slate-500" />
          )}
        </div>

        {/* CONTENT - Kliknutí sem vede na DETAIL */}
        <div 
          className="flex-1 min-w-0 cursor-pointer"
          onClick={handleNavigate}
        >
          <div className="flex items-center gap-3">
            <span className="text-xs font-bold tracking-widest text-slate-200 uppercase truncate group-hover:text-brand-accent transition-colors">
              {node.name}
            </span>
            <span className={`text-[8px] px-1.5 py-0.5 border font-mono font-bold whitespace-nowrap ${getLevelStyle(node.level)}`}>
              {node.level}
            </span>
          </div>
        </div>

        {/* ACTION AREA - Tlačítko a ID */}
        <div className="flex items-center gap-4 font-mono text-[9px]">
          {/* INSPECT BUTTON - Viditelný na hoveru */}
          <button 
            onClick={handleNavigate}
            className="hidden group-hover:flex items-center gap-1.5 px-2 py-1 bg-brand-accent text-brand-dark font-black tracking-tighter uppercase animate-in fade-in slide-in-from-right-2 duration-200"
          >
            <SearchCode size={10} />
            Inspect_Node
          </button>

          {node.isStateOwned && (
            <div className="flex items-center gap-1 text-yellow-500/60 uppercase">
              <Zap size={10} />
              <span className="hidden sm:block">State_Owned</span>
            </div>
          )}
          <span className="text-slate-700 hidden lg:block tracking-tighter">REF_{node.publicId.split('-')[0]}</span>
        </div>
      </div>

      {/* RECURSION RENDER */}
      {isExpanded && node.children && node.children.length > 0 && (
        <div className="ml-6 border-l border-brand-border/20 pl-2 animate-in fade-in slide-in-from-left-1 duration-300">
          {node.children.map((child) => (
            <InstitutionTree key={child.publicId} node={child} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

export default InstitutionTree;