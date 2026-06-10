import React, { useState } from 'react';
import { ChevronDown, ChevronRight, User, ShieldAlert, ShieldCheck, ArrowUpRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { OccupationTreeResponse } from '../../types';

interface Props {
  node: OccupationTreeResponse;
  depth: number;
}

const OccupationNode: React.FC<Props> = ({ node, depth }) => {
  // Inicializace na false
  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();
  const hasSubordinates = node.subordinates && node.subordinates.length > 0;

  return (
    <div className="font-mono">
      <div 
        className={`
          group flex items-center p-3 border-l-2 transition-all cursor-pointer
          ${node.isVacant ? 'border-red-500/50 bg-red-500/5' : 'border-brand-accent/30 bg-brand-panel/20'}
          hover:bg-brand-accent/10 mb-1
        `}
        style={{ marginLeft: `${depth * 24}px` }}
      >
        {/* Toggle - stopPropagation brání navigaci při kliknutí na šipku */}
        <div 
          onClick={(e) => { 
            e.stopPropagation(); 
            setIsOpen(!isOpen); 
          }}
          className="p-1 hover:bg-white/10 rounded mr-2"
        >
          {hasSubordinates ? (
            isOpen ? <ChevronDown size={14} className="text-brand-accent" /> : <ChevronRight size={14} />
          ) : (
            <div className="w-[14px]" />
          )}
        </div>

        <div className="mr-4">
          {node.isVacant ? (
            <ShieldAlert size={16} className="text-red-500 animate-pulse" />
          ) : (
            <ShieldCheck size={16} className="text-brand-accent" />
          )}
        </div>

        <div className="flex-1 flex items-center justify-between min-w-0" onClick={() => navigate(`/occupations/${node.publicId}`)}>
          <div className="truncate mr-4">
            <span className="text-[10px] text-slate-500 mr-2">[{node.code}]</span>
            <span className="text-sm font-bold text-white uppercase tracking-tighter group-hover:text-brand-accent transition-colors">
              {node.title}
            </span>
          </div>          

          <div className="flex items-center gap-6 flex-shrink-0">
            <div className="hidden md:flex items-center gap-2 px-3 py-1 bg-black/40 border border-white/5 rounded-sm">
              <User size={12} className={node.isVacant ? 'text-red-500/50' : 'text-brand-accent'} />
              <span className={`text-[10px] font-black uppercase tracking-widest ${node.isVacant ? 'text-red-500/40 italic' : 'text-slate-300'}`}>
                {node.isVacant ? 'UNOCCUPIED' : node.currentOccupantName}
              </span>
            </div>

            <div className="text-[9px] bg-brand-accent/10 border border-brand-accent/20 px-2 py-0.5 text-brand-accent font-black">
              RANK_{node.rank}
            </div>
            
            <ArrowUpRight size={14} className="text-slate-700 opacity-0 group-hover:opacity-100 transition-all" />
          </div>
        </div>
      </div>

      {/* KRITICKÁ OPRAVA: Děti se renderují pouze při isOpen === true */}
      {isOpen && hasSubordinates && (
        <div className="animate-in fade-in slide-in-from-top-1 duration-200">
          {node.subordinates.map((child) => (
            <OccupationNode key={child.publicId} node={child} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

export default OccupationNode;