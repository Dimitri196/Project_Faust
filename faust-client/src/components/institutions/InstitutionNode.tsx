import React, { useState } from 'react';
import { ChevronRight, ChevronDown, Building2, Shield, Landmark } from 'lucide-react';
import type { InstitutionTreeResponse } from '../../types';

interface NodeProps {
  node: InstitutionTreeResponse;
  depth: number;
}

const InstitutionNode: React.FC<NodeProps> = ({ node, depth }) => {
  const [isOpen, setIsOpen] = useState(depth < 1); // Auto-expand first level

  const getIcon = (type: string) => {
    switch (type) {
      case 'INTELLIGENCE_SERVICE': return <Shield size={16} className="text-red-500" />;
      case 'GOVERNMENT': return <Landmark size={16} className="text-brand-accent" />;
      default: return <Building2 size={16} className="text-slate-400" />;
    }
  };

  return (
    <div className="ml-4 border-l border-brand-border/50">
      <div 
        onClick={() => setIsOpen(!isOpen)}
        className="group flex items-center gap-3 py-2 px-4 cursor-pointer hover:bg-white/5 transition-colors relative"
      >
        {/* Connection Line Indicator */}
        <div className="absolute left-0 top-1/2 w-4 h-px bg-brand-border/50" />
        
        {node.children.length > 0 ? (
          isOpen ? <ChevronDown size={14} className="text-brand-accent" /> : <ChevronRight size={14} />
        ) : (
          <div className="w-[14px]" />
        )}

        {getIcon(node.type)}

        <div className="flex flex-col">
          <span className={`text-xs font-bold tracking-tight ${node.isStateOwned ? 'text-slate-200' : 'text-slate-400'}`}>
            {node.name.toUpperCase()}
          </span>
          <span className="text-[9px] font-mono text-slate-500 uppercase tracking-tighter">
            {node.level} // {node.type}
          </span>
        </div>

        {node.isStateOwned && (
          <span className="text-[8px] px-1.5 py-0.5 border border-brand-accent/30 text-brand-accent font-mono ml-auto">
            STATE_OWNED
          </span>
        )}
      </div>

      {isOpen && node.children.length > 0 && (
        <div className="animate-in slide-in-from-left-2 duration-300">
          {node.children.map((child) => (
            <InstitutionNode key={child.publicId} node={child} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

export default InstitutionNode;